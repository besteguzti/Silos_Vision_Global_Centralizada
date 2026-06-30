# Frontend

Interfaz React del dashboard multiproveedor. Desde aquí se muestran el dashboard principal, las vistas de Aruba, Citrix, Microsoft 365 y GLPI, el panel de análisis, la configuración y el banco de pruebas.

El frontend no calcula los KPIs principales. Recibe los datos ya preparados por el backend y se encarga de mostrarlos en tarjetas, tablas, gráficas y mensajes de estado.

## Tecnologías

- React
- Vite
- CSS propio del proyecto

## Arranque

Acceder a la carpeta `frontend` con PowerShell y:

```powershell
npm install
npm run dev
```


Por defecto, Vite levanta la aplicación en:

```text
http://localhost:5173
```


Para generar una build de producción:

```powershell
npm run build
```


## Configuración

La URL del backend se centraliza en:

```text
src/config/api.js
```


Variable opcional:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
```


Si no se define, se usa por defecto:

```text
http://localhost:8080
```


## Páginas principales

- **Dashboard principal**: KPIs transversales y estado global de la infraestructura.
- **Resumen operativo**: lectura de alto nivel para responsable IT.
- **Aruba**: APs, switches, clientes WiFi, firmware y estado de red.
- **Citrix**: sesiones, Delivery Controllers, errores de inicio y disponibilidad.
- **Microsoft 365**: identidad, seguridad, cumplimiento y servicios cloud.
- **GLPI**: tickets, SLA, backlog y presión operativa.
- **Panel de análisis**: relaciones entre indicadores y evolución temporal.
- **Configuración**: umbrales, pesos y sincronización manual.
- **Banco de pruebas**: evaluación manual de escenarios sin persistir datos reales.

## Endpoints principales

Endpoints usados en el flujo normal del frontend:

```http
GET /dashboard/summary
GET /api/dashboard/executive-summary
GET /aruba/summary
GET /citrix/summary
GET /microsoft365/summary
GET /glpi/summary
GET /api/analysis/glpi-platform-relation
POST /api/test-scenarios/evaluate
POST /api/metrics/sync
```

El endpoint `/api/analysis/glpi-platform-relation` alimenta el panel de análisis. Devuelve relaciones técnicas, evolución temporal y relaciones específicas entre KPIs.

## Endpoints auxiliares

El backend también expone endpoints útiles para consulta o validación técnica. No todos se usan directamente desde el flujo principal de React.

```http
GET /api/analysis/technical-degradation-impact
GET /api/analysis/platform-evolution
GET /api/analysis/snapshots
GET /api/kpis/definitions
GET /api/reports/monthly-context
```


## Sincronización y diagnóstico

Desde la pantalla de configuración se puede lanzar una sincronización manual:

```http
POST /api/metrics/sync
```


También existen endpoints específicos de Aruba para sincronización o diagnóstico local:

```http
POST /aruba/sync-all
POST /aruba/sync-aps
POST /aruba/sync-switches
POST /aruba/sync-switch-client-usage
GET /aruba/wifi-clients/diagnostics
```


## Panel de análisis

El panel de análisis compara snapshots históricos para mostrar relaciones aparentes, co-ocurrencias y evolución temporal.

No intenta detectar causa raíz de forma automática. Su objetivo es ayudar a revisar posibles relaciones entre la presión operativa de GLPI y la afección técnica de Aruba, Citrix o Microsoft 365.

## Banco de pruebas

El banco de pruebas envía escenarios manuales al backend mediante:

```http
POST /api/test-scenarios/evaluate
```


React no calcula los KPIs ni el resumen operativo. Solo muestra la respuesta devuelta por el backend.

Los tickets abiertos totales se muestran como suma de los tickets asociados a Aruba, Citrix y Microsoft 365. Ese valor puede aparecer en pantalla, pero no forma parte del payload enviado al backend.

La tendencia aparece como no disponible porque los escenarios manuales no tienen histórico real.
