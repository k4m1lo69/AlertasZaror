package cl.zaror.alertas.backend.alertas;

/** Datos para crear una alerta. El usuario sale del token, no del cuerpo. */
public record AlertaRequest(String tipo, String descripcion, String ubicacion, String fotoUri) {
}
