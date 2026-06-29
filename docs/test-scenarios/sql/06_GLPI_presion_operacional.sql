-- Escenario 06: presion GLPI alta sin degradacion tecnica clara.
--
-- Objetivo:
-- - Simular 90 dias historicos.
-- - Mantener Aruba correcto.
-- - Mantener Citrix correcto.
-- - Mantener Microsoft 365 correcto.
-- - Generar presion operativa alta en GLPI por acumulacion de tickets,
--   backlog, tickets criticos y SLA vencidos.
-- - Demostrar que puede existir presion operativa sin una caida tecnica clara
--   en las plataformas monitorizadas.
--
-- Recomendacion:
-- Ejecutar antes:
-- SOURCE docs/test-scenarios/sql/01_clear_test_environment.sql;

START TRANSACTION;

DELETE FROM `analysis_snapshots`
WHERE `id` IS NOT NULL;

DELETE FROM `transversal_kpi_history`
WHERE `id` IS NOT NULL;

DELETE FROM `citrix_metrics_history`
WHERE `id` IS NOT NULL;

DELETE FROM `microsoft365_metrics_history`
WHERE `id` IS NOT NULL;

DELETE FROM `glpi_metrics_history`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_switch_interface_usage_history`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_switch_client_usage`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_switches`
WHERE `id` IS NOT NULL;

DELETE FROM `access_points`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_dashboard_metrics`
WHERE `id` IS NOT NULL;

DROP TEMPORARY TABLE IF EXISTS `test_scenario_days`;

