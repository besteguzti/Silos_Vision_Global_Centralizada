-- Consultas de validacion del escenario cargado.
-- No modifica datos.

SELECT
    'citrix_metrics_history' AS table_name,
    COUNT(*) AS rows_loaded,
    MIN(`collected_at`) AS min_timestamp,
    MAX(`collected_at`) AS max_timestamp
FROM `citrix_metrics_history`
UNION ALL
SELECT
    'microsoft365_metrics_history',
    COUNT(*),
    MIN(`collected_at`),
    MAX(`collected_at`)
FROM `microsoft365_metrics_history`
UNION ALL
SELECT
    'glpi_metrics_history',
    COUNT(*),
    MIN(`collected_at`),
    MAX(`collected_at`)
FROM `glpi_metrics_history`
UNION ALL
SELECT
    'analysis_snapshots',
    COUNT(*),
    MIN(`timestamp`),
    MAX(`timestamp`)
FROM `analysis_snapshots`
UNION ALL
SELECT
    'transversal_kpi_history',
    COUNT(*),
    MIN(`collected_at`),
    MAX(`collected_at`)
FROM `transversal_kpi_history`;

SELECT
    `active_sessions`,
    `available_delivery_controllers`,
    `total_delivery_controllers`,
    `average_logon_duration_seconds`,
    `server_load_percent`,
    `failed_logons`,
    `collected_at`
FROM `citrix_metrics_history`
ORDER BY `collected_at` DESC
LIMIT 1;

SELECT
    `active_users`,
    `share_point_storage_percent`,
    `risky_users`,
    `failed_sign_ins`,
    `users_without_mfa`,
    `non_compliant_devices`,
    `collected_at`
FROM `microsoft365_metrics_history`
ORDER BY `collected_at` DESC
LIMIT 1;

SELECT
    `open_tickets`,
    `aruba_open_tickets`,
    `citrix_open_tickets`,
    `microsoft365open_tickets`,
    `critical_open_tickets`,
    `sla_breached_tickets`,
    `created_today`,
    `closed_today`,
    `created_this_week`,
    `closed_this_week`,
    `collected_at`
FROM `glpi_metrics_history`
ORDER BY `collected_at` DESC
LIMIT 1;

SELECT
    `aruba_health`,
    `citrix_health`,
    `microsoft365health`,
    `glpi_operational_pressure`,
    `glpi_open_tickets`,
    `glpi_created_today`,
    `glpi_closed_today`,
    `glpi_created_this_week`,
    `glpi_closed_this_week`,
    `glpi_operational_backlog`,
    `technical_degradation`,
    `user_impact`,
    `microsoft365_active_users`,
    `citrix_available_delivery_controllers`,
    `aruba_down_aps`,
    `global_status`,
    `affected_services_percent`,
    `generated_scenario`,
    `timestamp`
FROM `analysis_snapshots`
ORDER BY `timestamp` DESC
LIMIT 1;

SELECT
    `kpi_code`,
    `kpi_name`,
    `value`,
    `unit`,
    `collected_at`
FROM `transversal_kpi_history`
WHERE `collected_at` = (
    SELECT MAX(`collected_at`)
    FROM `transversal_kpi_history`
)
ORDER BY `kpi_code`;

SELECT
    COUNT(*) AS total_aps,
    SUM(CASE WHEN LOWER(`status`) <> 'up' OR `status` IS NULL THEN 1 ELSE 0 END) AS down_aps,
    SUM(CASE WHEN `last_seen_at` < DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS inactive_aps,
    MIN(`last_seen_at`) AS oldest_last_seen_at,
    MAX(`last_seen_at`) AS newest_last_seen_at
FROM `access_points`;

SELECT COUNT(*) AS access_points_rows
FROM `access_points`;

SELECT
    COUNT(*) AS total_switches,
    SUM(CASE WHEN LOWER(`device_status`) <> 'up' OR `device_status` IS NULL THEN 1 ELSE 0 END) AS down_switches,
    SUM(CASE WHEN `upgrade_required` = TRUE THEN 1 ELSE 0 END) AS switches_with_upgrade,
    MIN(`last_seen_at`) AS oldest_switch_last_seen_at,
    MAX(`last_seen_at`) AS newest_switch_last_seen_at
FROM `aruba_switches`;

SELECT COUNT(*) AS aruba_switches_rows
FROM `aruba_switches`;

SELECT
    `id`,
    `firmware_outdated`,
    `total_wifi_clients`,
    `mutualia_aps_clients`,
    `mutualia_wifi_clients`,
    `updated_at`
FROM `aruba_dashboard_metrics`
ORDER BY `updated_at` DESC
LIMIT 1;

SELECT COUNT(*) AS aruba_dashboard_metrics_rows
FROM `aruba_dashboard_metrics`;

SELECT
    COUNT(*) AS current_switch_usage_rows,
    SUM(CASE WHEN `down_interfaces` > 17 THEN 1 ELSE 0 END) AS switches_over_underuse_limit,
    MAX(`updated_at`) AS latest_switch_usage
FROM `aruba_switch_client_usage`;

SELECT COUNT(*) AS aruba_switch_client_usage_rows
FROM `aruba_switch_client_usage`;

SELECT COUNT(*) AS aruba_switch_interface_usage_history_rows
FROM `aruba_switch_interface_usage_history`;

SELECT
    ap_counts.total_aps,
    ap_counts.active_aps,
    ap_counts.down_aps,
    ap_counts.inactive_aps,
    switch_counts.total_switches,
    switch_counts.down_switches,
    metrics.total_wifi_clients,
    metrics.mutualia_aps_clients,
    metrics.mutualia_wifi_clients
FROM (
    SELECT
        COUNT(*) AS total_aps,
        SUM(CASE WHEN LOWER(`status`) = 'up' THEN 1 ELSE 0 END) AS active_aps,
        SUM(CASE WHEN LOWER(`status`) <> 'up' OR `status` IS NULL THEN 1 ELSE 0 END) AS down_aps,
        SUM(CASE WHEN `last_seen_at` < DATE_SUB(NOW(), INTERVAL 30 DAY) THEN 1 ELSE 0 END) AS inactive_aps
    FROM `access_points`
) ap_counts
CROSS JOIN (
    SELECT
        COUNT(*) AS total_switches,
        SUM(CASE WHEN LOWER(`device_status`) <> 'up' OR `device_status` IS NULL THEN 1 ELSE 0 END) AS down_switches
    FROM `aruba_switches`
) switch_counts
CROSS JOIN (
    SELECT
        `total_wifi_clients`,
        `mutualia_aps_clients`,
        `mutualia_wifi_clients`
    FROM `aruba_dashboard_metrics`
    ORDER BY `updated_at` DESC
    LIMIT 1
) metrics;

SELECT
    `kpi_code`,
    `kpi_name`,
    `value`,
    `unit`,
    `collected_at`
FROM `transversal_kpi_history`
WHERE `kpi_code` IN (
    'aruba_network_affectation',
    'aruba_network_degradation',
    'aruba_network_health'
)
ORDER BY `collected_at` DESC, `kpi_code`
LIMIT 3;
