package cl.zaror.alertas.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsuariosRepositoryTest {

    private val dao = mockk<UsuarioDao>(relaxed = true)
    private val repositorio = UsuariosRepository(dao)

    /** Registra con el repositorio real para obtener una entidad con hash válido. */
    private suspend fun registrarYObtener(clave: String): UsuarioEntity {
        val capturada = slot<UsuarioEntity>()
        coEvery { dao.buscarPorTelefono("912345678") } returns null
        coEvery { dao.insertar(capture(capturada)) } returns 4L
        repositorio.registrar("Ana", "912345678", "Calle 1", clave)
        return capturada.captured
    }

    @Test
    fun registrarGuardaDatosLimpiosYHashDeLaClave() = runTest {
        val capturada = slot<UsuarioEntity>()
        coEvery { dao.buscarPorTelefono("912345678") } returns null
        coEvery { dao.insertar(capture(capturada)) } returns 4L

        val id = repositorio.registrar("  Ana ", " 912345678 ", " Calle 1 ", "Clave1234")

        assertEquals(4, id)
        assertEquals("Ana", capturada.captured.nombre)
        assertEquals("912345678", capturada.captured.telefono)
        assertEquals("Calle 1", capturada.captured.direccion)
        assertNotEquals("Clave1234", capturada.captured.claveHash)
    }

    @Test
    fun registrarConTelefonoExistenteDevuelveNull() = runTest {
        coEvery { dao.buscarPorTelefono("912345678") } returns mockk<UsuarioEntity>(relaxed = true)

        assertNull(repositorio.registrar("Ana", "912345678", "Calle 1", "Clave1234"))
        coVerify(exactly = 0) { dao.insertar(any()) }
    }

    @Test
    fun autenticarConClaveCorrectaDevuelveExito() = runTest {
        val usuario = registrarYObtener("Clave1234")
        coEvery { dao.buscarPorTelefono("912345678") } returns usuario

        val resultado = repositorio.autenticar(" 912345678 ", "Clave1234", ahora = 1000L)

        assertTrue(resultado is ResultadoAuth.Exito)
    }

    @Test
    fun autenticarConClaveIncorrectaDevuelveInvalidasYCuentaElFallo() = runTest {
        val usuario = registrarYObtener("Clave1234")
        coEvery { dao.buscarPorTelefono("912345678") } returns usuario

        val resultado = repositorio.autenticar("912345678", "Otra5678", ahora = 1000L)

        assertTrue(resultado is ResultadoAuth.Invalidas)
        coVerify { dao.actualizarIntentos(usuario.id, any(), any()) }
    }

    @Test
    fun autenticarConTelefonoDesconocidoDevuelveInvalidas() = runTest {
        coEvery { dao.buscarPorTelefono("900000000") } returns null

        assertTrue(repositorio.autenticar("900000000", "x", ahora = 1000L) is ResultadoAuth.Invalidas)
    }

    @Test
    fun autenticarConCuentaBloqueadaDevuelveBloqueado() = runTest {
        val bloqueado = mockk<UsuarioEntity>(relaxed = true)
        every { bloqueado.id } returns 1
        every { bloqueado.bloqueadoHasta } returns 100_000L
        coEvery { dao.buscarPorTelefono("912345678") } returns bloqueado

        val resultado = repositorio.autenticar("912345678", "Clave1234", ahora = 1000L)

        assertTrue(resultado is ResultadoAuth.Bloqueado)
        assertTrue((resultado as ResultadoAuth.Bloqueado).segundos > 0)
    }

    @Test
    fun obtenerPorIdDelegaEnElDao() = runTest {
        val entidad = mockk<UsuarioEntity>(relaxed = true)
        coEvery { dao.buscarPorId(2) } returns entidad

        assertEquals(entidad, repositorio.obtenerPorId(2))
    }
}