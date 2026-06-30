# Escenarios SQL de validación

Esta carpeta contiene scripts SQL para cargar escenarios de prueba en la base de datos. Sirven para comprobar rápidamente cómo responde el dashboard ante situaciones conocidas: normalidad, degradaciones, presión operativa, recuperación o datos desactualizados.

Estos scripts son solo para desarrollo y validación. No forman parte de la funcionalidad de producción y pueden borrar datos locales de prueba.

## Antes de ejecutar

Se recomienda pausar la sincronización automática desde el panel de **Configuración** antes de cargar un escenario. Así se evita que la aplicación genere nuevos datos mientras se está probando un caso concreto.

Si después se quieren recuperar datos reales de Aruba, basta con volver a sincronizar con Aruba Central desde la aplicación.

## Uso rápido

El flujo habitual es:

```sql
SOURCE docs/test-scenarios/sql/01_Borrar_datos_tablas.sql;
SOURCE docs/test-scenarios/sql/<escenario_elegido>.sql;
SOURCE docs/test-scenarios/sql/99_validacion_escenario_cargado.sql;
```

Ejemplo:

```sql
SOURCE docs/test-scenarios/sql/01_Borrar_datos_tablas.sql;
SOURCE docs/test-scenarios/sql/07_Degradacion_aruba_citrix.sql;
SOURCE docs/test-scenarios/sql/99_validacion_escenario_cargado.sql;
```


Si la base de datos local no tiene todavía la estructura esperada para los snapshots de análisis, ejecutar antes:

```sql
SOURCE docs/test-scenarios/sql/00_Prepara_Estructura.sql;
```

Este script solo prepara estructura. No carga ningún escenario funcional.

## Scripts disponibles

| Script | Tipo | Qué prueba |
|-------|-------|-------|
| `00_Prepara_Estructura.sql` | Auxiliar | Prepara la estructura de `analysis_snapshots` si la base local está antigua o incompleta. |
| `01_Borrar_datos_tablas.sql` | Limpieza | Limpia datos locales antes de cargar un escenario. |
| `02_Todo_verde.sql` | Escenario | Todas las plataformas en situación estable. |
| `03_Degradacion_aruba_impacto_usuario.sql` | Escenario | Degradación de Aruba con impacto en usuarios. |
| `04_Citrix_delivery_controllers_caidos.sql` | Escenario | Incidencia Citrix por caída o degradación de Delivery Controllers. |
| `05_Microsoft365_riesgo_identidad.sql` | Escenario | Riesgo de identidad y cumplimiento en Microsoft 365. |
| `06_GLPI_presion_operacional.sql` | Escenario | Presión operativa en GLPI sin una causa técnica única. |
| `07_Degradacion_aruba_citrix.sql` | Escenario | Degradación combinada de Aruba y Citrix. |
| `08_Recuperacion_de_incidente.sql` | Escenario | Evolución completa: normalidad, degradación, pico y recuperación. |
| `09_Datos_no_obtenidos.sql` | Escenario | Datos antiguos o no actualizados recientemente. |
| `99_validacion_escenario_cargado.sql` | Validación | Comprueba recuentos, últimos valores, snapshots e históricos cargados. |

## Qué revisar después

Después de cargar un escenario, revisar:

- dashboard principal;
- vistas de Aruba, Citrix, Microsoft 365 y GLPI;
- panel de análisis, si el escenario tiene histórico;
- salida del script `99_validacion_escenario_cargado.sql`.

En los escenarios históricos debería haber snapshots suficientes para revisar periodos de 30 o 90 días.

## Relación con la memoria

Estos scripts apoyan el **Anexo C. Escenarios SQL de validación** de la memoria. La memoria explica qué valida cada escenario; este README indica cómo ejecutarlos en el proyecto.

Los escenarios SQL no sustituyen la sincronización normal con Aruba Central ni la generación dinámica de métricas desde el backend. Solo sirven para cargar casos controlados y repetir pruebas de forma sencilla.
