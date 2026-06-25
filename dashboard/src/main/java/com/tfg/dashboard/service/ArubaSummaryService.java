package com.tfg.dashboard.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.ArubaApAnnotationDto;
import com.tfg.dashboard.dto.ArubaApAnnotationRequest;
import com.tfg.dashboard.dto.ArubaInactiveApDto;
import com.tfg.dashboard.dto.ArubaNetworkStatusDto;
import com.tfg.dashboard.dto.ArubaSwitchClientUsageDto;
import com.tfg.dashboard.model.AccessPoint;
import com.tfg.dashboard.model.ArubaApAnnotation;
import com.tfg.dashboard.model.ArubaDashboardMetrics;
import com.tfg.dashboard.dto.summary.ArubaSummary;
import com.tfg.dashboard.model.ArubaSwitch;
import com.tfg.dashboard.model.ArubaSwitchClientUsage;
import com.tfg.dashboard.model.ArubaSwitchInterfaceUsageHistory;
import com.tfg.dashboard.repository.AccessPointRepository;
import com.tfg.dashboard.repository.ArubaApAnnotationRepository;
import com.tfg.dashboard.repository.ArubaDashboardMetricsRepository;
import com.tfg.dashboard.repository.ArubaSwitchClientUsageRepository;
import com.tfg.dashboard.repository.ArubaSwitchInterfaceUsageHistoryRepository;
import com.tfg.dashboard.repository.ArubaSwitchRepository;

/**
 * Construye el resumen Aruba que consume el frontend.
 *
 * Lee entidades persistidas en MySQL, calcula contadores de APs, switches,
 * clientes WiFi y frescura, y delega el índice de salud Aruba en
 * ArubaNetworkStatusService.
 */
@Service
public class ArubaSummaryService {

        private static final Logger log = LoggerFactory.getLogger(ArubaSummaryService.class);
        private static final long METRICS_ID = 1L;
        private static final int MAX_ANNOTATION_LENGTH = 1000;
        private final AccessPointRepository accessPointRepository;
        private final ArubaApAnnotationRepository annotationRepository;
        private final ArubaSwitchRepository arubaSwitchRepository;
        private final ArubaSwitchClientUsageRepository switchClientUsageRepository;
        private final ArubaSwitchInterfaceUsageHistoryRepository switchInterfaceUsageHistoryRepository;
        private final ArubaDashboardMetricsRepository dashboardMetricsRepository;
        private final ArubaSwitchUsageService switchUsageService;
        private final ArubaNetworkStatusService networkStatusService;
        private final GlpiPlatformTicketService glpiPlatformTicketService;
        private final KpiProperties kpiProperties;

        public ArubaSummaryService(
                        AccessPointRepository accessPointRepository,
                        ArubaApAnnotationRepository annotationRepository,
                        ArubaSwitchRepository arubaSwitchRepository,
                        ArubaSwitchClientUsageRepository switchClientUsageRepository,
                        ArubaSwitchInterfaceUsageHistoryRepository switchInterfaceUsageHistoryRepository,
                        ArubaDashboardMetricsRepository dashboardMetricsRepository,
                        ArubaSwitchUsageService switchUsageService,
                        ArubaNetworkStatusService networkStatusService,
                        GlpiPlatformTicketService glpiPlatformTicketService,
                        KpiProperties kpiProperties) {

                this.accessPointRepository = accessPointRepository;
                this.annotationRepository = annotationRepository;
                this.arubaSwitchRepository = arubaSwitchRepository;
                this.switchClientUsageRepository = switchClientUsageRepository;
                this.switchInterfaceUsageHistoryRepository = switchInterfaceUsageHistoryRepository;
                this.dashboardMetricsRepository = dashboardMetricsRepository;
                this.switchUsageService = switchUsageService;
                this.networkStatusService = networkStatusService;
                this.glpiPlatformTicketService = glpiPlatformTicketService;
                this.kpiProperties = kpiProperties;
        }

