package cl.zaror.alertas.viewmodel

import cl.zaror.alertas.data.SesionManager
import cl.zaror.alertas.data.TokenManager
import cl.zaror.alertas.data.UsuarioDao
import cl.zaror.alertas.data.UsuarioEntity
import cl.zaror.alertas.data.UsuariosRepository
import cl.zaror.alertas.data.remote.AuthApi
import cl.zaror.alertas.data.remote.CambiarClaveRequest
import cl.zaror.alertas.data.remote.PerfilRequest
import cl.zaror.alertas.data.remote.TokenResponse
import cl.zaror.alertas.data.remote.UsuarioResponse
import cl.zaror.alertas.domain.Rol
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val usuarios = mockk<UsuariosRepository>(relaxed = true)
    private val sesion = mockk<SesionManager>(relaxed = true)
    private val usuarioDao = mockk<UsuarioDao>(relaxed = true)
    private val api = mockk<AuthApi>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)

    @Before
    fun configurar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun crearVm(usuarioId: Int? = null, rol: String? = null, token: String? = "abc"): AuthViewModel {
        every { sesion.usuarioId } returns flowOf(usuarioId)
        every { tokenManager.rol } returns flowOf(rol)
        every { tokenManager.token } returns flowOf(token)
        return AuthViewModel(usuarios, sesion, usuarioDao, api, tokenManager)
    }

    private fun http(codigo: Int, cuerpo: String = ""): HttpException =
        HttpException(Response.error<Any>(codigo, ResponseBody.create(null, cuerpo)))

    private fun perfil(n: String, t: String, r: String): UsuarioResponse {
        val p = mockk<UsuarioResponse>(relaxed = true)
        every { p.nombre } returns n
        every { p.telefono } returns t
        every { p.rol } returns r
        return p
    }

    private fun respuestaLogin(): TokenResponse {
        val t = mockk<TokenResponse>(relaxed = true)
        every { t.token } returns "tok"
        every { t.tipo } returns "Bearer"
        return t
    }

    // ---------- estado de sesión ----------

    @Test
    fun sinUsuarioGuardadoLaSesionEstaCerrada() = runTest {
        val vm = crearVm(usuarioId = null)
        assertTrue(vm.estadoSesion.value is EstadoSesion.SinSesion)
    }

    @Test
    fun conUsuarioYRolDelServidorLaSesionEstaActiva() = runTest {
        val entidad = mockk<UsuarioEntity>(relaxed = true)
        every { entidad.id } returns 5
        every { entidad.nombre } returns "Ana"
        every { entidad.telefono } returns "912345678"
        every { entidad.direccion } returns "Calle 1"
        coEvery { usuarios.obtenerPorId(5) } returns entidad

        val vm = crearVm(usuarioId = 5, rol = "ADMIN")
        val estado = vm.estadoSesion.value

        assertTrue(estado is EstadoSesion.Activa)
        val u = (estado as EstadoSesion.Activa).usuario
        assertEquals(5, u.id)
        assertEquals("Ana", u.nombre)
        assertEquals(Rol.ADMINISTRADOR, u.rol)
    }

    @Test
    fun idSinUsuarioLocalDejaLaSesionCerrada() = runTest {
        coEvery { usuarios.obtenerPorId(8) } returns null
        val vm = crearVm(usuarioId = 8, rol = "VECINO")
        assertTrue(vm.estadoSesion.value is EstadoSesion.SinSesion)
    }

    // ---------- registrar ----------

    @Test
    fun registrarPendienteInformaQueFaltaAprobacion() = runTest {
        coEvery { api.registrar(any()) } returns mockk(relaxed = true) {
            every { estado } returns "PENDIENTE"
        }
        val vm = crearVm()

        vm.registrar("Ana Perez", "912345678", "Calle 1", "Clave1234")

        assertTrue(vm.error.value!!.startsWith("Registro enviado"))
        assertFalse(vm.procesando.value)
    }

    @Test
    fun registrarConOtroEstadoConfirmaRecepcion() = runTest {
        coEvery { api.registrar(any()) } returns mockk(relaxed = true) {
            every { estado } returns "APROBADO"
        }
        val vm = crearVm()

        vm.registrar("Ana Perez", "912345678", "Calle 1", "Clave1234")

        assertEquals("Registro recibido correctamente.", vm.error.value)
    }

    @Test
    fun registrarConTelefonoRepetidoMuestraConflicto() = runTest {
        coEvery { api.registrar(any()) } throws http(409)
        val vm = crearVm()

        vm.registrar("Ana Perez", "912345678", "Calle 1", "Clave1234")

        assertTrue(vm.error.value!!.contains("registrado"))
        assertFalse(vm.procesando.value)
    }

    // ---------- iniciar sesión ----------

    @Test
    fun loginDeUsuarioLocalExistenteGuardaTokenYSesion() = runTest {
        val existente = mockk<UsuarioEntity>(relaxed = true)
        every { existente.id } returns 7
        every { existente.direccion } returns "Calle 1"
        coEvery { api.login(any()) } returns respuestaLogin()
        coEvery { api.obtenerPerfil("Bearer tok") } returns perfil("Ana", "912345678", "ADMIN")
        coEvery { usuarioDao.buscarPorTelefono("912345678") } returns existente
        val vm = crearVm()

        vm.iniciarSesion("912345678", "Clave1234")

        coVerify { usuarioDao.actualizarPerfilLocal(7, "Ana", "Calle 1") }
        coVerify { tokenManager.guardar("tok", "ADMIN") }
        coVerify { sesion.iniciarSesion(7) }
        assertNull(vm.error.value)
        assertFalse(vm.procesando.value)
    }

    @Test
    fun loginDeUsuarioNuevoCreaRegistroLocal() = runTest {
        coEvery { api.login(any()) } returns respuestaLogin()
        coEvery { api.obtenerPerfil("Bearer tok") } returns perfil("Luis", "911111111", "VECINO")
        coEvery { usuarioDao.buscarPorTelefono("911111111") } returns null
        coEvery { usuarioDao.insertar(any()) } returns 9L
        val vm = crearVm()

        vm.iniciarSesion("911111111", "Clave1234")

        coVerify { usuarioDao.insertar(any()) }
        coVerify { tokenManager.guardar("tok", "VECINO") }
        coVerify { sesion.iniciarSesion(9) }
    }

    @Test
    fun loginConClaveIncorrectaMuestraError() = runTest {
        coEvery { api.login(any()) } throws http(401)
        val vm = crearVm()

        vm.iniciarSesion("912345678", "mala")

        assertEquals("Teléfono o contraseña incorrectos", vm.error.value)
        coVerify(exactly = 0) { sesion.iniciarSesion(any()) }
        assertFalse(vm.procesando.value)
    }

    @Test
    fun loginDeCuentaNoAprobadaYBloqueadaMuestraMensajes() = runTest {
        val vm = crearVm()

        coEvery { api.login(any()) } throws http(403)
        vm.iniciarSesion("912345678", "x")
        assertEquals("Tu cuenta aún no está aprobada", vm.error.value)

        coEvery { api.login(any()) } throws http(429)
        vm.iniciarSesion("912345678", "x")
        assertTrue(vm.error.value!!.startsWith("Demasiados intentos"))

        coEvery { api.login(any()) } throws http(500)
        vm.iniciarSesion("912345678", "x")
        assertEquals("Error del servidor: 500", vm.error.value)
    }

    @Test
    fun errorDelServidorUsaElTextoDelCuerpo() = runTest {
        coEvery { api.login(any()) } throws http(400, """{"error":"Dato inválido"}""")
        val vm = crearVm()
        vm.iniciarSesion("912345678", "x")
        assertEquals("Dato inválido", vm.error.value)

        coEvery { api.login(any()) } throws http(400, """{"detalle":{"clave":"Clave débil"}}""")
        vm.iniciarSesion("912345678", "x")
        assertEquals("Clave débil", vm.error.value)
    }

    @Test
    fun sinConexionMuestraMensajeDeRed() = runTest {
        coEvery { api.login(any()) } throws java.net.UnknownHostException("sin red")
        val vm = crearVm()

        vm.iniciarSesion("912345678", "x")

        assertEquals("No se pudo conectar con el servidor.", vm.error.value)
    }

    @Test
    fun otroErrorUsaSuMensaje() = runTest {
        coEvery { api.login(any()) } throws RuntimeException("falla rara")
        val vm = crearVm()

        vm.iniciarSesion("912345678", "x")

        assertEquals("falla rara", vm.error.value)
    }

    @Test
    fun limpiarErrorVaciaElMensaje() = runTest {
        coEvery { api.login(any()) } throws http(401)
        val vm = crearVm()
        vm.iniciarSesion("912345678", "x")
        assertNotNull(vm.error.value)

        vm.limpiarError()

        assertNull(vm.error.value)
    }

    // ---------- perfil y contraseña ----------

    @Test
    fun guardarPerfilActualizaServidorYCopiaLocal() = runTest {
        coEvery {
            api.actualizarPerfil("Bearer abc", PerfilRequest("Ana Maria", "Calle 2"))
        } returns perfil("Ana Maria", "912345678", "VECINO")
        val vm = crearVm()

        vm.guardarPerfil(3, "  Ana Maria ", " Calle 2 ")

        coVerify { usuarioDao.actualizarPerfilLocal(3, "Ana Maria", "Calle 2") }
        assertEquals("Perfil actualizado.", vm.mensaje.value?.texto)
        assertFalse(vm.mensaje.value!!.esError)
        assertFalse(vm.procesando.value)
    }

    @Test
    fun guardarPerfilSinTokenMuestraErrorDeSesion() = runTest {
        val vm = crearVm(token = null)

        vm.guardarPerfil(3, "Ana", "Calle 2")

        assertTrue(vm.mensaje.value!!.esError)
        assertTrue(vm.mensaje.value!!.texto.contains("expir"))
    }

    @Test
    fun guardarPerfilConErrorDelServidorMarcaError() = runTest {
        coEvery { api.actualizarPerfil(any(), any()) } throws http(403)
        val vm = crearVm()

        vm.guardarPerfil(3, "Ana", "Calle 2")

        assertTrue(vm.mensaje.value!!.esError)
    }

    @Test
    fun cambiarClaveExitosa() = runTest {
        coEvery { api.cambiarClave("Bearer abc", CambiarClaveRequest("Vieja1234", "Nueva5678")) } returns Unit
        val vm = crearVm()

        vm.cambiarClave("Vieja1234", "Nueva5678")

        assertEquals("Contraseña actualizada.", vm.mensaje.value?.texto)
        assertFalse(vm.mensaje.value!!.esError)
    }

    @Test
    fun cambiarClaveConErrorOSinTokenMarcaError() = runTest {
        val vm = crearVm()
        coEvery { api.cambiarClave(any(), any()) } throws http(401)
        vm.cambiarClave("a", "b")
        assertTrue(vm.mensaje.value!!.esError)

        val vmSinToken = crearVm(token = null)
        vmSinToken.cambiarClave("a", "b")
        assertTrue(vmSinToken.mensaje.value!!.esError)
    }

    @Test
    fun limpiarMensajeVaciaElAviso() = runTest {
        val vm = crearVm(token = null)
        vm.cambiarClave("a", "b")
        assertNotNull(vm.mensaje.value)

        vm.limpiarMensaje()

        assertNull(vm.mensaje.value)
    }

    @Test
    fun cerrarSesionLimpiaTokenYSesion() = runTest {
        val vm = crearVm()

        vm.cerrarSesion()

        coVerify { tokenManager.limpiar() }
        coVerify { sesion.cerrarSesion() }
    }
}