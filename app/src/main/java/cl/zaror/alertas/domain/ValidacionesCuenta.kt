package cl.zaror.alertas.domain

/** Validaciones de la pantalla «Mi cuenta». Sin dependencias de Android, se prueban con JUnit. */
object ValidacionesCuenta {

    private const val MAX_BYTES_CLAVE = 72

    fun validarNombre(nombre: String): String? {
        val valor = nombre.trim()
        return when {
            valor.length < 3 -> "El nombre debe tener al menos 3 caracteres"
            valor.length > 100 -> "El nombre no puede superar 100 caracteres"
            else -> null
        }
    }

    fun validarDireccion(direccion: String): String? {
        val valor = direccion.trim()
        return when {
            valor.length < 5 -> "La dirección debe tener al menos 5 caracteres"
            valor.length > 150 -> "La dirección no puede superar 150 caracteres"
            else -> null
        }
    }

    fun validarClaveActual(clave: String): String? =
        if (clave.isBlank()) "Ingresa tu contraseña actual" else null

    fun validarClaveNueva(clave: String): String? {
        return when {
            clave.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
            clave.toByteArray(Charsets.UTF_8).size > MAX_BYTES_CLAVE -> "La contraseña no puede superar 72 caracteres"
            clave.none { it.isLetter() } || clave.none { it.isDigit() } ->
                "La contraseña debe incluir letras y números"
            else -> null
        }
    }

    fun validarConfirmacion(nueva: String, confirmacion: String): String? =
        if (nueva != confirmacion) "Las contraseñas no coinciden" else null
}