CREATE TEMPORARY TABLE `test_scenario_days` AS
SELECT
    base.day_index,
    base.snapshot_time,

    -- Aruba correcto.
    2 + MOD(base.day_index, 3) AS aruba_affectation,
    120 + MOD(base.day_index, 8) AS aruba_wifi_clients,
    8 + MOD(base.day_index, 3) AS aruba_open_tickets,

    -- Citrix correcto.
    180 + MOD(base.day_index, 12) AS citrix_active_sessions,
    4 AS citrix_available_delivery_controllers,
    MOD(base.day_index, 3) AS citrix_failed_logons,
    10 + MOD(base.day_index, 4) AS citrix_open_tickets,
    2 + MOD(base.day_index, 4) AS citrix_affectation,

    -- Microsoft 365 correcto.
    1200 + MOD(base.day_index, 50) AS microsoft365_active_users,
    9 + MOD(base.day_index, 3) AS microsoft365_open_tickets,
    2 + MOD(base.day_index, 4) AS microsoft365_affectation,

    -- Tickets GLPI totales: crecen por acumulacion operativa,
    -- no por degradacion tecnica clara de una plataforma concreta.
    CASE
        WHEN base.day_index <= 35 THEN 70 + MOD(base.day_index, 8)
        WHEN base.day_index <= 60 THEN 105 + ((base.day_index - 36) * 3) + MOD(base.day_index, 6)
        WHEN base.day_index <= 79 THEN 185 + ((base.day_index - 61) * 2) + MOD(base.day_index, 8)
        ELSE 225 + ((base.day_index - 80) * 2) + MOD(base.day_index, 6)
    END AS glpi_open_tickets,

    CASE
        WHEN base.day_index <= 35 THEN 0
        WHEN base.day_index <= 60 THEN 3 + MOD(base.day_index, 3)
        WHEN base.day_index <= 79 THEN 8 + MOD(base.day_index, 4)
        ELSE 13 + MOD(base.day_index, 5)
    END AS critical_open_tickets,

    CASE
        WHEN base.day_index <= 35 THEN 0
        WHEN base.day_index <= 60 THEN 5 + MOD(base.day_index, 4)
        WHEN base.day_index <= 79 THEN 12 + MOD(base.day_index, 5)
        ELSE 20 + MOD(base.day_index, 6)
    END AS sla_breached_tickets,

    CASE
        WHEN base.day_index <= 35 THEN 6
        WHEN base.day_index <= 60 THEN 12
        WHEN base.day_index <= 79 THEN 18
        ELSE 26
    END AS average_resolution_hours,

    CASE
        WHEN base.day_index <= 35 THEN 22 + MOD(base.day_index, 5)
        WHEN base.day_index <= 60 THEN 42 + MOD(base.day_index, 8)
        WHEN base.day_index <= 79 THEN 56 + MOD(base.day_index, 10)
        ELSE 68 + MOD(base.day_index, 12)
    END AS created_today,

    CASE
        WHEN base.day_index <= 35 THEN 22 + MOD(base.day_index, 5)
        WHEN base.day_index <= 60 THEN 24 + MOD(base.day_index, 5)
        WHEN base.day_index <= 79 THEN 20 + MOD(base.day_index, 5)
        ELSE 16 + MOD(base.day_index, 4)
    END AS closed_today,

    CASE
        WHEN base.day_index <= 35 THEN 120 + MOD(base.day_index, 8)
        WHEN base.day_index <= 60 THEN 185 + MOD(base.day_index, 12)
        WHEN base.day_index <= 79 THEN 245 + MOD(base.day_index, 15)
        ELSE 310 + MOD(base.day_index, 18)
    END AS created_this_week,

    CASE
        WHEN base.day_index <= 35 THEN 120 + MOD(base.day_index, 8)
        WHEN base.day_index <= 60 THEN 105 + MOD(base.day_index, 10)
        WHEN base.day_index <= 79 THEN 90 + MOD(base.day_index, 8)
        ELSE 75 + MOD(base.day_index, 8)
    END AS closed_this_week,

    -- Presion GLPI alta.
    CASE
        WHEN base.day_index <= 35 THEN 8 + MOD(base.day_index, 4)
        WHEN base.day_index <= 60 THEN 35 + (base.day_index - 36)
        WHEN base.day_index <= 79 THEN 62 + (base.day_index - 61)
        ELSE 82 + (base.day_index - 80)
    END AS glpi_operational_pressure,

    -- Degradacion tecnica baja: no hay fallo claro en plataformas.
    5 + MOD(base.day_index, 4) AS technical_degradation,

    -- Impacto en usuarios moderado: no es una caida tecnica, pero hay retraso operativo.
    CASE
        WHEN base.day_index <= 35 THEN 4 + MOD(base.day_index, 3)
        WHEN base.day_index <= 60 THEN 10 + MOD(base.day_index, 6)
        WHEN base.day_index <= 79 THEN 16 + MOD(base.day_index, 6)
        ELSE 22 + MOD(base.day_index, 6)
    END AS user_impact,

    -- Criticidad global sube por presion operativa, no por degradacion tecnica.
    CASE
        WHEN base.day_index <= 35 THEN 8 + MOD(base.day_index, 3)
        WHEN base.day_index <= 60 THEN 22 + MOD(base.day_index, 6)
        WHEN base.day_index <= 79 THEN 32 + MOD(base.day_index, 7)
        ELSE 38 + MOD(base.day_index, 8)
    END AS global_criticality,

    CASE
        WHEN base.day_index <= 35 THEN 0
        WHEN base.day_index <= 60 THEN 15
        WHEN base.day_index <= 79 THEN 25
        ELSE 35
    END AS affected_services_percent,

    CASE
        WHEN base.day_index <= 35 THEN 0
        WHEN base.day_index <= 60 THEN 24 + MOD(base.day_index, 5)
        WHEN base.day_index <= 79 THEN 48 + MOD(base.day_index, 6)
        ELSE 70 + MOD(base.day_index, 8)
    END AS sla_risk

FROM (
    SELECT
        numbers.day_index,
        CASE
            WHEN numbers.day_index = 89 THEN NOW()
            ELSE DATE_ADD(
                DATE_ADD(CURRENT_DATE(), INTERVAL (numbers.day_index - 89) DAY),
                INTERVAL 12 HOUR
            )
        END AS snapshot_time
    FROM (
        SELECT ones.n + tens.n * 10 AS day_index
        FROM (
            SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
        ) ones
        CROSS JOIN (
            SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
            UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
        ) tens
    ) numbers
    WHERE numbers.day_index BETWEEN 0 AND 89
) base
ORDER BY base.day_index;

