package cl.zaror.alertas.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class LoginRequest(
    val telefono: String,
    val clave: String
)

data class RegistroRequest(
    val nombre: String,
    val telefono: String,
    val direccion: String,
    val clave: String
)

data class TokenResponse(
    val token: String,
    val tipo: String,
    val expiraEnSegundos: Long
)

data class UsuarioResponse(
    val id: Long,
    val nombre: String,
    val telefono: String,
    val rol: String,
    val estado: String
)

data class PerfilRequest(
    val nombre: String,
    val direccion: String
)

data class CambiarClaveRequest(
    val claveActual: String,
    val claveNueva: String
)

data class AprobarRequest(
    val cuadraId: Long
)

data class RolRequest(
    val rol: String
)

data class CuadraResponse(
    val id: Long,
    val calle: String,
    val tramo: String,
    val sector: String
)

data class ClaveTemporalResponse(
    val claveTemporal: String
)

interface AuthApi {

    @POST("api/auth/login")
    suspend fun login(
        @Body datos: LoginRequest
    ): TokenResponse

    @POST("api/auth/registro")
    suspend fun registrar(
        @Body datos: RegistroRequest
    ): UsuarioResponse

    @GET("api/auth/yo")
    suspend fun obtenerPerfil(
        @Header("Authorization") autorizacion: String
    ): UsuarioResponse

    @PATCH("api/auth/perfil")
    suspend fun actualizarPerfil(
        @Header("Authorization") autorizacion: String,
        @Body datos: PerfilRequest
    ): UsuarioResponse

    @POST("api/auth/cambiar-clave")
    suspend fun cambiarClave(
        @Header("Authorization") autorizacion: String,
        @Body datos: CambiarClaveRequest
    ): Unit

    @GET("api/admin/usuarios")
    suspend fun usuariosPorEstado(
        @Header("Authorization") autorizacion: String,
        @Query("estado") estado: String
    ): List<UsuarioResponse>

    @GET("api/admin/cuadras")
    suspend fun cuadras(
        @Header("Authorization") autorizacion: String
    ): List<CuadraResponse>

    @POST("api/admin/usuarios/{id}/aprobar")
    suspend fun aprobarUsuario(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: Long,
        @Body datos: AprobarRequest
    ): UsuarioResponse

    @POST("api/admin/usuarios/{id}/rechazar")
    suspend fun rechazarUsuario(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: Long
    ): UsuarioResponse

    @PATCH("api/admin/usuarios/{id}/rol")
    suspend fun cambiarRolUsuario(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: Long,
        @Body datos: RolRequest
    ): UsuarioResponse

    @POST("api/admin/usuarios/{id}/restablecer-clave")
    suspend fun restablecerClaveUsuario(
        @Header("Authorization") autorizacion: String,
        @Path("id") id: Long
    ): ClaveTemporalResponse
}