package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.ClimaApi

data class ClimaActual(
    val hora: String?,
    val zonaHoraria: String?,
    val temperatura: Double?,
    val humedad: Double?,
    val precipitacion: Double?,
    val codigoClima: Int?,
    val velocidadViento: Double?
)

class ClimaRepository(
    private val api: ClimaApi
) {

    suspend fun consultar(coordenadas: Coordenadas): ClimaActual {
        val respuesta = api.consultarClima(
            latitud = coordenadas.latitud,
            longitud = coordenadas.longitud
        )

        val actual = respuesta.current
            ?: throw IllegalStateException(
                "Open-Meteo no entregó las condiciones meteorológicas"
            )

        return ClimaActual(
            hora = actual.time,
            zonaHoraria = respuesta.timezone,
            temperatura = actual.temperatura,
            humedad = actual.humedad,
            precipitacion = actual.precipitation,
            codigoClima = actual.codigoClima,
            velocidadViento = actual.velocidadViento
        )
    }
}