        /**
         * Ensambla el DTO ArubaSummary a partir de datos ya sincronizados.
         */
        public ArubaSummary getSummary() {

                List<AccessPoint> aps = accessPointRepository.findAll();
                List<ArubaSwitch> switches = arubaSwitchRepository.findAll();
                ArubaDashboardMetrics metrics = dashboardMetricsRepository.findById(METRICS_ID)
                                .orElseGet(ArubaDashboardMetrics::new);

                int totalAps = aps.size();
                int upAps = (int) aps.stream()
                                .filter(ap -> ap.getStatus() != null && ap.getStatus().equalsIgnoreCase("Up"))
                                .count();
                int downAps = totalAps - upAps;
                int totalSites = (int) aps.stream().map(AccessPoint::getSite)
                                .filter(site -> site != null && !site.isBlank()).distinct().count();
                int totalSwarms = (int) aps.stream().map(AccessPoint::getSwarmName)
                                .filter(swarm -> swarm != null && !swarm.isBlank()).distinct().count();
                int apsWithoutPublicIp = (int) aps.stream()
                                .filter(ap -> ap.getPublicIpAddress() == null || ap.getPublicIpAddress().isBlank())
                                .count();
                int totalSwitches = switches.size();
                int downSwitches = (int) switches.stream().filter(switchInfo -> switchInfo.getDeviceStatus() == null
                                || !switchInfo.getDeviceStatus().equalsIgnoreCase("Up")).count();
                int switchesFirmwareUpgradeRequired = (int) switches.stream().filter(ArubaSwitch::isUpgradeRequired)
                                .count();
                List<ArubaSwitchClientUsage> underusedSwitches = switchUsageService.getUnderusedSwitches();
                int arubaOpenTickets = glpiPlatformTicketService.getArubaOpenTickets();

                // El umbral de APs inactivos define cuantos dias puede estar un AP
                // sin aparecer antes de considerarse no visto recientemente.
                // Por defecto son 30 dias, pero puede modificarse desde el panel
                // de configuración.
                int inactiveApDaysThreshold = kpiProperties.getAruba().getInactiveApDaysThreshold();
                LocalDateTime limitDate = LocalDateTime.now()
                                .minusDays(inactiveApDaysThreshold);
                long inactiveAps = accessPointRepository.countBySerialIsNotNullAndLastSeenAtBefore(limitDate);
                logInactiveApDiagnostics(inactiveApDaysThreshold, limitDate, inactiveAps);

                ArubaNetworkStatusDto networkStatusDetails = networkStatusService.buildNetworkStatusDetails(
                                totalAps,
                                downAps,
                                (int) inactiveAps,
                                metrics.getFirmwareOutdated(),
                                metrics.getTotalWifiClients(),
                                metrics.getMutualiaApsClients(),
                                metrics.getMutualiaWifiClients(),
                                totalSwitches,
                                downSwitches,
                                switchesFirmwareUpgradeRequired,
                                underusedSwitches.size(),
                                arubaOpenTickets);

                LocalDateTime lastUpdated = resolveArubaLastUpdated();
                String dataStatus = calculateDataStatus(lastUpdated);

                ArubaSummary summary = new ArubaSummary();

                summary.setTotalAps(totalAps);
                summary.setUpAps(upAps);
                summary.setDownAps(downAps);
                summary.setTotalSites(totalSites);
                summary.setTotalSwarms(totalSwarms);
                summary.setFirmwareOutdated(metrics.getFirmwareOutdated());
                summary.setApsWithoutPublicIp(apsWithoutPublicIp);
                summary.setInactiveAps((int) inactiveAps);
                summary.setNetworkStatusDetails(networkStatusDetails);
                summary.setNetworkStatusKpi(networkStatusService.buildNetworkStatusKpi(networkStatusDetails, lastUpdated, dataStatus));
                summary.setKpiStatuses(networkStatusDetails.getIndicatorStatuses());
                summary.setTotalSwitches(totalSwitches);
                summary.setDownSwitches(downSwitches);
                summary.setSwitchesFirmwareUpgradeRequired(switchesFirmwareUpgradeRequired);
                summary.setUnderusedSwitches(underusedSwitches.stream()
                                .map(ArubaSwitchClientUsageDto::new)
                                .toList());
                summary.setTotalWifiClients(metrics.getTotalWifiClients());
                summary.setArubaOpenTickets(arubaOpenTickets);
                summary.setMutualiaApsClients(metrics.getMutualiaApsClients());
                summary.setMutualiaWifiClients(metrics.getMutualiaWifiClients());
                summary.setMutualiaLangileakClients(metrics.getMutualiaLangileakClients());
                summary.setMutualiaClients(metrics.getMutualiaClients());
                summary.setMutualiaRedInternaClients(metrics.getMutualiaRedInternaClients());
                summary.setMutualiaRedExternaClients(metrics.getMutualiaRedExternaClients());
                summary.setMutualiaKorporatiboaClients(metrics.getMutualiaKorporatiboaClients());
                summary.setWifiPacsClients(metrics.getWifiPacsClients());
                summary.setMutVideoClients(metrics.getMutVideoClients());
                summary.setLastUpdated(lastUpdated);
                summary.setDataStatus(dataStatus);

                return summary;
        }

