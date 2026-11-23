package cl.zaror.alertas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.zaror.alertas.data.TokenManager
import cl.zaror.alertas.data.remote.AprobarRequest
import cl.zaror.alertas.data.remote.AuthApi
import cl.zaror.alertas.data.remote.CuadraResponse
import cl.zaror.alertas.data.remote.RolRequest
import cl.zaror.alertas.data.remote.UsuarioResponse
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Mensaje para el usuario: éxito o error. */
data class Aviso(val texto: String, val esError: Boolean = false)

data class ClaveTemporalInfo(val nombre: String, val clave: String)

data class EstadoAdmin(
    val filtro: String = "PENDIENTE",
    val usuarios: List<UsuarioResponse> = emptyList(),
    val cuadras: List<CuadraResponse> = emptyList(),
    val cargando: Boolean = false,
    val aviso: Aviso? = null,
    val claveTemporal: ClaveTemporalInfo? = null
)

/** Panel del administrador: aprobar vecinos, asignar roles y restablecer contraseñas. */
class AdminViewModel(
    private val api: AuthApi,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoAdmin())
    val estado: StateFlow<EstadoAdmin> = _estado.asStateFlow()

    fun cargar(filtro: String = _estado.value.filtro) {
        ejecutar { auth ->
            val lista = api.usuariosPorEstado(auth, filtro)
            val cuadras = _estado.value.cuadras.ifEmpty { api.cuadras(auth) }
            _estado.update { it.copy(filtro = filtro, usuarios = lista, cuadras = cuadras) }
        }
    }

    fun aprobar(id: Long, cuadraId: Long) {
        ejecutar("Vecino aprobado.") { auth ->
            api.aprobarUsuario(auth, id, AprobarRequest(cuadraId))
            recargar(auth)
        }
    }

    fun rechazar(id: Long) {
        ejecutar("Solicitud rechazada.") { auth ->
            api.rechazarUsuario(auth, id)
            recargar(auth)
        }
    }

    /** El nuevo rol se aplica la próxima vez que esa persona inicie sesión. */
    fun cambiarRol(id: Long, rol: String) {
        ejecutar("Rol actualizado. Se aplicará en su próximo inicio de sesión.") { auth ->
            api.cambiarRolUsuario(auth, id, RolRequest(rol))
            recargar(auth)
        }
    }

    fun restablecerClave(id: Long, nombre: String) {
        ejecutar { auth ->
            val respuesta = api.restablecerClaveUsuario(auth, id)
            _estado.update { it.copy(claveTemporal = ClaveTemporalInfo(nombre, respuesta.claveTemporal)) }
        }
    }

    fun descartarClave() {
        _estado.update { it.copy(claveTemporal = null) }
    }

    private suspend fun recargar(auth: String) {
        val filtro = _estado.value.filtro
        val lista = api.usuariosPorEstado(auth, filtro)
        _estado.update { it.copy(usuarios = lista) }
    }

    private fun ejecutar(exito: String? = null, operacion: suspend (String) -> Unit) {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, aviso = null) }
            try {
                val token = tokenManager.token.first()
                    ?: throw IllegalStateException("Tu sesión expiró. Inicia sesión de nuevo.")
                operacion("Bearer $token")
                if (exito != null) {
                    _estado.update { it.copy(aviso = Aviso(exito)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update { it.copy(aviso = Aviso(textoDeError(e), esError = true)) }
            } finally {
                _estado.update { it.copy(cargando = false) }
            }
        }
    }
}

class AdminViewModelFactory(
    private val api: AuthApi,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            return AdminViewModel(api, tokenManager) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}

/** Convierte un error de red o del servidor en un texto para el usuario. */
internal fun textoDeError(e: Exception): String {
    if (e is HttpException) {
        val cuerpo = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (!cuerpo.isNullOrBlank()) {
            try {
                val json = JsonParser.parseString(cuerpo).asJsonObject
                if (json.has("detalle") && json.get("detalle").isJsonObject) {
                    val primero = json.getAsJsonObject("detalle").entrySet().firstOrNull()
                    if (primero != null) return primero.value.asString
                }
                if (json.has("error")) return json.get("error").asString
            } catch (_: Exception) {
                // Se usa el mensaje según el código HTTP.
            }
        }
        return when (e.code()) {
            401 -> "Tu sesión expiró. Cierra sesión y vuelve a entrar."
            403 -> "No tienes permiso para esta acción."
            404 -> "No se encontró el usuario."
            429 -> "Demasiados intentos. Intenta más tarde."
            else -> "Error del servidor: ${e.code()}"
        }
    }
    return when (e) {
        is java.net.UnknownHostException,
        is java.net.ConnectException,
        is java.net.SocketTimeoutException -> "No se pudo conectar con el servidor."
        else -> e.message ?: "Ocurrió un error."
    }
}