package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.AlertaRequest
import cl.zaror.alertas.data.remote.AlertasApi
import cl.zaror.alertas.data.remote.EstadoRequest
import cl.zaror.alertas.data.remote.MapeoAlertas
import cl.zaror.alertas.data.remote.MapeoAlertas.aEntidad
import cl.zaror.alertas.domain.Permisos
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

/**
 * El servidor (Spring Boot) es la fuente de verdad. Room guarda una copia local para que la
 * interfaz siga observando el mismo Flow de siempre y se vea algo aunque no haya conexión.
 * Cada operación llama primero al servidor y solo si resulta bien actualiza la copia local.
 */
class AlertasRepository(
    private val alertaDao: AlertaDao,
    private val api: AlertasApi,
    private val tokenManager: TokenManager
) {

    /** Todas las alertas. Solo debe usarse para administradores. */
    fun observarTodas(): Flow<List<Alerta>> {
        return alertaDao.observarTodas().map { lista ->
            lista.map { entidad ->
                entidad.toModelo()
            }
        }
    }

    /** Solo las alertas del usuario indicado. */
    fun observarDeUsuario(usuarioId: Int): Flow<List<Alerta>> {
        return alertaDao.observarDeUsuario(usuarioId).map { lista ->
            lista.map { entidad ->
                entidad.toModelo()
            }
        }
    }

    /** Descarga las alertas que el servidor permite ver a este usuario y renueva la copia local. */
    suspend fun refrescar(usuarioLocalId: Int) {
        val entidades = api.listar(cabecera()).map { it.aEntidad(usuarioLocalId) }
        alertaDao.insertarOReemplazarTodas(entidades)
        alertaDao.eliminarFueraDe(entidades.map { it.id })
    }

    suspend fun guardarAlerta(alerta: Alerta) {
        val creada = api.crear(
            cabecera(),
            AlertaRequest(
                tipo = MapeoAlertas.tipoAServidor(alerta.tipo),
                descripcion = alerta.descripcion,
                ubicacion = alerta.ubicacion,
                fotoUri = alerta.fotoUri
            )
        )
        alertaDao.insertarOReemplazar(creada.aEntidad(alerta.usuarioId))
    }

    suspend fun marcarAtendida(id: Int) {
        api.cambiarEstado(cabecera(), id.toLong(), EstadoRequest(MapeoAlertas.SERVIDOR_ATENDIDA))
        alertaDao.actualizarEstado(id, Permisos.ESTADO_ATENDIDA)
    }

    /** Cancela una alerta del propio usuario que sigue activa. */
    suspend fun cancelarPropia(id: Int) {
        api.cambiarEstado(cabecera(), id.toLong(), EstadoRequest(MapeoAlertas.SERVIDOR_CANCELADA))
        alertaDao.actualizarEstado(id, Permisos.ESTADO_CANCELADA)
    }

    suspend fun eliminarAlerta(id: Int) {
        val respuesta = api.eliminar(cabecera(), id.toLong())
        if (!respuesta.isSuccessful) {
            throw HttpException(respuesta)
        }
        alertaDao.eliminar(id)
    }

    private suspend fun cabecera(): String {
        val token = tokenManager.token.first()
        check(!token.isNullOrBlank()) { "No hay sesión con el servidor. Cierra sesión e ingresa de nuevo" }
        return "Bearer $token"
    }

    private fun AlertaEntity.toModelo(): Alerta {
        return Alerta(
            id = id,
            tipo = tipo,
            descripcion = descripcion,
            ubicacion = ubicacion,
            fotoUri = fotoUri,
            estado = estado,
            fechaHora = fechaHora,
            reportadaPor = reportadaPor,
            usuarioId = usuarioId
        )
    }
}
