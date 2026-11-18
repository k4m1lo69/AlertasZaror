package cl.zaror.alertas.backend.alertas;

import java.time.Instant;
import java.util.Set;

/** Fila de la tabla ALERTAS. */
public record Alerta(
        long id,
        long usuarioId,
        String tipo,
        String descripcion,
        String ubicacion,
        String fotoUri,
        String estado,
        Instant fechaHora,
        Long atendidaPor,
        Instant fechaAtencion) {

    public static final String ESTADO_ACTIVA = "ACTIVA";
    public static final String ESTADO_ATENDIDA = "ATENDIDA";
    public static final String ESTADO_CANCELADA = "CANCELADA";

    public static final Set<String> TIPOS = Set.of("EMERGENCIA", "FALLA_ALARMA", "SOLICITUD_APOYO");
}
