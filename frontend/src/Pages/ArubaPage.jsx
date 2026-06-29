import { useEffect, useState } from 'react'

import "../App.css";
import InactiveApsPanel from '../components/InactiveApsPanel'
import KpiCard from '../components/KpiCard'
import { API_BASE_URL, FRONTEND_REFRESH_INTERVAL_MS } from '../config/api'
import { formatDataStatus, formatStatus } from '../utils/statusFormatters'

const arubaKpiInfo = {
  totalAps: {
    description: 'Número total de Access Points guardados en el dashboard.',
    algorithm: 'Se obtiene desde /aruba/summary a partir de los APs almacenados en MySQL tras la sincronización de Aruba.',
    interpretation: 'Permite conocer el tamaño del parque WiFi gestionado. Cambios bruscos pueden indicar altas, bajas o problemas de sincronización.'
  },
  upAps: {
    description: 'Access Points que aparecen como activos.',
    algorithm: 'Se calcula en backend a partir del estado de los APs almacenados y se expone en /aruba/summary.',
    interpretation: 'Un valor cercano al total indica buena disponibilidad de la red WiFi.'
  },
  downAps: {
    description: 'Access Points detectados como caídos o no operativos.',
    algorithm: 'Se calcula en backend comparando estados de AP almacenados y se expone en /aruba/summary.',
    interpretation: 'Si es mayor que cero conviene revisar cobertura, alimentación, conectividad o estado del equipo.'
  },
  firmwareOutdated: {
    description: 'APs con firmware pendiente de actualización.',
    algorithm: 'Se calcula con los datos de firmware sincronizados desde Aruba y guardados para el resumen.',
    interpretation: 'Un valor mayor que cero indica equipos pendientes de mantenimiento o actualización.'
  },
  inactiveAps: {
    description: 'APs que no han sido vistos recientemente según el histórico disponible.',
    algorithm: 'El backend calcula el valor a partir de la última actividad registrada por número de serie.',
    interpretation: 'Un valor mayor que cero puede indicar APs retirados, apagados o con problemas de comunicación.'
  },
  totalSwitches: {
    description: 'Número total de switches Aruba guardados en MySQL.',
    algorithm: 'Se obtiene desde /aruba/summary a partir de la tabla de switches sincronizada con Aruba.',
    interpretation: 'Permite conocer el inventario de switches gestionados por el dashboard.'
  },
  downSwitches: {
    description: 'Switches que aparecen apagados o no disponibles.',
    algorithm: 'Se calcula en backend a partir del estado de los switches almacenados.',
    interpretation: 'Si es mayor que cero debe revisarse disponibilidad, alimentación o conectividad de esos switches.'
  },
  switchesFirmwareUpgradeRequired: {
    description: 'Switches que necesitan actualización de firmware.',
    algorithm: 'Se calcula con los datos de firmware de switches sincronizados desde Aruba.',
    interpretation: 'Un valor mayor que cero indica mantenimiento pendiente en la capa de switching.'
  },
  underusedSwitches: {
    description: 'Switches considerados infrautilizados por interfaces down persistentes.',
    algorithm: 'El backend usa el histórico de interfaces down y muestra switches que superan el umbral definido durante el periodo analizado.',
    interpretation: 'Ayuda a detectar switches con poca utilización sostenida para revisar capacidad o redistribución.'
  },
  totalWifiClients: {
    description: 'Total de clientes WiFi observados por Aruba.',
    algorithm: 'Se calcula desde los clientes WiFi obtenidos por Aruba y agregados en el resumen almacenado.',
    interpretation: 'Indica actividad inalámbrica total observada en el momento de la sincronización.'
  },
  arubaOpenTickets: {
    description: 'Tickets abiertos GLPI asociados a Aruba.',
    algorithm: 'El backend lee el último snapshot GLPI y devuelve arubaOpenTickets dentro del resumen Aruba.',
    interpretation: 'Permite relacionar la afección de red con carga operativa clasificada como Aruba, sin afirmar causalidad.'
  },
  mutualiaApsClients: {
    description: 'Clientes WiFi asociados al grupo MUTUALIA-APs.',
    algorithm: 'Se filtran los clientes WiFi por group_name MUTUALIA-APs en los datos recibidos de Aruba.',
    interpretation: 'Permite separar actividad del grupo de APs respecto al resto de redes WiFi.'
  },
  mutualiaWifiClients: {
    description: 'Clientes WiFi asociados al grupo MUTUALIA-WIFI.',
    algorithm: 'Se filtran los clientes WiFi por group_name MUTUALIA-WIFI en los datos recibidos de Aruba.',
    interpretation: 'Permite ver la actividad del grupo principal de redes WiFi Mutualia.'
  },
  mutualiaLangileakClients: {
    description: 'Clientes conectados a la red MUTUALIA_LANGILEAK.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network MUTUALIA_LANGILEAK.',
    interpretation: 'Ayuda a ver cuándo esta red concentra más actividad.'
  },
  mutualiaClients: {
    description: 'Clientes conectados a la red MUTUALIA.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network MUTUALIA.',
    interpretation: 'Permite analizar la actividad concreta de la red MUTUALIA.'
  },
  mutualiaRedInternaClients: {
    description: 'Clientes conectados a MUTUALIA_RED_INTERNA.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network MUTUALIA_RED_INTERNA.',
    interpretation: 'Ayuda a controlar el uso de la red interna.'
  },
  mutualiaRedExternaClients: {
    description: 'Clientes conectados a MUTUALIA_RED_EXTERNA.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network MUTUALIA_RED_EXTERNA.',
    interpretation: 'Ayuda a controlar el uso de la red externa.'
  },
  mutualiaKorporatiboaClients: {
    description: 'Clientes conectados a MUTUALIA_KORPORATIBOA.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network MUTUALIA_KORPORATIBOA.',
    interpretation: 'Permite analizar el uso de esta red corporativa específica.'
  },
  wifiPacsClients: {
    description: 'Clientes conectados a la red WIFI_PACs.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network WIFI_PACs.',
    interpretation: 'Permite controlar la actividad de la red PACS.'
  },
  mutVideoClients: {
    description: 'Clientes conectados a la red MUT_VIDEO.',
    algorithm: 'Se filtran clientes del grupo MUTUALIA-WIFI por el campo network MUT_VIDEO.',
    interpretation: 'Permite controlar la actividad de la red de vídeo.'
  }
}

