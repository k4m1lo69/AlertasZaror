
package cl.zaror.alertas.viewmodel

import cl.zaror.alertas.data.ClimaRepository
import cl.zaror.alertas.data.Coordenadas
import cl.zaror.alertas.data.FuenteUbicacion
import cl.zaror.alertas.data.GeocodingRepository
import cl.zaror.alertas.data.remote.ClimaApi
import cl.zaror.alertas.data.remote.ClimaRespuesta
import cl.zaror.alertas.data.remote.DireccionNominatim
import cl.zaror.alertas.data.remote.GeocodingApi
import cl.zaror.alertas.data.remote.GeocodingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class UbicacionViewModelTest {

    @Before
    fun configurar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private class ApiFalsa(
        private val respuesta: GeocodingResponse
    ) : GeocodingApi {

        override suspend fun direccionDe(
            latitud: Double,
            longitud: Double
        ): GeocodingResponse {
            return respuesta
        }
    }

    // API meteorológica falsa: evita conexiones reales durante los tests.
    private class ApiClimaFalsa : ClimaApi {

        override suspend fun consultarClima(
            latitud: Double,
            longitud: Double,
            variables: String,
            zonaHoraria: String,
            unidadViento: String,
            unidadTemperatura: String
        ): ClimaRespuesta {
            return ClimaRespuesta(
                timezone = null,
                abreviaturaZona = null,
                current = null
            )
        }
    }

    private fun crearViewModel(
        api: GeocodingApi,
        coordenadas: Coordenadas?
    ): UbicacionViewModel {

        val fuente = object : FuenteUbicacion {
            override suspend fun actual(): Coordenadas? {
                return coordenadas
            }
        }

        return UbicacionViewModel(
            geocoding = GeocodingRepository(api),
            clima = ClimaRepository(ApiClimaFalsa()),
            fuente = fuente
        )
    }

    @Test
    fun debeObtenerYMostrarLaDireccion() = runTest {
        val api = ApiFalsa(
            GeocodingResponse(
                nombreCompleto = null,
                direccion = DireccionNominatim(
                    "123",
                    "Pasaje Uno",
                    null,
                    null,
                    "Cerrillos",
                    null,
                    null,
                    null
                ),
                error = null
            )
        )

        val viewModel = crearViewModel(
            api,
            Coordenadas(-33.5, -70.7)
        )

        viewModel.buscar()
        advanceUntilIdle()

        assertEquals(
            EstadoUbicacion.Lista("Pasaje Uno 123, Cerrillos"),
            viewModel.estado.value
        )
    }

    @Test
    fun debeInformarErrorCuandoNoObtieneCoordenadas() = runTest {
        val api = ApiFalsa(
            GeocodingResponse(null, null, null)
        )

        val viewModel = crearViewModel(api, null)

        viewModel.buscar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value is EstadoUbicacion.Error)
    }

    @Test
    fun debeInformarErrorCuandoNoEncuentraDireccion() = runTest {
        val api = ApiFalsa(
            GeocodingResponse(
                nombreCompleto = null,
                direccion = null,
                error = "Unable to geocode"
            )
        )

        val viewModel = crearViewModel(
            api,
            Coordenadas(-33.5, -70.7)
        )

        viewModel.buscar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value is EstadoUbicacion.Error)
    }

    @Test
    fun debeInformarErrorCuandoFallaLaConexion() = runTest {
        val api = object : GeocodingApi {
            override suspend fun direccionDe(
                latitud: Double,
                longitud: Double
            ): GeocodingResponse {
                throw IOException("Sin conexión")
            }
        }

        val viewModel = crearViewModel(
            api,
            Coordenadas(-33.5, -70.7)
        )

        viewModel.buscar()
        advanceUntilIdle()

        assertTrue(viewModel.estado.value is EstadoUbicacion.Error)
    }
}
