package cl.zaror.alertas.backend.alertas;

import java.time.Instant;

public record AlertaResponse(
        long id,
        long usuarioId,
        String tipo,
        String descripcion,
        String ubicacion,
        String fotoUri,
        String estado,
        Instant fechaHora,
        Long atendidaPor,
        Instant fechaAtencion,
        String reportadaPor) {

    static AlertaResponse desde(Alerta a) {
        return new AlertaResponse(a.id(), a.usuarioId(), a.tipo(), a.descripcion(), a.ubicacion(),
                a.fotoUri(), a.estado(), a.fechaHora(), a.atendidaPor(), a.fechaAtencion(),
                a.reportadaPor());
    }
}
