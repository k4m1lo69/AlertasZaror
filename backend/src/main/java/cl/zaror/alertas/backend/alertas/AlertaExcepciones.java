package cl.zaror.alertas.backend.alertas;

/** Errores de negocio del módulo de alertas. */
public final class AlertaExcepciones {

    private AlertaExcepciones() {
    }

    public static class AlertaNoEncontradaException extends RuntimeException {
        public AlertaNoEncontradaException() {
            super("La alerta no existe");
        }
    }

    public static class AccesoDenegadoException extends RuntimeException {
        public AccesoDenegadoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class EstadoInvalidoException extends RuntimeException {
        public EstadoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}
