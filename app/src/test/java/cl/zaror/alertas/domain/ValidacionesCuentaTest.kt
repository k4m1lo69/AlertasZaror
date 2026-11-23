package cl.zaror.alertas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesCuentaTest {

    @Test
    fun nombreMuyCortoEsInvalido() {
        assertNotNull(ValidacionesCuenta.validarNombre(" ab "))
    }

    @Test
    fun nombreValidoNoDevuelveError() {
        assertNull(ValidacionesCuenta.validarNombre("Ana Perez"))
    }

    @Test
    fun nombreDemasiadoLargoEsInvalido() {
        assertNotNull(ValidacionesCuenta.validarNombre("a".repeat(101)))
    }

    @Test
    fun direccionCortaOLargaEsInvalida() {
        assertNotNull(ValidacionesCuenta.validarDireccion("abc"))
        assertNotNull(ValidacionesCuenta.validarDireccion("a".repeat(151)))
        assertNull(ValidacionesCuenta.validarDireccion("Calle Ejemplo 123"))
    }

    @Test
    fun claveActualVaciaEsInvalida() {
        assertNotNull(ValidacionesCuenta.validarClaveActual("   "))
        assertNull(ValidacionesCuenta.validarClaveActual("Clave1234"))
    }

    @Test
    fun claveNuevaExigeLargoLetrasYNumeros() {
        assertNotNull(ValidacionesCuenta.validarClaveNueva("abc123"))
        assertNotNull(ValidacionesCuenta.validarClaveNueva("sololetras"))
        assertNotNull(ValidacionesCuenta.validarClaveNueva("12345678"))
        assertNull(ValidacionesCuenta.validarClaveNueva("Nueva5678"))
    }

    @Test
    fun claveNuevaNoPuedeSuperar72Bytes() {
        assertNotNull(ValidacionesCuenta.validarClaveNueva("a1".repeat(37)))
    }

    @Test
    fun confirmacionDebeCoincidir() {
        assertEquals("Las contraseñas no coinciden", ValidacionesCuenta.validarConfirmacion("Nueva5678", "Otra5678"))
        assertNull(ValidacionesCuenta.validarConfirmacion("Nueva5678", "Nueva5678"))
    }
}