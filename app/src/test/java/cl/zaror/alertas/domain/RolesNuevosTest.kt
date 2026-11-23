package cl.zaror.alertas.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesNuevosTest {

    @Test
    fun administradorCoordinadorYDirectivaVenTodasLasAlertas() {
        assertTrue(Permisos.puedeVerTodas(Rol.ADMINISTRADOR))
        assertTrue(Permisos.puedeVerTodas(Rol.COORDINADOR))
        assertTrue(Permisos.puedeVerTodas(Rol.DIRECTIVA))
        assertFalse(Permisos.puedeVerTodas(Rol.VECINO))
    }

    @Test
    fun soloAdministradorYCoordinadorMarcanAtendida() {
        assertTrue(Permisos.puedeMarcarAtendida(Rol.ADMINISTRADOR))
        assertTrue(Permisos.puedeMarcarAtendida(Rol.COORDINADOR))
        assertFalse(Permisos.puedeMarcarAtendida(Rol.DIRECTIVA))
        assertFalse(Permisos.puedeMarcarAtendida(Rol.VECINO))
    }

    @Test
    fun soloAdministradorEliminaYAdministraUsuarios() {
        Rol.entries.forEach { rol ->
            assertEquals(rol == Rol.ADMINISTRADOR, Permisos.puedeEliminar(rol))
            assertEquals(rol == Rol.ADMINISTRADOR, Permisos.puedeAdministrarUsuarios(rol))
        }
    }

    @Test
    fun desdeServidorReconoceRolesYTextosAntiguos() {
        assertEquals(Rol.ADMINISTRADOR, Rol.desdeServidor("ADMIN"))
        assertEquals(Rol.ADMINISTRADOR, Rol.desdeServidor("administrador"))
        assertEquals(Rol.COORDINADOR, Rol.desdeServidor(" Coordinador "))
        assertEquals(Rol.DIRECTIVA, Rol.desdeServidor("DIRECTIVA"))
        assertEquals(Rol.VECINO, Rol.desdeServidor(null))
        assertEquals(Rol.VECINO, Rol.desdeServidor("cualquier cosa"))
    }
}