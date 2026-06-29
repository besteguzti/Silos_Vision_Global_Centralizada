package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

class KpiConfigurationServiceTest {

    @Mock
    private KpiThresholdConfigurationRepository thresholdRepository;

    @Mock
    private PlatformWeightConfigurationRepository weightRepository;

    private KpiConfigurationService service;
    private KpiProperties kpiProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        kpiProperties = new KpiProperties();
        service =
                new KpiConfigurationService(
                        thresholdRepository,
                        weightRepository,
                        kpiProperties);

        when(thresholdRepository.findByConfigKey(anyString()))
                .thenAnswer(invocation -> Optional.of(threshold(invocation.getArgument(0), 0)));
        when(thresholdRepository.save(any(KpiThresholdConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(thresholdRepository.findAll())
                .thenReturn(defaultThresholds());
        when(weightRepository.findByPlatform(anyString()))
                .thenReturn(Optional.of(new PlatformWeightConfiguration()));
        when(weightRepository.save(any(PlatformWeightConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(weightRepository.findAll())
                .thenReturn(List.of(
                        weight("ARUBA", 40),
                        weight("CITRIX", 30),
                        weight("MICROSOFT365", 20),
                        weight("GLPI", 10)));
    }

    @Test
    void createsDefaultThresholdsAndWeightsWhenDatabaseIsEmpty() {
        when(thresholdRepository.findByConfigKey(anyString()))
                .thenReturn(Optional.empty());
        when(thresholdRepository.findAll())
                .thenReturn(List.of());
        when(weightRepository.findByPlatform(anyString()))
                .thenReturn(Optional.empty());
        when(weightRepository.findAll())
                .thenReturn(List.of());

        ThresholdConfigurationDto response = service.getThresholds();

        assertThat(response.getSections()).isNotEmpty();
        verify(thresholdRepository, times(defaultThresholds().size())).save(any(KpiThresholdConfiguration.class));
        verify(weightRepository, times(4)).save(any(PlatformWeightConfiguration.class));
    }

    @Test
    void doesNotOverwriteExistingCustomThresholdsOnStartup() {
        List<KpiThresholdConfiguration> customThresholds =
                defaultThresholdsWith("transversal.globalStatus.yellowMin", 45);
        when(thresholdRepository.findByConfigKey(anyString()))
                .thenReturn(Optional.of(new KpiThresholdConfiguration()));
        when(thresholdRepository.findAll())
                .thenReturn(customThresholds);

        service.ensureDefaultConfigurationExists();

        verify(thresholdRepository, never()).save(any(KpiThresholdConfiguration.class));
        assertThat(kpiProperties.getTransversal().getGlobalStatus().getYellowMin()).isEqualTo(45);
    }

    @Test
    void repairsIncompleteThresholdConfigurationWithDefaults() {
        when(thresholdRepository.findAll())
                .thenReturn(defaultThresholds().stream()
                        .filter(threshold ->
                                !"citrix.logonDurationYellowAboveSeconds".equals(threshold.getConfigKey()))
                        .toList());

        service.ensureDefaultThresholdsExist();

        ArgumentCaptor<KpiThresholdConfiguration> captor =
                ArgumentCaptor.forClass(KpiThresholdConfiguration.class);
        verify(thresholdRepository).save(captor.capture());
        assertThat(captor.getAllValues())
                .anySatisfy(threshold -> {
                    assertThat(threshold.getConfigKey())
                            .isEqualTo("citrix.logonDurationYellowAboveSeconds");
                    assertThat(threshold.getValue()).isEqualTo(20);
                });
    }

    @Test
    void addsMissingThresholdWithoutOverwritingCustomValues() {
        when(thresholdRepository.findAll())
                .thenReturn(defaultThresholdsWith("transversal.globalStatus.yellowMin", 45).stream()
                        .filter(threshold ->
                                !"aruba.inactiveApDaysThreshold".equals(threshold.getConfigKey()))
                        .toList());

        service.ensureDefaultConfigurationExists();

        ArgumentCaptor<KpiThresholdConfiguration> captor =
                ArgumentCaptor.forClass(KpiThresholdConfiguration.class);
        verify(thresholdRepository).save(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("aruba.inactiveApDaysThreshold");
        assertThat(captor.getValue().getValue()).isEqualTo(30);
        assertThat(kpiProperties.getTransversal().getGlobalStatus().getYellowMin()).isEqualTo(45);
    }

    @Test
    void keepsPersistedCitrixThresholds() {
        List<KpiThresholdConfiguration> stored = defaultThresholds().stream()
                .map(threshold -> {
                    if ("citrix.deliveryControllerYellowBelowPercent".equals(threshold.getConfigKey())) {
                        return threshold("citrix.deliveryControllerYellowBelowPercent", 50);
                    }
                    if ("citrix.failedLogonsYellowAbove".equals(threshold.getConfigKey())) {
                        return threshold("citrix.failedLogonsYellowAbove", 10);
                    }
                    if ("citrix.failedLogonsRedAbove".equals(threshold.getConfigKey())) {
                        return threshold("citrix.failedLogonsRedAbove", 30);
                    }
                    return threshold(threshold.getConfigKey(), threshold.getValue());
                })
                .toList();
        when(thresholdRepository.findAll()).thenReturn(stored);

        service.ensureDefaultConfigurationExists();

        assertThat(kpiProperties.getCitrix().getDeliveryControllerYellowBelowPercent()).isEqualTo(50);
        assertThat(kpiProperties.getCitrix().getFailedLogonsYellowAbove()).isEqualTo(10);
        assertThat(kpiProperties.getCitrix().getFailedLogonsRedAbove()).isEqualTo(30);
        assertThat(findStoredThreshold(stored, "citrix.deliveryControllerYellowBelowPercent").getValue()).isEqualTo(50);
        assertThat(findStoredThreshold(stored, "citrix.failedLogonsYellowAbove").getValue()).isEqualTo(10);
        assertThat(findStoredThreshold(stored, "citrix.failedLogonsRedAbove").getValue()).isEqualTo(30);
        verify(thresholdRepository, never()).save(any(KpiThresholdConfiguration.class));
    }

    @Test
    void keepsPersistedMicrosoft365Thresholds() {
        List<KpiThresholdConfiguration> stored = defaultThresholds().stream()
                .map(threshold -> {
                    if ("microsoft365.usersWithoutMfaRedAbove".equals(threshold.getConfigKey())) {
                        return threshold("microsoft365.usersWithoutMfaRedAbove", 3);
                    }
                    if ("microsoft365.nonCompliantDevicesYellowAbove".equals(threshold.getConfigKey())) {
                        return threshold("microsoft365.nonCompliantDevicesYellowAbove", 50);
                    }
                    if ("microsoft365.nonCompliantDevicesRedAbove".equals(threshold.getConfigKey())) {
                        return threshold("microsoft365.nonCompliantDevicesRedAbove", 100);
                    }
                    if ("microsoft365.devicesWithoutEncryptionRedAbove".equals(threshold.getConfigKey())) {
                        return threshold("microsoft365.devicesWithoutEncryptionRedAbove", 5);
                    }
                    return threshold(threshold.getConfigKey(), threshold.getValue());
                })
                .toList();
        when(thresholdRepository.findAll()).thenReturn(stored);

        service.ensureDefaultConfigurationExists();

        assertThat(kpiProperties.getMicrosoft365().getUsersWithoutMfaRedAbove()).isEqualTo(3);
        assertThat(kpiProperties.getMicrosoft365().getNonCompliantDevicesYellowAbove()).isEqualTo(50);
        assertThat(kpiProperties.getMicrosoft365().getNonCompliantDevicesRedAbove()).isEqualTo(100);
        assertThat(kpiProperties.getMicrosoft365().getDevicesWithoutEncryptionRedAbove()).isEqualTo(5);
        assertThat(findStoredThreshold(stored, "microsoft365.usersWithoutMfaRedAbove").getValue()).isEqualTo(3);
        assertThat(findStoredThreshold(stored, "microsoft365.nonCompliantDevicesYellowAbove").getValue()).isEqualTo(50);
        assertThat(findStoredThreshold(stored, "microsoft365.nonCompliantDevicesRedAbove").getValue()).isEqualTo(100);
        assertThat(findStoredThreshold(stored, "microsoft365.devicesWithoutEncryptionRedAbove").getValue()).isEqualTo(5);
        verify(thresholdRepository, never()).save(any(KpiThresholdConfiguration.class));
    }

    @Test
    void citrixCalculatorUsesThresholdsLoadedFromDatabase() {
        List<KpiThresholdConfiguration> stored = defaultThresholds().stream()
                .map(threshold -> {
                    if ("citrix.serverLoadYellowMin".equals(threshold.getConfigKey())) {
                        return threshold("citrix.serverLoadYellowMin", 50);
                    }
                    if ("citrix.serverLoadRedMin".equals(threshold.getConfigKey())) {
                        return threshold("citrix.serverLoadRedMin", 80);
                    }
                    if ("citrix.failedLogonsYellowAbove".equals(threshold.getConfigKey())) {
                        return threshold("citrix.failedLogonsYellowAbove", 10);
                    }
                    return threshold(threshold.getConfigKey(), threshold.getValue());
                })
                .toList();
        when(thresholdRepository.findAll()).thenReturn(stored);

        service.ensureDefaultConfigurationExists();

        CitrixAffectationCalculator.Result result =
                CitrixAffectationCalculator.calculate(
                        new CitrixAffectationCalculator.Input(
                                42,
                                580,
                                4,
                                4,
                                0,
                                20,
                                75,
                                8,
                                26),
                        kpiProperties);

        assertThat(result.percentage()).isEqualTo(15);
        assertThat(result.color()).isEqualTo("GREEN");
        assertThat(result.indicators())
                .anySatisfy(indicator -> {
                    assertThat(indicator.getName()).isEqualTo("Carga de servidores");
                    assertThat(indicator.getColor()).isEqualTo("YELLOW");
                })
                .anySatisfy(indicator -> {
                    assertThat(indicator.getName()).isEqualTo("Errores de inicio");
                    assertThat(indicator.getColor()).isEqualTo("GREEN");
                });
    }

    @Test
    void updateThresholdsStoresOnlyRequestedKeys() {
        ThresholdConfigurationDto request =
                new ThresholdConfigurationDto(List.of(
                        section(value("transversal.globalCriticality.yellowMin", 40))));

        service.updateThresholds(request);

        ArgumentCaptor<KpiThresholdConfiguration> captor =
                ArgumentCaptor.forClass(KpiThresholdConfiguration.class);
        verify(thresholdRepository).save(captor.capture());
        assertThat(captor.getValue().getValue()).isEqualTo(40);
        assertThat(kpiProperties.getTransversal().getGlobalCriticality().getYellowMin()).isEqualTo(40);
        assertThat(kpiProperties.getTransversal().getGlobalStatus().getYellowMin()).isEqualTo(34);
    }

    @Test
    void keepsCustomizedPlatformWeightsOnStartup() {
        when(weightRepository.findAll())
                .thenReturn(List.of(
                        weight("ARUBA", 50),
                        weight("CITRIX", 20),
                        weight("MICROSOFT365", 20),
                        weight("GLPI", 10)));

        service.ensureDefaultConfigurationExists();

        verify(weightRepository, never()).save(any(PlatformWeightConfiguration.class));
        assertThat(kpiProperties.getWeights().getGlobalStatus().getAruba())
                .isEqualTo(0.50);
    }

    @Test
    void exposesInactiveApDaysThresholdInArubaSection() {
        ThresholdConfigurationDto response = service.getThresholds();

        ThresholdValueDto threshold = findThreshold(response, "aruba.inactiveApDaysThreshold");

        assertThat(threshold.getLabel()).isEqualTo("APs inactivos");
        assertThat(threshold.getValue()).isEqualTo(30);
        assertThat(threshold.getDefaultValue()).isEqualTo(30);
        assertThat(threshold.getUnit()).isEqualTo("días");
    }

    @Test
    void updateThresholdsAppliesInactiveApDaysThreshold() {
        ThresholdConfigurationDto request =
                new ThresholdConfigurationDto(List.of(
                        section(value("aruba.inactiveApDaysThreshold", 60))));

        service.updateThresholds(request);

        ArgumentCaptor<KpiThresholdConfiguration> captor =
                ArgumentCaptor.forClass(KpiThresholdConfiguration.class);
        verify(thresholdRepository).save(captor.capture());
        assertThat(captor.getValue().getConfigKey()).isEqualTo("aruba.inactiveApDaysThreshold");
        assertThat(captor.getValue().getValue()).isEqualTo(60);
        assertThat(kpiProperties.getAruba().getInactiveApDaysThreshold()).isEqualTo(60);
    }

    @Test
    void resetRestoresDefaultThresholdsAndWeights() {
        when(thresholdRepository.findByConfigKey(anyString()))
                .thenAnswer(invocation -> Optional.of(threshold(invocation.getArgument(0), 999)));
        when(weightRepository.findByPlatform(anyString()))
                .thenAnswer(invocation -> Optional.of(weight(invocation.getArgument(0), 25)));

        service.resetConfiguration();

        assertThat(kpiProperties.getTransversal().getGlobalStatus().getYellowMin()).isEqualTo(34);
        assertThat(kpiProperties.getWeights().getGlobalStatus().getAruba())
                .isEqualTo(0.40);
        verify(weightRepository, times(4)).save(any(PlatformWeightConfiguration.class));
    }

    @Test
    void rejectsIncoherentGlobalStatusThresholds() {
        ThresholdConfigurationDto request =
                new ThresholdConfigurationDto(List.of(
                        section(
                                value("transversal.globalStatus.yellowMin", 70),
                                value("transversal.globalStatus.redMin", 60))));

        assertThatThrownBy(() -> service.updateThresholds(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("transversal.globalStatus");
    }

    @Test
    void rejectsPlatformWeightsThatDoNotSumOneHundred() {
        PlatformWeightsConfigurationDto request =
                new PlatformWeightsConfigurationDto(40, 30, 20, 20);

        assertThatThrownBy(() -> service.updatePlatformWeights(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("sumar 100");
    }

    @Test
    void repairsInvalidStoredThresholdsBeforeApplyingThem() {
        when(thresholdRepository.findAll())
                .thenReturn(defaultThresholdsWith("transversal.globalStatus.yellowMin", 80));

        service.ensureDefaultConfigurationExists();

        assertThat(kpiProperties.getTransversal().getGlobalStatus().getYellowMin()).isEqualTo(34);
        verify(thresholdRepository, atLeast(defaultThresholds().size())).save(any(KpiThresholdConfiguration.class));
    }

    @Test
    void addsMissingPlatformWeightWithoutOverwritingCustomValues() {
        when(weightRepository.findAll())
                .thenReturn(List.of(
                        weight("ARUBA", 50),
                        weight("CITRIX", 20),
                        weight("MICROSOFT365", 20)));

        service.ensureDefaultConfigurationExists();

        ArgumentCaptor<PlatformWeightConfiguration> captor =
                ArgumentCaptor.forClass(PlatformWeightConfiguration.class);
        verify(weightRepository).save(captor.capture());
        assertThat(captor.getValue().getPlatform()).isEqualTo("GLPI");
        assertThat(captor.getValue().getWeightPercent()).isEqualTo(10);
        assertThat(kpiProperties.getWeights().getGlobalStatus().getAruba())
                .isEqualTo(0.50);
    }

    @Test
    void repairsInvalidStoredPlatformWeightsBeforeApplyingThem() {
        when(weightRepository.findAll())
                .thenReturn(List.of(
                        weight("ARUBA", 40),
                        weight("CITRIX", 30),
                        weight("MICROSOFT365", 20),
                        weight("GLPI", 20)));

        service.ensureDefaultConfigurationExists();

        assertThat(kpiProperties.getWeights().getGlobalStatus().getAruba())
                .isEqualTo(0.40);
        verify(weightRepository, atLeast(4)).save(any(PlatformWeightConfiguration.class));
    }

    @Test
    void rejectsInactiveApDaysThresholdOutsideAcceptedRange() {
        ThresholdConfigurationDto request =
                new ThresholdConfigurationDto(List.of(
                        section(value("aruba.inactiveApDaysThreshold", 0))));

        assertThatThrownBy(() -> service.updateThresholds(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("aruba.inactiveApDaysThreshold");
    }

    private ThresholdSectionDto section(ThresholdValueDto... values) {
        return new ThresholdSectionDto(
                "transversal",
                "KPIs transversales",
                "Rangos",
                List.of(values));
    }

    private ThresholdValueDto value(String key, int value) {
        return new ThresholdValueDto(key, key, value, value, null, key);
    }

    private ThresholdValueDto findThreshold(ThresholdConfigurationDto response, String key) {
        return response.getSections().stream()
                .flatMap(section -> section.getValues().stream())
                .filter(value -> key.equals(value.getKey()))
                .findFirst()
                .orElseThrow();
    }

    private PlatformWeightConfiguration weight(String platform, int value) {
        PlatformWeightConfiguration weight =
                new PlatformWeightConfiguration();
        weight.setPlatform(platform);
        weight.setWeightPercent(value);
        weight.setDefaultWeightPercent(value);
        return weight;
    }

    private KpiThresholdConfiguration threshold(String key, int value) {
        KpiThresholdConfiguration threshold =
                new KpiThresholdConfiguration();
        threshold.setConfigKey(key);
        threshold.setSectionKey("test");
        threshold.setLabel(key);
        threshold.setValue(value);
        threshold.setDefaultValue(value);
        return threshold;
    }

    private KpiThresholdConfiguration findStoredThreshold(List<KpiThresholdConfiguration> thresholds, String key) {
        return thresholds.stream()
                .filter(threshold -> key.equals(threshold.getConfigKey()))
                .findFirst()
                .orElseThrow();
    }

    private List<KpiThresholdConfiguration> defaultThresholdsWith(String key, int value) {
        return defaultThresholds().stream()
                .map(threshold ->
                        key.equals(threshold.getConfigKey())
                                ? threshold(key, value)
                                : threshold(threshold.getConfigKey(), threshold.getValue()))
                .toList();
    }

    private List<KpiThresholdConfiguration> defaultThresholds() {
        try {
            Object thresholdSpecs = readPrivateField(service, "thresholdSpecs");
            List<?> specs = (List<?>) thresholdSpecs;
            List<KpiThresholdConfiguration> thresholds = new ArrayList<>();

            for (Object spec : specs) {
                thresholds.add(threshold(
                        invokeString(spec, "key"),
                        invokeInt(spec, "defaultValue")));
            }

            return thresholds;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("No se pudieron leer los umbrales por defecto", exception);
        }
    }

    private Object readPrivateField(Object target, String fieldName) throws ReflectiveOperationException {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private String invokeString(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(target);
    }

    private int invokeInt(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (Integer) method.invoke(target);
    }
}

