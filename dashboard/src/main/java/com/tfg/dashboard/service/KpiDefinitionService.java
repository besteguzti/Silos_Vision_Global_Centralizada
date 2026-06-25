package com.tfg.dashboard.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tfg.dashboard.config.properties.KpiProperties;
import com.tfg.dashboard.dto.KpiDefinitionDto;
import com.tfg.dashboard.dto.KpiThresholdDto;

/**
 * Proporciona definiciones documentales de los KPIs expuestos por el backend.
 *
 * No calcula valores en tiempo real: explica fórmulas, fuentes y umbrales para
 * el endpoint /api/kpis/definitions, alineando los textos con KpiProperties
 * cuando las reglas están configuradas.
 */
@Service
public class KpiDefinitionService {

    private final KpiProperties kpiProperties;

    public KpiDefinitionService(KpiProperties kpiProperties) {

        this.kpiProperties = kpiProperties;
    }

    /**
     * Devuelve el catálogo de definiciones de KPIs de plataforma, dashboard y
     * análisis.
     */
    public List<KpiDefinitionDto> getDefinitions() {

        return List.of(
                definition(
                        "aruba_network_status",
                        "Índice de salud Aruba",
                        "PLATFORM",
                        "ARUBA",
                        "Resume el índice de salud Aruba usando Access Points, switches, clientes WiFi y tickets asociados.",
                        arubaNetworkStatusFormula(),
                        commonAffectionThresholds(),
                        List.of(
                                "Access Points totales, caidos, inactivos y firmware pendiente",
                                "Clientes WiFi totales, MUTUALIA-APs y MUTUALIA-WIFI",
                                "Switches totales, caidos y firmware pendiente"
                        )
                ),
                definition(
                        "citrix_health",
                        "Citrix indice de salud Citrix",
                        "PLATFORM",
                        "CITRIX",
                        "Reúne las señales principales de Citrix: sesiones, Delivery Controllers, logon, carga y errores.",
                        citrixHealthFormula(),
                        commonAffectionThresholds(),
                        List.of(
                                "Sesiones activas",
                                "Delivery Controllers disponibles",
                                "Average Logon Duration",
                                "Carga de servidores",
                                "Errores de inicio"
                        )
                ),
                definition(
                        "microsoft365_health",
                        "Microsoft 365 indice de salud Microsoft 365",
                        "PLATFORM",
                        "MICROSOFT365",
                        "Agrupa señales de capacidad, identidad, seguridad, dispositivos y tickets de Microsoft 365.",
                        microsoft365HealthFormula(),
                        commonAffectionThresholds(),
                        List.of(
                                "Licencias no asignadas",
                                "Estado de Outlook, Teams y SharePoint",
                                "Buzones casi llenos",
                                "Emails en cuarentena",
                                "Almacenamiento SharePoint",
                                "Usuarios en riesgo e inicios fallidos",
                                "Usuarios sin MFA",
                                "Secretos proximos a caducar",
                                "Aplicaciones sin uso y con permisos elevados",
                                "Equipos no conformes",
                                "Tickets abiertos Microsoft 365",
                                "Windows desactualizados",
                                "Equipos sin cifrado y sin check-in"
                        )
                ),
                definition(
                        "glpi_health",
                        "GLPI indice de salud GLPI",
                        "PLATFORM",
                        "GLPI",
                        "Resume la situación de soporte en GLPI usando volumen de tickets, SLA y capacidad de cierre.",
                        glpiHealthFormula(),
                        commonAffectionThresholds(),
                        List.of(
                                "Tickets abiertos",
                                "Tickets críticos abiertos",
                                "Tickets vencidos SLA",
                                "Porcentaje de tickets cerrados",
                                "Porcentaje de tickets cerrados semana"
                        )
                ),
                definition(
                        "global_status",
                        "Estado global",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Resume el estado conjunto de Aruba, Citrix, Microsoft 365 y GLPI con los pesos configurados.",
                        platformWeightFormula(
                                "Índice de salud Aruba",
                                "Citrix indice de salud",
                                "Microsoft 365 indice de salud",
                                "GLPI indice de salud",
                                kpiProperties.getWeights().getGlobalStatus()),
                        transversalRiskThresholds("transversal.globalStatus"),
                        platformHealthSources()
                ),
                definition(
                        "global_criticality",
                        "Criticidad global",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Recoge cuántas señales críticas aparecen dentro de las plataformas.",
                        "Media de indicadores críticos normalizados: correcto 0, advertencia 50 y critico 100.",
                        transversalRiskThresholds("transversal.globalCriticality"),
                        List.of(
                                "Condiciones críticas Aruba",
                                "Condiciones críticas Citrix",
                                "Condiciones críticas Microsoft 365",
                                "Condiciones críticas GLPI"
                        )
                ),
                definition(
                        "global_availability",
                        "Disponibilidad global",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Resume la disponibilidad estimada de los servicios principales.",
                        platformWeightFormula(
                                "Aruba disponibilidad",
                                "Citrix disponibilidad",
                                "Microsoft 365 disponibilidad",
                                "GLPI disponibilidad",
                                kpiProperties.getWeights().getAvailability()),
                        transversalHealthThresholds("transversal.globalAvailability"),
                        List.of(
                                "Disponibilidad APs y switches Aruba",
                                "Sesiones activas y Delivery Controllers Citrix",
                                "SharePoint y secretos Microsoft 365",
                                "GLPI como soporte operativo"
                        )
                ),
                definition(
                        "operational_pressure",
                        "Presión operativa",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Resume la carga técnica y operativa acumulada.",
                        platformWeightFormula(
                                "Aruba",
                                "Citrix",
                                "Microsoft 365",
                                "GLPI",
                                kpiProperties.getWeights().getOperationalPressure()),
                        transversalRiskThresholds("transversal.operationalPressure"),
                        List.of(
                                "Tickets GLPI",
                                "Errores y carga Citrix",
                                "Dispositivos Microsoft 365",
                                "APs inactivos y firmware Aruba"
                        )
                ),
                definition(
                        "technical_degradation",
                        "Degradación técnica",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Detecta deterioro técnico aunque no exista una caída total.",
                        platformWeightFormula(
                                "Aruba",
                                "Citrix",
                                "Microsoft 365",
                                "GLPI",
                                kpiProperties.getWeights().getTechnicalDegradation()),
                        transversalRiskThresholds("transversal.technicalDegradation"),
                        List.of(
                                "Firmware e inactividad Aruba",
                                "Logon, carga y errores Citrix",
                                "SharePoint, secretos y dispositivos Microsoft 365",
                                "Tickets críticos GLPI"
                        )
                ),
                definition(
                        "sla_risk",
                        "Riesgo SLA",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Estima si hay señales que puedan comprometer niveles de servicio.",
                        platformWeightFormula(
                                "Aruba",
                                "Citrix",
                                "Microsoft 365",
                                "GLPI",
                                kpiProperties.getWeights().getSlaRisk()),
                        transversalRiskThresholds("transversal.slaRisk"),
                        List.of(
                                "Logon, sesiones, Delivery Controllers y errores Citrix",
                                "Índice de salud Aruba",
                                "Tickets SLA vencidos, tickets críticos y cierre GLPI",
                                "SharePoint, secretos y equipos Microsoft 365"
                        )
                ),
                definition(
                        "operational_backlog",
                        "Backlog operativo",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Resume el trabajo pendiente acumulado.",
                        platformWeightFormula(
                                "Aruba",
                                "Citrix",
                                "Microsoft 365",
                                "GLPI",
                                kpiProperties.getWeights().getOperationalBacklog()),
                        transversalRiskThresholds("transversal.operationalBacklog"),
                        List.of(
                                "Tickets GLPI",
                                "Equipos Microsoft 365",
                                "Firmware Aruba",
                                "Errores Citrix"
                        )
                ),
                definition(
                        "user_impact",
                        "Impacto en usuarios",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Aproxima la afección que pueden percibir los usuarios.",
                        platformWeightFormula(
                                "Aruba",
                                "Citrix",
                                "Microsoft 365",
                                "GLPI",
                                kpiProperties.getWeights().getUserImpact()),
                        transversalRiskThresholds("transversal.userImpact"),
                        List.of(
                                "Clientes WiFi, APs y switches Aruba",
                                "Sesiones, logon, errores y Delivery Controllers Citrix",
                                "SharePoint, MFA y dispositivos Microsoft 365",
                                "Tickets abiertos y críticos GLPI"
                        )
                ),
                definition(
                        "affected_services",
                        "Servicios afectados",
                        "TRANSVERSAL",
                        "GLOBAL",
                        "Indica cuántas plataformas están afectadas a la vez.",
                        affectedServicesFormula(),
                        transversalRiskThresholds("transversal.affectedServices"),
                        platformHealthSources()
                ),
                definition(
                        "glpi_operational_pressure",
                        "Presión operativa GLPI",
                        "ANALYSIS",
                        "GLPI",
                        "Describe la presión de GLPI como reflejo del trabajo que llega a soporte.",
                        glpiOperationalPressureFormula(),
                        transversalRiskThresholds("transversal.operationalPressure"),
                        List.of(
                                "Tickets abiertos",
                                "Tickets críticos abiertos",
                                "Porcentaje de cierre diario",
                                "Porcentaje de cierre semanal"
                        )
                ),
                definition(
                        "technical_platform_relations",
                        "Relacion tecnica aparente entre plataformas",
                        "ANALYSIS",
                        "GLOBAL",
                        "Documenta la tabla actual que compara afeccion entre plataformas tecnicas.",
                        "Para cada par origen/destino calcula co-ocurrencia tecnica e incremento medio del destino cuando el origen esta afectado.",
                        relationReadingThresholds(),
                        List.of(
                                "Índice de salud Aruba",
                                "Citrix indice de salud Citrix",
                                "Microsoft 365 indice de salud Microsoft 365",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                definition(
                        "analysis_technical_degradation",
                        "Degradación técnica",
                        "ANALYSIS",
                        "GLOBAL",
                        "Agrupa la afección técnica de Aruba, Citrix y Microsoft 365 para el panel de análisis.",
                        platformWeightFormula(
                                "Índice de salud Aruba",
                                "Citrix indice de salud",
                                "Microsoft 365 indice de salud",
                                "GLPI",
                                kpiProperties.getWeights().getAnalysisTechnicalDegradation()),
                        transversalRiskThresholds("transversal.technicalDegradation"),
                        List.of(
                                "Índice de salud Aruba",
                                "Citrix indice de salud Citrix",
                                "Microsoft 365 indice de salud Microsoft 365"
                        )
                ),
                definition(
                        "analysis_user_impact",
                        "Impacto en usuarios",
                        "ANALYSIS",
                        "GLOBAL",
                        "Compara degradación técnica con señales que podrían notar los usuarios.",
                        platformWeightFormula(
                                "Aruba impacto usuario",
                                "Citrix impacto usuario",
                                "Microsoft 365 impacto usuario",
                                "presión GLPI",
                                kpiProperties.getWeights().getAnalysisUserImpact()),
                        transversalRiskThresholds("transversal.userImpact"),
                        List.of(
                                "Impacto usuario Aruba",
                                "Impacto usuario Citrix",
                                "Impacto usuario Microsoft 365",
                                "Presión operativa GLPI"
                        )
                ),
                specificRelationDefinition(
                        "aruba_affectation_vs_wifi_clients",
                        "Afectacion Aruba vs clientes WiFi",
                        "Compara la afeccion Aruba con el volumen de clientes WiFi conectados.",
                        "Eje X: afeccion Aruba (%). Eje Y: clientes WiFi Aruba. Lectura inversa: se revisan los dias donde sube la afeccion y baja el uso WiFi.",
                        List.of(
                                "Afeccion Aruba",
                                "Clientes WiFi Aruba",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "aruba_affectation_vs_aruba_tickets",
                        "Afectacion Aruba vs tickets Aruba",
                        "Relaciona la afeccion de red Aruba con tickets GLPI clasificados como Aruba.",
                        "Eje X: afeccion Aruba (%). Eje Y: tickets abiertos Aruba. Ayuda a ver coincidencias entre degradacion de red y trabajo de soporte asociado.",
                        List.of(
                                "Afeccion Aruba",
                                "Tickets abiertos Aruba",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "citrix_affectation_vs_citrix_tickets",
                        "Afectacion Citrix vs tickets Citrix",
                        "Relaciona la afeccion Citrix con tickets GLPI clasificados como Citrix.",
                        "Eje X: afeccion Citrix (%). Eje Y: tickets abiertos Citrix. Sirve para revisar si los problemas de acceso coinciden con mayor carga de soporte.",
                        List.of(
                                "Afeccion Citrix",
                                "Tickets abiertos Citrix",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "microsoft365_affectation_vs_microsoft365_tickets",
                        "Afectacion Microsoft 365 vs tickets Microsoft 365",
                        "Relaciona la afeccion Microsoft 365 con tickets GLPI clasificados como Microsoft 365.",
                        "Eje X: afeccion Microsoft 365 (%). Eje Y: tickets abiertos Microsoft 365. Orienta la revision de identidad, cloud y puesto de usuario.",
                        List.of(
                                "Afeccion Microsoft 365",
                                "Tickets abiertos Microsoft 365",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "aruba_wifi_clients_vs_citrix_sessions",
                        "Clientes WiFi Aruba vs sesiones Citrix",
                        "Compara el volumen de clientes WiFi con las sesiones activas de Citrix para ver si una caida de conectividad coincide con menor acceso a aplicaciones.",
                        "Eje X: clientes WiFi Aruba. Eje Y: sesiones Citrix. Si bajan los clientes WiFi y tambien bajan las sesiones Citrix, puede existir una relacion operativa entre conectividad y acceso a Citrix.",
                        List.of(
                                "Clientes WiFi Aruba",
                                "Sesiones activas Citrix",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "aruba_wifi_clients_vs_microsoft365_active_users",
                        "Clientes WiFi Aruba vs usuarios activos Microsoft 365",
                        "Relaciona clientes WiFi con usuarios activos en Microsoft 365 para comprobar si una incidencia de red reduce la actividad cloud observada.",
                        "Eje X: clientes WiFi Aruba. Eje Y: usuarios activos Microsoft 365. Una bajada simultanea puede orientar la revision de conectividad y uso de servicios cloud.",
                        List.of(
                                "Clientes WiFi Aruba",
                                "Usuarios activos Microsoft 365",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "citrix_delivery_controllers_vs_failed_logons",
                        "Delivery Controllers disponibles vs errores de inicio Citrix",
                        "Compara la disponibilidad de Delivery Controllers con los errores de inicio de sesion en Citrix.",
                        "Eje X: Delivery Controllers disponibles. Eje Y: errores de inicio Citrix. Si disminuyen los Delivery Controllers disponibles y aumentan los errores de inicio, conviene revisar la capa de acceso Citrix.",
                        List.of(
                                "Delivery Controllers disponibles",
                                "Errores de inicio Citrix",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "citrix_delivery_controllers_vs_sessions",
                        "Delivery Controllers disponibles vs sesiones Citrix",
                        "Compara la disponibilidad de Delivery Controllers con las sesiones activas Citrix.",
                        "Eje X: Delivery Controllers disponibles. Eje Y: sesiones Citrix. Ayuda a interpretar capacidad y uso sin afirmar causalidad.",
                        List.of(
                                "Delivery Controllers disponibles",
                                "Sesiones activas Citrix",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "glpi_pressure_vs_operational_backlog",
                        "Presion operativa GLPI vs backlog operativo",
                        "Compara la presion operativa GLPI con el trabajo pendiente acumulado.",
                        "Eje X: presion operativa GLPI (%). Eje Y: backlog operativo. Ayuda a separar carga puntual de acumulacion real de trabajo.",
                        List.of(
                                "Presion operativa GLPI",
                                "Backlog operativo",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "glpi_pressure_vs_open_tickets",
                        "Presion operativa GLPI vs tickets abiertos GLPI",
                        "Relaciona la presion operativa GLPI con el volumen total de tickets abiertos.",
                        "Eje X: presion operativa GLPI (%). Eje Y: tickets abiertos GLPI. Permite comprobar si la presion sube junto al volumen de entrada pendiente.",
                        List.of(
                                "Presion operativa GLPI",
                                "Tickets abiertos GLPI",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "aruba_down_switches_vs_down_aps",
                        "Switches apagados vs APs caidos",
                        "Relaciona switches apagados con APs caidos para comprobar si una incidencia de switching coincide con perdida de puntos de acceso.",
                        "Eje X: switches apagados. Eje Y: APs caidos. Una subida conjunta puede indicar que el problema afecta a conectividad de acceso o alimentacion de APs.",
                        List.of(
                                "Switches apagados",
                                "APs caidos",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "glpi_created_vs_closed_tickets",
                        "Tickets creados GLPI vs tickets cerrados GLPI",
                        "Compara tickets creados con tickets cerrados para ver si soporte absorbe la entrada de trabajo.",
                        "Eje X: tickets creados GLPI. Eje Y: tickets cerrados GLPI. Si los creados superan a los cerrados de forma persistente, puede crecer el backlog.",
                        List.of(
                                "Tickets creados GLPI",
                                "Tickets cerrados GLPI",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                specificRelationDefinition(
                        "microsoft365_active_users_vs_citrix_sessions",
                        "Usuarios activos Microsoft 365 vs sesiones Citrix",
                        "Compara la actividad de Microsoft 365 con las sesiones Citrix para diferenciar una caida especifica de Citrix de una caida general de actividad.",
                        "Eje X: usuarios activos Microsoft 365. Eje Y: sesiones Citrix. Si Microsoft 365 mantiene usuarios activos pero Citrix no tiene sesiones, el problema apunta mas a Citrix que a una caida general de conectividad o servicios cloud.",
                        List.of(
                                "Usuarios activos Microsoft 365",
                                "Sesiones activas Citrix",
                                "Snapshots diarios del panel de analisis"
                        )
                ),
                definition(
                        "period_temporal_evolution",
                        "Evolucion temporal del periodo",
                        "ANALYSIS",
                        "GLOBAL",
                        "Muestra como evolucionan los indicadores principales durante el periodo seleccionado.",
                        "Serie temporal basada en snapshots diarios para 30 o 90 dias: degradacion tecnica, presion GLPI, impacto en usuarios, afeccion Aruba, afeccion Citrix y estado global.",
                        commonAffectionThresholds(),
                        List.of(
                                "Snapshots diarios del panel de analisis",
                                "Degradacion tecnica",
                                "Presion operativa GLPI",
                                "Impacto en usuarios",
                                "Afeccion Aruba",
                                "Afeccion Citrix",
                                "Estado global"
                        )
                )
        );
    }

    private KpiDefinitionDto specificRelationDefinition(
            String id,
            String name,
            String description,
            String formula,
            List<String> sources
    ) {

        return definition(
                id,
                name,
                "ANALYSIS_RELATION",
                "GLOBAL",
                description + " La grafica es exploratoria y no demuestra causalidad.",
                formula,
                relationReadingThresholds(),
                sources
        );
    }

    private KpiDefinitionDto definition(
            String id,
            String name,
            String type,
            String platform,
            String description,
            String formula,
            KpiThresholdDto thresholds,
            List<String> sources
    ) {

        return new KpiDefinitionDto(
                id,
                name,
                type,
                platform,
                description,
                formula,
                thresholds,
                sources
        );
    }

    private KpiThresholdDto relationReadingThresholds() {

        return new KpiThresholdDto(
                "Relacion baja o sin datos suficientes",
                "Coincidencia moderada con variacion suficiente",
                "Coincidencia alta con variacion suficiente"
        );
    }

    private List<String> platformHealthSources() {

        return List.of(
                "Índice de salud Aruba",
                "Citrix indice de salud Citrix",
                "Microsoft 365 indice de salud Microsoft 365",
                "GLPI indice de salud GLPI"
        );
    }

    private String platformWeightFormula(
            String arubaLabel,
            String citrixLabel,
            String microsoft365Label,
            String glpiLabel,
            KpiProperties.PlatformWeights weights
    ) {

        List<String> parts = new java.util.ArrayList<>();

        addWeightedPart(parts, arubaLabel, weights.getAruba());
        addWeightedPart(parts, citrixLabel, weights.getCitrix());
        addWeightedPart(parts, microsoft365Label, weights.getMicrosoft365());
        addWeightedPart(parts, glpiLabel, weights.getGlpi());

        return String.join(" + ", parts);
    }

    private void addWeightedPart(List<String> parts, String label, double weight) {

        if (weight == 0) {
            return;
        }

        parts.add(label + " * " + kpiProperties.formatWeight(weight));
    }

    private String glpiOperationalPressureFormula() {

        KpiProperties.GlpiPressureWeights weights =
                kpiProperties.getWeights().getGlpiOperationalPressure();

        return "Tickets abiertos * " + kpiProperties.formatWeight(weights.getOpenTickets())
                + " + cierre diario * " + kpiProperties.formatWeight(weights.getClosedTodayPercent())
                + " + tickets críticos * " + kpiProperties.formatWeight(weights.getCriticalTickets())
                + " + cierre semanal * " + kpiProperties.formatWeight(weights.getClosedWeekPercent());
    }

    private String affectedServicesFormula() {

        int platformContribution =
                kpiProperties.getStatus().getMax() / platformHealthSources().size();

        return "Cada plataforma en amarillo o rojo suma " + platformContribution
                + "%: Aruba, Citrix, Microsoft 365 y GLPI.";
    }

    private String arubaNetworkStatusFormula() {

        return "Suma de afecciones parciales con limite 100: tickets Aruba desde "
                + kpiProperties.getAruba().getArubaOpenTicketsYellowMin()
                + " amarillo y desde "
                + kpiProperties.getAruba().getArubaOpenTicketsRedMin()
                + " rojo; APs caidos amarillo desde el "
                + kpiProperties.getAruba().getAccessPointDownYellowPercent()
                + "% y rojo desde el "
                + kpiProperties.getAruba().getAccessPointDownRedPercent()
                + "%; firmware AP amarillo desde "
                + kpiProperties.getAruba().getPendingFirmwareApsYellowMin()
                + "; APs inactivos amarillo desde "
                + kpiProperties.getAruba().getInactiveApsYellowMin()
                + "; clientes WiFi, Mutualia-APS y Mutualia-WIFI rojo si no hay clientes; switches apagados amarillo si son mas de "
                + kpiProperties.getAruba().getSwitchDownYellowAbove()
                + " y rojo si todos los switches estan caidos"
                + "; switches con upgrade pendiente amarillo desde "
                + kpiProperties.getAruba().getSwitchUpgradeYellowMin()
                + "; switches infrautilizados amarillo si son mas de "
                + kpiProperties.getAruba().getUnderusedSwitchesYellowAbove()
                + " y rojo si son mas de "
                + kpiProperties.getAruba().getUnderusedSwitchesRedAbove()
                + ".";
    }

    private String citrixHealthFormula() {

        return "Suma de afecciones parciales limitada a 100. El porcentaje mide intensidad y el estado superior se calcula con los rangos generales de afeccion. "
                + "Sesiones activas a 0 y Delivery Controllers a 0 representan afeccion maxima. "
                + "Delivery Controllers: 0-33% disponibles rojo, 34-"
                + (kpiProperties.getCitrix().getDeliveryControllerYellowBelowPercent() - 1)
                + "% amarillo, "
                + kpiProperties.getCitrix().getDeliveryControllerYellowBelowPercent()
                + "-100% verde. Logon: > "
                + kpiProperties.getCitrix().getLogonDurationRedAboveSeconds()
                + "s rojo, > "
                + kpiProperties.getCitrix().getLogonDurationYellowAboveSeconds()
                + "s amarillo. Carga: >= "
                + kpiProperties.getCitrix().getServerLoadRedMin()
                + "% rojo, >= "
                + kpiProperties.getCitrix().getServerLoadYellowMin()
                + "% amarillo; la carga roja aporta afeccion alta proporcional, no 100% automatico. Errores: > "
                + kpiProperties.getCitrix().getFailedLogonsRedAbove()
                + " rojo, > "
                + kpiProperties.getCitrix().getFailedLogonsYellowAbove()
                + " amarillo.";
    }

    private String microsoft365HealthFormula() {

        return "Suma de afecciones parciales limitada a 100. "
                + "Licencias no asignadas: menos de "
                + kpiProperties.getMicrosoft365().getUnassignedLicensesYellowBelow()
                + " amarillo, igual o menor que "
                + kpiProperties.getMicrosoft365().getUnassignedLicensesRedBelowOrEqual()
                + " rojo. Servicios Outlook/Teams/SharePoint: degradado amarillo e incidencia rojo. "
                + "Buzones casi llenos y emails en cuarentena aportan amarillo si existen. SharePoint: >= "
                + kpiProperties.getMicrosoft365().getSharePointRedAbove()
                + "% rojo, >= "
                + kpiProperties.getMicrosoft365().getSharePointYellowMin()
                + "% amarillo. Usuarios en riesgo: > "
                + kpiProperties.getMicrosoft365().getRiskyUsersRedAbove()
                + " rojo, > "
                + kpiProperties.getMicrosoft365().getRiskyUsersYellowAbove()
                + " amarillo. Inicios fallidos: >= "
                + kpiProperties.getMicrosoft365().getFailedSignInsRedMin()
                + " rojo, >= "
                + kpiProperties.getMicrosoft365().getFailedSignInsYellowMin()
                + " amarillo. Usuarios sin MFA: > "
                + kpiProperties.getMicrosoft365().getUsersWithoutMfaRedAbove()
                + " rojo, > "
                + kpiProperties.getMicrosoft365().getUsersWithoutMfaYellowAbove()
                + " amarillo. Secretos proximos a caducar: > "
                + kpiProperties.getMicrosoft365().getSecretsYellowAbove()
                + " amarillo. Aplicaciones sin uso y permisos elevados: si existen, amarillo. Equipos no conformes: > "
                + kpiProperties.getMicrosoft365().getNonCompliantDevicesRedAbove()
                + " rojo, > "
                + kpiProperties.getMicrosoft365().getNonCompliantDevicesYellowAbove()
                + " amarillo. Tickets Microsoft 365: >= "
                + kpiProperties.getMicrosoft365().getMicrosoft365OpenTicketsRedMin()
                + " rojo, >= "
                + kpiProperties.getMicrosoft365().getMicrosoft365OpenTicketsYellowMin()
                + " amarillo. Windows desactualizados: > "
                + kpiProperties.getMicrosoft365().getOutdatedWindowsYellowAbove()
                + " amarillo. Equipos sin cifrado: > "
                + kpiProperties.getMicrosoft365().getDevicesWithoutEncryptionRedAbove()
                + " rojo. Sin check-in >90 dias: > "
                + kpiProperties.getMicrosoft365().getStaleDevicesRedAbove()
                + " rojo.";
    }

    private String glpiHealthFormula() {

        return "Media uniforme de tickets abiertos, tickets críticos, tickets vencidos SLA, porcentaje de cierre diario y porcentaje de cierre semanal. "
                + "Tickets abiertos: >= "
                + kpiProperties.getGlpi().getOpenTicketsRedMin()
                + " rojo, >= "
                + kpiProperties.getGlpi().getOpenTicketsYellowMin()
                + " amarillo. Tickets críticos: > "
                + kpiProperties.getGlpi().getCriticalTicketsRedAbove()
                + " rojo, > "
                + kpiProperties.getGlpi().getCriticalTicketsYellowAbove()
                + " amarillo. Tickets vencidos SLA: > "
                + kpiProperties.getGlpi().getSlaBreachedTicketsRedAbove()
                + " rojo, > "
                + kpiProperties.getGlpi().getSlaBreachedTicketsYellowAbove()
                + " amarillo. Porcentaje de cierre diario/semanal: >= "
                + kpiProperties.getGlpi().getClosedPercentGreenMin()
                + "% verde, por debajo amarillo.";
    }

    private KpiThresholdDto transversalRiskThresholds(String metricKey) {

        KpiProperties.TransversalKpiThreshold threshold =
                kpiProperties.getTransversal().thresholdFor(metricKey);
        int yellowMin = threshold.getYellowMin();
        int redMin = threshold.getRedMin();
        int max = kpiProperties.getStatus().getMax();

        return new KpiThresholdDto(
                "0-" + (yellowMin - 1),
                yellowMin + "-" + (redMin - 1),
                redMin + "-" + max
        );
    }

    private KpiThresholdDto transversalHealthThresholds(String metricKey) {

        KpiProperties.TransversalKpiThreshold threshold =
                kpiProperties.getTransversal().thresholdFor(metricKey);
        int yellowMin = threshold.getYellowMin();
        int greenMin = threshold.getGreenMin();
        int max = kpiProperties.getStatus().getMax();

        return new KpiThresholdDto(
                greenMin + "-" + max,
                yellowMin + "-" + (greenMin - 1),
                "0-" + (yellowMin - 1)
        );
    }

    private KpiThresholdDto commonAffectionThresholds() {

        int yellowMin = kpiProperties.getStatus().getYellowMin();
        int redMin = kpiProperties.getStatus().getRedMin();
        int max = kpiProperties.getStatus().getMax();

        return new KpiThresholdDto(
                "0-" + (yellowMin - 1),
                yellowMin + "-" + (redMin - 1),
                redMin + "-" + max
        );
    }
}



