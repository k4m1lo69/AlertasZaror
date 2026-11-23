package cl.zaror.alertas.backend.alertas;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AlertaNoEncontradaException;
import cl.zaror.alertas.backend.alertas.AlmacenamientoFotosService.FotoContenido;

@Service
public class FotoAlertaService {

    private final AlertaRepository repositorio;
    private final AlmacenamientoFotosService almacenamiento;

    public FotoAlertaService(
            AlertaRepository repositorio,
            AlmacenamientoFotosService almacenamiento) {
        this.repositorio = repositorio;
        this.almacenamiento = almacenamiento;
    }

    public String subir(long alertaId, long usuarioId, boolean administrador,
            MultipartFile archivo) {

        Alerta alerta = cargarConAcceso(alertaId, usuarioId, administrador);
        String anterior = alerta.fotoUri();
        String nuevo = almacenamiento.guardar(archivo);

        try {
            repositorio.actualizarFotoUri(alertaId, nuevo);
        } catch (RuntimeException e) {
            almacenamiento.eliminar(nuevo);
            throw e;
        }

        if (anterior != null && !anterior.equals(nuevo)) {
            almacenamiento.eliminar(anterior);
        }

        return "/api/alertas/" + alertaId + "/foto";
    }

    public FotoContenido obtener(long alertaId, long usuarioId, boolean administrador) {
        Alerta alerta = cargarConAcceso(alertaId, usuarioId, administrador);

        if (alerta.fotoUri() == null) {
            throw new AlertaNoEncontradaException();
        }

        return almacenamiento.leer(alerta.fotoUri());
    }

    private Alerta cargarConAcceso(long id, long usuarioId, boolean administrador) {
        Alerta alerta = repositorio.buscarPorId(id)
                .orElseThrow(AlertaNoEncontradaException::new);

        if (!administrador && alerta.usuarioId() != usuarioId) {
            throw new AlertaNoEncontradaException();
        }

        return alerta;
    }
}