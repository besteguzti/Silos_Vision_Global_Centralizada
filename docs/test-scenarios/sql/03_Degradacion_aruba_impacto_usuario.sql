-- Escenario 03: degradacion progresiva Aruba con impacto de uso.
--
-- Cargar despues de 01_clear_test_environment.sql y de
-- 00_alter_analysis_snapshots_for_relations.sql.
--
-- Objetivo del escenario:
-- - Aruba empeora de forma progresiva.
-- - Citrix y Microsoft 365 se mantienen sin ser causa principal.
-- - Clientes WiFi, sesiones Citrix y usuarios activos Microsoft 365 bajan
--   gradualmente para alimentar las relaciones especificas.
-- - Las tablas operativas Aruba quedan pobladas para que /aruba/summary
--   muestre el estado degradado actual, no "sin datos".
-- - La serie historica introduce pequenas oscilaciones deterministas para
--   evitar nubes de puntos artificialmente lineales en el panel de analisis.

START TRANSACTION;

DROP TEMPORARY TABLE IF EXISTS `test_scenario_days`;

CREATE TEMPORARY TABLE `test_scenario_days` AS
SELECT
    adjusted.day_index,
    adjusted.snapshot_time,
    adjusted.aruba_affection,
    CASE
        WHEN adjusted.day_index = 89 THEN 34
        ELSE GREATEST(
            34,
            LEAST(
                230,
                232
                - adjusted.aruba_affection * 2
                + CASE MOD(adjusted.day_index, 12)
                    WHEN 0 THEN 14
                    WHEN 2 THEN -12
                    WHEN 5 THEN 10
                    WHEN 8 THEN -16
                    WHEN 10 THEN 12
                    ELSE 0
                END
                + CASE
                    WHEN adjusted.day_index BETWEEN 18 AND 23 THEN 18
                    WHEN adjusted.day_index BETWEEN 39 AND 44 THEN -22
                    WHEN adjusted.day_index BETWEEN 58 AND 62 THEN 16
                    WHEN adjusted.day_index BETWEEN 72 AND 76 THEN -18
                    WHEN adjusted.day_index BETWEEN 82 AND 86 THEN 12
                    ELSE 0
                END
            )
        )
    END AS aruba_wifi_clients
FROM (
    SELECT
        base.day_index,
        base.snapshot_time,
        CASE
            WHEN base.day_index = 89 THEN 80
            ELSE LEAST(
                80,
                GREATEST(
                    5,
                    5
                    + FLOOR(base.day_index * 75 / 89)
                    + CASE MOD(base.day_index, 14)
                        WHEN 0 THEN -3
                        WHEN 1 THEN 2
                        WHEN 5 THEN -2
                        WHEN 8 THEN 3
                        WHEN 11 THEN -1
                        ELSE 0
                    END
                    + CASE
                        WHEN base.day_index BETWEEN 24 AND 30 THEN 4
                        WHEN base.day_index BETWEEN 44 AND 48 THEN -3
                        WHEN base.day_index BETWEEN 63 AND 68 THEN 5
                        ELSE 0
                    END
                )
            )
        END AS aruba_affection
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
    ORDER BY base.day_index
) adjusted
ORDER BY adjusted.day_index;

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
    ('TFG-AP-01', 'Up', '10.30.10.11', '80.30.10.11', 'TFG-AP-0301', 'Sede TFG', '10.6.0.0', '00:33:22:33:44:01', 'TFG-SWARM-DEGRADED', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-02', 'Up', '10.30.10.12', '80.30.10.12', 'TFG-AP-0302', 'Sede TFG', '10.6.0.0', '00:33:22:33:44:02', 'TFG-SWARM-DEGRADED', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-AP-03', 'Down', '10.30.10.13', '80.30.10.13', 'TFG-AP-0303', 'Sede TFG', '10.6.0.0', '00:33:22:33:44:03', 'TFG-SWARM-DEGRADED', NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 45 DAY),
    ('TFG-AP-04', 'Down', '10.30.10.14', '80.30.10.14', 'TFG-AP-0304', 'Sede TFG', '10.6.0.0', '00:33:22:33:44:04', 'TFG-SWARM-DEGRADED', NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 40 DAY),
    ('TFG-AP-05', 'Down', '10.30.10.15', '80.30.10.15', 'TFG-AP-0305', 'Sede TFG', '10.6.0.0', '00:33:22:33:44:05', 'TFG-SWARM-DEGRADED', NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 35 DAY),
    ('TFG-AP-06', 'Down', '10.30.10.16', '80.30.10.16', 'TFG-AP-0306', 'Sede TFG', '10.6.0.0', '00:33:22:33:44:06', 'TFG-SWARM-DEGRADED', NOW() - INTERVAL 120 DAY, NOW());

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
    ('TFG-SW-0301', '00:CC:BB:CC:DD:01', 'TFG-SW-DEG-01', 'Aruba 2930F', 'Up', FALSE, 'Up', NOW() - INTERVAL 120 DAY, NOW()),
    ('TFG-SW-0302', '00:CC:BB:CC:DD:02', 'TFG-SW-DEG-02', 'Aruba 2930F', 'Down', FALSE, 'Down', NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 2 DAY),
    ('TFG-SW-0303', '00:CC:BB:CC:DD:03', 'TFG-SW-DEG-03', 'Aruba 2930F', 'Down', TRUE, 'Down', NOW() - INTERVAL 120 DAY, NOW() - INTERVAL 3 DAY);

