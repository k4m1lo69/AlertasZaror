package cl.zaror.alertas.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface ClimaApi {

    @GET("v1/forecast")
    suspend fun consultarClima(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") variables: String =
            "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m",
        @Query("timezone") zonaHoraria: String = "auto",
        @Query("wind_speed_unit") unidadViento: String = "kmh",
        @Query("temperature_unit") unidadTemperatura: String = "celsius"
    ): ClimaRespuesta
}