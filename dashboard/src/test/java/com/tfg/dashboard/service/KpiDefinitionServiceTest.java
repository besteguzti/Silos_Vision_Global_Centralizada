package com.tfg.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.KpiDefinitionDto;

class KpiDefinitionServiceTest {

    private final KpiDefinitionService service =
            new KpiDefinitionService(new KpiProperties());

    @Test
    void returnsDefinitionsForPlatformGlobalAndAnalysisKpis() {

        var definitions =
                service.getDefinitions();

        assertThat(definitions)
                .hasSize(31);

        assertThat(definitions)
                .extracting("id")
                .contains(
                        "aruba_network_status",
                        "global_status",
                        "glpi_operational_pressure",
                        "technical_platform_relations",
                        "aruba_affectation_vs_wifi_clients",
                        "aruba_wifi_clients_vs_citrix_sessions",
                        "aruba_wifi_clients_vs_microsoft365_active_users",
                        "citrix_affectation_vs_citrix_tickets",
                        "citrix_delivery_controllers_vs_failed_logons",
                        "microsoft365_affectation_vs_microsoft365_tickets",
                        "aruba_down_switches_vs_down_aps",
                        "microsoft365_active_users_vs_citrix_sessions",
                        "glpi_pressure_vs_open_tickets",
                        "period_temporal_evolution"
                );
    }

    @Test
    void doesNotExposeObsoleteAnalysisDefinitions() {

        assertThat(service.getDefinitions())
                .extracting(KpiDefinitionDto::getId)
                .doesNotContain(
                        "aruba_glpi_relation",
                        "citrix_glpi_relation",
                        "microsoft365_glpi_relation",
                        "technical_operational_conversion",
                        "high_high_cooccurrence"
                );
    }

    @Test
    void globalStatusDefinitionIncludesFormulaThresholdsAndSources() {

        var globalStatus =
                service.getDefinitions().stream()
                        .filter(definition -> "global_status".equals(
                                definition.getId()
                        ))
                        .findFirst()
                        .orElseThrow();

        assertThat(globalStatus.getFormula())
                .contains("Índice de salud Aruba * 0.40");
        assertThat(globalStatus.getThresholds().getGreen())
                .isEqualTo("0-33");
        assertThat(globalStatus.getSources())
                .contains("Citrix indice de salud Citrix");
    }

    @Test
    void globalStatusDefinitionUsesConfiguredWeights() {

        KpiProperties properties =
                new KpiProperties();
        properties.getWeights().setGlobalStatus(
                new KpiProperties.PlatformWeights(
                        0.41,
                        0.29,
                        0.21,
                        0.09
                )
        );

        KpiDefinitionDto globalStatus =
                findDefinition(
                        new KpiDefinitionService(properties),
                        "global_status"
                );

        assertThat(globalStatus.getFormula())
                .contains(
                        "Índice de salud Aruba * 0.41",
                        "Citrix indice de salud * 0.29",
                        "Microsoft 365 indice de salud * 0.21",
                        "GLPI indice de salud * 0.09"
                );
    }

    @Test
    void globalStatusDefinitionUsesConfiguredTransversalThresholds() {

        KpiProperties properties =
                new KpiProperties();
        properties.getTransversal().getGlobalStatus().setYellowMin(40);
        properties.getTransversal().getGlobalStatus().setRedMin(70);
        properties.getStatus().setMax(120);

        KpiDefinitionDto globalStatus =
                findDefinition(
                        new KpiDefinitionService(properties),
                        "global_status"
                );

        assertThat(globalStatus.getThresholds().getGreen())
                .isEqualTo("0-39");
        assertThat(globalStatus.getThresholds().getYellow())
                .isEqualTo("40-69");
        assertThat(globalStatus.getThresholds().getRed())
                .isEqualTo("70-120");
    }

    @Test
    void doesNotExposeOldMicrosoftOperationalRiskDefinition() {

        assertThat(service.getDefinitions())
                .allSatisfy(definition -> {
                    assertThat(definition.getId().toLowerCase())
                            .doesNotContain("operational_risk");
                    assertThat(definition.getName().toLowerCase())
                            .doesNotContain("riesgo operativo microsoft");
                    assertThat(definition.getFormula().toLowerCase())
                            .doesNotContain("riesgo operativo microsoft");
                });
    }

