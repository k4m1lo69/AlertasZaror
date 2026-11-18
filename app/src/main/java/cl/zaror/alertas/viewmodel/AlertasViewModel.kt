package cl.zaror.alertas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zaror.alertas.data.Alerta
import cl.zaror.alertas.data.AlertasRepository
import cl.zaror.alertas.data.remote.mensajeDeError
import cl.zaror.alertas.domain.Permisos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlertasViewModel(
    private val repositorio: AlertasRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)

    /** Último error al hablar con el servidor, listo para mostrarse. */
    val error: StateFlow<String?> = _error

    fun limpiarError() {
        _error.value = null
    }

    /**
     * Alertas que el usuario tiene permiso de ver:
     * el administrador ve todas; el vecino solo las suyas.
     */
    fun alertasVisibles(usuario: UsuarioSesion): Flow<List<Alerta>> {
        return if (Permisos.puedeVerTodas(usuario.rol)) {
            repositorio.observarTodas()
        } else {
            repositorio.observarDeUsuario(usuario.id)
        }
    }

    /** Trae las alertas del servidor. Si falla, se sigue mostrando la copia local. */
    fun refrescar(usuario: UsuarioSesion) {
        ejecutar { repositorio.refrescar(usuario.id) }
    }

    /** Crea una alerta identificando siempre al usuario de la sesión. */
    fun registrarAlerta(
        usuario: UsuarioSesion,
        tipo: String,
        descripcion: String,
        ubicacion: String,
        fotoUri: String? = null
    ) {
        ejecutar {
            repositorio.guardarAlerta(
                Alerta(
                    tipo = tipo,
                    descripcion = descripcion,
                    ubicacion = ubicacion,
                    fotoUri = fotoUri,
                    reportadaPor = usuario.nombre,
                    usuarioId = usuario.id
                )
            )
        }
    }

    fun marcarComoAtendida(usuario: UsuarioSesion, id: Int) {
        if (!Permisos.puedeMarcarAtendida(usuario.rol)) return
        ejecutar { repositorio.marcarAtendida(id) }
    }

    fun cancelarAlerta(usuario: UsuarioSesion, id: Int) {
        ejecutar { repositorio.cancelarPropia(id) }
    }

    fun eliminarAlerta(usuario: UsuarioSesion, id: Int) {
        if (!Permisos.puedeEliminar(usuario.rol)) return
        ejecutar { repositorio.eliminarAlerta(id) }
    }

    private fun ejecutar(operacion: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                operacion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = mensajeDeError(e)
            }
        }
    }
}
