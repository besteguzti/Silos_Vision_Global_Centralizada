package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.client.ArubaApiClient;
import com.tfg.dashboard.dto.ArubaApInfo;
import com.tfg.dashboard.dto.ArubaApAnnotationDto;
import com.tfg.dashboard.dto.ArubaApAnnotationRequest;
import com.tfg.dashboard.dto.ArubaInactiveApDto;
import com.tfg.dashboard.dto.ArubaSwitchInfo;
import com.tfg.dashboard.dto.ArubaWifiClientInfo;
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
import com.tfg.dashboard.repository.ArubaNetworkStatusHistoryRepository;
import com.tfg.dashboard.repository.ArubaSwitchClientUsageRepository;
import com.tfg.dashboard.repository.ArubaSwitchInterfaceUsageHistoryRepository;
import com.tfg.dashboard.repository.ArubaSwitchRepository;
import com.tfg.dashboard.repository.TransversalKpiHistoryRepository;

@ExtendWith(MockitoExtension.class)
class ArubaServiceTest {

    @Mock
    private ArubaApiClient client;

    @Mock
    private AccessPointRepository accessPointRepository;

    @Mock
    private ArubaApAnnotationRepository annotationRepository;

    @Mock
    private ArubaSwitchRepository arubaSwitchRepository;

    @Mock
    private ArubaSwitchClientUsageRepository switchClientUsageRepository;

    @Mock
    private ArubaSwitchInterfaceUsageHistoryRepository
            switchInterfaceUsageHistoryRepository;

    @Mock
    private ArubaDashboardMetricsRepository dashboardMetricsRepository;

    @Mock
    private ArubaNetworkStatusHistoryRepository
            networkStatusHistoryRepository;

    @Mock
    private TransversalKpiHistoryRepository
            transversalKpiHistoryRepository;

    @Mock
    private GlpiPlatformTicketService glpiPlatformTicketService;

    private ArubaService arubaService;
    private KpiProperties kpiProperties;

    @BeforeEach
    void setUp() {

        kpiProperties =
                new KpiProperties();

        ArubaWifiClientAggregationService wifiClientAggregationService =
                new ArubaWifiClientAggregationService();

        ArubaSwitchUsageService switchUsageService =
                new ArubaSwitchUsageService(
                        client,
                        switchClientUsageRepository,
                        switchInterfaceUsageHistoryRepository,
                        kpiProperties);

        ArubaNetworkStatusService networkStatusService =
                new ArubaNetworkStatusService(
                        networkStatusHistoryRepository,
                        transversalKpiHistoryRepository,
                        kpiProperties);

        ArubaInventorySyncService inventorySyncService =
                new ArubaInventorySyncService(
                        client,
                        accessPointRepository,
                        arubaSwitchRepository,
                        dashboardMetricsRepository,
                        wifiClientAggregationService,
                        switchUsageService);

        ArubaSummaryService summaryService =
                new ArubaSummaryService(
                        accessPointRepository,
                        annotationRepository,
                        arubaSwitchRepository,
                        switchClientUsageRepository,
                        switchInterfaceUsageHistoryRepository,
                        dashboardMetricsRepository,
                        switchUsageService,
                        networkStatusService,
                        glpiPlatformTicketService,
                        kpiProperties);

        arubaService =
                new ArubaService(
                        inventorySyncService,
                        wifiClientAggregationService,
                        switchUsageService,
                        networkStatusService,
                        summaryService);
    }

