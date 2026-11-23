package cl.zaror.alertas.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.zaror.alertas.backend.auth.AuthExcepciones.CuentaBloqueadaException;
import cl.zaror.alertas.backend.auth.AuthExcepciones.ValidacionException;
import cl.zaror.alertas.backend.seguridad.TokenService;

class AuthServicePerfilTest {

    private static final Instant AHORA = Instant.parse("2026-01-01T12:00:00Z");

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final TokenService tokens = mock(TokenService.class);
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final AuthService servicio = new AuthService(usuarios, encoder, tokens, reloj);

    private Usuario usuario(int intentos, Instant bloqueadoHasta) {
        return new Usuario(1L, "Ana Perez", "912345678", "Calle Ejemplo 123",
                encoder.encode("Clave1234"), "VECINO", "APROBADO", intentos, bloqueadoHasta);
    }

    @Test
    void cambiarClaveCorrectaGuardaElNuevoHash() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, null)));

        servicio.cambiarClave(1L, new CambiarClaveRequest("Clave1234", "Nueva5678"));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(usuarios).actualizarClave(eq(1L), hash.capture());
        assertTrue(encoder.matches("Nueva5678", hash.getValue()));
    }

    @Test
    void claveActualIncorrectaSumaIntentoYNoCambiaNada() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, null)));

        assertThrows(ValidacionException.class,
                () -> servicio.cambiarClave(1L, new CambiarClaveRequest("Equivocada1", "Nueva5678")));

        verify(usuarios).actualizarIntentos(eq(1L), eq(1), isNull());
        verify(usuarios, never()).actualizarClave(anyLong(), anyString());
    }

    @Test
    void quintoIntentoFallidoBloqueaLaCuenta() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario(4, null)));

        assertThrows(CuentaBloqueadaException.class,
                () -> servicio.cambiarClave(1L, new CambiarClaveRequest("Equivocada1", "Nueva5678")));
    }

    @Test
    void cuentaBloqueadaNoPermiteCambiarClave() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, AHORA.plusSeconds(120))));

        assertThrows(CuentaBloqueadaException.class,
                () -> servicio.cambiarClave(1L, new CambiarClaveRequest("Clave1234", "Nueva5678")));
        verify(usuarios, never()).actualizarClave(anyLong(), anyString());
    }

    @Test
    void claveNuevaDebilSeRechazaSinConsultarLaBase() {
        assertThrows(ValidacionException.class,
                () -> servicio.cambiarClave(1L, new CambiarClaveRequest("Clave1234", "abc")));
        verify(usuarios, never()).buscarPorId(anyLong());
    }

    @Test
    void actualizarPerfilGuardaDatosSinEspacios() {
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(usuario(0, null)));

        UsuarioResponse respuesta = servicio.actualizarPerfil(1L, new PerfilRequest("  Ana Maria Perez ", " Pasaje Nuevo 456 "));

        verify(usuarios).actualizarPerfil(1L, "Ana Maria Perez", "Pasaje Nuevo 456");
        assertEquals(1L, respuesta.id());
    }

    @Test
    void actualizarPerfilConDatosInvalidosFalla() {
        assertThrows(ValidacionException.class,
                () -> servicio.actualizarPerfil(1L, new PerfilRequest("ab", "x")));
        verify(usuarios, never()).actualizarPerfil(anyLong(), anyString(), anyString());
    }
}