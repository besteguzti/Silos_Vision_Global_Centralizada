import { useEffect, useRef, useState } from 'react'

function KpiCard({ title, value, critical = false, status, info, onValueClick }) {
    const [isInfoOpen, setIsInfoOpen] = useState(false)
    const cardRef = useRef(null)
    const hasInfo = Boolean(info)
    const isValueClickable = typeof onValueClick === 'function'
    const tone = critical ? 'danger' : normalizeStatus(status)
    const classNames = ['kpi-card']

    // Cierra el popover informativo al hacer click fuera de la tarjeta.
    useEffect(() => {
        if (!isInfoOpen) {
            return undefined
        }

        const handleOutsideClick = (event) => {
            if (
                cardRef.current
                && !cardRef.current.contains(event.target)
            ) {
                setIsInfoOpen(false)
            }
        }

        document.addEventListener('mousedown', handleOutsideClick)

        return () => {
            document.removeEventListener('mousedown', handleOutsideClick)
        }
    }, [isInfoOpen])

    if (hasInfo) {
        classNames.push('kpi-card-with-info')
    }

    if (tone === 'ok') {
        classNames.push('kpi-card-ok')
    }

    if (tone === 'danger') {
        classNames.push('kpi-card-critical')
    }

    if (tone === 'warning') {
        classNames.push('kpi-card-warning')
    }

    if (tone === 'no-data') {
        classNames.push('kpi-card-no-data')
    }

    return (

        <article ref={cardRef} className={classNames.join(' ')}>

            <h2>{title}</h2>

            {isValueClickable ? (
                <button
                    type="button"
                    className="clickable-kpi-value"
                    onClick={onValueClick}
                    aria-label={`Ver detalle de ${title}`}
                >
                    {value}
                </button>
            ) : (
                <p>{value}</p>
            )}

            {hasInfo && (
                <>
                    <button
                        type="button"
                        className="kpi-info-button"
                        aria-label={`Información sobre ${title}`}
                        aria-expanded={isInfoOpen}
                        onClick={() => setIsInfoOpen(!isInfoOpen)}
                    >
                        i
                    </button>

                    {isInfoOpen && (
                        <div className="kpi-info-popover">
                            <h3 className="kpi-info-title">{title}</h3>

                            <div className="kpi-info-section">
                                <strong>Explicación</strong>
                                <p>{info.description}</p>
                            </div>

                            <div className="kpi-info-section">
                                <strong>Algoritmo</strong>
                                <p>{info.algorithm}</p>
                            </div>

                            <div className="kpi-info-section">
                                <strong>Interpretación</strong>
                                <p>{info.interpretation}</p>
                            </div>

                        </div>
                    )}
                </>
            )}

        </article>
    )
}

function normalizeStatus(status) {
    // El backend decide el estado; la tarjeta solo traduce estados explícitos a clases CSS.
    // Null o vacío se tratan como neutro para no pintar como GREEN ni como RED una tarjeta informativa.
    const normalized =
        status === null || status === undefined
            ? 'NEUTRAL'
            : String(status).trim().toUpperCase()

    if (normalized === '') {
        return 'neutral'
    }

    if (
        normalized === 'RED'
        || normalized === 'DANGER'
        || normalized === 'INCIDENT'
    ) {
        return 'danger'
    }

    if (
        normalized === 'NO_DATA'
        || normalized === 'STALE'
    ) {
        return 'no-data'
    }

    if (
        normalized === 'YELLOW'
        || normalized === 'WARNING'
        || normalized === 'WARN'
        || normalized === 'DEGRADED'
    ) {
        return 'warning'
    }

    if (
        normalized === 'GREEN'
        || normalized === 'OK'
        || normalized === 'HEALTHY'
    ) {
        return 'ok'
    }

    if (
        normalized === 'NEUTRAL'
        || normalized === 'INFO'
        || normalized === 'UNKNOWN'
    ) {
        return 'neutral'
    }

    return 'neutral'
}

export default KpiCard