    @Test
    void getSummaryCalculatesArubaKpis() throws Exception {

        kpiProperties.getAruba().setInactiveApDaysThreshold(60);

        when(accessPointRepository.findAll()).thenReturn(List.of(
                storedAp("AP-1", "Up", "SER-1", "Site A", "Swarm A", "1.1.1.1"),
                storedAp("AP-2", "Down", "SER-2", "Site A", "Swarm A", ""),
                storedAp("AP-3", "Up", "SER-3", "Site B", "Swarm B", null)
        ));

        when(arubaSwitchRepository.findAll()).thenReturn(List.of(
                storedSwitch("SW-1", "Up", false),
                storedSwitch("SW-2", "Down", true)
        ));

        when(dashboardMetricsRepository.findById(1L))
                .thenReturn(Optional.of(metrics()));

        when(switchClientUsageRepository
                .findByAssociatedDeviceInOrderByDownInterfacesDescAssociatedDeviceAsc(
                        List.of("SW-1")))
                .thenReturn(List.of(switchUsage("SW-1", "Switch bajo", 18)));

        when(switchInterfaceUsageHistoryRepository
                .findDevicesAlwaysOverDownInterfaceLimitSince(
                        eq("Up"),
                        eq(17),
                        any()))
                .thenReturn(List.of("SW-1"));

        when(accessPointRepository
                .countBySerialIsNotNullAndLastSeenAtBefore(any()))
                .thenReturn(4L);
        when(glpiPlatformTicketService.getArubaOpenTickets())
                .thenReturn(35);

        ArubaSummary summary =
                arubaService.getSummary();

        assertThat(summary.getTotalAps()).isEqualTo(3);
        assertThat(summary.getUpAps()).isEqualTo(2);
        assertThat(summary.getDownAps()).isEqualTo(1);
        assertThat(summary.getTotalSites()).isEqualTo(2);
        assertThat(summary.getTotalSwarms()).isEqualTo(2);
        assertThat(summary.getFirmwareOutdated()).isEqualTo(1);
        assertThat(summary.getApsWithoutPublicIp()).isEqualTo(2);
        assertThat(summary.getInactiveAps()).isEqualTo(4);
        assertThat(summary.getTotalSwitches()).isEqualTo(2);
        assertThat(summary.getDownSwitches()).isEqualTo(1);
        assertThat(summary.getSwitchesFirmwareUpgradeRequired()).isEqualTo(1);
        assertThat(summary.getUnderusedSwitches()).hasSize(1);
        assertThat(summary.getUnderusedSwitches().get(0).getDownInterfaces())
                .isEqualTo(18);
        assertThat(summary.getTotalWifiClients()).isEqualTo(6);
        assertThat(summary.getArubaOpenTickets()).isEqualTo(35);
        assertThat(summary.getMutualiaApsClients()).isEqualTo(1);
        assertThat(summary.getMutualiaWifiClients()).isEqualTo(5);
        assertThat(summary.getMutualiaClients()).isEqualTo(2);
        assertThat(summary.getMutualiaRedInternaClients()).isEqualTo(1);
        assertThat(summary.getWifiPacsClients()).isEqualTo(1);
        assertThat(summary.getMutVideoClients()).isEqualTo(1);
        assertThat(summary.getNetworkStatusDetails().getColor()).isEqualTo("GREEN");
        assertThat(summary.getNetworkStatusDetails().getPercentage())
                .isEqualTo(14);
        assertThat(summary.getNetworkStatusDetails()
                .getAccessPointStatus().getColor())
                .isEqualTo("YELLOW");
        assertThat(summary.getNetworkStatusDetails()
                .getSwitchStatus().getColor())
                .isEqualTo("YELLOW");
        assertThat(summary.getKpiStatuses())
                .containsEntry("downAps", "GREEN")
                .containsEntry("firmwareOutdated", "YELLOW")
                .containsEntry("switchesFirmwareUpgradeRequired", "YELLOW")
                .containsEntry("totalWifiClients", "NEUTRAL")
                .containsEntry("totalAps", "NEUTRAL");
        assertThat(summary.getLastUpdated()).isNotNull();
        assertThat(summary.getDataStatus()).isEqualTo("OK");

        ArgumentCaptor<LocalDateTime> inactiveLimitCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(accessPointRepository)
                .countBySerialIsNotNullAndLastSeenAtBefore(inactiveLimitCaptor.capture());
        assertThat(inactiveLimitCaptor.getValue())
                .isAfter(LocalDateTime.now().minusDays(61))
                .isBefore(LocalDateTime.now().minusDays(59));
    }