    @Test
    void microsoft365DefinitionUsesConfiguredThresholds() {

        KpiDefinitionDto microsoft365 =
                findDefinition(service, "microsoft365_health");

        assertThat(microsoft365.getFormula())
                .contains(
                        "Suma de afecciones parciales limitada a 100",
                        "SharePoint: >= 90% rojo, >= 80% amarillo",
                        "Usuarios sin MFA: > 4 rojo, > 0 amarillo",
                        "Equipos no conformes: > 50 rojo, > 30 amarillo",
                        "Equipos sin cifrado: > 0 rojo"
                );
    }

    @Test
    void citrixDefinitionUsesConfiguredServerLoadThresholdsAndTopStatusDescription() {

        KpiDefinitionDto citrix =
                findDefinition(service, "citrix_health");

        assertThat(citrix.getFormula())
                .contains(
                        "estado superior se calcula con los rangos generales de afeccion",
                        "Carga: >= 90% rojo, >= 80% amarillo"
                )
                .doesNotContain("peor indicador interno");
    }

    @Test
    void documentsCurrentAnalysisRelationsAndTemporalEvolution() {

        assertThat(service.getDefinitions())
                .extracting(KpiDefinitionDto::getId)
                .contains(
                        "aruba_affectation_vs_wifi_clients",
                        "aruba_affectation_vs_aruba_tickets",
                        "citrix_affectation_vs_citrix_tickets",
                        "microsoft365_affectation_vs_microsoft365_tickets",
                        "aruba_wifi_clients_vs_citrix_sessions",
                        "aruba_wifi_clients_vs_microsoft365_active_users",
                        "technical_platform_relations",
                        "citrix_delivery_controllers_vs_failed_logons",
                        "citrix_delivery_controllers_vs_sessions",
                        "glpi_pressure_vs_open_tickets",
                        "glpi_pressure_vs_operational_backlog",
                        "aruba_down_switches_vs_down_aps",
                        "glpi_created_vs_closed_tickets",
                        "microsoft365_active_users_vs_citrix_sessions",
                        "period_temporal_evolution"
                );

        KpiDefinitionDto relation =
                findDefinition(service, "aruba_affectation_vs_wifi_clients");
        assertThat(relation.getType())
                .isEqualTo("ANALYSIS_RELATION");
        assertThat(relation.getFormula())
                .contains("Eje X: afeccion Aruba", "Eje Y: clientes WiFi Aruba");
        assertThat(relation.getThresholds().getGreen())
                .contains("Relacion baja");

        assertThat(findDefinition(service, "aruba_wifi_clients_vs_citrix_sessions")
                .getFormula())
                .contains("Eje X: clientes WiFi Aruba", "Eje Y: sesiones Citrix");
        assertThat(findDefinition(service, "aruba_wifi_clients_vs_microsoft365_active_users")
                .getFormula())
                .contains("Eje X: clientes WiFi Aruba", "Eje Y: usuarios activos Microsoft 365");
        assertThat(findDefinition(service, "citrix_delivery_controllers_vs_failed_logons")
                .getFormula())
                .contains("Eje X: Delivery Controllers disponibles", "Eje Y: errores de inicio Citrix");
        assertThat(findDefinition(service, "aruba_down_switches_vs_down_aps")
                .getFormula())
                .contains("Eje X: switches apagados", "Eje Y: APs caidos");
        assertThat(findDefinition(service, "microsoft365_active_users_vs_citrix_sessions")
                .getFormula())
                .contains("Eje X: usuarios activos Microsoft 365", "Eje Y: sesiones Citrix");

        KpiDefinitionDto timeline =
                findDefinition(service, "period_temporal_evolution");
        assertThat(timeline.getFormula())
                .contains(
                        "snapshots diarios",
                        "degradacion tecnica",
                        "presion GLPI",
                        "impacto en usuarios",
                        "afeccion Aruba",
                        "estado global"
                );
    }

    private KpiDefinitionDto findDefinition(
            KpiDefinitionService definitionService,
            String id
    ) {

        return definitionService.getDefinitions().stream()
                .filter(definition -> id.equals(definition.getId()))
                .findFirst()
                .orElseThrow();
    }
}
