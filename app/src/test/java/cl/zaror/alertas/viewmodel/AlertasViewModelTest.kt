package cl.zaror.alertas.viewmodel

import cl.zaror.alertas.data.Alerta
import cl.zaror.alertas.data.AlertasRepository
import cl.zaror.alertas.domain.Rol
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlertasViewModelTest {

    private val repositorio = mockk<AlertasRepository>(relaxed = true)

    @Before
    fun configurar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun usuario(rol: Rol, id: Int = 3) =
        UsuarioSesion(id, "Ana", "912345678", "Calle 1", rol)

    @Test
    fun elVecinoSoloVeSusAlertas() = runTest {
        every { repositorio.observarDeUsuario(3) } returns flowOf(emptyList())
        val vm = AlertasViewModel(repositorio)

        vm.alertasVisibles(usuario(Rol.VECINO)).first()

        verify { repositorio.observarDeUsuario(3) }
        verify(exactly = 0) { repositorio.observarTodas() }
    }

    @Test
    fun administradorCoordinadorYDirectivaVenTodas() = runTest {
        every { repositorio.observarTodas() } returns flowOf(emptyList())
        val vm = AlertasViewModel(repositorio)

        vm.alertasVisibles(usuario(Rol.ADMINISTRADOR)).first()
        vm.alertasVisibles(usuario(Rol.COORDINADOR)).first()
        vm.alertasVisibles(usuario(Rol.DIRECTIVA)).first()

        verify(exactly = 3) { repositorio.observarTodas() }
    }

    @Test
    fun refrescarLlamaAlRepositorioConElUsuario() = runTest {
        val vm = AlertasViewModel(repositorio)

        vm.refrescar(usuario(Rol.VECINO, id = 4))

        coVerify { repositorio.refrescar(4) }
        assertNull(vm.error.value)
    }

    @Test
    fun siRefrescarFallaSeGuardaElError() = runTest {
        coEvery { repositorio.refrescar(any()) } throws RuntimeException("sin red")
        val vm = AlertasViewModel(repositorio)

        vm.refrescar(usuario(Rol.VECINO))

        assertNotNull(vm.error.value)

        vm.limpiarError()

        assertNull(vm.error.value)
    }

    @Test
    fun registrarAlertaIdentificaAlUsuarioDeLaSesion() = runTest {
        val capturada = slot<Alerta>()
        coEvery { repositorio.guardarAlerta(capture(capturada)) } just Runs
        val vm = AlertasViewModel(repositorio)

        vm.registrarAlerta(usuario(Rol.VECINO), "Robo", "Descripcion", "Calle 5", "content://foto")

        assertEquals("Robo", capturada.captured.tipo)
        assertEquals("Descripcion", capturada.captured.descripcion)
        assertEquals("Calle 5", capturada.captured.ubicacion)
        assertEquals("content://foto", capturada.captured.fotoUri)
        assertEquals("Ana", capturada.captured.reportadaPor)
        assertEquals(3, capturada.captured.usuarioId)
    }

    @Test
    fun soloAdministradorYCoordinadorMarcanAtendida() = runTest {
        val vm = AlertasViewModel(repositorio)

        vm.marcarComoAtendida(usuario(Rol.VECINO), 1)
        vm.marcarComoAtendida(usuario(Rol.DIRECTIVA), 2)
        coVerify(exactly = 0) { repositorio.marcarAtendida(any()) }

        vm.marcarComoAtendida(usuario(Rol.ADMINISTRADOR), 3)
        vm.marcarComoAtendida(usuario(Rol.COORDINADOR), 4)
        coVerify { repositorio.marcarAtendida(3) }
        coVerify { repositorio.marcarAtendida(4) }
    }

    @Test
    fun cancelarAlertaLlamaAlRepositorio() = runTest {
        val vm = AlertasViewModel(repositorio)

        vm.cancelarAlerta(usuario(Rol.VECINO), 9)

        coVerify { repositorio.cancelarPropia(9) }
    }

    @Test
    fun soloElAdministradorEliminaAlertas() = runTest {
        val vm = AlertasViewModel(repositorio)

        vm.eliminarAlerta(usuario(Rol.VECINO), 1)
        vm.eliminarAlerta(usuario(Rol.COORDINADOR), 2)
        vm.eliminarAlerta(usuario(Rol.DIRECTIVA), 3)
        coVerify(exactly = 0) { repositorio.eliminarAlerta(any()) }

        vm.eliminarAlerta(usuario(Rol.ADMINISTRADOR), 4)
        coVerify { repositorio.eliminarAlerta(4) }
    }
}