-- Escenario base: 90 dias con todas las plataformas en rango verde.
--
-- El escenario inserta datos en las tablas que leen los endpoints actuales:
-- - citrix_metrics_history
-- - microsoft365_metrics_history
-- - glpi_metrics_history
-- - transversal_kpi_history
-- - analysis_snapshots
--
-- No modifica inventario Aruba real. Por tanto, el panel Aruba completo
-- seguira reflejando los APs/switches reales sincronizados. El analisis y los
-- KPIs historicos quedan cargados con Aruba en verde.

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `test_scenario_days`;

CREATE TEMPORARY TABLE `test_scenario_days` AS
SELECT
    numbers.day_index,
    CASE
        WHEN numbers.day_index = 89 THEN NOW()
        ELSE DATE_ADD(
            DATE_ADD(CURRENT_DATE(), INTERVAL (numbers.day_index - 89) DAY),
            INTERVAL 12 HOUR
        )
    END AS snapshot_time,
    8 + MOD(numbers.day_index, 3) AS aruba_open_tickets,
    12 + MOD(numbers.day_index, 4) AS citrix_open_tickets,
    10 + MOD(numbers.day_index, 3) AS microsoft365_open_tickets
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
ORDER BY numbers.day_index;

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
    180 + MOD(day_index, 12),
    580,
    4,
    4,
    MOD(day_index, 3),
    10 + MOD(day_index, 6),
    30 + MOD(day_index, 12),
    MOD(day_index, 3),
    'GREEN',
    snapshot_time
FROM `test_scenario_days`;

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
    1200 + MOD(day_index, 50),
    80,
    'HEALTHY',
    'HEALTHY',
    'HEALTHY',
    0,
    0,
    45 + MOD(day_index, 20),
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
    aruba_open_tickets + citrix_open_tickets + microsoft365_open_tickets,
    aruba_open_tickets,
    citrix_open_tickets,
    microsoft365_open_tickets,
    0,
    0,
    6,
    20 + MOD(day_index, 4),
    20 + MOD(day_index, 4),
    120 + MOD(day_index, 6) + aruba_open_tickets + citrix_open_tickets + microsoft365_open_tickets,
    120 + MOD(day_index, 6),
    aruba_open_tickets + citrix_open_tickets + microsoft365_open_tickets,
    snapshot_time
FROM `test_scenario_days`;

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
        WHEN 'global_health' THEN 97 - MOD(days.day_index, 3)
        WHEN 'global_criticality' THEN 2 + MOD(days.day_index, 3)
        WHEN 'global_availability' THEN 98 - MOD(days.day_index, 3)
        WHEN 'user_impact' THEN 2 + MOD(days.day_index, 3)
        WHEN 'affected_services' THEN 0
        WHEN 'technical_degradation' THEN 2 + MOD(days.day_index, 4)
        WHEN 'operational_pressure' THEN 5 + MOD(days.day_index, 4)
        WHEN 'operational_backlog' THEN 18 + MOD(days.day_index, 5)
        WHEN 'sla_risk' THEN 0
        WHEN 'environment_stability' THEN 98 - MOD(days.day_index, 3)
        WHEN 'operational_priority' THEN 3 + MOD(days.day_index, 3)
        WHEN 'aruba_network_affectation' THEN 2 + MOD(days.day_index, 4)
        WHEN 'aruba_network_degradation' THEN 2 + MOD(days.day_index, 4)
        WHEN 'aruba_network_health' THEN 98 - MOD(days.day_index, 4)
        WHEN 'glpi_operational_pressure' THEN 5 + MOD(days.day_index, 5)
        WHEN 'citrix_technical_affection' THEN 2 + MOD(days.day_index, 4)
        WHEN 'microsoft365_technical_affection' THEN 2 + MOD(days.day_index, 4)
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
    `aruba_down_switches`,
    `aruba_down_aps`,
    `citrix_average_logon_duration_seconds`,
    `citrix_active_sessions`,
    `citrix_available_delivery_controllers`,
    `citrix_server_load_percent`,
    `citrix_failed_logons`,
    `glpi_open_tickets`,
    `glpi_created_today`,
    `glpi_closed_today`,
    `glpi_created_this_week`,
    `glpi_closed_this_week`,
    `glpi_operational_backlog`,
    `aruba_open_tickets`,
    `citrix_open_tickets`,
    `microsoft365open_tickets`,
    `microsoft365_active_users`,
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
    2 + MOD(day_index, 4),
    2 + MOD(day_index, 4),
    2 + MOD(day_index, 4),
    5 + MOD(day_index, 5),
    5 + MOD(day_index, 5),
    2 + MOD(day_index, 4),
    2 + MOD(day_index, 3),
    2 + MOD(day_index, 4),
    180 + MOD(day_index, 20),
    0,
    0,
    0,
    10 + MOD(day_index, 6),
    180 + MOD(day_index, 12),
    4,
    30 + MOD(day_index, 12),
    MOD(day_index, 3),
    aruba_open_tickets + citrix_open_tickets + microsoft365_open_tickets,
    20 + MOD(day_index, 4),
    20 + MOD(day_index, 4),
    120 + MOD(day_index, 6) + aruba_open_tickets + citrix_open_tickets + microsoft365_open_tickets,
    120 + MOD(day_index, 6),
    aruba_open_tickets + citrix_open_tickets + microsoft365_open_tickets,
    aruba_open_tickets,
    citrix_open_tickets,
    microsoft365_open_tickets,
    1200 + MOD(day_index, 50),
    8 + MOD(day_index, 10),
    0,
    1 + MOD(day_index, 4),
    0,
    'GREEN',
    'GREEN',
    'GREEN',
    'GREEN',
    TRUE
FROM `test_scenario_days`;

DROP TEMPORARY TABLE IF EXISTS `test_scenario_days`;
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

COMMIT;
