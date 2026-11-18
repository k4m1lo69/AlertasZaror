package cl.zaror.alertas.data.remote

/** Arma un texto corto ("Calle 123, Sector, Comuna") a partir de la respuesta de Nominatim. */
object FormatoDireccion {

    /** Largo máximo que acepta el servidor propio para la ubicación de una alerta. */
    const val MAX_LARGO = 200

    fun armar(respuesta: GeocodingResponse): String? {
        if (respuesta.error != null) return null

        val direccion = respuesta.direccion
        if (direccion != null) {
            val via = listOf(direccion.calle, direccion.numero)
                .mapNotNull { limpiar(it) }
                .joinToString(" ")
                .ifEmpty { null }
            val sector = limpiar(direccion.sector) ?: limpiar(direccion.barrio)
            val comuna = listOf(direccion.ciudad, direccion.pueblo, direccion.aldea, direccion.municipio)
                .firstNotNullOfOrNull { limpiar(it) }

            val partes = listOfNotNull(via, sector, comuna).distinct()
            if (partes.isNotEmpty()) {
                return partes.joinToString(", ").take(MAX_LARGO)
            }
        }

        return limpiar(respuesta.nombreCompleto)?.take(MAX_LARGO)
    }

    private fun limpiar(texto: String?): String? = texto?.trim()?.ifEmpty { null }
}
