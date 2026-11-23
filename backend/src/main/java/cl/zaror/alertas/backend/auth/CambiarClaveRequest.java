package cl.zaror.alertas.backend.auth;

public record CambiarClaveRequest(String claveActual, String claveNueva) {
}