package cl.zaror.alertas.viewmodel

import cl.zaror.alertas.data.Contacto
import cl.zaror.alertas.data.ContactosRepository
import cl.zaror.alertas.domain.Rol
import cl.zaror.alertas.domain.ValidacionesContacto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactosViewModelTest {

    private val repositorio = mockk<ContactosRepository>(relaxed = true)
    private val usuario = UsuarioSesion(1, "Ana", "000", "Calle 1", Rol.VECINO)

    // Se elige un teléfono que la validación real acepte, sin asumir el formato.
    private val telefonoValido = listOf(
        "+56912345678", "912345678", "+56 9 1234 5678", "+56987654321", "987654321"
    ).first { ValidacionesContacto.validarTelefono(it) == null }

    @Before
    fun configurar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    @Test
    fun contactosSoloObservaLosDelUsuarioDeLaSesion() = runTest {
        val lista = listOf(Contacto(1, 1, "Luis", telefonoValido))
        every { repositorio.observar(1) } returns flowOf(lista)
        val vm = ContactosViewModel(repositorio)

        assertEquals(lista, vm.contactos(usuario).first())
    }

    @Test
    fun agregarConNombreInvalidoMuestraErrorYNoGuarda() = runTest {
        val vm = ContactosViewModel(repositorio)
        var exito = false

        vm.agregar(usuario, "", telefonoValido) { exito = true }

        assertNotNull(vm.error.value)
        assertFalse(exito)
        coVerify(exactly = 0) { repositorio.agregar(any(), any(), any()) }
    }

    @Test
    fun agregarConTelefonoInvalidoMuestraError() = runTest {
        val vm = ContactosViewModel(repositorio)
        var exito = false

        vm.agregar(usuario, "Luis Soto", "abc") { exito = true }

        assertNotNull(vm.error.value)
        assertFalse(exito)
    }

    @Test
    fun agregarContactoValidoLlamaAlExito() = runTest {
        coEvery { repositorio.agregar(1, "Luis Soto", telefonoValido) } returns true
        val vm = ContactosViewModel(repositorio)
        var exito = false

        vm.agregar(usuario, "Luis Soto", telefonoValido) { exito = true }

        assertTrue(exito)
        assertNull(vm.error.value)
    }

    @Test
    fun agregarContactoRepetidoMuestraAviso() = runTest {
        coEvery { repositorio.agregar(any(), any(), any()) } returns false
        val vm = ContactosViewModel(repositorio)
        var exito = false

        vm.agregar(usuario, "Luis Soto", telefonoValido) { exito = true }

        assertFalse(exito)
        assertEquals("Ese contacto ya está en tu lista", vm.error.value)

        vm.limpiarError()

        assertNull(vm.error.value)
    }

    @Test
    fun eliminarPasaElUsuarioDeLaSesion() = runTest {
        val vm = ContactosViewModel(repositorio)

        vm.eliminar(usuario, 7)

        coVerify { repositorio.eliminar(1, 7) }
    }
}