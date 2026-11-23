package cl.zaror.alertas.domain

/** Roles de la app. El rol se calcula, nunca lo elige el usuario al registrarse. */
enum class Rol(val etiqueta: String) {
    VECINO("Vecino"),
    COORDINADOR("Coordinador de cuadra"),
    DIRECTIVA("Directiva"),
    ADMINISTRADOR("Administrador");

    companion object {
        /** Convierte el texto que manda el servidor. Cualquier valor desconocido es VECINO. */
        fun desdeServidor(texto: String?): Rol = when (texto?.trim()?.uppercase()) {
            "ADMIN", "ADMINISTRADOR" -> ADMINISTRADOR
            "COORDINADOR" -> COORDINADOR
            "DIRECTIVA" -> DIRECTIVA
            else -> VECINO
        }
    }
}
