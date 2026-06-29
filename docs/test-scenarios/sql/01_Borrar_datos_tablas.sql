SET SQL_SAFE_UPDATES = 0;

START TRANSACTION;

-- Históricos y snapshots generales
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

-- Datos operativos Aruba usados por el fixture de pruebas
DELETE FROM `aruba_switch_interface_usage_history`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_switch_client_usage`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_dashboard_metrics`
WHERE `id` IS NOT NULL;

DELETE FROM `aruba_switches`
WHERE `id` IS NOT NULL;

DELETE FROM `access_points`
WHERE `id` IS NOT NULL;

COMMIT;
