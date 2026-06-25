package com.tfg.dashboard.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.PlatformWeightsConfigurationDto;
import com.tfg.dashboard.dto.ThresholdConfigurationDto;
import com.tfg.dashboard.dto.ThresholdSectionDto;
import com.tfg.dashboard.dto.ThresholdValueDto;
import com.tfg.dashboard.model.KpiThresholdConfiguration;
import com.tfg.dashboard.model.PlatformWeightConfiguration;
import com.tfg.dashboard.repository.KpiThresholdConfigurationRepository;
import com.tfg.dashboard.repository.PlatformWeightConfigurationRepository;

/**
 * Gestiona la configuración editable de pesos y umbrales KPI.
 *
 * La prioridad funcional es: valores persistidos en MySQL, valores por defecto
 * de KpiProperties y, solo como último recurso, los defaults internos del
 * servicio. El arranque de la aplicación solo crea claves inexistentes; no debe
 * sobrescribir cambios realizados por el usuario desde el panel de
 * configuración.
 */
@Service
public class KpiConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KpiConfigurationService.class);

    private static final String ARUBA = "ARUBA";
    private static final String CITRIX = "CITRIX";
    private static final String MICROSOFT365 = "MICROSOFT365";
    private static final String GLPI = "GLPI";

    private final KpiThresholdConfigurationRepository thresholdRepository;
    private final PlatformWeightConfigurationRepository weightRepository;
    private final KpiProperties kpiProperties;
    private final List<ThresholdSpec> thresholdSpecs;
    private final Map<String, SectionSpec> sectionSpecs;

    public KpiConfigurationService(
            KpiThresholdConfigurationRepository thresholdRepository,
            PlatformWeightConfigurationRepository weightRepository,
            KpiProperties kpiProperties) {

        this.thresholdRepository = thresholdRepository;
        this.weightRepository = weightRepository;
        this.kpiProperties = kpiProperties;
        this.sectionSpecs = buildSections();
        this.thresholdSpecs = buildThresholdSpecs();
    }

    @Transactional
    public void ensureDefaultConfigurationExists() {
        ensureValidThresholdConfigurationExists();
        ensureValidPlatformWeightsExist();
        applyThresholdValues(validatedCurrentThresholdValues());
        applyPlatformWeights(validatedCurrentPlatformWeights());
    }

    @Transactional
    public ThresholdConfigurationDto getThresholds() {
        ensureDefaultConfigurationExists();
        applyThresholdValues(validatedCurrentThresholdValues());
        return buildThresholdResponse();
    }

    @Transactional
    public ThresholdConfigurationDto updateThresholds(ThresholdConfigurationDto request) {
        ensureDefaultConfigurationExists();

        Map<String, Integer> requestedValues = extractRequestedThresholds(request);
        Map<String, Integer> values = validatedCurrentThresholdValues();
        values.putAll(requestedValues);
        validateThresholdValues(values);

        Map<String, ThresholdSpec> specsByKey = specsByKey();
        for (String key : requestedValues.keySet()) {
            ThresholdSpec spec = specsByKey.get(key);
            KpiThresholdConfiguration entity =
                    thresholdRepository.findByConfigKey(spec.key())
                            .orElseGet(() -> newThresholdEntity(spec));
            entity.setValue(values.get(spec.key()));
            thresholdRepository.save(entity);
        }

        applyThresholdValues(values);
        return buildThresholdResponse();
    }

    @Transactional
    public PlatformWeightsConfigurationDto getPlatformWeights() {
        ensureDefaultConfigurationExists();
        PlatformWeightsConfigurationDto weights = validatedCurrentPlatformWeights();
        applyPlatformWeights(weights);
        return weights;
    }

    @Transactional
    public PlatformWeightsConfigurationDto updatePlatformWeights(PlatformWeightsConfigurationDto request) {
        ensureValidPlatformWeightsExist();
        validatePlatformWeights(request);

        saveWeight(ARUBA, request.getAruba());
        saveWeight(CITRIX, request.getCitrix());
        saveWeight(MICROSOFT365, request.getMicrosoft365());
        saveWeight(GLPI, request.getGlpi());

        PlatformWeightsConfigurationDto weights = currentPlatformWeights();
        applyPlatformWeights(weights);
        return weights;
    }

    @Transactional
    public ThresholdConfigurationDto resetConfiguration() {
        resetThresholdsToDefaults(null);
        resetPlatformWeightsToDefaults(null);

        applyThresholdValues(defaultThresholdValues());
        applyPlatformWeights(new PlatformWeightsConfigurationDto(40, 30, 20, 10));
        return buildThresholdResponse();
    }

    @Transactional
    public void ensureDefaultThresholdsExist() {
        ensureValidThresholdConfigurationExists();
    }

    @Transactional
    public void ensureDefaultPlatformWeightsExist() {
        ensureValidPlatformWeightsExist();
    }

    private void ensureValidThresholdConfigurationExists() {
        Map<String, KpiThresholdConfiguration> stored = storedThresholdsByKey();

        if (stored.isEmpty()) {
            resetThresholdsToDefaults("No existe configuración de umbrales persistida; se cargan valores por defecto.");
            return;
        }

        if (isThresholdConfigurationIncomplete(stored)) {
            addMissingThresholdsWithDefaults(stored, "La configuración de umbrales esta incompleta; se anaden los valores por defecto que faltan.");
        }

        Map<String, Integer> values = new LinkedHashMap<>();
        for (ThresholdSpec spec : thresholdSpecs) {
            KpiThresholdConfiguration config = stored.get(spec.key());
            if (config.getValue() == null) {
                resetThresholdsToDefaults("La configuración de umbrales contiene valores nulos; se restauran valores por defecto.");
                return;
            }
            values.put(spec.key(), config.getValue());
        }

        try {
            validateThresholdValues(values);
        } catch (ResponseStatusException exception) {
            resetThresholdsToDefaults("La configuración de umbrales persistida no es valida; se restauran valores por defecto.");
        }
    }

    private void ensureValidPlatformWeightsExist() {
        Map<String, PlatformWeightConfiguration> stored = storedWeightsByPlatform();

        if (stored.isEmpty()) {
            resetPlatformWeightsToDefaults("No existe configuración de pesos persistida; se cargan valores por defecto.");
            return;
        }

        if (!stored.containsKey(ARUBA)
                || !stored.containsKey(CITRIX)
                || !stored.containsKey(MICROSOFT365)
                || !stored.containsKey(GLPI)) {

            addMissingPlatformWeightsWithDefaults(stored, "La configuración de pesos esta incompleta; se añaden los valores por defecto que faltan.");
        }

        PlatformWeightsConfigurationDto weights =
                new PlatformWeightsConfigurationDto(
                        stored.get(ARUBA).getWeightPercent(),
                        stored.get(CITRIX).getWeightPercent(),
                        stored.get(MICROSOFT365).getWeightPercent(),
                        stored.get(GLPI).getWeightPercent());

        try {
            validatePlatformWeights(weights);
        } catch (ResponseStatusException exception) {
            resetPlatformWeightsToDefaults("La configuración de pesos persistida no es valida; se restauran valores por defecto.");
        }
    }

    private boolean isThresholdConfigurationIncomplete(Map<String, KpiThresholdConfiguration> stored) {
        return thresholdSpecs.stream()
                .anyMatch(spec -> !stored.containsKey(spec.key()));
    }

    private void addMissingThresholdsWithDefaults(
            Map<String, KpiThresholdConfiguration> stored,
            String warningMessage) {

        if (warningMessage != null) {
            LOGGER.warn(warningMessage);
        }

        for (ThresholdSpec spec : thresholdSpecs) {
            if (!stored.containsKey(spec.key())) {
                KpiThresholdConfiguration entity = newThresholdEntity(spec);
                thresholdRepository.save(entity);
                stored.put(spec.key(), entity);
            }
        }
    }

    private void addMissingPlatformWeightsWithDefaults(
            Map<String, PlatformWeightConfiguration> stored,
            String warningMessage) {

        if (warningMessage != null) {
            LOGGER.warn(warningMessage);
        }

        addMissingPlatformWeight(stored, ARUBA, 40);
        addMissingPlatformWeight(stored, CITRIX, 30);
        addMissingPlatformWeight(stored, MICROSOFT365, 20);
        addMissingPlatformWeight(stored, GLPI, 10);
    }

    private void addMissingPlatformWeight(
            Map<String, PlatformWeightConfiguration> stored,
            String platform,
            int defaultWeight) {

        if (!stored.containsKey(platform)) {
            PlatformWeightConfiguration entity = newWeightEntity(platform, defaultWeight);
            weightRepository.save(entity);
            stored.put(platform, entity);
        }
    }

    private void resetThresholdsToDefaults(String warningMessage) {
        if (warningMessage != null) {
            LOGGER.warn(warningMessage);
        }

        for (ThresholdSpec spec : thresholdSpecs) {
            KpiThresholdConfiguration entity =
                    thresholdRepository.findByConfigKey(spec.key())
                            .orElseGet(() -> newThresholdEntity(spec));
            entity.setConfigKey(spec.key());
            entity.setSectionKey(spec.sectionKey());
            entity.setLabel(spec.label());
            entity.setUnit(spec.unit());
            entity.setValue(spec.defaultValue());
            entity.setDefaultValue(spec.defaultValue());
            entity.setDescription(spec.description());
            thresholdRepository.save(entity);
        }
    }

    private void resetPlatformWeightsToDefaults(String warningMessage) {
        if (warningMessage != null) {
            LOGGER.warn(warningMessage);
        }

        saveWeight(ARUBA, 40);
        saveWeight(CITRIX, 30);
        saveWeight(MICROSOFT365, 20);
        saveWeight(GLPI, 10);
    }

    private KpiThresholdConfiguration newThresholdEntity(ThresholdSpec spec) {
        KpiThresholdConfiguration entity = new KpiThresholdConfiguration();
        entity.setConfigKey(spec.key());
        entity.setSectionKey(spec.sectionKey());
        entity.setLabel(spec.label());
        entity.setUnit(spec.unit());
        entity.setValue(spec.defaultValue());
        entity.setDefaultValue(spec.defaultValue());
        entity.setDescription(spec.description());
        return entity;
    }

    private PlatformWeightConfiguration newWeightEntity(String platform, int defaultWeight) {
        PlatformWeightConfiguration entity = new PlatformWeightConfiguration();
        entity.setPlatform(platform);
        entity.setWeightPercent(defaultWeight);
        entity.setDefaultWeightPercent(defaultWeight);
        return entity;
    }

    private void saveWeight(String platform, Integer value) {
        PlatformWeightConfiguration entity =
                weightRepository.findByPlatform(platform)
                        .orElseGet(() -> newWeightEntity(platform, value == null ? 0 : value));
        entity.setPlatform(platform);
        entity.setWeightPercent(value);
        if (entity.getDefaultWeightPercent() == null) {
            entity.setDefaultWeightPercent(defaultWeightFor(platform));
        }
        weightRepository.save(entity);
    }

    private int defaultWeightFor(String platform) {
        return switch (platform) {
            case ARUBA -> 40;
            case CITRIX -> 30;
            case MICROSOFT365 -> 20;
            case GLPI -> 10;
            default -> 0;
        };
    }

    private ThresholdConfigurationDto buildThresholdResponse() {
        Map<String, List<ThresholdValueDto>> grouped = new LinkedHashMap<>();
        for (String sectionKey : sectionSpecs.keySet()) {
            grouped.put(sectionKey, new ArrayList<>());
        }

        Map<String, KpiThresholdConfiguration> stored = storedThresholdsByKey();

        thresholdSpecs.stream()
                .sorted(Comparator.comparingInt(ThresholdSpec::order))
                .forEach(spec -> {
                    KpiThresholdConfiguration config = stored.get(spec.key());
                    grouped.get(spec.sectionKey()).add(
                            new ThresholdValueDto(
                                    spec.key(),
                                    spec.label(),
                                    config == null ? spec.defaultValue() : config.getValue(),
                                    spec.defaultValue(),
                                    spec.unit(),
                                    spec.description()));
                });

        List<ThresholdSectionDto> sections = grouped.entrySet().stream()
                .map(entry -> {
                    SectionSpec section = sectionSpecs.get(entry.getKey());
                    return new ThresholdSectionDto(
                            section.key(),
                            section.title(),
                            section.description(),
                            entry.getValue());
                })
                .toList();

        return new ThresholdConfigurationDto(sections);
    }

    private Map<String, Integer> currentThresholdValues() {
        Map<String, Integer> values = defaultThresholdValues();
        for (KpiThresholdConfiguration config : thresholdRepository.findAll()) {
            if (values.containsKey(config.getConfigKey()) && config.getValue() != null) {
                values.put(config.getConfigKey(), config.getValue());
            }
        }
        return values;
    }

    private Map<String, Integer> validatedCurrentThresholdValues() {
        Map<String, Integer> values = currentThresholdValues();
        try {
            validateThresholdValues(values);
            return values;
        } catch (ResponseStatusException exception) {
            resetThresholdsToDefaults("La configuración de umbrales no supera la validación final; se aplican valores por defecto.");
            return defaultThresholdValues();
        }
    }

    private Map<String, KpiThresholdConfiguration> storedThresholdsByKey() {
        Map<String, KpiThresholdConfiguration> stored = new LinkedHashMap<>();
        for (KpiThresholdConfiguration config : thresholdRepository.findAll()) {
            stored.put(config.getConfigKey(), config);
        }
        return stored;
    }

    private Map<String, PlatformWeightConfiguration> storedWeightsByPlatform() {
        Map<String, PlatformWeightConfiguration> stored = new LinkedHashMap<>();
        for (PlatformWeightConfiguration weight : weightRepository.findAll()) {
            stored.put(weight.getPlatform(), weight);
        }
        return stored;
    }

    private Map<String, Integer> defaultThresholdValues() {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (ThresholdSpec spec : thresholdSpecs) {
            values.put(spec.key(), spec.defaultValue());
        }
        return values;
    }

    private Map<String, Integer> extractRequestedThresholds(ThresholdConfigurationDto request) {
        if (request == null || request.getSections() == null) {
            throw badRequest("La configuración de umbrales es obligatoria.");
        }

        Map<String, Integer> values = new LinkedHashMap<>();
        Map<String, ThresholdSpec> specsByKey = specsByKey();

        for (ThresholdSectionDto section : request.getSections()) {
            if (section == null || section.getValues() == null) {
                throw badRequest("Cada seccion debe contener valores.");
            }

            for (ThresholdValueDto value : section.getValues()) {
                if (value == null || value.getKey() == null || value.getKey().isBlank()) {
                    throw badRequest("Cada umbral debe tener clave.");
                }

                if (!specsByKey.containsKey(value.getKey())) {
                    throw badRequest("Umbral no reconocido: " + value.getKey());
                }

                if (value.getValue() == null) {
                    throw badRequest("El umbral " + value.getKey() + " no puede ser nulo.");
                }

                values.put(value.getKey(), value.getValue());
            }
        }

        return values;
    }

    private void validateThresholdValues(Map<String, Integer> values) {
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            if (entry.getValue() == null) {
                throw badRequest("El umbral " + entry.getKey() + " no puede ser nulo.");
            }

            if (entry.getValue() < 0) {
                throw badRequest("El umbral " + entry.getKey() + " no puede ser negativo.");
            }
        }

        int statusMax = value(values, "status.max");
        validatePercent("status.max", statusMax);
        validateRiskTransversal(values, "transversal.globalStatus", statusMax);
        validateRiskTransversal(values, "transversal.globalCriticality", statusMax);
        validateHealthTransversal(values, "transversal.globalAvailability", statusMax);
        validateRiskTransversal(values, "transversal.operationalPressure", statusMax);
        validateRiskTransversal(values, "transversal.technicalDegradation", statusMax);
        validateRiskTransversal(values, "transversal.slaRisk", statusMax);
        validateRiskTransversal(values, "transversal.operationalBacklog", statusMax);
        validateRiskTransversal(values, "transversal.userImpact", statusMax);
        validateRiskTransversal(values, "transversal.affectedServices", statusMax);

        int apYellow = value(values, "aruba.accessPointDownYellowPercent");
        int apRed = value(values, "aruba.accessPointDownRedPercent");
        validatePercent("aruba.accessPointDownYellowPercent", apYellow);
        validatePercent("aruba.accessPointDownRedPercent", apRed);
        require(apYellow < apRed, "El umbral amarillo de APs caidos debe ser menor que el rojo.");
        require(
                value(values, "aruba.arubaOpenTicketsYellowMin") < value(values, "aruba.arubaOpenTicketsRedMin"),
                "El umbral amarillo de tickets Aruba debe ser menor que el rojo.");
        require(
                value(values, "aruba.underusedSwitchesYellowAbove") < value(values, "aruba.underusedSwitchesRedAbove"),
                "El umbral amarillo de switches infrautilizados debe ser menor que el rojo.");
        int inactiveApDaysThreshold = value(values, "aruba.inactiveApDaysThreshold");
        require(
                inactiveApDaysThreshold >= 1 && inactiveApDaysThreshold <= 365,
                "El umbral aruba.inactiveApDaysThreshold debe estar entre 1 y 365 dias.");

        validatePercent("citrix.deliveryControllerYellowBelowPercent", value(values, "citrix.deliveryControllerYellowBelowPercent"));
        require(
                value(values, "citrix.logonDurationYellowAboveSeconds") < value(values, "citrix.logonDurationRedAboveSeconds"),
                "El umbral amarillo de logon Citrix debe ser menor que el rojo.");
        validatePercent("citrix.serverLoadYellowMin", value(values, "citrix.serverLoadYellowMin"));
        validatePercent("citrix.serverLoadRedMin", value(values, "citrix.serverLoadRedMin"));
        require(
                value(values, "citrix.serverLoadYellowMin") < value(values, "citrix.serverLoadRedMin"),
                "El umbral amarillo de carga Citrix debe ser menor que el rojo.");
        require(
                value(values, "citrix.failedLogonsYellowAbove") < value(values, "citrix.failedLogonsRedAbove"),
                "El umbral amarillo de errores Citrix debe ser menor que el rojo.");

        validatePercent("microsoft365.sharePointYellowMin", value(values, "microsoft365.sharePointYellowMin"));
        validatePercent("microsoft365.sharePointRedAbove", value(values, "microsoft365.sharePointRedAbove"));
        require(
                value(values, "microsoft365.sharePointYellowMin") <= value(values, "microsoft365.sharePointRedAbove"),
                "El umbral amarillo de SharePoint debe ser menor o igual que el rojo.");
        require(
                value(values, "microsoft365.unassignedLicensesRedBelowOrEqual") < value(values, "microsoft365.unassignedLicensesYellowBelow"),
                "El umbral rojo de licencias no asignadas debe ser menor que el amarillo.");
        require(
                value(values, "microsoft365.riskyUsersYellowAbove") < value(values, "microsoft365.riskyUsersRedAbove"),
                "El umbral amarillo de usuarios en riesgo debe ser menor que el rojo.");
        require(
                value(values, "microsoft365.failedSignInsYellowMin") < value(values, "microsoft365.failedSignInsRedMin"),
                "El umbral amarillo de inicios fallidos debe ser menor que el rojo.");
        require(
                value(values, "microsoft365.usersWithoutMfaYellowAbove") < value(values, "microsoft365.usersWithoutMfaRedAbove"),
                "El umbral amarillo de usuarios sin MFA debe ser menor que el rojo.");
        require(
                value(values, "microsoft365.nonCompliantDevicesYellowAbove") < value(values, "microsoft365.nonCompliantDevicesRedAbove"),
                "El umbral amarillo de equipos no conformes debe ser menor que el rojo.");
        require(
                value(values, "microsoft365.microsoft365OpenTicketsYellowMin") < value(values, "microsoft365.microsoft365OpenTicketsRedMin"),
                "El umbral amarillo de tickets Microsoft 365 debe ser menor que el rojo.");

        require(
                value(values, "glpi.openTicketsYellowMin") < value(values, "glpi.openTicketsRedMin"),
                "El umbral amarillo de tickets abiertos debe ser menor que el rojo.");
        require(
                value(values, "glpi.criticalTicketsYellowAbove") < value(values, "glpi.criticalTicketsRedAbove"),
                "El umbral amarillo de tickets críticos debe ser menor que el rojo.");
        require(
                value(values, "glpi.slaBreachedTicketsYellowAbove") < value(values, "glpi.slaBreachedTicketsRedAbove"),
                "El umbral amarillo de tickets SLA vencidos debe ser menor que el rojo.");
        validatePercent("glpi.closedPercentGreenMin", value(values, "glpi.closedPercentGreenMin"));
    }

    private void validatePlatformWeights(PlatformWeightsConfigurationDto request) {
        if (request == null
                || request.getAruba() == null
                || request.getCitrix() == null
                || request.getMicrosoft365() == null
                || request.getGlpi() == null) {
            throw badRequest("Todos los pesos de plataforma son obligatorios.");
        }

        require(request.getAruba() >= 0, "El peso de Aruba no puede ser negativo.");
        require(request.getCitrix() >= 0, "El peso de Citrix no puede ser negativo.");
        require(request.getMicrosoft365() >= 0, "El peso de Microsoft 365 no puede ser negativo.");
        require(request.getGlpi() >= 0, "El peso de GLPI no puede ser negativo.");

        int total = request.getAruba()
                + request.getCitrix()
                + request.getMicrosoft365()
                + request.getGlpi();

        require(total == 100, "Los pesos globales deben sumar 100.");
    }

    private void applyThresholdValues(Map<String, Integer> values) {
        apply(values, "status.max", kpiProperties.getStatus()::setMax);

        applyTransversalRisk(values, "transversal.globalStatus", kpiProperties.getTransversal().getGlobalStatus());
        applyTransversalRisk(values, "transversal.globalCriticality", kpiProperties.getTransversal().getGlobalCriticality());
        applyTransversalHealth(values, "transversal.globalAvailability", kpiProperties.getTransversal().getGlobalAvailability());
        applyTransversalRisk(values, "transversal.operationalPressure", kpiProperties.getTransversal().getOperationalPressure());
        applyTransversalRisk(values, "transversal.technicalDegradation", kpiProperties.getTransversal().getTechnicalDegradation());
        applyTransversalRisk(values, "transversal.slaRisk", kpiProperties.getTransversal().getSlaRisk());
        applyTransversalRisk(values, "transversal.operationalBacklog", kpiProperties.getTransversal().getOperationalBacklog());
        applyTransversalRisk(values, "transversal.userImpact", kpiProperties.getTransversal().getUserImpact());
        applyTransversalRisk(values, "transversal.affectedServices", kpiProperties.getTransversal().getAffectedServices());

        apply(values, "aruba.accessPointDownYellowPercent", kpiProperties.getAruba()::setAccessPointDownYellowPercent);
        apply(values, "aruba.accessPointDownRedPercent", kpiProperties.getAruba()::setAccessPointDownRedPercent);
        apply(values, "aruba.arubaOpenTicketsYellowMin", kpiProperties.getAruba()::setArubaOpenTicketsYellowMin);
        apply(values, "aruba.arubaOpenTicketsRedMin", kpiProperties.getAruba()::setArubaOpenTicketsRedMin);
        apply(values, "aruba.switchDownYellowAbove", kpiProperties.getAruba()::setSwitchDownYellowAbove);
        apply(values, "aruba.switchUpgradeYellowMin", kpiProperties.getAruba()::setSwitchUpgradeYellowMin);
        apply(values, "aruba.pendingFirmwareApsYellowMin", kpiProperties.getAruba()::setPendingFirmwareApsYellowMin);
        apply(values, "aruba.inactiveApsYellowMin", kpiProperties.getAruba()::setInactiveApsYellowMin);
        apply(values, "aruba.inactiveApDaysThreshold", kpiProperties.getAruba()::setInactiveApDaysThreshold);
        apply(values, "aruba.criticalClientsGreenAbove", kpiProperties.getAruba()::setCriticalClientsGreenAbove);
        apply(values, "aruba.underusedSwitchesYellowAbove", kpiProperties.getAruba()::setUnderusedSwitchesYellowAbove);
        apply(values, "aruba.underusedSwitchesRedAbove", kpiProperties.getAruba()::setUnderusedSwitchesRedAbove);

        apply(values, "citrix.deliveryControllerYellowBelowPercent", kpiProperties.getCitrix()::setDeliveryControllerYellowBelowPercent);
        apply(values, "citrix.logonDurationYellowAboveSeconds", kpiProperties.getCitrix()::setLogonDurationYellowAboveSeconds);
        apply(values, "citrix.logonDurationRedAboveSeconds", kpiProperties.getCitrix()::setLogonDurationRedAboveSeconds);
        apply(values, "citrix.serverLoadYellowMin", kpiProperties.getCitrix()::setServerLoadYellowMin);
        apply(values, "citrix.serverLoadRedMin", kpiProperties.getCitrix()::setServerLoadRedMin);
        apply(values, "citrix.failedLogonsYellowAbove", kpiProperties.getCitrix()::setFailedLogonsYellowAbove);
        apply(values, "citrix.failedLogonsRedAbove", kpiProperties.getCitrix()::setFailedLogonsRedAbove);

        apply(values, "microsoft365.sharePointYellowMin", kpiProperties.getMicrosoft365()::setSharePointYellowMin);
        apply(values, "microsoft365.sharePointRedAbove", kpiProperties.getMicrosoft365()::setSharePointRedAbove);
        apply(values, "microsoft365.unassignedLicensesYellowBelow", kpiProperties.getMicrosoft365()::setUnassignedLicensesYellowBelow);
        apply(values, "microsoft365.unassignedLicensesRedBelowOrEqual", kpiProperties.getMicrosoft365()::setUnassignedLicensesRedBelowOrEqual);
        apply(values, "microsoft365.riskyUsersYellowAbove", kpiProperties.getMicrosoft365()::setRiskyUsersYellowAbove);
        apply(values, "microsoft365.riskyUsersRedAbove", kpiProperties.getMicrosoft365()::setRiskyUsersRedAbove);
        apply(values, "microsoft365.failedSignInsYellowMin", kpiProperties.getMicrosoft365()::setFailedSignInsYellowMin);
        apply(values, "microsoft365.failedSignInsRedMin", kpiProperties.getMicrosoft365()::setFailedSignInsRedMin);
        apply(values, "microsoft365.usersWithoutMfaYellowAbove", kpiProperties.getMicrosoft365()::setUsersWithoutMfaYellowAbove);
        apply(values, "microsoft365.usersWithoutMfaRedAbove", kpiProperties.getMicrosoft365()::setUsersWithoutMfaRedAbove);
        apply(values, "microsoft365.secretsYellowAbove", kpiProperties.getMicrosoft365()::setSecretsYellowAbove);
        apply(values, "microsoft365.unusedApplicationsYellowAbove", kpiProperties.getMicrosoft365()::setUnusedApplicationsYellowAbove);
        apply(values, "microsoft365.highPrivilegeApplicationsYellowAbove", kpiProperties.getMicrosoft365()::setHighPrivilegeApplicationsYellowAbove);
        apply(values, "microsoft365.nonCompliantDevicesYellowAbove", kpiProperties.getMicrosoft365()::setNonCompliantDevicesYellowAbove);
        apply(values, "microsoft365.nonCompliantDevicesRedAbove", kpiProperties.getMicrosoft365()::setNonCompliantDevicesRedAbove);
        apply(values, "microsoft365.microsoft365OpenTicketsYellowMin", kpiProperties.getMicrosoft365()::setMicrosoft365OpenTicketsYellowMin);
        apply(values, "microsoft365.microsoft365OpenTicketsRedMin", kpiProperties.getMicrosoft365()::setMicrosoft365OpenTicketsRedMin);
        apply(values, "microsoft365.outdatedWindowsYellowAbove", kpiProperties.getMicrosoft365()::setOutdatedWindowsYellowAbove);
        apply(values, "microsoft365.devicesWithoutEncryptionYellowAbove", kpiProperties.getMicrosoft365()::setDevicesWithoutEncryptionYellowAbove);
        apply(values, "microsoft365.devicesWithoutEncryptionRedAbove", kpiProperties.getMicrosoft365()::setDevicesWithoutEncryptionRedAbove);
        apply(values, "microsoft365.staleDevicesRedAbove", kpiProperties.getMicrosoft365()::setStaleDevicesRedAbove);

        apply(values, "glpi.openTicketsYellowMin", kpiProperties.getGlpi()::setOpenTicketsYellowMin);
        apply(values, "glpi.openTicketsRedMin", kpiProperties.getGlpi()::setOpenTicketsRedMin);
        apply(values, "glpi.criticalTicketsYellowAbove", kpiProperties.getGlpi()::setCriticalTicketsYellowAbove);
        apply(values, "glpi.criticalTicketsRedAbove", kpiProperties.getGlpi()::setCriticalTicketsRedAbove);
        apply(values, "glpi.slaBreachedTicketsYellowAbove", kpiProperties.getGlpi()::setSlaBreachedTicketsYellowAbove);
        apply(values, "glpi.slaBreachedTicketsRedAbove", kpiProperties.getGlpi()::setSlaBreachedTicketsRedAbove);
        apply(values, "glpi.closedPercentGreenMin", kpiProperties.getGlpi()::setClosedPercentGreenMin);
    }

    private void applyPlatformWeights(PlatformWeightsConfigurationDto weights) {
        KpiProperties.PlatformWeights globalWeights = kpiProperties.getWeights().getGlobalStatus();
        globalWeights.setAruba(weights.getAruba() / 100.0);
        globalWeights.setCitrix(weights.getCitrix() / 100.0);
        globalWeights.setMicrosoft365(weights.getMicrosoft365() / 100.0);
        globalWeights.setGlpi(weights.getGlpi() / 100.0);
    }

    private PlatformWeightsConfigurationDto currentPlatformWeights() {
        Map<String, Integer> stored = new LinkedHashMap<>();
        stored.put(ARUBA, 40);
        stored.put(CITRIX, 30);
        stored.put(MICROSOFT365, 20);
        stored.put(GLPI, 10);

        for (PlatformWeightConfiguration weight : weightRepository.findAll()) {
            if (stored.containsKey(weight.getPlatform()) && weight.getWeightPercent() != null) {
                stored.put(weight.getPlatform(), weight.getWeightPercent());
            }
        }

        PlatformWeightsConfigurationDto weights =
                new PlatformWeightsConfigurationDto(
                        stored.get(ARUBA),
                        stored.get(CITRIX),
                        stored.get(MICROSOFT365),
                        stored.get(GLPI));
        validatePlatformWeights(weights);
        return weights;
    }

    private PlatformWeightsConfigurationDto validatedCurrentPlatformWeights() {
        try {
            return currentPlatformWeights();
        } catch (ResponseStatusException exception) {
            resetPlatformWeightsToDefaults("La configuración de pesos no supera la validación final; se aplican valores por defecto.");
            return new PlatformWeightsConfigurationDto(40, 30, 20, 10);
        }
    }

    private void apply(Map<String, Integer> values, String key, IntConsumer setter) {
        setter.accept(values.get(key));
    }

    private void applyTransversalRisk(
            Map<String, Integer> values,
            String metricKey,
            KpiProperties.TransversalKpiThreshold threshold) {

        threshold.setDirection("RISK");
        threshold.setYellowMin(value(values, metricKey + ".yellowMin"));
        threshold.setRedMin(value(values, metricKey + ".redMin"));
    }

    private void applyTransversalHealth(
            Map<String, Integer> values,
            String metricKey,
            KpiProperties.TransversalKpiThreshold threshold) {

        threshold.setDirection("HEALTH");
        threshold.setYellowMin(value(values, metricKey + ".yellowMin"));
        threshold.setGreenMin(value(values, metricKey + ".greenMin"));
    }

    private int value(Map<String, Integer> values, String key) {
        Integer value = values.get(key);
        if (value == null) {
            throw badRequest("Falta el umbral " + key + ".");
        }
        return value;
    }

    private void validatePercent(String key, int value) {
        require(value <= 100, "El umbral " + key + " debe estar entre 0 y 100.");
    }

    private void validateRiskTransversal(Map<String, Integer> values, String metricKey, int max) {
        int yellowMin = value(values, metricKey + ".yellowMin");
        int redMin = value(values, metricKey + ".redMin");
        validatePercent(metricKey + ".yellowMin", yellowMin);
        validatePercent(metricKey + ".redMin", redMin);
        require(yellowMin < redMin && redMin <= max,
                "Los rangos de " + metricKey + " deben ser verde < amarillo < rojo.");
    }

    private void validateHealthTransversal(Map<String, Integer> values, String metricKey, int max) {
        int yellowMin = value(values, metricKey + ".yellowMin");
        int greenMin = value(values, metricKey + ".greenMin");
        validatePercent(metricKey + ".yellowMin", yellowMin);
        validatePercent(metricKey + ".greenMin", greenMin);
        require(yellowMin < greenMin && greenMin <= max,
                "Los rangos de " + metricKey + " deben ser rojo < amarillo < verde.");
    }

    private void require(boolean valid, String message) {
        if (!valid) {
            throw badRequest(message);
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Map<String, ThresholdSpec> specsByKey() {
        Map<String, ThresholdSpec> specs = new LinkedHashMap<>();
        for (ThresholdSpec spec : thresholdSpecs) {
            specs.put(spec.key(), spec);
        }
        return specs;
    }

    private Map<String, SectionSpec> buildSections() {
        Map<String, SectionSpec> sections = new LinkedHashMap<>();
        sections.put("transversal", new SectionSpec(
                "transversal",
                "Estado global y KPIs transversales",
                "Umbrales independientes por KPI transversal. Los KPIs RISK interpretan 100 como peor valor; Disponibilidad global usa HEALTH y considera 100 como mejor valor."));
        sections.put("aruba", new SectionSpec(
                "aruba",
                "Aruba",
                "Umbrales del índice de salud Aruba y pesos internos de Access Points y switches."));
        sections.put("citrix", new SectionSpec(
                "citrix",
                "Citrix",
                "Umbrales internos del indice de salud Citrix."));
        sections.put("microsoft365", new SectionSpec(
                "microsoft365",
                "Microsoft 365",
                "Umbrales internos del indice de salud Microsoft 365."));
        sections.put("glpi", new SectionSpec(
                "glpi",
                "GLPI",
                "Umbrales de tickets y capacidad de cierre usados por GLPI."));
        return sections;
    }

    private List<ThresholdSpec> buildThresholdSpecs() {
        List<ThresholdSpec> specs = new ArrayList<>();
        add(specs, "transversal", "status.max", "Maximo de escala", 100, "%", "Valor maximo de la escala común de KPIs.");
        addRiskTransversal(specs, "transversal.globalStatus", "Estado global");
        addRiskTransversal(specs, "transversal.globalCriticality", "Criticidad global");
        addHealthTransversal(specs, "transversal.globalAvailability", "Disponibilidad global");
        addRiskTransversal(specs, "transversal.operationalPressure", "Presión operativa");
        addRiskTransversal(specs, "transversal.technicalDegradation", "Degradación técnica");
        addRiskTransversal(specs, "transversal.slaRisk", "Riesgo SLA");
        addRiskTransversal(specs, "transversal.operationalBacklog", "Backlog operativo");
        addRiskTransversal(specs, "transversal.userImpact", "Impacto en usuarios");
        addRiskTransversal(specs, "transversal.affectedServices", "Servicios afectados");

        add(specs, "aruba", "aruba.arubaOpenTicketsYellowMin", "Tickets abiertos Aruba amarillo desde", 100, "tickets", "Tickets GLPI asociados a Aruba desde los que el indicador entra en advertencia.");
        add(specs, "aruba", "aruba.arubaOpenTicketsRedMin", "Tickets abiertos Aruba rojo desde", 200, "tickets", "Tickets GLPI asociados a Aruba desde los que el indicador entra en critico.");
        add(specs, "aruba", "aruba.accessPointDownYellowPercent", "APs caidos amarillo desde", 50, "%", "Porcentaje de APs caidos desde el que el indicador entra en advertencia.");
        add(specs, "aruba", "aruba.accessPointDownRedPercent", "APs caidos rojo desde", 100, "%", "Porcentaje de APs caidos desde el que el indicador entra en critico.");
        add(specs, "aruba", "aruba.pendingFirmwareApsYellowMin", "Firmware AP amarillo desde", 1, "APs", "APs con firmware pendiente desde los que el indicador entra en advertencia.");
        add(specs, "aruba", "aruba.inactiveApsYellowMin", "APs inactivos amarillo desde", 1, "APs", "APs inactivos desde los que el indicador entra en advertencia.");
        add(specs, "aruba", "aruba.inactiveApDaysThreshold", "APs inactivos", 30, "días", "Número de días sin ver un AP para considerarlo inactivo.");
        add(specs, "aruba", "aruba.criticalClientsGreenAbove", "Clientes WiFi rojo si es igual o menor que", 0, "clientes", "Si el total de clientes WiFi es igual o inferior a este valor, aporta afeccion roja.");
        add(specs, "aruba", "aruba.switchDownYellowAbove", "Switches apagados amarillo si es mayor que", 1, "switches", "Switches apagados por encima de este valor aportan advertencia.");
        add(specs, "aruba", "aruba.switchUpgradeYellowMin", "Switches con upgrade amarillo desde", 1, "switches", "Switches con upgrade pendiente desde los que el indicador aporta advertencia.");
        add(specs, "aruba", "aruba.underusedSwitchesYellowAbove", "Switches infrautilizados amarillo si es mayor que", 1, "switches", "Switches infrautilizados por encima de este valor aportan advertencia.");
        add(specs, "aruba", "aruba.underusedSwitchesRedAbove", "Switches infrautilizados rojo si es mayor que", 5, "switches", "Switches infrautilizados por encima de este valor aportan estado critico.");

        add(specs, "citrix", "citrix.deliveryControllerYellowBelowPercent", "Delivery Controllers amarillo por debajo de", 67, "%", "Porcentaje disponible por debajo del cual Citrix pasa a amarillo.");
        add(specs, "citrix", "citrix.logonDurationYellowAboveSeconds", "Logon amarillo si es mayor que", 20, "s", "Duracion media de logon que activa advertencia.");
        add(specs, "citrix", "citrix.logonDurationRedAboveSeconds", "Logon rojo si es mayor que", 60, "s", "Duracion media de logon que activa estado critico.");
        add(specs, "citrix", "citrix.serverLoadYellowMin", "Carga servidores amarilla desde", 80, "%", "Carga de servidores que activa advertencia.");
        add(specs, "citrix", "citrix.serverLoadRedMin", "Carga servidores roja desde", 90, "%", "Carga de servidores que activa estado critico.");
        add(specs, "citrix", "citrix.failedLogonsYellowAbove", "Errores inicio amarillo si es mayor que", 5, "errores", "Errores de inicio que activan advertencia.");
        add(specs, "citrix", "citrix.failedLogonsRedAbove", "Errores inicio rojo si es mayor que", 20, "errores", "Errores de inicio que activan estado critico.");

        add(specs, "microsoft365", "microsoft365.sharePointYellowMin", "SharePoint amarillo desde", 80, "%", "Uso de almacenamiento que activa advertencia.");
        add(specs, "microsoft365", "microsoft365.sharePointRedAbove", "SharePoint rojo desde", 90, "%", "Uso de almacenamiento que activa estado critico.");
        add(specs, "microsoft365", "microsoft365.unassignedLicensesYellowBelow", "Licencias no asignadas amarillo por debajo de", 20, "licencias", "Licencias disponibles por debajo de este valor activan advertencia.");
        add(specs, "microsoft365", "microsoft365.unassignedLicensesRedBelowOrEqual", "Licencias no asignadas rojo si es igual o menor que", 2, "licencias", "Licencias disponibles iguales o inferiores a este valor activan estado critico.");
        add(specs, "microsoft365", "microsoft365.riskyUsersYellowAbove", "Usuarios en riesgo amarillo si es mayor que", 0, "usuarios", "Usuarios en riesgo que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.riskyUsersRedAbove", "Usuarios en riesgo rojo si es mayor que", 9, "usuarios", "Usuarios en riesgo por encima de este valor activan estado critico.");
        add(specs, "microsoft365", "microsoft365.failedSignInsYellowMin", "Inicios fallidos amarillo desde", 10, "inicios", "Inicios fallidos desde los que el indicador entra en advertencia.");
        add(specs, "microsoft365", "microsoft365.failedSignInsRedMin", "Inicios fallidos rojo desde", 20, "inicios", "Inicios fallidos desde los que el indicador entra en critico.");
        add(specs, "microsoft365", "microsoft365.usersWithoutMfaYellowAbove", "Usuarios sin MFA amarillo si es mayor que", 0, "usuarios", "Usuarios sin MFA que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.usersWithoutMfaRedAbove", "Usuarios sin MFA rojo si es mayor que", 4, "usuarios", "Usuarios sin MFA que activan estado critico.");
        add(specs, "microsoft365", "microsoft365.secretsYellowAbove", "Secretos amarillo si es mayor que", 0, "secretos", "Secretos proximos a caducar que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.unusedApplicationsYellowAbove", "Aplicaciones sin uso amarillo si es mayor que", 0, "apps", "Aplicaciones sin uso que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.highPrivilegeApplicationsYellowAbove", "Apps permisos elevados amarillo si es mayor que", 0, "apps", "Aplicaciones con permisos elevados que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.nonCompliantDevicesYellowAbove", "No conformes amarillo si es mayor que", 30, "equipos", "Equipos no conformes que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.nonCompliantDevicesRedAbove", "No conformes rojo si es mayor que", 50, "equipos", "Equipos no conformes que activan estado critico.");
        add(specs, "microsoft365", "microsoft365.microsoft365OpenTicketsYellowMin", "Tickets Microsoft 365 amarillo desde", 100, "tickets", "Tickets GLPI asociados a Microsoft 365 desde los que el indicador entra en advertencia.");
        add(specs, "microsoft365", "microsoft365.microsoft365OpenTicketsRedMin", "Tickets Microsoft 365 rojo desde", 200, "tickets", "Tickets GLPI asociados a Microsoft 365 desde los que el indicador entra en critico.");
        add(specs, "microsoft365", "microsoft365.outdatedWindowsYellowAbove", "Windows desactualizados amarillo si es mayor que", 0, "equipos", "Equipos con Windows desactualizado que activan advertencia.");
        add(specs, "microsoft365", "microsoft365.devicesWithoutEncryptionYellowAbove", "Sin cifrado umbral auxiliar", 0, "equipos", "Umbral mantenido por compatibilidad; la regla actual considera rojo cualquier equipo sin cifrado.");
        add(specs, "microsoft365", "microsoft365.devicesWithoutEncryptionRedAbove", "Sin cifrado rojo si es mayor que", 0, "equipos", "Equipos sin cifrado que activan estado critico.");
        add(specs, "microsoft365", "microsoft365.staleDevicesRedAbove", "Sin check-in rojo si es mayor que", 0, "equipos", "Dispositivos sin check-in durante mas de 90 dias que activan estado critico.");

        add(specs, "glpi", "glpi.openTicketsYellowMin", "Tickets abiertos amarillo desde", 101, "tickets", "Tickets abiertos que activan advertencia.");
        add(specs, "glpi", "glpi.openTicketsRedMin", "Tickets abiertos rojo desde", 201, "tickets", "Tickets abiertos que activan estado critico.");
        add(specs, "glpi", "glpi.criticalTicketsYellowAbove", "Tickets críticos amarillo si es mayor que", 0, "tickets", "Tickets críticos que activan advertencia.");
        add(specs, "glpi", "glpi.criticalTicketsRedAbove", "Tickets críticos rojo si es mayor que", 10, "tickets", "Tickets críticos que activan estado critico.");
        add(specs, "glpi", "glpi.slaBreachedTicketsYellowAbove", "Tickets SLA vencidos amarillo si es mayor que", 0, "tickets", "Tickets vencidos SLA que activan advertencia.");
        add(specs, "glpi", "glpi.slaBreachedTicketsRedAbove", "Tickets SLA vencidos rojo si es mayor que", 10, "tickets", "Tickets vencidos SLA que activan estado critico.");
        add(specs, "glpi", "glpi.closedPercentGreenMin", "Cierre minimo verde", 50, "%", "Porcentaje minimo de cierre diario/semanal para considerar GLPI en verde.");

        return specs;
    }

    private void addRiskTransversal(List<ThresholdSpec> specs, String metricKey, String label) {
        add(specs, "transversal", metricKey + ".yellowMin", label + " - inicio amarillo", 34, "%", "KPI RISK: de 0 a 33 es verde y desde este valor pasa a amarillo.");
        add(specs, "transversal", metricKey + ".redMin", label + " - inicio rojo", 67, "%", "KPI RISK: desde este valor pasa a rojo.");
    }

    private void addHealthTransversal(List<ThresholdSpec> specs, String metricKey, String label) {
        add(specs, "transversal", metricKey + ".yellowMin", label + " - inicio amarillo", 34, "%", "KPI HEALTH: por debajo de este valor es rojo.");
        add(specs, "transversal", metricKey + ".greenMin", label + " - inicio verde", 67, "%", "KPI HEALTH: desde este valor pasa a verde.");
    }

    private void add(
            List<ThresholdSpec> specs,
            String sectionKey,
            String key,
            String label,
            int defaultValue,
            String unit,
            String description) {

        specs.add(new ThresholdSpec(
                key,
                sectionKey,
                label,
                defaultValue,
                unit,
                description,
                specs.size()));
    }

    private record SectionSpec(String key, String title, String description) {
    }

    private record ThresholdSpec(
            String key,
            String sectionKey,
            String label,
            int defaultValue,
            String unit,
            String description,
            int order) {
    }
}

