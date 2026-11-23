package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.AlertaResponse
import cl.zaror.alertas.data.remote.AlertasApi
import cl.zaror.alertas.data.remote.EstadoRequest
import cl.zaror.alertas.data.remote.MapeoAlertas
import cl.zaror.alertas.domain.Permisos
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AlertasRepositoryTest {

    private val dao = mockk<AlertaDao>(relaxed = true)
    private val api = mockk<AlertasApi>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val repositorio = AlertasRepository(dao, api, tokenManager)

    @Before
    fun configurar() {
        every { tokenManager.token } returns flowOf("abc")
    }

    private fun entidad(): AlertaEntity {
        val e = mockk<AlertaEntity>(relaxed = true)
        every { e.id } returns 1
        every { e.tipo } returns "Robo"
        every { e.descripcion } returns "Desc"
        every { e.ubicacion } returns "Calle"
        every { e.usuarioId } returns 3
        every { e.reportadaPor } returns "Ana"
        return e
    }

    @Test
    fun observarTodasConvierteEntidadesEnAlertas() = runTest {
        every { dao.observarTodas() } returns flowOf(listOf(entidad()))

        val lista = repositorio.observarTodas().first()

        assertEquals(1, lista.size)
        assertEquals(1, lista[0].id)
        assertEquals("Robo", lista[0].tipo)
        assertEquals(3, lista[0].usuarioId)
    }

    @Test
    fun observarDeUsuarioUsaElFiltroDelDao() = runTest {
        every { dao.observarDeUsuario(3) } returns flowOf(listOf(entidad()))

        val lista = repositorio.observarDeUsuario(3).first()

        assertEquals("Ana", lista[0].reportadaPor)
    }

    @Test
    fun refrescarRenuevaLaCopiaLocal() = runTest {
        coEvery { api.listar("Bearer abc") } returns emptyList()

        repositorio.refrescar(3)

        coVerify { dao.insertarOReemplazarTodas(emptyList()) }
        coVerify { dao.eliminarFueraDe(emptyList()) }
    }

    @Test
    fun guardarAlertaLlamaAlServidorYGuardaLaCopia() = runTest {
        coEvery { api.crear("Bearer abc", any()) } returns AlertaResponse(
            id = 10, usuarioId = 3, tipo = "ROBO", descripcion = "Desc", ubicacion = "Calle",
            fotoUri = null, estado = "ACTIVA", fechaHora = null, atendidaPor = null,
            fechaAtencion = null, reportadaPor = "Ana"
        )

        repositorio.guardarAlerta(
            Alerta(tipo = "Robo", descripcion = "Desc", ubicacion = "Calle", fotoUri = null, reportadaPor = "Ana", usuarioId = 3)
        )

        coVerify { api.crear("Bearer abc", any()) }
        coVerify { dao.insertarOReemplazar(any()) }
    }

    @Test
    fun marcarAtendidaCambiaElEstadoEnServidorYLocal() = runTest {
        coEvery {
            api.cambiarEstado("Bearer abc", 5L, EstadoRequest(MapeoAlertas.SERVIDOR_ATENDIDA))
        } returns mockk(relaxed = true)

        repositorio.marcarAtendida(5)

        coVerify { dao.actualizarEstado(5, Permisos.ESTADO_ATENDIDA) }
    }

    @Test
    fun cancelarPropiaCambiaElEstadoEnServidorYLocal() = runTest {
        coEvery {
            api.cambiarEstado("Bearer abc", 5L, EstadoRequest(MapeoAlertas.SERVIDOR_CANCELADA))
        } returns mockk(relaxed = true)

        repositorio.cancelarPropia(5)

        coVerify { dao.actualizarEstado(5, Permisos.ESTADO_CANCELADA) }
    }

    @Test
    fun eliminarBorraLaCopiaSoloSiElServidorAcepta() = runTest {
        coEvery { api.eliminar("Bearer abc", 5L) } returns Response.success(Unit)

        repositorio.eliminarAlerta(5)

        coVerify { dao.eliminar(5) }
    }

    @Test
    fun eliminarConRespuestaDeErrorLanzaYNoBorraLaCopia() = runTest {
        coEvery { api.eliminar("Bearer abc", 5L) } returns
            Response.error(500, ResponseBody.create(null, ""))

        var lanzada: Exception? = null
        try {
            repositorio.eliminarAlerta(5)
        } catch (e: HttpException) {
            lanzada = e
        }

        assertNotNull(lanzada)
        coVerify(exactly = 0) { dao.eliminar(any()) }
    }

    @Test
    fun sinTokenLasOperacionesFallanConMensajeClaro() = runTest {
        every { tokenManager.token } returns flowOf(null)

        var lanzada: Exception? = null
        try {
            repositorio.marcarAtendida(5)
        } catch (e: IllegalStateException) {
            lanzada = e
        }

        assertNotNull(lanzada)
        assertTrue(lanzada!!.message!!.contains("sesión"))
    }
}