-- Citrix correcto.
INSERT INTO `citrix_metrics_history` (
    `active_sessions`,
    `active_licenses`,
    `available_delivery_controllers`,
    `total_delivery_controllers`,
    `disconnected_sessions`,
    `average_logon_duration_seconds`,
    `server_load_percent`,
    `failed_logons`,
    `citrix_health`,
    `collected_at`
)
SELECT
    citrix_active_sessions,
    580,
    citrix_available_delivery_controllers,
    4,
    MOD(day_index, 3),
    10 + MOD(day_index, 6),
    30 + MOD(day_index, 12),
    citrix_failed_logons,
    'GREEN',
    snapshot_time
FROM `test_scenario_days`;

-- Microsoft 365 correcto.
INSERT INTO `microsoft365_metrics_history` (
    `active_users`,
    `unassigned_licenses`,
    `outlook_status`,
    `teams_status`,
    `share_point_status`,
    `nearly_full_mailboxes`,
    `emails_quarantined`,
    `share_point_storage_percent`,
    `risky_users`,
    `failed_sign_ins`,
    `users_without_mfa`,
    `apps_secrets_expiring_soon`,
    `unused_applications`,
    `high_privilege_applications`,
    `non_compliant_devices`,
    `outdated_windows_devices`,
    `devices_without_encryption`,
    `stale_devices`,
    `microsoft365health`,
    `collected_at`
)
SELECT
    microsoft365_active_users,
    80,
    'HEALTHY',
    'HEALTHY',
    'HEALTHY',
    0,
    0,
    45 + MOD(day_index, 15),
    0,
    1 + MOD(day_index, 4),
    0,
    0,
    0,
    0,
    8 + MOD(day_index, 10),
    0,
    0,
    0,
    'GREEN',
    snapshot_time
FROM `test_scenario_days`;

-- GLPI con presion alta acumulada.
INSERT INTO `glpi_metrics_history` (
    `open_tickets`,
    `aruba_open_tickets`,
    `citrix_open_tickets`,
    `microsoft365open_tickets`,
    `critical_open_tickets`,
    `sla_breached_tickets`,
    `average_resolution_hours`,
    `created_today`,
    `closed_today`,
    `created_this_week`,
    `closed_this_week`,
    `operational_backlog`,
    `collected_at`
)
SELECT
    glpi_open_tickets,
    aruba_open_tickets,
    citrix_open_tickets,
    microsoft365_open_tickets,
    critical_open_tickets,
    sla_breached_tickets,
    average_resolution_hours,
    created_today,
    closed_today,
    created_this_week,
    closed_this_week,
    glpi_open_tickets,
    snapshot_time
FROM `test_scenario_days`;

-- KPIs transversales.
INSERT INTO `transversal_kpi_history` (
    `kpi_code`,
    `kpi_name`,
    `value`,
    `unit`,
    `collected_at`
)
SELECT
    kpi.kpi_code,
    kpi.kpi_name,
    CASE kpi.kpi_code
        WHEN 'global_health' THEN 100 - days.global_criticality
        WHEN 'global_criticality' THEN days.global_criticality
        WHEN 'global_availability' THEN 100 - days.user_impact
        WHEN 'user_impact' THEN days.user_impact
        WHEN 'affected_services' THEN days.affected_services_percent
        WHEN 'technical_degradation' THEN days.technical_degradation
        WHEN 'operational_pressure' THEN days.glpi_operational_pressure
        WHEN 'operational_backlog' THEN LEAST(100, days.glpi_operational_pressure + 12)
        WHEN 'sla_risk' THEN days.sla_risk
        WHEN 'environment_stability' THEN 100 - days.technical_degradation
        WHEN 'operational_priority' THEN LEAST(100, GREATEST(days.glpi_operational_pressure, days.sla_risk))
        WHEN 'aruba_network_affectation' THEN days.aruba_affectation
        WHEN 'aruba_network_degradation' THEN days.aruba_affectation
        WHEN 'aruba_network_health' THEN 100 - days.aruba_affectation
        WHEN 'glpi_operational_pressure' THEN days.glpi_operational_pressure
        WHEN 'citrix_technical_affection' THEN days.citrix_affectation
        WHEN 'microsoft365_technical_affection' THEN days.microsoft365_affectation
        ELSE 0
    END AS value,
    kpi.unit,
    days.snapshot_time
