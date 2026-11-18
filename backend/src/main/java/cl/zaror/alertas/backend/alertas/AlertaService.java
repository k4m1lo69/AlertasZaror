package cl.zaror.alertas.backend.alertas;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AccesoDenegadoException;
import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AlertaNoEncontradaException;
import cl.zaror.alertas.backend.alertas.AlertaExcepciones.EstadoInvalidoException;
import cl.zaror.alertas.backend.auth.AuthExcepciones.ValidacionException;

/**
 * Reglas de visibilidad: el vecino ve y modifica solo sus alertas; el administrador ve todas
 * y es el único que puede marcarlas como atendidas.
 */
@Service
public class AlertaService {

    private final AlertaRepository alertas;
    private final Clock reloj;

    public AlertaService(AlertaRepository alertas, Clock reloj) {
        this.alertas = alertas;
        this.reloj = reloj;
    }

    public AlertaResponse crear(long usuarioId, AlertaRequest solicitud) {
        Map<String, String> errores = new LinkedHashMap<>();
        agregar(errores, "tipo", ValidacionesAlerta.validarTipo(solicitud.tipo()));
        agregar(errores, "descripcion", ValidacionesAlerta.validarDescripcion(solicitud.descripcion()));
        agregar(errores, "ubicacion", ValidacionesAlerta.validarUbicacion(solicitud.ubicacion()));
        agregar(errores, "fotoUri", ValidacionesAlerta.validarFoto(solicitud.fotoUri()));
        if (!errores.isEmpty()) {
            throw new ValidacionException(errores);
        }
        long id = alertas.crear(usuarioId, solicitud.tipo(), solicitud.descripcion().trim(),
                solicitud.ubicacion().trim(), solicitud.fotoUri());
        return AlertaResponse.desde(alertas.buscarPorId(id).orElseThrow(AlertaNoEncontradaException::new));
    }

    public List<AlertaResponse> listar(long usuarioId, boolean esAdministrador) {
        List<Alerta> lista = esAdministrador ? alertas.listarTodas() : alertas.listarDeUsuario(usuarioId);
        return lista.stream().map(AlertaResponse::desde).toList();
    }

    public AlertaResponse obtener(long id, long usuarioId, boolean esAdministrador) {
        return AlertaResponse.desde(cargarConAcceso(id, usuarioId, esAdministrador));
    }

    public AlertaResponse cambiarEstado(long id, long usuarioId, boolean esAdministrador, String nuevoEstado) {
        Alerta alerta = cargarConAcceso(id, usuarioId, esAdministrador);
        if (!Alerta.ESTADO_ATENDIDA.equals(nuevoEstado) && !Alerta.ESTADO_CANCELADA.equals(nuevoEstado)) {
            throw new ValidacionException(Map.of("estado", "El estado debe ser ATENDIDA o CANCELADA"));
        }
        if (!Alerta.ESTADO_ACTIVA.equals(alerta.estado())) {
            throw new EstadoInvalidoException("Solo se puede cambiar una alerta que está ACTIVA");
        }
        if (Alerta.ESTADO_ATENDIDA.equals(nuevoEstado)) {
            if (!esAdministrador) {
                throw new AccesoDenegadoException("Solo un administrador puede marcar una alerta como atendida");
            }
            alertas.actualizarEstado(id, nuevoEstado, usuarioId, reloj.instant());
        } else {
            alertas.actualizarEstado(id, nuevoEstado, null, null);
        }
        return AlertaResponse.desde(alertas.buscarPorId(id).orElseThrow(AlertaNoEncontradaException::new));
    }

    public void eliminar(long id, long usuarioId, boolean esAdministrador) {
        cargarConAcceso(id, usuarioId, esAdministrador);
        alertas.eliminar(id);
    }

    /** Si la alerta es de otro vecino se responde como si no existiera, para no revelar su id. */
    private Alerta cargarConAcceso(long id, long usuarioId, boolean esAdministrador) {
        Alerta alerta = alertas.buscarPorId(id).orElseThrow(AlertaNoEncontradaException::new);
        if (!esAdministrador && alerta.usuarioId() != usuarioId) {
            throw new AlertaNoEncontradaException();
        }
        return alerta;
    }

    private static void agregar(Map<String, String> errores, String campo, String error) {
        if (error != null) {
            errores.put(campo, error);
        }
    }
}