function ArubaPage() {

  const [summary, setSummary] = useState(null)
  const [error, setError] = useState(null)
  const [showInactiveApsPanel, setShowInactiveApsPanel] = useState(false)

  const loadDashboard = () => {
    // ArubaPage pinta datos ya sincronizados y calculados en backend; no llama
    // directamente a Aruba Central desde el navegador.
    fetch(`${API_BASE_URL}/aruba/summary`)
      .then(response => {

        if (!response.ok) {

          throw new Error('No se pudo cargar el resumen Aruba')
        }

        return response.json()
      })
      .then(data => {

        setSummary(data)
        setError(null)
      })
      .catch(() => {

        setError('No se pudo conectar con el backend o Aruba Central.')
      })
  }

  useEffect(() => {

    loadDashboard()

    const interval = setInterval(() => {

      loadDashboard()

    }, FRONTEND_REFRESH_INTERVAL_MS)

    return () => clearInterval(interval)

  }, [])

  if (!summary && !error) {

    return (
      <main className="dashboard">
        <h1>TFG Dashboard</h1>
        <p className="loading">Cargando dashboard...</p>
      </main>
    )
  }

  const networkStatusDetails = summary?.networkStatusDetails
  const status = networkStatusDetails?.color ?? 'UNKNOWN'
  const statusReasons = networkStatusDetails?.reasons ?? []
  const kpiStatuses = summary?.kpiStatuses ?? networkStatusDetails?.indicatorStatuses ?? {}
  const statusFor = (key, fallback = 'NEUTRAL') => kpiStatuses[key] ?? fallback

  const apCards = summary
    ? [
      { title: 'Total APs', value: summary.totalAps, status: statusFor('totalAps'), info: arubaKpiInfo.totalAps },
      {
        title: 'APs activos',
        value: summary.upAps,
        status: statusFor('upAps'),
        info: arubaKpiInfo.upAps
      },
      {
        title: 'APs caidos',
        value: summary.downAps,
        status: statusFor('downAps', 'NO_DATA'),
        info: arubaKpiInfo.downAps
      },
      {
        title: 'Firmware pendiente',
        value: summary.firmwareOutdated,
        status: statusFor('firmwareOutdated', 'NO_DATA'),
        info: arubaKpiInfo.firmwareOutdated
      },
      {
        title: 'APs inactivos',
        value: summary.inactiveAps,
        status: statusFor('inactiveAps', 'NO_DATA'),
        info: arubaKpiInfo.inactiveAps,
        onValueClick: () => setShowInactiveApsPanel(true)
      }
    ]
    : []

  const switchCards = summary
    ? [
      { title: 'Total switches', value: summary.totalSwitches, status: statusFor('totalSwitches'), info: arubaKpiInfo.totalSwitches },
      {
        title: 'Switches apagados',
        value: summary.downSwitches,
        status: statusFor('downSwitches', 'NO_DATA'),
        info: arubaKpiInfo.downSwitches
      },
      {
        title: 'Switches con upgrade',
        value: summary.switchesFirmwareUpgradeRequired,
        status: statusFor('switchesFirmwareUpgradeRequired'),
        info: arubaKpiInfo.switchesFirmwareUpgradeRequired
      }
    ]
    : []

  const underusedSwitchCards = summary
    ? (summary.underusedSwitches ?? []).map(switchUsage => ({
      title: switchUsage.associatedDeviceName || switchUsage.associatedDevice,
      value: `${switchUsage.downInterfaces} interfaces down`,
      status: statusFor('underusedSwitches'),
      info: arubaKpiInfo.underusedSwitches
    }))
    : []

  const wifiGroupCards = summary
    ? [
      {
        title: 'Total clientes WiFi',
        value: summary.totalWifiClients,
        status: statusFor('totalWifiClients', 'NO_DATA'),
        info: arubaKpiInfo.totalWifiClients
      },
      {
        title: 'Clientes MUTUALIA-APs',
        value: summary.mutualiaApsClients,
        status: statusFor('mutualiaApsClients'),
        info: arubaKpiInfo.mutualiaApsClients
      },
      {
        title: 'Clientes MUTUALIA-WIFI',
        value: summary.mutualiaWifiClients,
        status: statusFor('mutualiaWifiClients'),
        info: arubaKpiInfo.mutualiaWifiClients
      }
    ]
    : []

  const wifiNetworkCards = summary
    ? [
      // Redes informativas: si no son críticas, se muestran neutras y no como error.
      { title: 'MUTUALIA_LANGILEAK', value: summary.mutualiaLangileakClients, info: arubaKpiInfo.mutualiaLangileakClients },
      { title: 'MUTUALIA', value: summary.mutualiaClients, info: arubaKpiInfo.mutualiaClients },
      { title: 'MUTUALIA_RED_INTERNA', value: summary.mutualiaRedInternaClients, info: arubaKpiInfo.mutualiaRedInternaClients },
      { title: 'MUTUALIA_RED_EXTERNA', value: summary.mutualiaRedExternaClients, info: arubaKpiInfo.mutualiaRedExternaClients },
      { title: 'MUTUALIA_KORPORATIBOA', value: summary.mutualiaKorporatiboaClients, info: arubaKpiInfo.mutualiaKorporatiboaClients },
      { title: 'WIFI_PACs', value: summary.wifiPacsClients, info: arubaKpiInfo.wifiPacsClients },
      { title: 'MUT_VIDEO', value: summary.mutVideoClients, info: arubaKpiInfo.mutVideoClients }
    ]
    : []

  return (
    <main className="dashboard">
      <header className="dashboard-header">
        <div>
          <p className="eyebrow">Monitorización Aruba</p>
          <h1>TFG Dashboard</h1>
        </div>

        {summary && (
          <div className="freshness">
            <p className="updated">
              Última actualización: {formatSnapshotDate(summary.lastUpdated)}
            </p>
            <p className="updated">
              Estado de datos:{" "}
              <span className={`freshness-status freshness-status-${(summary.dataStatus ?? "NO_DATA").toLowerCase()}`}>
                {formatDataStatus(summary.dataStatus)}
              </span>
            </p>
          </div>
        )}
      </header>

      {error && (
        <section className="alert" role="alert">
          {error}
        </section>
      )}

      {summary && showInactiveApsPanel && (
        <InactiveApsPanel onBack={() => setShowInactiveApsPanel(false)} />
      )}

      {summary && !showInactiveApsPanel && (
        <>
          <section className={`status status-${status.toLowerCase()}`}>
            <div className="status-main">
              <span>Índice de salud Aruba</span>
              <strong>Afección: {networkStatusDetails?.percentage ?? 0} %</strong>
              <p>Estado: {formatStatus(status)}</p>
            </div>

            <div className="status-reasons">
              <span>Motivos</span>
              {statusReasons.length > 0 ? (
                <ul>
                  {statusReasons.slice(0, 4).map(reason => (
                    <li key={reason}>{reason}</li>
                  ))}
                </ul>
              ) : (
                <p>Sin motivos activos</p>
              )}
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Soporte GLPI asociado</h2>

            <div className="kpi-grid">
              <KpiCard
                title="Tickets abiertos Aruba"
                value={summary.arubaOpenTickets}
                status={statusFor('arubaOpenTickets', 'NO_DATA')}
                info={arubaKpiInfo.arubaOpenTickets}
              />
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Access Points</h2>

            <div className="kpi-grid">
              {apCards.map(card => (
                <KpiCard
                  key={card.title}
                  title={card.title}
                  value={card.value}
                  status={card.status}
                  info={card.info}
                  onValueClick={card.onValueClick}
                />
              ))}
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Clientes WiFi por grupo</h2>

            <div className="kpi-grid">
              {wifiGroupCards.map(card => (
                <KpiCard
                  key={card.title}
                  title={card.title}
                  value={card.value}
                  status={card.status}
                  info={card.info}
                />
              ))}
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Clientes MUTUALIA-WIFI por red</h2>

            <div className="kpi-grid">
              {wifiNetworkCards.map(card => (
                <KpiCard
                  key={card.title}
                  title={card.title}
                  value={card.value}
                  info={card.info}
                />
              ))}
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Switches</h2>

            <div className="kpi-grid">
              {switchCards.map(card => (
                <KpiCard
                  key={card.title}
                  title={card.title}
                  value={card.value}
                  status={card.status}
                  info={card.info}
                />
              ))}
            </div>
          </section>

          <section className="dashboard-section">
            <h2>Switches infrautilizados</h2>

            <div className="kpi-grid">
              {underusedSwitchCards.length > 0 ? (
                underusedSwitchCards.map(card => (
                  <KpiCard
                    key={card.title}
                    title={card.title}
                    value={card.value}
                    status={card.status}
                    info={card.info}
                  />
                ))
              ) : (
                <KpiCard
                  title="Switches infrautilizados"
                  value="0"
                  status={statusFor('underusedSwitches')}
                  info={arubaKpiInfo.underusedSwitches}
                />
              )}
            </div>
          </section>
        </>
      )}
    </main>
  )
}

function formatSnapshotDate(value) {
  if (!value) {
    return "Sin datos";
  }

  return new Date(value).toLocaleString();
}

export default ArubaPage