    @Test
    void getInactiveApsReturnsDetailsUsingConfiguredThreshold() {

        kpiProperties.getAruba().setInactiveApDaysThreshold(2);
        AccessPoint inactive =
                storedAp("AP viejo", "Down", "SER-OLD", "Site A", "Swarm A", "1.1.1.1");
        inactive.setLastSeenAt(LocalDateTime.now().minusDays(5).minusHours(2));
        ArubaApAnnotation annotation =
                annotation("SER-OLD", "Pendiente revisar alimentacion");

        when(accessPointRepository
                .findBySerialIsNotNullAndLastSeenAtBeforeOrderByLastSeenAtAsc(any()))
                .thenReturn(List.of(inactive));
        when(annotationRepository.findBySerialIn(List.of("SER-OLD")))
                .thenReturn(List.of(annotation));

        List<ArubaInactiveApDto> inactiveAps =
                arubaService.getInactiveAps();

        assertThat(inactiveAps).hasSize(1);
        assertThat(inactiveAps.get(0).getSerial()).isEqualTo("SER-OLD");
        assertThat(inactiveAps.get(0).getName()).isEqualTo("AP viejo");
        assertThat(inactiveAps.get(0).getStatus()).isEqualTo("Down");
        assertThat(inactiveAps.get(0).getSite()).isEqualTo("Site A");
        assertThat(inactiveAps.get(0).getSwarmName()).isEqualTo("Swarm A");
        assertThat(inactiveAps.get(0).getDaysInactive()).isGreaterThanOrEqualTo(5);
        assertThat(inactiveAps.get(0).getAnnotation()).isEqualTo("Pendiente revisar alimentacion");

        ArgumentCaptor<LocalDateTime> inactiveLimitCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(accessPointRepository)
                .findBySerialIsNotNullAndLastSeenAtBeforeOrderByLastSeenAtAsc(inactiveLimitCaptor.capture());
        assertThat(inactiveLimitCaptor.getValue())
                .isAfter(LocalDateTime.now().minusDays(3))
                .isBefore(LocalDateTime.now().minusDays(1));
    }

    @Test
    void arubaSnapshotNewerThanSeventyMinutesIsFresh() {

        mockMinimalSummaryData(LocalDateTime.now().minusMinutes(69));

        ArubaSummary summary =
                arubaService.getSummary();

        assertThat(summary.getDataStatus()).isEqualTo("OK");
    }

    @Test
    void arubaSnapshotOlderThanSeventyMinutesIsStale() {

        mockMinimalSummaryData(LocalDateTime.now().minusMinutes(71));

        ArubaSummary summary =
                arubaService.getSummary();

        assertThat(summary.getDataStatus()).isEqualTo("STALE");
    }

