package cl.zaror.alertas.data.remote

import com.google.gson.annotations.SerializedName

data class ClimaRespuesta(
    val timezone: String?,

    @SerializedName("timezone_abbreviation")
    val abreviaturaZona: String?,

    val current: ClimaActualRespuesta?
)

data class ClimaActualRespuesta(
    val time: String?,

    @SerializedName("temperature_2m")
    val temperatura: Double?,

    @SerializedName("relative_humidity_2m")
    val humedad: Double?,

    val precipitation: Double?,

    @SerializedName("weather_code")
    val codigoClima: Int?,

    @SerializedName("wind_speed_10m")
    val velocidadViento: Double?
)