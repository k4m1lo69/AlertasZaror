package cl.zaror.alertas.backend.alertas;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AccesoDenegadoException;
import cl.zaror.alertas.backend.alertas.AlertaExcepciones.AlertaNoEncontradaException;
import cl.zaror.alertas.backend.alertas.AlertaExcepciones.EstadoInvalidoException;

/** Los errores de validación (400) los maneja el ManejadorErrores de auth. */
@RestControllerAdvice
public class ManejadorErroresAlertas {

    @ExceptionHandler(AlertaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> noEncontrada(AlertaNoEncontradaException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, Object>> denegado(AccesoDenegadoException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<Map<String, Object>> estadoInvalido(EstadoInvalidoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
