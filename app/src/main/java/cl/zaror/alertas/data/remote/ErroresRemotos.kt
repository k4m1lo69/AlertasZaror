package cl.zaror.alertas.data.remote

import com.google.gson.JsonParser
import java.io.IOException
import retrofit2.HttpException

/** Convierte un error de red o del servidor en un mensaje corto para el usuario. */
fun mensajeDeError(e: Throwable): String {
    return when (e) {
        is HttpException -> {
            val delServidor = textoDelServidor(e)
            when {
                delServidor != null -> delServidor
                e.code() == 401 -> "Tu sesión expiró. Cierra sesión e ingresa de nuevo"
                e.code() == 403 -> "No tienes permiso para hacer eso"
                e.code() == 404 -> "La alerta ya no existe"
                else -> "El servidor respondió con un error (${e.code()})"
            }
        }
        is IOException -> "No se pudo conectar con el servidor"
        is IllegalStateException -> e.message ?: "Error inesperado"
        else -> "Ocurrió un error inesperado"
    }
}

private fun textoDelServidor(e: HttpException): String? {
    return runCatching {
        val cuerpo = e.response()?.errorBody()?.string().orEmpty()
        JsonParser.parseString(cuerpo).asJsonObject.get("error").asString
    }.getOrNull()
}
