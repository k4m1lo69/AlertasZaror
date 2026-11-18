package cl.zaror.alertas.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

data class DireccionNominatim(
    @SerializedName("house_number") val numero: String?,
    @SerializedName("road") val calle: String?,
    @SerializedName("neighbourhood") val barrio: String?,
    @SerializedName("suburb") val sector: String?,
    @SerializedName("city") val ciudad: String?,
    @SerializedName("town") val pueblo: String?,
    @SerializedName("village") val aldea: String?,
    @SerializedName("municipality") val municipio: String?
)

data class GeocodingResponse(
    @SerializedName("display_name") val nombreCompleto: String?,
    @SerializedName("address") val direccion: DireccionNominatim?,
    @SerializedName("error") val error: String?
)

/**
 * API externa de OpenStreetMap (Nominatim). No usa el token del microservicio propio.
 * Su política de uso exige identificar la app con User-Agent y no pasar de 1 consulta por segundo.
 */
interface GeocodingApi {

    @Headers(
        "User-Agent: AlertasZaror/1.0 (proyecto academico DUOC UC)",
        "Accept-Language: es"
    )
    @GET("reverse?format=jsonv2&addressdetails=1&zoom=18")
    suspend fun direccionDe(
        @Query("lat") latitud: Double,
        @Query("lon") longitud: Double
    ): GeocodingResponse
}
