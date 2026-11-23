package cl.zaror.alertas.data

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactosRepositoryTest {

    private val dao = mockk<ContactoDao>(relaxed = true)
    private val repositorio = ContactosRepository(dao)

    @Test
    fun observarConvierteLasEntidadesEnContactos() = runTest {
        every { dao.observarDeUsuario(1) } returns flowOf(
            listOf(ContactoEntity(id = 4, usuarioId = 1, nombre = "Luis", telefono = "912345678"))
        )

        val lista = repositorio.observar(1).first()

        assertEquals(listOf(Contacto(4, 1, "Luis", "912345678")), lista)
    }

    @Test
    fun agregarGuardaConEspaciosRecortadosYDevuelveTrue() = runTest {
        val capturada = slot<ContactoEntity>()
        coEvery { dao.insertar(capture(capturada)) } returns 10L

        val agregado = repositorio.agregar(1, "  Luis ", " 912345678 ")

        assertTrue(agregado)
        assertEquals("Luis", capturada.captured.nombre)
        assertEquals("912345678", capturada.captured.telefono)
        assertEquals(1, capturada.captured.usuarioId)
    }

    @Test
    fun agregarUnContactoRepetidoDevuelveFalse() = runTest {
        coEvery { dao.insertar(any()) } returns -1L

        assertFalse(repositorio.agregar(1, "Luis", "912345678"))
    }

    @Test
    fun eliminarSoloAfectaAlUsuarioIndicado() = runTest {
        repositorio.eliminar(1, 4)

        coVerify { dao.eliminar(4, 1) }
    }
}