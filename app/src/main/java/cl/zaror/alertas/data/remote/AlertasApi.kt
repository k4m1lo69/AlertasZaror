package cl.zaror.alertas.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

data class AlertaRequest(
    val tipo: String,
    val descripcion: String,
    val ubicacion: String,
    val fotoUri: String?
)

data class EstadoRequest(
    val estado: String
)

data class AlertaResponse(
    val id: Long,
    val usuarioId: Long,
    val tipo: String,
    val descripcion: String,
    val ubicacion: String,
    val fotoUri: String?,
    val estado: String,
    val fechaHora: String?,
    val atendidaPor: Long?,
    val fechaAtencion: String?,
    val reportadaPor: String?
)

/** Endpoints propios del microservicio de alertas (Spring Boot). Todos requieren token. */
interface AlertasApi {

    @POST("api/alertas")
    suspend fun crear(
        @Header("Authorization") autorizacion: String,
        @Body datos: AlertaRequest
    ): AlertaResponse

    @GET("api/alertas")
    suspend fun listar(
        @Header("Authorization") autorizacion: String
    ): List<AlertaResponse>

    @PATCH("api/alertas/{id}/estado")
    suspend fun cambiarEstado(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: Long,
        @Body datos: EstadoRequest
    ): AlertaResponse

    @DELETE("api/alertas/{id}")
    suspend fun eliminar(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: Long
    ): Response<Unit>
}