    @Test
    void saveInactiveApAnnotationCreatesManualAnnotation() {

        ArubaApAnnotationRequest request =
                new ArubaApAnnotationRequest();
        request.setAnnotation("Revisar alimentacion");

        when(annotationRepository.findBySerial("SER-1"))
                .thenReturn(Optional.empty());
        when(annotationRepository.save(any(ArubaApAnnotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArubaApAnnotationDto saved =
                arubaService.saveInactiveApAnnotation("SER-1", request);

        assertThat(saved.getSerial()).isEqualTo("SER-1");
        assertThat(saved.getAnnotation()).isEqualTo("Revisar alimentacion");
        assertThat(saved.getUpdatedAt()).isNotNull();

        ArgumentCaptor<ArubaApAnnotation> captor =
                ArgumentCaptor.forClass(ArubaApAnnotation.class);
        verify(annotationRepository).save(captor.capture());
        assertThat(captor.getValue().getSerial()).isEqualTo("SER-1");
        assertThat(captor.getValue().getAnnotation()).isEqualTo("Revisar alimentacion");
    }

    @Test
    void saveInactiveApAnnotationAllowsEmptyTextToClearNote() {

        ArubaApAnnotation existing =
                annotation("SER-1", "Nota anterior");
        ArubaApAnnotationRequest request =
                new ArubaApAnnotationRequest();
        request.setAnnotation("");

        when(annotationRepository.findBySerial("SER-1"))
                .thenReturn(Optional.of(existing));
        when(annotationRepository.save(any(ArubaApAnnotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ArubaApAnnotationDto saved =
                arubaService.saveInactiveApAnnotation("SER-1", request);

        assertThat(saved.getAnnotation()).isEmpty();
    }

    @Test
    void saveInactiveApAnnotationRejectsLongText() {

        ArubaApAnnotationRequest request =
                new ArubaApAnnotationRequest();
        request.setAnnotation("x".repeat(1001));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> arubaService.saveInactiveApAnnotation("SER-1", request))
                .hasMessageContaining("1000 caracteres");
    }

    @Test
    void syncAccessPointsCreatesNewApWithFirstSeenAt() {

        when(client.getApsList()).thenReturn(List.of(
                ap("AP-1", "Up", "SER-1", "Site A", "Swarm A", "1.1.1.1")
        ));

        when(accessPointRepository.findBySerial("SER-1"))
                .thenReturn(Optional.empty());

        arubaService.syncAccessPoints();

        ArgumentCaptor<AccessPoint> captor =
                ArgumentCaptor.forClass(AccessPoint.class);

        verify(accessPointRepository).save(captor.capture());

        AccessPoint saved =
                captor.getValue();

        assertThat(saved.getSerial()).isEqualTo("SER-1");
        assertThat(saved.getFirstSeenAt()).isNotNull();
        assertThat(saved.getLastSeenAt()).isNotNull();
    }

    @Test
    void syncAccessPointsKeepsExistingFirstSeenAt() {

        LocalDateTime originalFirstSeenAt =
                LocalDateTime.now().minusDays(10);

        AccessPoint existing =
                new AccessPoint();

        existing.setSerial("SER-1");
        existing.setFirstSeenAt(originalFirstSeenAt);

        when(client.getApsList()).thenReturn(List.of(
                ap("AP-1-renamed", "Up", "SER-1", "Site B", "Swarm B", "2.2.2.2")
        ));

        when(accessPointRepository.findBySerial("SER-1"))
                .thenReturn(Optional.of(existing));

        arubaService.syncAccessPoints();

        ArgumentCaptor<AccessPoint> captor =
                ArgumentCaptor.forClass(AccessPoint.class);

        verify(accessPointRepository).save(captor.capture());

        AccessPoint saved =
                captor.getValue();

        assertThat(saved.getFirstSeenAt()).isEqualTo(originalFirstSeenAt);
        assertThat(saved.getName()).isEqualTo("AP-1-renamed");
        assertThat(saved.getIpAddress()).isEqualTo("2.2.2.2");
        assertThat(saved.getLastSeenAt()).isAfter(originalFirstSeenAt);
    }

    @Test
    void syncAccessPointsUsesRealArubaLastSeenWhenAvailable() {

        LocalDateTime realLastSeenAt =
                LocalDateTime.now().minusDays(5);
        ArubaApInfo ap =
                ap("AP-1", "Down", "SER-1", "Site A", "Swarm A", "1.1.1.1");
        ap.setLastSeenAt(realLastSeenAt);

        when(client.getApsList()).thenReturn(List.of(ap));
        when(accessPointRepository.findBySerial("SER-1"))
                .thenReturn(Optional.empty());

        arubaService.syncAccessPoints();

        ArgumentCaptor<AccessPoint> captor =
                ArgumentCaptor.forClass(AccessPoint.class);
        verify(accessPointRepository).save(captor.capture());

        assertThat(captor.getValue().getLastSeenAt()).isEqualTo(realLastSeenAt);
    }

    @Test
    void syncAccessPointsDoesNotRefreshDownApWithoutRealLastSeen() {

        LocalDateTime previousLastSeenAt =
                LocalDateTime.now().minusDays(10);

        AccessPoint existing =
                new AccessPoint();
        existing.setSerial("SER-1");
        existing.setFirstSeenAt(LocalDateTime.now().minusDays(20));
        existing.setLastSeenAt(previousLastSeenAt);

        when(client.getApsList()).thenReturn(List.of(
                ap("AP-1", "Down", "SER-1", "Site A", "Swarm A", "1.1.1.1")
        ));
        when(accessPointRepository.findBySerial("SER-1"))
                .thenReturn(Optional.of(existing));

        arubaService.syncAccessPoints();

        ArgumentCaptor<AccessPoint> captor =
                ArgumentCaptor.forClass(AccessPoint.class);
        verify(accessPointRepository).save(captor.capture());

        assertThat(captor.getValue().getLastSeenAt()).isEqualTo(previousLastSeenAt);
    }

    @Test
    void syncSwitchesKeepsExistingFirstSeenAt() {

        LocalDateTime originalFirstSeenAt =
                LocalDateTime.now().minusDays(20);

        ArubaSwitch existing =
                new ArubaSwitch();

        existing.setSerial("SW-1");
        existing.setFirstSeenAt(originalFirstSeenAt);

        when(client.getMonitoringSwitchesList()).thenReturn(List.of(
                arubaSwitch("SW-1", "Down", true)
        ));

        when(arubaSwitchRepository.findBySerial("SW-1"))
                .thenReturn(Optional.of(existing));

        arubaService.syncSwitches();

        ArgumentCaptor<ArubaSwitch> captor =
                ArgumentCaptor.forClass(ArubaSwitch.class);

        verify(arubaSwitchRepository).save(captor.capture());

        ArubaSwitch saved =
                captor.getValue();

        assertThat(saved.getFirstSeenAt()).isEqualTo(originalFirstSeenAt);
        assertThat(saved.getSerial()).isEqualTo("SW-1");
        assertThat(saved.getDeviceStatus()).isEqualTo("Down");
        assertThat(saved.isUpgradeRequired()).isTrue();
        assertThat(saved.getLastSeenAt()).isAfter(originalFirstSeenAt);
    }

    @Test
    void syncSwitchClientUsageCountsDownInterfacesBySwitchSerial() {

        when(client.getMonitoringSwitchesList()).thenReturn(List.of(
                arubaSwitch("SW-1", "Up", false),
                arubaSwitch("SW-2", "Up", false)
        ));

        when(switchClientUsageRepository.findAll())
                .thenReturn(List.of());

        when(switchClientUsageRepository.findByAssociatedDevice(any()))
                .thenReturn(Optional.empty());

        when(client.countSwitchPortsDown("SW-1")).thenReturn(4);
        when(client.countSwitchPortsDown("SW-2")).thenReturn(9);

        arubaService.syncSwitchClientUsage();

        ArgumentCaptor<ArubaSwitchClientUsage> captor =
                ArgumentCaptor.forClass(ArubaSwitchClientUsage.class);

        verify(switchClientUsageRepository, times(2))
                .save(captor.capture());

        ArgumentCaptor<ArubaSwitchInterfaceUsageHistory> historyCaptor =
                ArgumentCaptor.forClass(
                        ArubaSwitchInterfaceUsageHistory.class);

        verify(switchInterfaceUsageHistoryRepository, times(2))
                .save(historyCaptor.capture());

        List<ArubaSwitchClientUsage> saved =
                captor.getAllValues();

        assertThat(saved)
                .extracting(
                        ArubaSwitchClientUsage::getAssociatedDevice,
                        ArubaSwitchClientUsage::getDeviceStatus,
                        ArubaSwitchClientUsage::getDownInterfaces
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("SW-1", "Up", 4),
                        org.assertj.core.api.Assertions.tuple("SW-2", "Up", 9)
                );

        assertThat(historyCaptor.getAllValues())
                .extracting(
                        ArubaSwitchInterfaceUsageHistory::getAssociatedDevice,
                        ArubaSwitchInterfaceUsageHistory::getDownInterfaces
                )
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("SW-1", 4),
                        org.assertj.core.api.Assertions.tuple("SW-2", 9)
                );
    }

    private ArubaApInfo ap(
            String name,
            String status,
            String serial,
            String site,
            String swarm,
            String publicIp
    ) {

        ArubaApInfo ap =
                new ArubaApInfo();

        ap.setName(name);
        ap.setStatus(status);
        ap.setSerial(serial);
        ap.setSite(site);
        ap.setSwarmName(swarm);
        ap.setIpAddress(publicIp);
        ap.setPublicIpAddress(publicIp);
        ap.setFirmwareVersion("8.13.0");
        ap.setMacaddr("00:11:22:33:44:55");

        return ap;
    }

    private AccessPoint storedAp(
            String name,
            String status,
            String serial,
            String site,
            String swarm,
            String publicIp
    ) {

        AccessPoint ap =
                new AccessPoint();

        ap.setName(name);
        ap.setStatus(status);
        ap.setSerial(serial);
        ap.setSite(site);
        ap.setSwarmName(swarm);
        ap.setIpAddress(publicIp);
        ap.setPublicIpAddress(publicIp);
        ap.setFirmwareVersion("8.13.0");
        ap.setMacaddr("00:11:22:33:44:55");

        return ap;
    }

    private ArubaSwitch storedSwitch(
            String serial,
            String deviceStatus,
            boolean upgradeRequired
    ) {

        ArubaSwitch switchInfo =
                new ArubaSwitch();

        switchInfo.setSerial(serial);
        switchInfo.setMacAddress("00:aa:bb:cc:dd:ee");
        switchInfo.setHostname("switch-" + serial);
        switchInfo.setModel("Aruba 6300");
        switchInfo.setDeviceStatus(deviceStatus);
        switchInfo.setUpgradeRequired(upgradeRequired);
        switchInfo.setStatusState(
                upgradeRequired ? "UPGRADE_REQUIRED" : "UP_TO_DATE");

        return switchInfo;
    }

    private ArubaApAnnotation annotation(String serial,String text) {

        ArubaApAnnotation annotation =
                new ArubaApAnnotation();

        annotation.setSerial(serial);
        annotation.setAnnotation(text);
        annotation.setUpdatedAt(LocalDateTime.now());

        return annotation;
    }

    private ArubaDashboardMetrics metrics() {

        return metrics(LocalDateTime.now());
    }

    private ArubaDashboardMetrics metrics(LocalDateTime updatedAt) {

        ArubaDashboardMetrics metrics =
                new ArubaDashboardMetrics();

        metrics.setId(1L);
        metrics.setFirmwareOutdated(1);
        metrics.setTotalWifiClients(6);
        metrics.setMutualiaApsClients(1);
        metrics.setMutualiaWifiClients(5);
        metrics.setMutualiaClients(2);
        metrics.setMutualiaRedInternaClients(1);
        metrics.setWifiPacsClients(1);
        metrics.setMutVideoClients(1);
        metrics.setUpdatedAt(updatedAt);

        return metrics;
    }

    private void mockMinimalSummaryData(LocalDateTime updatedAt) {
        when(accessPointRepository.findAll()).thenReturn(List.of());
        when(arubaSwitchRepository.findAll()).thenReturn(List.of());
        when(dashboardMetricsRepository.findById(1L))
                .thenReturn(Optional.of(metrics(updatedAt)));
        when(switchInterfaceUsageHistoryRepository
                .findDevicesAlwaysOverDownInterfaceLimitSince(anyString(), anyInt(), any()))
                .thenReturn(List.of());
        when(accessPointRepository
                .countBySerialIsNotNullAndLastSeenAtBefore(any()))
                .thenReturn(0L);
        when(glpiPlatformTicketService.getArubaOpenTickets())
                .thenReturn(0);
    }

    private ArubaSwitchInfo arubaSwitch(
            String serial,
            String deviceStatus,
            boolean upgradeRequired
    ) {

        ArubaSwitchInfo switchInfo =
                new ArubaSwitchInfo();

        switchInfo.setSerial(serial);
        switchInfo.setMacAddress("00:aa:bb:cc:dd:ee");
        switchInfo.setHostname("switch-" + serial);
        switchInfo.setModel("Aruba 6300");
        switchInfo.setDeviceStatus(deviceStatus);
        switchInfo.setUpgradeRequired(upgradeRequired);
        switchInfo.setStatusState(upgradeRequired ? "UPGRADE_REQUIRED" : "UP_TO_DATE");

        return switchInfo;
    }

    private ArubaWifiClientInfo wifiClient(
            String groupName,
            String network
    ) {

        ArubaWifiClientInfo client =
                new ArubaWifiClientInfo();

        client.setAssociatedDevice("ap-serial");
        client.setAssociatedDeviceMac("00:11:22:33:44:55");
        client.setAssociatedDeviceName("AP-1");
        client.setGroupName(groupName);
        client.setHostname("client-host");
        client.setIpAddress("192.168.1.20");
        client.setLastConnectionTime(123456789L);
        client.setMacaddr("aa:bb:cc:dd:ee:ff");
        client.setNetwork(network);
        client.setOsType("Windows");

        return client;
    }

    private ArubaSwitchClientUsage switchUsage(
            String associatedDevice,
            String associatedDeviceName,
            int downInterfaces
    ) {

        ArubaSwitchClientUsage usage =
                new ArubaSwitchClientUsage();

        usage.setAssociatedDevice(associatedDevice);
        usage.setAssociatedDeviceName(associatedDeviceName);
        usage.setAssociatedDeviceMac("00:aa:bb:cc:dd:01");
        usage.setDeviceStatus("Up");
        usage.setDownInterfaces(downInterfaces);

        return usage;
    }
}
