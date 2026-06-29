-- Ampliacion minima de analysis_snapshots para las relaciones especificas.
--
-- Ejecutar una vez en entornos locales que ya tengan la tabla creada antes de
-- cargar los escenarios SQL. Las columnas son NULL para no convertir snapshots
-- antiguos en ceros falsos.

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'microsoft365_active_users'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `microsoft365_active_users` INT NULL',
    'SELECT ''analysis_snapshots.microsoft365_active_users already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'glpi_created_today'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `glpi_created_today` INT NULL',
    'SELECT ''analysis_snapshots.glpi_created_today already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'glpi_closed_today'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `glpi_closed_today` INT NULL',
    'SELECT ''analysis_snapshots.glpi_closed_today already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'glpi_created_this_week'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `glpi_created_this_week` INT NULL',
    'SELECT ''analysis_snapshots.glpi_created_this_week already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'glpi_closed_this_week'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `glpi_closed_this_week` INT NULL',
    'SELECT ''analysis_snapshots.glpi_closed_this_week already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'glpi_operational_backlog'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `glpi_operational_backlog` INT NULL',
    'SELECT ''analysis_snapshots.glpi_operational_backlog already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'citrix_available_delivery_controllers'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `citrix_available_delivery_controllers` INT NULL',
    'SELECT ''analysis_snapshots.citrix_available_delivery_controllers already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_snapshots'
      AND COLUMN_NAME = 'aruba_down_aps'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE `analysis_snapshots` ADD COLUMN `aruba_down_aps` INT NULL',
    'SELECT ''analysis_snapshots.aruba_down_aps already exists'' AS info'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
