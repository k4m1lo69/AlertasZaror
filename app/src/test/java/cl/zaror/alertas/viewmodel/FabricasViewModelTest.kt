package cl.zaror.alertas.viewmodel

import androidx.lifecycle.ViewModelProvider
import cl.zaror.alertas.data.SesionManager
import cl.zaror.alertas.data.TokenManager
import cl.zaror.alertas.data.UsuarioDao
import cl.zaror.alertas.data.UsuariosRepository
import cl.zaror.alertas.data.remote.AuthApi
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FabricasViewModelTest {

    @Before
    fun configurar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    /** Arma la fábrica con mocks de lo que pida su constructor. */
    private fun fabrica(nombre: String): ViewModelProvider.Factory {
        val clase = Class.forName("cl.zaror.alertas.viewmodel.$nombre")
        val constructor = clase.constructors.first()
        val argumentos = constructor.parameterTypes
            .map { mockkClass(it.kotlin, relaxed = true) }
            .toTypedArray()
        return constructor.newInstance(*argumentos) as ViewModelProvider.Factory
    }

    @Test
    fun fabricaDeAuthCreaSuViewModel() {
        val fabrica = AuthViewModelFactory(
            mockk<UsuariosRepository>(relaxed = true),
            mockk<SesionManager>(relaxed = true),
            mockk<UsuarioDao>(relaxed = true),
            mockk<AuthApi>(relaxed = true),
            mockk<TokenManager>(relaxed = true)
        )

        assertNotNull(fabrica.create(AuthViewModel::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            fabrica.create(AdminViewModel::class.java)
        }
    }

    @Test
    fun fabricaDeAlertasCreaSuViewModel() {
        val fabrica = fabrica("AlertasViewModelFactory")

        assertNotNull(fabrica.create(AlertasViewModel::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            fabrica.create(AdminViewModel::class.java)
        }
    }

    @Test
    fun fabricaDeContactosCreaSuViewModel() {
        val fabrica = fabrica("ContactosViewModelFactory")

        assertNotNull(fabrica.create(ContactosViewModel::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            fabrica.create(AdminViewModel::class.java)
        }
    }

    @Test
    fun fabricaDeUbicacionCreaSuViewModel() {
        val fabrica = fabrica("UbicacionViewModelFactory")

        assertNotNull(fabrica.create(UbicacionViewModel::class.java))
        assertThrows(IllegalArgumentException::class.java) {
            fabrica.create(AdminViewModel::class.java)
        }
    }
}