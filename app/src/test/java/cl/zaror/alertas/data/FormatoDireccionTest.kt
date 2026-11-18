package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.DireccionNominatim
import cl.zaror.alertas.data.remote.FormatoDireccion
import cl.zaror.alertas.data.remote.GeocodingResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatoDireccionTest {

    private fun direccion(
        calle: String? = null,
        numero: String? = null,
        barrio: String? = null,
        sector: String? = null,
        ciudad: String? = null,
        pueblo: String? = null,
        aldea: String? = null,
        municipio: String? = null
    ) = DireccionNominatim(numero, calle, barrio, sector, ciudad, pueblo, aldea, municipio)

    private fun respuesta(
        direccion: DireccionNominatim? = null,
        nombre: String? = null,
        error: String? = null
    ) = GeocodingResponse(nombre, direccion, error)

    @Test
    fun debeArmarCalleNumeroSectorYComuna() {
        val r = respuesta(direccion(calle = "Pasaje Uno", numero = "123", sector = "Población Zaror", ciudad = "Cerrillos"))

        assertEquals("Pasaje Uno 123, Población Zaror, Cerrillos", FormatoDireccion.armar(r))
    }

    @Test
    fun sinNumeroDebeUsarSoloLaCalle() {
        val r = respuesta(direccion(calle = "Pasaje Uno", ciudad = "Cerrillos"))

        assertEquals("Pasaje Uno, Cerrillos", FormatoDireccion.armar(r))
    }

    @Test
    fun siNoHaySectorDebeUsarElBarrio() {
        val r = respuesta(direccion(calle = "Pasaje Uno", barrio = "Zaror", ciudad = "Cerrillos"))

        assertEquals("Pasaje Uno, Zaror, Cerrillos", FormatoDireccion.armar(r))
    }

    @Test
    fun noDebeRepetirPartesIguales() {
        val r = respuesta(direccion(calle = "Pasaje Uno", sector = "Cerrillos", ciudad = "Cerrillos"))

        assertEquals("Pasaje Uno, Cerrillos", FormatoDireccion.armar(r))
    }

    @Test
    fun siNoHayCiudadDebeUsarOtroNombreDeLocalidad() {
        val r = respuesta(direccion(calle = "Camino Real", pueblo = "Pueblito"))

        assertEquals("Camino Real, Pueblito", FormatoDireccion.armar(r))
    }

    @Test
    fun ignoraCamposEnBlanco() {
        val r = respuesta(direccion(calle = "  ", numero = "", ciudad = " Cerrillos "))

        assertEquals("Cerrillos", FormatoDireccion.armar(r))
    }

    @Test
    fun sinDireccionDetalladaDebeUsarElNombreCompleto() {
        val r = respuesta(direccion = null, nombre = "  Cerrillos, Región Metropolitana, Chile ")

        assertEquals("Cerrillos, Región Metropolitana, Chile", FormatoDireccion.armar(r))
    }

    @Test
    fun siElServicioInformaUnErrorDebeDevolverNulo() {
        assertNull(FormatoDireccion.armar(respuesta(error = "Unable to geocode")))
    }

    @Test
    fun sinNingunDatoDebeDevolverNulo() {
        assertNull(FormatoDireccion.armar(respuesta()))
    }

    @Test
    fun noDebeSuperarElLargoQueAceptaElServidor() {
        val r = respuesta(direccion(calle = "x".repeat(300)))

        assertEquals(FormatoDireccion.MAX_LARGO, FormatoDireccion.armar(r)!!.length)
    }
}
