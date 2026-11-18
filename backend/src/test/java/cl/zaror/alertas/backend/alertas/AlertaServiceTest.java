package cl.zaror.alertas.backend.alertas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AccesoDenegadoException;
import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AlertaNoEncontradaException;
import cl.zaror.alertas.backend.alertas.AlertaExcepciones.EstadoInvalidoException;
import cl.zaror.alertas.backend.auth.AuthExcepciones.ValidacionException;

class AlertaServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-12T12:00:00Z");
    private static final long VECINO = 10L;
    private static final long OTRO_VECINO = 11L;
    private static final long ADMIN = 1L;

    private final AlertaRepository repo = mock(AlertaRepository.class);
    private final AlertaService servicio = new AlertaService(repo, Clock.fixed(AHORA, ZoneOffset.UTC));

    private Alerta alerta(long id, long usuarioId, String estado) {
        return new Alerta(id, usuarioId, "EMERGENCIA", "Descripción de prueba", "Pasaje Uno 123",
                null, estado, AHORA, null, null);
    }

    private AlertaRequest solicitudValida() {
        return new AlertaRequest("EMERGENCIA", "  Humo en el pasaje  ", " Pasaje Uno 123 ", null);
    }

    @Test
    void crearDebeGuardarConElUsuarioDelTokenYTextosRecortados() {
        when(repo.crear(anyLong(), anyString(), anyString(), anyString(), any())).thenReturn(5L);
        when(repo.buscarPorId(5L)).thenReturn(Optional.of(alerta(5L, VECINO, "ACTIVA")));

        AlertaResponse r = servicio.crear(VECINO, solicitudValida());

        verify(repo).crear(VECINO, "EMERGENCIA", "Humo en el pasaje", "Pasaje Uno 123", null);
        assertThat(r.id()).isEqualTo(5L);
        assertThat(r.estado()).isEqualTo("ACTIVA");
    }

    @Test
    void crearConDatosInvalidosDebeInformarTodosLosErrores() {
        AlertaRequest mala = new AlertaRequest("OTRO", "corta", " ", null);

        assertThatThrownBy(() -> servicio.crear(VECINO, mala))
                .isInstanceOfSatisfying(ValidacionException.class, e ->
                        assertThat(e.errores()).containsKeys("tipo", "descripcion", "ubicacion"));
        verify(repo, never()).crear(anyLong(), anyString(), anyString(), anyString(), any());
    }

    @Test
    void vecinoSoloDebeListarSusAlertas() {
        when(repo.listarDeUsuario(VECINO)).thenReturn(List.of(alerta(1L, VECINO, "ACTIVA")));

        List<AlertaResponse> lista = servicio.listar(VECINO, false);

        assertThat(lista).hasSize(1);
        verify(repo, never()).listarTodas();
    }

    @Test
    void administradorDebeListarTodas() {
        when(repo.listarTodas()).thenReturn(List.of(
                alerta(1L, VECINO, "ACTIVA"), alerta(2L, OTRO_VECINO, "ACTIVA")));

        assertThat(servicio.listar(ADMIN, true)).hasSize(2);
        verify(repo, never()).listarDeUsuario(anyLong());
    }

    @Test
    void vecinoNoDebeVerLaAlertaDeOtroVecino() {
        when(repo.buscarPorId(2L)).thenReturn(Optional.of(alerta(2L, OTRO_VECINO, "ACTIVA")));

        assertThatThrownBy(() -> servicio.obtener(2L, VECINO, false))
                .isInstanceOf(AlertaNoEncontradaException.class);
    }

    @Test
    void obtenerInexistenteDebeFallar() {
        when(repo.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L, ADMIN, true))
                .isInstanceOf(AlertaNoEncontradaException.class);
    }

    @Test
    void administradorDebePoderMarcarAtendidaYQuedaRegistrado() {
        when(repo.buscarPorId(3L)).thenReturn(Optional.of(alerta(3L, VECINO, "ACTIVA")));

        servicio.cambiarEstado(3L, ADMIN, true, "ATENDIDA");

        verify(repo).actualizarEstado(3L, "ATENDIDA", ADMIN, AHORA);
    }

    @Test
    void vecinoNoDebePoderMarcarAtendida() {
        when(repo.buscarPorId(3L)).thenReturn(Optional.of(alerta(3L, VECINO, "ACTIVA")));

        assertThatThrownBy(() -> servicio.cambiarEstado(3L, VECINO, false, "ATENDIDA"))
                .isInstanceOf(AccesoDenegadoException.class);
        verify(repo, never()).actualizarEstado(anyLong(), anyString(), any(), any());
    }

    @Test
    void vecinoDebePoderCancelarSuPropiaAlerta() {
        when(repo.buscarPorId(3L)).thenReturn(Optional.of(alerta(3L, VECINO, "ACTIVA")));

        servicio.cambiarEstado(3L, VECINO, false, "CANCELADA");

        verify(repo).actualizarEstado(eq(3L), eq("CANCELADA"), isNull(), isNull());
    }

    @Test
    void noDebePermitirCambiarUnaAlertaQueYaNoEstaActiva() {
        when(repo.buscarPorId(3L)).thenReturn(Optional.of(alerta(3L, VECINO, "ATENDIDA")));

        assertThatThrownBy(() -> servicio.cambiarEstado(3L, ADMIN, true, "CANCELADA"))
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void estadoDesconocidoDebeRechazarse() {
        when(repo.buscarPorId(3L)).thenReturn(Optional.of(alerta(3L, VECINO, "ACTIVA")));

        assertThatThrownBy(() -> servicio.cambiarEstado(3L, ADMIN, true, "BORRADA"))
                .isInstanceOf(ValidacionException.class);
    }

    @Test
    void vecinoDebePoderEliminarSuAlerta() {
        when(repo.buscarPorId(4L)).thenReturn(Optional.of(alerta(4L, VECINO, "ACTIVA")));

        servicio.eliminar(4L, VECINO, false);

        verify(repo).eliminar(4L);
    }

    @Test
    void vecinoNoDebePoderEliminarLaAlertaDeOtro() {
        when(repo.buscarPorId(4L)).thenReturn(Optional.of(alerta(4L, OTRO_VECINO, "ACTIVA")));

        assertThatThrownBy(() -> servicio.eliminar(4L, VECINO, false))
                .isInstanceOf(AlertaNoEncontradaException.class);
        verify(repo, never()).eliminar(anyLong());
    }
}
