package cl.zaror.alertas.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import cl.zaror.alertas.backend.auth.AuthExcepciones.ValidacionException;

class AdminServiceTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final AdminService servicio = new AdminService(usuarios, encoder);

    private static Usuario usuario(long id, String rol, String estado) {
        return new Usuario(id, "Vecino Prueba", "912345678", "Calle Ejemplo 123", "hash", rol, estado, 0, null);
    }

    @Test
    void restablecerClaveGeneraClaveValidaYGuardaSoloElHash() {
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "VECINO", "APROBADO")));

        AdminService.ClaveTemporalResponse respuesta = servicio.restablecerClave(2L);

        assertEquals(10, respuesta.claveTemporal().length());
        assertNull(ValidacionesAuth.validarClave(respuesta.claveTemporal()));
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarios).actualizarClave(eq(2L), hash.capture());
        assertTrue(encoder.matches(respuesta.claveTemporal(), hash.getValue()));
    }

    @Test
    void restablecerClaveDeUsuarioInexistenteDevuelve404() {
        when(usuarios.buscarPorId(9L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> servicio.restablecerClave(9L));
        verify(usuarios, never()).actualizarClave(anyLong(), anyString());
    }

    @Test
    void cambiarRolRechazaRolDesconocido() {
        assertThrows(ValidacionException.class, () -> servicio.cambiarRol(1L, 2L, "SUPERUSUARIO"));
        verify(usuarios, never()).actualizarRol(anyLong(), anyString());
    }

    @Test
    void cambiarRolImpideQuitarseElPropioAdministrador() {
        assertThrows(ValidacionException.class, () -> servicio.cambiarRol(1L, 1L, "VECINO"));
        verify(usuarios, never()).actualizarRol(anyLong(), anyString());
    }

    @Test
    void cambiarRolAceptaCoordinadorEnMinusculas() {
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "VECINO", "APROBADO")));
        servicio.cambiarRol(1L, 2L, "coordinador");
        verify(usuarios).actualizarRol(2L, "COORDINADOR");
    }

    @Test
    void aprobarUsuarioYaAprobadoFalla() {
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "VECINO", "APROBADO")));
        assertThrows(ValidacionException.class, () -> servicio.aprobar(1L, 2L, 3L));
        verify(usuarios, never()).aprobar(anyLong(), anyLong(), anyLong());
    }

    @Test
    void aprobarExigeCuadraExistente() {
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "VECINO", "PENDIENTE")));
        when(usuarios.existeCuadra(99L)).thenReturn(false);
        assertThrows(ValidacionException.class, () -> servicio.aprobar(1L, 2L, 99L));
        assertThrows(ValidacionException.class, () -> servicio.aprobar(1L, 2L, null));
    }

    @Test
    void aprobarPendienteConCuadraValida() {
        when(usuarios.buscarPorId(2L)).thenReturn(Optional.of(usuario(2L, "VECINO", "PENDIENTE")));
        when(usuarios.existeCuadra(3L)).thenReturn(true);
        servicio.aprobar(1L, 2L, 3L);
        verify(usuarios).aprobar(2L, 3L, 1L);
    }

    @Test
    void listarPorEstadoInvalidoFalla() {
        assertThrows(ValidacionException.class, () -> servicio.listarPorEstado("BORRADO"));
    }
}