package cl.zaror.alertas.viewmodel

import cl.zaror.alertas.data.TokenManager
import cl.zaror.alertas.data.remote.AprobarRequest
import cl.zaror.alertas.data.remote.AuthApi
import cl.zaror.alertas.data.remote.ClaveTemporalResponse
import cl.zaror.alertas.data.remote.CuadraResponse
import cl.zaror.alertas.data.remote.RolRequest
import cl.zaror.alertas.data.remote.UsuarioResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModelTest {

    private val api = mockk<AuthApi>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private val cuadras = listOf(CuadraResponse(1, "Calle Uno", "100-200", "Norte"))

    @Before
    fun configurar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { tokenManager.token } returns flowOf("abc")
        coEvery { api.usuariosPorEstado(any(), any()) } returns emptyList()
        coEvery { api.cuadras(any()) } returns cuadras
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun http(codigo: Int, cuerpo: String = ""): HttpException =
        HttpException(Response.error<Any>(codigo, ResponseBody.create(null, cuerpo)))

    private fun crearVm() = AdminViewModel(api, tokenManager)

    @Test
    fun cargarTraeUsuariosYCuadras() = runTest {
        val lista = listOf(mockk<UsuarioResponse>(relaxed = true))
        coEvery { api.usuariosPorEstado("Bearer abc", "APROBADO") } returns lista
        val vm = crearVm()

        vm.cargar("APROBADO")

        val estado = vm.estado.value
        assertEquals("APROBADO", estado.filtro)
        assertEquals(1, estado.usuarios.size)
        assertEquals(cuadras, estado.cuadras)
        assertFalse(estado.cargando)
        assertNull(estado.aviso)
    }

    @Test
    fun lasCuadrasSoloSeDescarganUnaVez() = runTest {
        val vm = crearVm()

        vm.cargar("PENDIENTE")
        vm.cargar("APROBADO")

        coVerify(exactly = 1) { api.cuadras("Bearer abc") }
    }

    @Test
    fun aprobarEnviaLaCuadraYAvisa() = runTest {
        coEvery { api.aprobarUsuario("Bearer abc", 5L, AprobarRequest(2L)) } returns mockk(relaxed = true)
        val vm = crearVm()

        vm.aprobar(5L, 2L)

        coVerify { api.aprobarUsuario("Bearer abc", 5L, AprobarRequest(2L)) }
        assertEquals("Vecino aprobado.", vm.estado.value.aviso?.texto)
        assertFalse(vm.estado.value.aviso!!.esError)
    }

    @Test
    fun rechazarAvisaYRecarga() = runTest {
        coEvery { api.rechazarUsuario("Bearer abc", 6L) } returns mockk(relaxed = true)
        val vm = crearVm()

        vm.rechazar(6L)

        coVerify { api.rechazarUsuario("Bearer abc", 6L) }
        assertEquals("Solicitud rechazada.", vm.estado.value.aviso?.texto)
    }

    @Test
    fun cambiarRolEnviaElNuevoRol() = runTest {
        coEvery { api.cambiarRolUsuario("Bearer abc", 5L, RolRequest("COORDINADOR")) } returns mockk(relaxed = true)
        val vm = crearVm()

        vm.cambiarRol(5L, "COORDINADOR")

        coVerify { api.cambiarRolUsuario("Bearer abc", 5L, RolRequest("COORDINADOR")) }
        assertTrue(vm.estado.value.aviso!!.texto.startsWith("Rol actualizado"))
    }

    @Test
    fun restablecerClaveMuestraLaClaveTemporalYSeDescarta() = runTest {
        coEvery { api.restablecerClaveUsuario("Bearer abc", 5L) } returns ClaveTemporalResponse("Temp1234")
        val vm = crearVm()

        vm.restablecerClave(5L, "Ana")

        assertEquals(ClaveTemporalInfo("Ana", "Temp1234"), vm.estado.value.claveTemporal)

        vm.descartarClave()

        assertNull(vm.estado.value.claveTemporal)
    }

    @Test
    fun sinTokenMuestraErrorDeSesion() = runTest {
        every { tokenManager.token } returns flowOf(null)
        val vm = crearVm()

        vm.cargar("PENDIENTE")

        assertTrue(vm.estado.value.aviso!!.esError)
        assertTrue(vm.estado.value.aviso!!.texto.contains("expir"))
        assertFalse(vm.estado.value.cargando)
    }

    @Test
    fun unErrorHttpSeMuestraComoAvisoDeError() = runTest {
        coEvery { api.usuariosPorEstado(any(), any()) } throws http(403)
        val vm = crearVm()

        vm.cargar("PENDIENTE")

        assertEquals("No tienes permiso para esta acción.", vm.estado.value.aviso?.texto)
        assertTrue(vm.estado.value.aviso!!.esError)
    }

    @Test
    fun textoDeErrorCubreLosCodigosConocidos() {
        assertTrue(textoDeError(http(401)).contains("sesión"))
        assertEquals("No tienes permiso para esta acción.", textoDeError(http(403)))
        assertEquals("No se encontró el usuario.", textoDeError(http(404)))
        assertTrue(textoDeError(http(429)).startsWith("Demasiados intentos"))
        assertEquals("Error del servidor: 500", textoDeError(http(500)))
    }

    @Test
    fun textoDeErrorUsaElCuerpoDelServidor() {
        assertEquals("Dato inválido", textoDeError(http(400, """{"error":"Dato inválido"}""")))
        assertEquals("Clave débil", textoDeError(http(400, """{"detalle":{"clave":"Clave débil"}}""")))
        assertEquals("Error del servidor: 400", textoDeError(http(400, "no es json")))
    }

    @Test
    fun textoDeErrorDeRedYGenerico() {
        assertEquals("No se pudo conectar con el servidor.", textoDeError(java.net.UnknownHostException()))
        assertEquals("No se pudo conectar con el servidor.", textoDeError(java.net.ConnectException()))
        assertEquals("No se pudo conectar con el servidor.", textoDeError(java.net.SocketTimeoutException()))
        assertEquals("falla", textoDeError(RuntimeException("falla")))
        assertNotNull(textoDeError(RuntimeException()))
    }

    @Test
    fun laFabricaCreaElViewModelYRechazaOtros() {
        val fabrica = AdminViewModelFactory(api, tokenManager)

        assertNotNull(fabrica.create(AdminViewModel::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            fabrica.create(AuthViewModel::class.java)
        }
    }
}