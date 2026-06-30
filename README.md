# Cuadro de Mandos TFG

Dashboard para revisar Aruba, Citrix, Microsoft 365 y GLPI desde una misma interfaz.

El proyecto está formado por:

- backend Spring Boot;
- base de datos MySQL;
- frontend React/Vite;
- scripts SQL para cargar escenarios de validación.

Aruba Central es la integración real del proyecto. Citrix, Microsoft 365 y GLPI se simulan desde el backend para poder trabajar con históricos, KPIs y escenarios controlados.

## Estructura del proyecto


```text
dashboard/              Backend Spring Boot
frontend/               Interfaz React + Vite
docs/test-scenarios/    Escenarios SQL de validación
```


La base de datos MySQL almacena inventario, métricas, snapshots, históricos, umbrales, pesos y tokens OAuth de Aruba.

## Fuentes de datos

- **Aruba Central**: integración real. Se obtienen APs, switches, firmware, clientes WiFi y datos de puertos.
- **Citrix**: fuente simulada dinámica.
- **Microsoft 365**: fuente simulada dinámica.
- **GLPI**: fuente simulada dinámica.

Las simulaciones se generan en backend, se guardan en MySQL y alimentan las vistas y los KPIs igual que una fuente real.

El panel de análisis usa snapshots persistidos. Sirve para revisar tendencias, co-ocurrencias y relaciones aparentes entre indicadores, pero no demuestra causa raíz automática.

## Requisitos

- Java 17
- Maven
- Node.js y npm
- MySQL
- Base de datos llamada `dashboard`
- Credenciales de Aruba Central

## Variables de entorno

Backend:

```powershell
$env:DB_USERNAME="usuario_mysql"
$env:DB_PASSWORD="password_mysql"
$env:ARUBA_CLIENT_ID="client_id_aruba"
$env:ARUBA_CLIENT_SECRET="client_secret_aruba"
```

