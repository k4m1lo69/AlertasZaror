package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.ClimaActualRespuesta
import cl.zaror.alertas.data.remote.ClimaApi
import cl.zaror.alertas.data.remote.ClimaRespuesta
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClimaRepositoryTest {

    private val api = mockk<ClimaApi>()
    private val repositorio = ClimaRepository(api)

    private fun coordenadas(): Coordenadas {
        val c = Coordenadas::class.java.constructors.first()
        val args = c.parameterTypes.map { tipo ->
            when (tipo) {
                java.lang.Double.TYPE, Double::class.javaObjectType -> -33.5
                java.lang.Float.TYPE -> -33.5f
                else -> null
            }
        }.toTypedArray()
        return c.newInstance(*args) as Coordenadas
    }

    @Test
    fun consultarConvierteLaRespuestaEnClimaActual() = runTest {
        coEvery { api.consultarClima(any(), any(), any(), any(), any(), any()) } returns ClimaRespuesta(
            timezone = "America/Santiago",
            abreviaturaZona = "-03",
            current = ClimaActualRespuesta(
                time = "2026-11-23T19:00",
                temperatura = 21.5,
                humedad = 40.0,
                precipitation = 0.2,
                codigoClima = 3,
                velocidadViento = 12.0
            )
        )

        val clima = repositorio.consultar(coordenadas())

        assertEquals("2026-11-23T19:00", clima.hora)
        assertEquals("America/Santiago", clima.zonaHoraria)
        assertEquals(21.5, clima.temperatura!!, 0.001)
        assertEquals(40.0, clima.humedad!!, 0.001)
        assertEquals(0.2, clima.precipitacion!!, 0.001)
        assertEquals(3, clima.codigoClima)
        assertEquals(12.0, clima.velocidadViento!!, 0.001)
    }

    @Test
    fun consultarAceptaCamposVaciosDelServicio() = runTest {
        coEvery { api.consultarClima(any(), any(), any(), any(), any(), any()) } returns ClimaRespuesta(
            timezone = null,
            abreviaturaZona = null,
            current = ClimaActualRespuesta(null, null, null, null, null, null)
        )

        val clima = repositorio.consultar(coordenadas())

        assertNull(clima.temperatura)
        assertNull(clima.codigoClima)
    }

    @Test
    fun sinCondicionesActualesLanzaError() = runTest {
        coEvery { api.consultarClima(any(), any(), any(), any(), any(), any()) } returns
            ClimaRespuesta(timezone = "x", abreviaturaZona = null, current = null)

        var error: Exception? = null
        try {
            repositorio.consultar(coordenadas())
        } catch (e: IllegalStateException) {
            error = e
        }

        assertTrue(error!!.message!!.contains("Open-Meteo"))
    }
}