FROM `test_scenario_days` days
CROSS JOIN (
    SELECT 'global_health' AS kpi_code, 'Salud global' AS kpi_name, '%' AS unit
    UNION ALL SELECT 'global_criticality', 'Criticidad global', 'indice 0-100'
    UNION ALL SELECT 'global_availability', 'Disponibilidad global', '%'
    UNION ALL SELECT 'user_impact', 'Impacto en usuarios', '%'
    UNION ALL SELECT 'affected_services', 'Servicios afectados', '%'
    UNION ALL SELECT 'technical_degradation', 'Degradacion tecnica', 'indice 0-100'
    UNION ALL SELECT 'operational_pressure', 'Presion operativa', 'indice 0-100'
    UNION ALL SELECT 'operational_backlog', 'Backlog operativo', 'indice 0-100'
    UNION ALL SELECT 'sla_risk', 'Riesgo de SLA', 'indice 0-100'
    UNION ALL SELECT 'environment_stability', 'Estabilidad del entorno', '%'
    UNION ALL SELECT 'operational_priority', 'Prioridad operativa', 'indice 0-100'
    UNION ALL SELECT 'aruba_network_affectation', 'Afectacion de red Aruba', '%'
    UNION ALL SELECT 'aruba_network_degradation', 'Degradacion de red Aruba', 'indice 0-100'
    UNION ALL SELECT 'aruba_network_health', 'Salud de red Aruba', '%'
    UNION ALL SELECT 'glpi_operational_pressure', 'Presion operativa GLPI', '%'
    UNION ALL SELECT 'citrix_technical_affection', 'Afeccion tecnica Citrix', '%'
    UNION ALL SELECT 'microsoft365_technical_affection', 'Afeccion tecnica Microsoft 365', '%'
) kpi;

-- Snapshots agregados para analisis.
INSERT INTO `analysis_snapshots` (
    `timestamp`,
    `aruba_health`,
    `citrix_health`,
    `microsoft365health`,
    `glpi_health`,
    `glpi_operational_pressure`,
    `technical_degradation`,
    `user_impact`,
    `global_status`,
    `aruba_wifi_clients`,
    `aruba_inactive_aps`,
    `aruba_down_aps`,
    `aruba_down_switches`,
    `citrix_average_logon_duration_seconds`,
    `citrix_active_sessions`,
    `citrix_available_delivery_controllers`,
    `citrix_server_load_percent`,
    `citrix_failed_logons`,
    `microsoft365_active_users`,
    `glpi_open_tickets`,
    `glpi_created_today`,
    `glpi_closed_today`,
    `glpi_created_this_week`,
    `glpi_closed_this_week`,
    `glpi_operational_backlog`,
    `aruba_open_tickets`,
    `citrix_open_tickets`,
    `microsoft365open_tickets`,
    `microsoft365non_compliant_devices`,
    `microsoft365users_without_mfa`,
    `microsoft365failed_sign_ins`,
    `affected_services_percent`,
    `aruba_status`,
    `citrix_status`,
    `microsoft365status`,
    `glpi_status`,
    `generated_scenario`
)
SELECT
    snapshot_time,
    aruba_affectation,
    citrix_affectation,
    microsoft365_affectation,
    glpi_operational_pressure,
    glpi_operational_pressure,
    technical_degradation,
    user_impact,
    global_criticality,
    aruba_wifi_clients,
    0,
    0,
    0,
    10 + MOD(day_index, 6),
    citrix_active_sessions,
    citrix_available_delivery_controllers,
    30 + MOD(day_index, 12),
    citrix_failed_logons,
    microsoft365_active_users,
    glpi_open_tickets,
    created_today,
    closed_today,
    created_this_week,
    closed_this_week,
    glpi_open_tickets,
    aruba_open_tickets,
    citrix_open_tickets,
    microsoft365_open_tickets,
    8 + MOD(day_index, 10),
    0,
    1 + MOD(day_index, 4),
    affected_services_percent,
    'GREEN',
    'GREEN',
    'GREEN',
    CASE
        WHEN glpi_operational_pressure >= 67 THEN 'RED'
        WHEN glpi_operational_pressure >= 34 THEN 'YELLOW'
        ELSE 'GREEN'
    END,
    TRUE
FROM `test_scenario_days`;

