import { useEffect, useState } from 'react'

import { API_BASE_URL } from '../config/api'

function InactiveApsPanel({ onBack }) {
  const [aps, setAps] = useState([])
  const [annotations, setAnnotations] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [savingSerial, setSavingSerial] = useState(null)
  const [rowMessages, setRowMessages] = useState({})

  useEffect(() => {
    let cancelled = false

    fetch(`${API_BASE_URL}/aruba/inactive-aps`)
      .then(response => {
        if (!response.ok) {
          throw new Error('No se pudo cargar el detalle de APs inactivos.')
        }

        return response.json()
      })
      .then(data => {
        if (cancelled) {
          return
        }

        setAps(data)
        setAnnotations(Object.fromEntries(
          data.map(ap => [ap.serial, ap.annotation ?? ''])
        ))
        setError(null)
      })
      .catch(err => {
        if (!cancelled) {
          setError(err.message || 'No se pudo cargar el detalle de APs inactivos.')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const updateAnnotation = (serial, value) => {
    setAnnotations(current => ({
      ...current,
      [serial]: value
    }))
    setRowMessages(current => ({
      ...current,
      [serial]: null
    }))
  }

  const saveAnnotation = async (serial) => {
    setSavingSerial(serial)
    setRowMessages(current => ({
      ...current,
      [serial]: null
    }))

    try {
      const response = await fetch(`${API_BASE_URL}/aruba/inactive-aps/${encodeURIComponent(serial)}/annotation`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          annotation: annotations[serial] ?? ''
        })
      })

      if (!response.ok) {
        throw new Error('Error guardando anotacion.')
      }

      const saved = await response.json()
      setAps(current => current.map(ap => (
        ap.serial === serial
          ? { ...ap, annotation: saved.annotation ?? '' }
          : ap
      )))
      setAnnotations(current => ({
        ...current,
        [serial]: saved.annotation ?? ''
      }))
      setRowMessages(current => ({
        ...current,
        [serial]: 'Guardado'
      }))
    } catch (err) {
      setRowMessages(current => ({
        ...current,
        [serial]: err.message || 'Error guardando anotacion'
      }))
    } finally {
      setSavingSerial(null)
    }
  }

  return (
    <section className="inactive-aps-panel">
      <div className="inactive-aps-header">
        <div>
          <p className="eyebrow">Detalle Aruba</p>
          <h2>APs inactivos</h2>
          <p>
            Estos APs se calculan con la fecha real de ultimo contacto en Aruba
            y el umbral configurable de APs inactivos. Las anotaciones son
            manuales y no se sobrescriben durante la sincronizacion.
          </p>
        </div>

        <button type="button" className="secondary-action" onClick={onBack}>
          Volver
        </button>
      </div>

      {loading && <p className="loading">Cargando APs inactivos...</p>}
      {error && <section className="alert">{error}</section>}

      {!loading && !error && aps.length === 0 && (
        <section className="success-message">
          No hay APs inactivos con el umbral actual.
        </section>
      )}

      {!loading && !error && aps.length > 0 && (
        <div className="inactive-aps-table-wrapper">
          <table className="inactive-aps-table">
            <thead>
              <tr>
                <th>Nombre</th>
                <th>Serial</th>
                <th>Estado</th>
                <th>Última vez visto</th>
                <th>Dias inactivo</th>
                <th>Anotacion</th>
                <th>Acción</th>
              </tr>
            </thead>
            <tbody>
              {aps.map(ap => (
                <tr key={ap.serial}>
                  <td>{ap.name || 'Sin nombre'}</td>
                  <td>{ap.serial}</td>
                  <td>{ap.status || 'Sin estado'}</td>
                  <td>{formatDate(ap.lastSeenAt)}</td>
                  <td>{ap.daysInactive}</td>
                  <td>
                    <textarea
                      className="inactive-aps-note-input"
                      value={annotations[ap.serial] ?? ''}
                      maxLength={1000}
                      onChange={(event) => updateAnnotation(ap.serial, event.target.value)}
                      aria-label={`Anotacion para ${ap.name || ap.serial}`}
                    />
                    {rowMessages[ap.serial] && (
                      <small className={rowMessages[ap.serial] === 'Guardado' ? 'row-save-ok' : 'row-save-error'}>
                        {rowMessages[ap.serial]}
                      </small>
                    )}
                  </td>
                  <td>
                    <button
                      type="button"
                      className="inactive-aps-save-button"
                      onClick={() => saveAnnotation(ap.serial)}
                      disabled={savingSerial === ap.serial}
                    >
                      {savingSerial === ap.serial ? 'Guardando...' : 'Guardar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

function formatDate(value) {
  if (!value) {
    return 'Sin datos'
  }

  return new Date(value).toLocaleString()
}

export default InactiveApsPanel