Frontend:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
```


Si no se define `VITE_API_BASE_URL`, el frontend usa por defecto:

```text
http://localhost:8080
```


También hay un ejemplo en:

```text
frontend/.env.example
```


## Arranque

Backend:

```powershell
cd dashboard
mvn spring-boot:run
```


Frontend:

```powershell
cd frontend
npm install
npm run dev
```


URLs habituales:

```text
Backend:  http://localhost:8080
Frontend: http://localhost:5173
```


## Sincronización

Aruba se sincroniza con Aruba Central desde backend mediante `ArubaScheduler` y `ArubaService`.

El endpoint `/aruba/summary` no consulta Aruba Central en cada carga de pantalla. Lee el último dato guardado en MySQL.

Citrix, Microsoft 365 y GLPI se generan y guardan mediante `MetricsSyncService`. Si una plataforma falla durante la sincronización, el resto puede seguir actualizándose.

Desde el panel de configuración se puede:

- pausar la sincronización automática;
- reanudarla;
- lanzar una sincronización manual con:

```http
POST /api/metrics/sync
```


## Frescura de datos

Los resúmenes de plataforma devuelven información de actualización:

- `lastUpdated`
- `dataStatus`

Valores posibles:

- `OK`: datos recientes.
- `STALE`: datos antiguos.
- `NO_DATA`: no hay datos suficientes.

La frescura está alineada con la sincronización horaria. En el prototipo se usa un margen de 70 minutos para Aruba, Citrix, Microsoft 365 y GLPI.

## Umbrales y pesos

El panel de configuración permite editar umbrales y pesos persistidos en MySQL.

La prioridad de configuración es:

1. valores guardados en base de datos;
2. valores por defecto de `application.properties` / `KpiProperties`;
3. valores internos de respaldo.

Los pesos se usan para calcular KPIs transversales, como el estado global. En los KPIs de afección, los valores altos representan peor estado. La disponibilidad global se trata como KPI de tipo HEALTH: valores altos son buenos y valores bajos indican peor disponibilidad.

## Pantallas principales

- **Dashboard principal**: KPIs transversales y resumen operativo.
- **Aruba**: APs, clientes WiFi, switches, firmware y APs inactivos.
- **Citrix**: sesiones, Delivery Controllers, logon, carga, errores y tickets.
- **Microsoft 365**: actividad, MFA, dispositivos, seguridad, cumplimiento y tickets.
- **GLPI**: tickets, SLA, backlog, cierre y presión operativa.
- **Análisis**: relaciones entre plataformas y evolución temporal.
- **Banco de pruebas**: evaluación manual de escenarios sin guardar datos reales.
- **Configuración**: umbrales, pesos y sincronización.

## Endpoints usados por React

```http
GET /dashboard/summary
GET /api/dashboard/executive-summary
GET /aruba/summary
GET /aruba/inactive-aps
PUT /aruba/inactive-aps/{serial}/annotation
GET /citrix/summary
GET /microsoft365/summary
GET /glpi/summary
GET /api/analysis/glpi-platform-relation
GET /api/config/thresholds
GET /api/config/platform-weights
GET /api/metrics/sync-control
POST /api/metrics/sync
POST /api/test-scenarios/evaluate
```


## Endpoints auxiliares

Endpoints útiles para consulta, análisis o validación técnica. No todos se usan directamente desde el flujo principal de React.

```http
GET /api/kpis/definitions
GET /api/analysis/technical-degradation-impact
GET /api/analysis/platform-evolution
GET /api/analysis/snapshots
GET /api/reports/monthly-context
```


También existen endpoints de diagnóstico de Aruba, como:

```http
GET /aruba/aps
GET /aruba/stored-aps
GET /aruba/stored-switches
GET /aruba/wifi-clients
GET /aruba/wifi-clients/diagnostics
```


## Panel de análisis

La página **Análisis** compara señales técnicas con la presión operativa de GLPI.

GLPI se usa como reflejo del trabajo de soporte. Aruba, Citrix y Microsoft 365 se revisan como posibles focos técnicos. La página no intenta demostrar causa raíz automática.

Endpoint principal:

```http
GET http://localhost:8080/api/analysis/glpi-platform-relation?period=30d
```

La respuesta incluye:

- `technicalRelations`
- `technicalTimeline`
- `specificKpiRelations`

## Banco de pruebas

El banco de pruebas permite introducir valores manuales para Aruba, Citrix, Microsoft 365 y GLPI.

No guarda datos en base de datos y no modifica el dashboard real.

Endpoint utilizado:

```http
POST /api/test-scenarios/evaluate
```

React no calcula KPIs ni resumen operativo. Solo muestra la respuesta del backend.

La tendencia aparece como no disponible porque los escenarios manuales no tienen histórico real.

## Definiciones de KPIs

```http
GET http://localhost:8080/api/kpis/definitions
```

Este endpoint devuelve identificador, nombre, tipo, plataforma, descripción, fórmula, umbrales y fuentes de cada KPI.

No calcula valores actuales. Sirve para revisar cómo está definido cada indicador.

## KPIs principales

Dashboard principal:

- Estado global
- Criticidad global
- Disponibilidad global
- Presión operativa
- Degradación técnica
- Riesgo SLA
- Backlog operativo
- Impacto en usuarios
- Servicios afectados

KPIs por plataforma:

- **Aruba**: APs, firmware, clientes WiFi, switches, tickets Aruba e índice de afección Aruba.
- **Citrix**: sesiones, licencias, Delivery Controllers, logon, carga, errores y tickets.
- **Microsoft 365**: usuarios, licencias, SharePoint, MFA, seguridad, dispositivos y tickets.
- **GLPI**: tickets abiertos, críticos, SLA vencido, cierre, backlog y actividad diaria/semanal.

## Escenarios SQL

Los escenarios de prueba están en:

```text
docs/test-scenarios/sql
```


Sirven para validar el prototipo en local. Antes de ejecutarlos conviene pausar la sincronización automática desde el panel de configuración.

La guía completa está en:

```text
docs/test-scenarios/README.md
```


## Comprobaciones rápidas

Backend:

```powershell
cd dashboard
mvn clean test
```


Frontend:

```powershell
cd frontend
npm run lint
npm run build
```

Consultas útiles:

```powershell
curl.exe http://localhost:8080/dashboard/summary
curl.exe http://localhost:8080/api/dashboard/executive-summary
curl.exe http://localhost:8080/api/kpis/definitions
curl.exe "http://localhost:8080/api/analysis/glpi-platform-relation?period=30d"
curl.exe http://localhost:8080/aruba/summary
curl.exe http://localhost:8080/citrix/summary
curl.exe http://localhost:8080/microsoft365/summary
curl.exe http://localhost:8080/glpi/summary
```
