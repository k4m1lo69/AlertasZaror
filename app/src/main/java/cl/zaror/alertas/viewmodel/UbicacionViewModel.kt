
package cl.zaror.alertas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.zaror.alertas.data.ClimaActual
import cl.zaror.alertas.data.ClimaRepository
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

sealed interface EstadoClima {
    data object Inactivo : EstadoClima
    data object Consultando : EstadoClima
    data class Disponible(val clima: ClimaActual) : EstadoClima
    data class Error(val mensaje: String) : EstadoClima
}

class UbicacionViewModel(
    private val geocoding: GeocodingRepository,
    private val clima: ClimaRepository,
    private val fuente: FuenteUbicacion
) : ViewModel() {

    // Estado independiente para la dirección.
    private val _estado = MutableStateFlow<EstadoUbicacion>(
        EstadoUbicacion.Inactivo
    )
    val estado: StateFlow<EstadoUbicacion> = _estado

    // Estado independiente para el clima.
    private val _estadoClima = MutableStateFlow<EstadoClima>(
        EstadoClima.Inactivo
    )
    val estadoClima: StateFlow<EstadoClima> = _estadoClima

    /** Obtiene el GPS y lo convierte en dirección mediante Nominatim. */
    fun buscar() {
        if (_estado.value is EstadoUbicacion.Buscando) return

        _estado.value = EstadoUbicacion.Buscando

        viewModelScope.launch {
            _estado.value = try {
                val coordenadas = fuente.actual()

                if (coordenadas == null) {
                    EstadoUbicacion.Error(
                        "No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo"
                    )
                } else {
                    val direccion = geocoding.direccionDe(coordenadas)

                    if (direccion == null) {
                        EstadoUbicacion.Error(
                            "No se encontró una dirección para tu ubicación"
                        )
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

    /** Consulta Open-Meteo usando el GPS, sin depender de la búsqueda de dirección. */
    fun consultarClima() {
        if (_estadoClima.value is EstadoClima.Consultando) return

        _estadoClima.value = EstadoClima.Consultando

        viewModelScope.launch {
            _estadoClima.value = try {
                val coordenadas = fuente.actual()

                if (coordenadas == null) {
                    EstadoClima.Error(
                        "No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo"
                    )
                } else {
                    val resultado = clima.consultar(coordenadas)
                    EstadoClima.Disponible(resultado)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                EstadoClima.Error("Falta el permiso de ubicación")
            } catch (e: Exception) {
                EstadoClima.Error(mensajeDeError(e))
            }
        }
    }

    /** La pantalla ya utilizó el resultado de la dirección. */
    fun consumir() {
        _estado.value = EstadoUbicacion.Inactivo
    }

    /** Permite limpiar el resultado meteorológico de la pantalla. */
    fun consumirClima() {
        _estadoClima.value = EstadoClima.Inactivo
    }
}
