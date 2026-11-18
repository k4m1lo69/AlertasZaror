package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.FormatoDireccion
import cl.zaror.alertas.data.remote.GeocodingApi

/** Convierte coordenadas en una dirección legible. No toca Room ni el microservicio propio. */
class GeocodingRepository(
    private val api: GeocodingApi
) {

    /** Devuelve la dirección, o null si el servicio no encontró ninguna para esas coordenadas. */
    suspend fun direccionDe(coordenadas: Coordenadas): String? {
        return FormatoDireccion.armar(api.direccionDe(coordenadas.latitud, coordenadas.longitud))
    }
}
