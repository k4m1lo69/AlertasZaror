package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.DireccionNominatim
import cl.zaror.alertas.data.remote.GeocodingApi
import cl.zaror.alertas.data.remote.GeocodingResponse
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class GeocodingRepositoryTest {

    private class ApiFalsa(private val respuesta: () -> GeocodingResponse) : GeocodingApi {
        var latitudRecibida: Double? = null
        var longitudRecibida: Double? = null

        override suspend fun direccionDe(latitud: Double, longitud: Double): GeocodingResponse {
            latitudRecibida = latitud
            longitudRecibida = longitud
            return respuesta()
        }
    }

    private val coordenadas = Coordenadas(-33.5, -70.7)

    @Test
    fun debePedirLaDireccionConLasCoordenadasRecibidas() = runBlocking {
        val api = ApiFalsa {
            GeocodingResponse(
                nombreCompleto = null,
                direccion = DireccionNominatim("123", "Pasaje Uno", null, null, "Cerrillos", null, null, null),
                error = null
            )
        }

        val direccion = GeocodingRepository(api).direccionDe(coordenadas)

        assertEquals("Pasaje Uno 123, Cerrillos", direccion)
        assertEquals(-33.5, api.latitudRecibida!!, 0.0)
        assertEquals(-70.7, api.longitudRecibida!!, 0.0)
    }

    @Test
    fun siElServicioNoEncuentraDireccionDevuelveNulo() = runBlocking {
        val api = ApiFalsa { GeocodingResponse(null, null, "Unable to geocode") }

        assertNull(GeocodingRepository(api).direccionDe(coordenadas))
    }

    @Test
    fun losErroresDeRedSePropagan() {
        val api = ApiFalsa { throw IOException("sin red") }

        assertThrows(IOException::class.java) {
            runBlocking { GeocodingRepository(api).direccionDe(coordenadas) }
        }
    }
}
