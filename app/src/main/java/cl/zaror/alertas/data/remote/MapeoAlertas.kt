package cl.zaror.alertas.data.remote

import cl.zaror.alertas.data.AlertaEntity
import cl.zaror.alertas.domain.Permisos
import java.time.Instant

/**
 * Traduce entre lo que muestra la app ("Emergencia", "Activa") y lo que guarda el servidor
 * ("EMERGENCIA", "ACTIVA"). Funciones puras: se prueban sin Android.
 */
object MapeoAlertas {

    private val tiposAServidor = mapOf(
        "Emergencia" to "EMERGENCIA",
        "Falla de alarma" to "FALLA_ALARMA",
        "Solicitud de apoyo" to "SOLICITUD_APOYO"
    )

    private val tiposDesdeServidor = tiposAServidor.entries.associate { (app, servidor) -> servidor to app }

    private val estadosDesdeServidor = mapOf(
        "ACTIVA" to Permisos.ESTADO_ACTIVA,
        "ATENDIDA" to Permisos.ESTADO_ATENDIDA,
        "CANCELADA" to Permisos.ESTADO_CANCELADA
    )

    const val SERVIDOR_ATENDIDA = "ATENDIDA"
    const val SERVIDOR_CANCELADA = "CANCELADA"

    fun tipoAServidor(tipo: String): String = tiposAServidor[tipo.trim()] ?: tipo.trim()

    fun tipoDesdeServidor(tipo: String): String = tiposDesdeServidor[tipo] ?: tipo

    fun estadoDesdeServidor(estado: String): String = estadosDesdeServidor[estado] ?: estado

    /** Acepta fecha ISO ("2026-11-12T12:00:00Z") o segundos con decimales; si falla, usa ahora. */
    fun fechaAMilisegundos(texto: String?, ahora: Long = System.currentTimeMillis()): Long {
        if (texto.isNullOrBlank()) return ahora
        return runCatching { Instant.parse(texto).toEpochMilli() }
            .recoverCatching { (texto.toDouble() * 1000).toLong() }
            .getOrDefault(ahora)
    }

    /**
     * El id del servidor pasa a ser el id de la fila local. usuarioLocalId es el usuario de esta
     * sesión: el servidor solo devuelve al vecino sus propias alertas, y el administrador no
     * filtra por dueño, así que ese valor basta para el caché local.
     */
    fun AlertaResponse.aEntidad(usuarioLocalId: Int): AlertaEntity {
        return AlertaEntity(
            id = id.toInt(),
            tipo = tipoDesdeServidor(tipo),
            descripcion = descripcion,
            ubicacion = ubicacion,
            fotoUri = fotoUri,
            estado = estadoDesdeServidor(estado),
            fechaHora = fechaAMilisegundos(fechaHora),
            reportadaPor = reportadaPor ?: "Vecino",
            usuarioId = usuarioLocalId
        )
    }
}
