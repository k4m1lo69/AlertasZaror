package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.mensajeDeError
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErroresRemotosTest {

    private fun http(codigo: Int, cuerpo: String): HttpException {
        val respuesta = Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType()))
        return HttpException(respuesta)
    }

    @Test
    fun sinConexionDebeAvisarQueNoHayServidor() {
        assertEquals("No se pudo conectar con el servidor", mensajeDeError(IOException()))
    }

    @Test
    fun debeUsarElMensajeQueEnviaElServidor() {
        val e = http(409, """{"error":"Solo se puede cambiar una alerta que está ACTIVA"}""")

        assertEquals("Solo se puede cambiar una alerta que está ACTIVA", mensajeDeError(e))
    }

    @Test
    fun tokenVencidoDebePedirIngresarDeNuevo() {
        assertEquals(
            "Tu sesión expiró. Cierra sesión e ingresa de nuevo",
            mensajeDeError(http(401, ""))
        )
    }

    @Test
    fun sinPermisoDebeAvisarlo() {
        assertEquals("No tienes permiso para hacer eso", mensajeDeError(http(403, "")))
    }

    @Test
    fun alertaInexistenteDebeAvisarlo() {
        assertEquals("La alerta ya no existe", mensajeDeError(http(404, "")))
    }

    @Test
    fun errorDelServidorSinMensajeMuestraElCodigo() {
        assertEquals("El servidor respondió con un error (500)", mensajeDeError(http(500, "")))
    }

    @Test
    fun faltaDeTokenDebeMostrarSuMensaje() {
        assertEquals("sin token", mensajeDeError(IllegalStateException("sin token")))
    }

    @Test
    fun errorDesconocidoDebeSerGenerico() {
        assertEquals("Ocurrió un error inesperado", mensajeDeError(RuntimeException("x")))
    }
}