INSERT INTO `aruba_switch_client_usage` (
    `associated_device`,
    `associated_device_name`,
    `associated_device_mac`,
    `device_status`,
    `down_interfaces`,
    `updated_at`
) VALUES
    ('TFG-SW-0301', 'TFG-SW-DEG-01', '00:CC:BB:CC:DD:01', 'Up', 4, NOW()),
    ('TFG-SW-0302', 'TFG-SW-DEG-02', '00:CC:BB:CC:DD:02', 'Down', 18, NOW()),
    ('TFG-SW-0303', 'TFG-SW-DEG-03', '00:CC:BB:CC:DD:03', 'Down', 20, NOW());

INSERT INTO `aruba_switch_interface_usage_history` (
    `associated_device`,
    `associated_device_name`,
    `associated_device_mac`,
    `device_status`,
    `down_interfaces`,
    `observed_at`
) VALUES
    ('TFG-SW-0301', 'TFG-SW-DEG-01', '00:CC:BB:CC:DD:01', 'Up', 4, NOW()),
    ('TFG-SW-0302', 'TFG-SW-DEG-02', '00:CC:BB:CC:DD:02', 'Down', 18, NOW()),
    ('TFG-SW-0303', 'TFG-SW-DEG-03', '00:CC:BB:CC:DD:03', 'Down', 20, NOW());

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
    4,
    34,
    12,
    22,
    0,
    34,
    18,
    6,
    4,
    4,
    2,
    NOW()
);

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
    GREATEST(60, 190 - aruba_affection),
    580,
    4,
    4,
    MOD(day_index, 2),
    12 + MOD(day_index, 4),
    35 + MOD(day_index, 8),
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
    GREATEST(700, 1200 - aruba_affection * 3),
    80,
    'HEALTHY',
    'HEALTHY',
    'HEALTHY',
    0,
    0,
    50 + MOD(day_index, 8),
    0,
    1 + MOD(day_index, 3),
    0,
    0,
    0,
    0,
    10 + MOD(day_index, 8),
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
    (18 + FLOOR(aruba_affection * 0.9)) + (12 + MOD(day_index, 3)) + (10 + MOD(day_index, 3)),
    18 + FLOOR(aruba_affection * 0.9),
    12 + MOD(day_index, 3),
    10 + MOD(day_index, 3),
    CASE WHEN aruba_affection >= 67 THEN 3 ELSE 0 END,
    CASE WHEN aruba_affection >= 67 THEN 2 ELSE 0 END,
    6 + FLOOR(aruba_affection / 20),
    22 + FLOOR(aruba_affection / 10),
    18 + FLOOR(aruba_affection / 20),
    140 + FLOOR(aruba_affection * 0.9),
    100,
    (18 + FLOOR(aruba_affection * 0.9)) + (12 + MOD(day_index, 3)) + (10 + MOD(day_index, 3)),
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
        WHEN 'global_health' THEN 100 - LEAST(100, FLOOR(aruba_affection * 0.40 + 4))
        WHEN 'global_criticality' THEN LEAST(100, FLOOR(aruba_affection * 0.45))
        WHEN 'global_availability' THEN 100 - LEAST(100, FLOOR(aruba_affection * 0.35))
        WHEN 'user_impact' THEN LEAST(100, FLOOR(aruba_affection * 0.60))
        WHEN 'affected_services' THEN CASE WHEN aruba_affection >= 34 THEN 25 ELSE 0 END
        WHEN 'technical_degradation' THEN LEAST(100, FLOOR(aruba_affection * 0.75))
        WHEN 'operational_pressure' THEN LEAST(100, 8 + FLOOR(aruba_affection * 0.45))
        WHEN 'operational_backlog' THEN 20 + FLOOR(aruba_affection * 0.4)
        WHEN 'sla_risk' THEN CASE WHEN aruba_affection >= 67 THEN 20 ELSE 0 END
        WHEN 'environment_stability' THEN 100 - LEAST(100, FLOOR(aruba_affection * 0.35))
        WHEN 'operational_priority' THEN 8 + FLOOR(aruba_affection * 0.5)
        WHEN 'aruba_network_affectation' THEN aruba_affection
        WHEN 'aruba_network_degradation' THEN aruba_affection
        WHEN 'aruba_network_health' THEN 100 - aruba_affection
        WHEN 'glpi_operational_pressure' THEN LEAST(100, 8 + FLOOR(aruba_affection * 0.45))
        WHEN 'citrix_technical_affection' THEN 4 + MOD(days.day_index, 3)
        WHEN 'microsoft365_technical_affection' THEN 4 + MOD(days.day_index, 3)
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
    aruba_affection,
    4 + MOD(day_index, 3),
    4 + MOD(day_index, 3),
    LEAST(100, 8 + FLOOR(aruba_affection * 0.45)),
    LEAST(100, 8 + FLOOR(aruba_affection * 0.45)),
    LEAST(100, FLOOR(aruba_affection * 0.75)),
    LEAST(100, FLOOR(aruba_affection * 0.60)),
    LEAST(100, FLOOR(aruba_affection * 0.40 + 4)),
    aruba_wifi_clients,
    FLOOR(aruba_affection / 10),
    FLOOR(aruba_affection / 25),
    LEAST(4, FLOOR(aruba_affection / 20)),
    12 + MOD(day_index, 4),
    GREATEST(60, 190 - aruba_affection),
    4,
    35 + MOD(day_index, 8),
    MOD(day_index, 3),
    (18 + FLOOR(aruba_affection * 0.9)) + (12 + MOD(day_index, 3)) + (10 + MOD(day_index, 3)),
    22 + FLOOR(aruba_affection / 10),
    18 + FLOOR(aruba_affection / 20),
    140 + FLOOR(aruba_affection * 0.9),
    100,
    (18 + FLOOR(aruba_affection * 0.9)) + (12 + MOD(day_index, 3)) + (10 + MOD(day_index, 3)),
    18 + FLOOR(aruba_affection * 0.9),
    12 + MOD(day_index, 3),
    10 + MOD(day_index, 3),
    GREATEST(700, 1200 - aruba_affection * 3),
    10 + MOD(day_index, 8),
    0,
    1 + MOD(day_index, 3),
    CASE WHEN aruba_affection >= 34 THEN 25 ELSE 0 END,
    CASE
        WHEN aruba_affection >= 67 THEN 'RED'
        WHEN aruba_affection >= 34 THEN 'YELLOW'
        ELSE 'GREEN'
    END,
    'GREEN',
    'GREEN',
    CASE
        WHEN aruba_affection >= 67 THEN 'YELLOW'
        ELSE 'GREEN'
    END,
    TRUE
FROM `test_scenario_days`;

DROP TEMPORARY TABLE IF EXISTS `test_scenario_days`;

COMMIT;