-- Estado operativo actual Aruba: correcto.
INSERT INTO `access_points` (
    `name`,
    `status`,
    `ip_address`,
    `public_ip_address`,
    `serial`,
    `site`,
    `firmware_version`,
    `macaddr`,
    `swarm_name`,
    `first_seen_at`,
    `last_seen_at`
) VALUES
    ('TFG-AP-01', 'Up', '10.10.10.11', '80.10.10.11', 'TFG-AP-0001', 'Sede TFG', '10.6.0.0', '00:11:22:33:44:01', 'TFG-SWARM', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-02', 'Up', '10.10.10.12', '80.10.10.12', 'TFG-AP-0002', 'Sede TFG', '10.6.0.0', '00:11:22:33:44:02', 'TFG-SWARM', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-03', 'Up', '10.10.10.13', '80.10.10.13', 'TFG-AP-0003', 'Sede TFG', '10.6.0.0', '00:11:22:33:44:03', 'TFG-SWARM', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-04', 'Up', '10.10.10.14', '80.10.10.14', 'TFG-AP-0004', 'Sede TFG', '10.6.0.0', '00:11:22:33:44:04', 'TFG-SWARM', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-05', 'Up', '10.10.10.15', '80.10.10.15', 'TFG-AP-0005', 'Sede TFG', '10.6.0.0', '00:11:22:33:44:05', 'TFG-SWARM', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-06', 'Up', '10.10.10.16', '80.10.10.16', 'TFG-AP-0006', 'Sede TFG', '10.6.0.0', '00:11:22:33:44:06', 'TFG-SWARM', NOW() - INTERVAL 120 DAY, NOW());

INSERT INTO `aruba_switches` (
    `serial`,
    `mac_address`,
    `hostname`,
    `model`,
    `device_status`,
    `upgrade_required`,
    `status_state`,
    `first_seen_at`,
    `last_seen_at`
) VALUES
    ('TFG-SW-0001', '00:AA:BB:CC:DD:01', 'TFG-SW-01', 'Aruba 2930F', 'Up', FALSE, 'Up', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-SW-0002', '00:AA:BB:CC:DD:02', 'TFG-SW-02', 'Aruba 2930F', 'Up', FALSE, 'Up', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-SW-0003', '00:AA:BB:CC:DD:03', 'TFG-SW-03', 'Aruba 2930F', 'Up', FALSE, 'Up', NOW() - INTERVAL 120 DAY, NOW());

INSERT INTO `aruba_switch_client_usage` (
    `associated_device`,
    `associated_device_name`,
    `associated_device_mac`,
    `device_status`,
    `down_interfaces`,
    `updated_at`
) VALUES
    ('TFG-SW-0001', 'TFG-SW-01', '00:AA:BB:CC:DD:01', 'Up', 2, NOW()),
    ('TFG-SW-0002', 'TFG-SW-02', '00:AA:BB:CC:DD:02', 'Up', 1, NOW()),
    ('TFG-SW-0003', 'TFG-SW-03', '00:AA:BB:CC:DD:03', 'Up', 0, NOW());

INSERT INTO `aruba_switch_interface_usage_history` (
    `associated_device`,
    `associated_device_name`,
    `associated_device_mac`,
    `device_status`,
    `down_interfaces`,
    `observed_at`
) VALUES
    ('TFG-SW-0001', 'TFG-SW-01', '00:AA:BB:CC:DD:01', 'Up', 2, NOW()),
    ('TFG-SW-0002', 'TFG-SW-02', '00:AA:BB:CC:DD:02', 'Up', 1, NOW()),
    ('TFG-SW-0003', 'TFG-SW-03', '00:AA:BB:CC:DD:03', 'Up', 0, NOW());

INSERT INTO `aruba_dashboard_metrics` (
    `id`,
    `firmware_outdated`,
    `total_wifi_clients`,
    `mutualia_aps_clients`,
    `mutualia_wifi_clients`,
    `mutualia_langileak_clients`,
    `mutualia_clients`,
    `mutualia_red_interna_clients`,
    `mutualia_red_externa_clients`,
    `mutualia_korporatiboa_clients`,
    `wifi_pacs_clients`,
    `mut_video_clients`,
    `updated_at`
) VALUES (
    1,
    0,
    120,
    45,
    75,
    0,
    120,
    70,
    20,
    15,
    10,
    5,
    NOW()
);

DROP TEMPORARY TABLE IF EXISTS `test_scenario_days`;

COMMIT;
