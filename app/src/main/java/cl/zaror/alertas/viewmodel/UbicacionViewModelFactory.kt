
package cl.zaror.alertas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cl.zaror.alertas.data.ClimaRepository
import cl.zaror.alertas.data.FuenteUbicacion
import cl.zaror.alertas.data.GeocodingRepository

class UbicacionViewModelFactory(
    private val geocoding: GeocodingRepository,
    private val clima: ClimaRepository,
    private val fuente: FuenteUbicacion
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UbicacionViewModel::class.java)) {
            return UbicacionViewModel(
                geocoding = geocoding,
                clima = clima,
                fuente = fuente
            ) as T
        }

        throw IllegalArgumentException(
            "ViewModel desconocido: ${modelClass.name}"
        )
    }
}