        private void logInactiveApDiagnostics(
                        int inactiveApDaysThreshold,
                        LocalDateTime limitDate,
                        long inactiveAps) {

                LocalDateTime oldestLastSeenAt = accessPointRepository
                                .findTopByLastSeenAtIsNotNullOrderByLastSeenAtAsc()
                                .map(AccessPoint::getLastSeenAt)
                                .orElse(null);
                LocalDateTime newestLastSeenAt = accessPointRepository
                                .findTopByLastSeenAtIsNotNullOrderByLastSeenAtDesc()
                                .map(AccessPoint::getLastSeenAt)
                                .orElse(null);
                Long apsWithoutLastSeenAt = accessPointRepository.countBySerialIsNotNullAndLastSeenAtIsNull();

                log.debug(
                                "APs inactivos Aruba: umbral={} dias, fechaLimite={}, inactivos={}, oldestLastSeenAt={}, newestLastSeenAt={}, apsSinLastSeenAt={}",
                                inactiveApDaysThreshold,
                                limitDate,
                                inactiveAps,
                                oldestLastSeenAt,
                                newestLastSeenAt,
                                apsWithoutLastSeenAt == null ? 0 : apsWithoutLastSeenAt);
        }

        /**
         * Devuelve solo el bloque normalizado del índice de salud Aruba.
         */
        public ArubaNetworkStatusDto getNetworkStatus() {

                return getSummary().getNetworkStatusDetails();
        }

        /**
         * Devuelve los APs que explican el contador de "APs inactivos".
         *
         * El criterio usa AccessPoint.lastSeenAt, que representa la ultima fecha
         * real conocida de visualizacion/contacto en Aruba. No se usa collectedAt
         * porque collectedAt solo indica cuando la aplicacion guardo el snapshot.
         * daysInactive facilita interpretar cuanto tiempo lleva cada AP sin verse.
         */
        public List<ArubaInactiveApDto> getInactiveAps() {

                int inactiveApDaysThreshold = kpiProperties.getAruba().getInactiveApDaysThreshold();
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime limitDate = now.minusDays(inactiveApDaysThreshold);
                List<AccessPoint> inactiveAps = accessPointRepository
                                .findBySerialIsNotNullAndLastSeenAtBeforeOrderByLastSeenAtAsc(limitDate);

                if (inactiveAps.isEmpty()) {
                        return List.of();
                }

                Map<String, ArubaApAnnotation> annotationsBySerial = annotationRepository
                                .findBySerialIn(inactiveAps.stream()
                                                .map(AccessPoint::getSerial)
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(
                                                ArubaApAnnotation::getSerial,
                                                Function.identity(),
                                                (first, ignored) -> first));

                return inactiveAps
                                .stream()
                                .map(ap -> toInactiveApDto(ap, now, annotationsBySerial.get(ap.getSerial())))
                                .toList();
        }

