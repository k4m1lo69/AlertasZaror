package cl.zaror.alertas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zaror.alertas.data.FuenteUbicacion
import cl.zaror.alertas.data.GeocodingRepository
import cl.zaror.alertas.data.remote.mensajeDeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface EstadoUbicacion {
    data object Inactivo : EstadoUbicacion
    data object Buscando : EstadoUbicacion
    data class Lista(val direccion: String) : EstadoUbicacion
    data class Error(val mensaje: String) : EstadoUbicacion
}

class UbicacionViewModel(
    private val geocoding: GeocodingRepository,
    private val fuente: FuenteUbicacion
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoUbicacion>(EstadoUbicacion.Inactivo)
    val estado: StateFlow<EstadoUbicacion> = _estado

    /** Toma el GPS y lo convierte en dirección. Ignora toques repetidos mientras busca. */
    fun buscar() {
        if (_estado.value is EstadoUbicacion.Buscando) return
        _estado.value = EstadoUbicacion.Buscando

        viewModelScope.launch {
            _estado.value = try {
                val coordenadas = fuente.actual()
                if (coordenadas == null) {
                    EstadoUbicacion.Error("No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo")
                } else {
                    val direccion = geocoding.direccionDe(coordenadas)
                    if (direccion == null) {
                        EstadoUbicacion.Error("No se encontró una dirección para tu ubicación")
                    } else {
                        EstadoUbicacion.Lista(direccion)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                EstadoUbicacion.Error("Falta el permiso de ubicación")
            } catch (e: Exception) {
                EstadoUbicacion.Error(mensajeDeError(e))
            }
        }
    }

    /** La pantalla ya usó el resultado (la dirección o el error). */
    fun consumir() {
        _estado.value = EstadoUbicacion.Inactivo
    }
}