        /**
         * Guarda una anotacion manual asociada a un AP por numero de serie.
         *
         * La anotacion no procede de Aruba y no se sobrescribe durante la
         * sincronizacion. Se permite texto vacio para que el usuario pueda limpiar
         * una nota existente.
         */
        public ArubaApAnnotationDto saveInactiveApAnnotation(
                        String serial,
                        ArubaApAnnotationRequest request) {

                if (serial == null || serial.isBlank()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El serial del AP es obligatorio.");
                }

                String annotation = request == null || request.getAnnotation() == null
                                ? ""
                                : request.getAnnotation();

                if (annotation.length() > MAX_ANNOTATION_LENGTH) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "La anotacion no puede superar 1000 caracteres.");
                }

                ArubaApAnnotation entity = annotationRepository
                                .findBySerial(serial)
                                .orElseGet(ArubaApAnnotation::new);
                entity.setSerial(serial);
                entity.setAnnotation(annotation);
                entity.setUpdatedAt(LocalDateTime.now());

                ArubaApAnnotation saved = annotationRepository.save(entity);
                return toAnnotationDto(saved);
        }

        private ArubaInactiveApDto toInactiveApDto(
                        AccessPoint ap,
                        LocalDateTime now,
                        ArubaApAnnotation annotation) {

                LocalDateTime lastSeenAt = ap.getLastSeenAt();
                long daysInactive = lastSeenAt == null
                                ? 0
                                : ChronoUnit.DAYS.between(lastSeenAt, now);

                return new ArubaInactiveApDto(
                                ap.getSerial(),
                                ap.getName(),
                                ap.getStatus(),
                                ap.getSite(),
                                ap.getSwarmName(),
                                lastSeenAt,
                                daysInactive,
                                annotation == null ? "" : annotation.getAnnotation());
        }

        private ArubaApAnnotationDto toAnnotationDto(ArubaApAnnotation annotation) {

                return new ArubaApAnnotationDto(
                                annotation.getSerial(),
                                annotation.getAnnotation(),
                                annotation.getUpdatedAt());
        }

        /**
         * Busca la fecha más reciente entre métricas agregadas, APs, switches e
         * históricos de uso para calcular frescura Aruba.
         */
        private LocalDateTime resolveArubaLastUpdated() {

                // Se prioriza la tabla agregada del dashboard Aruba porque resume firmware y clientes WiFi.
                // Si aun no existe, se usa la fecha mas reciente de los datos Aruba persistidos.

                LocalDateTime latest = dashboardMetricsRepository
                                .findById(METRICS_ID)
                                .map(ArubaDashboardMetrics::getUpdatedAt)
                                .orElse(null);

                latest = newer(
                                latest,
                                accessPointRepository
                                                .findTopByLastSeenAtIsNotNullOrderByLastSeenAtDesc()
                                                .map(AccessPoint::getLastSeenAt)
                                                .orElse(null));

                latest = newer(
                                latest,
                                arubaSwitchRepository
                                                .findTopByLastSeenAtIsNotNullOrderByLastSeenAtDesc()
                                                .map(ArubaSwitch::getLastSeenAt)
                                                .orElse(null));

                latest = newer(
                                latest,
                                switchClientUsageRepository
                                                .findTopByUpdatedAtIsNotNullOrderByUpdatedAtDesc()
                                                .map(ArubaSwitchClientUsage::getUpdatedAt)
                                                .orElse(null));

                latest = newer(
                                latest,
                                switchInterfaceUsageHistoryRepository
                                                .findTopByObservedAtIsNotNullOrderByObservedAtDesc()
                                                .map(ArubaSwitchInterfaceUsageHistory::getObservedAt)
                                                .orElse(null));

                return latest;
        }

        /**
         * Aruba tiene una ventana de frescura propia porque depende de APIs
         * reales y no del scheduler de datos simulados.
         */
        private String calculateDataStatus(LocalDateTime lastUpdated) {

                // Aruba se sincroniza normalmente cada hora. La ventana de frescura
                // usa 70 minutos para cubrir la cadencia horaria con un pequeño margen.

                if (lastUpdated == null) {

                        return "NO_DATA";
                }

                if (lastUpdated.isBefore(LocalDateTime.now().minusMinutes(kpiProperties.getAruba().getFreshnessMinutes()))) {

                        return "STALE";
                }

                return "OK";
        }

        private LocalDateTime newer(LocalDateTime current,LocalDateTime candidate) {

                if (candidate == null) {

                        return current;
                }

                if (current == null || candidate.isAfter(current)) {

                        return candidate;
                }

                return current;
        }
}

