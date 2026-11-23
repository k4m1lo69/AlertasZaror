package cl.zaror.alertas.backend.auth;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cl.zaror.alertas.backend.auth.AuthExcepciones.ValidacionException;

/** Gestión de usuarios por el administrador: aprobar, rechazar, cambiar rol y restablecer clave. */
@Service
public class AdminService {

    public static final Set<String> ROLES = Set.of("VECINO", "COORDINADOR", "DIRECTIVA", "ADMINISTRADOR");
    private static final Set<String> ESTADOS = Set.of("PENDIENTE", "APROBADO", "RECHAZADO");
    private static final String LETRAS = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGITOS = "23456789";
    private static final int LARGO_TEMPORAL = 10;

    /** Clave temporal: se muestra una sola vez al administrador. */
    public record ClaveTemporalResponse(String claveTemporal) {
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final SecureRandom azar = new SecureRandom();

    public AdminService(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    public List<UsuarioResponse> listarPorEstado(String estado) {
        String valor = estado == null || estado.isBlank()
                ? Usuario.ESTADO_PENDIENTE
                : estado.trim().toUpperCase(Locale.ROOT);
        if (!ESTADOS.contains(valor)) {
            throw new ValidacionException(Map.of("estado", "El estado debe ser PENDIENTE, APROBADO o RECHAZADO"));
        }
        return usuarios.listarPorEstado(valor).stream().map(UsuarioResponse::desde).toList();
    }

    public List<CuadraResponse> listarCuadras() {
        return usuarios.listarCuadras();
    }

    public UsuarioResponse aprobar(long adminId, long id, Long cuadraId) {
        if (cuadraId == null) {
            throw new ValidacionException(Map.of("cuadraId", "Debes elegir la cuadra del vecino"));
        }
        Usuario usuario = cargar(id);
        if (Usuario.ESTADO_APROBADO.equals(usuario.estado())) {
            throw new ValidacionException(Map.of("estado", "El usuario ya está aprobado"));
        }
        if (!usuarios.existeCuadra(cuadraId)) {
            throw new ValidacionException(Map.of("cuadraId", "La cuadra no existe"));
        }
        usuarios.aprobar(id, cuadraId, adminId);
        return UsuarioResponse.desde(cargar(id));
    }

    public UsuarioResponse rechazar(long adminId, long id) {
        Usuario usuario = cargar(id);
        if (!Usuario.ESTADO_PENDIENTE.equals(usuario.estado())) {
            throw new ValidacionException(Map.of("estado", "Solo se puede rechazar a un usuario pendiente"));
        }
        usuarios.rechazar(id, adminId);
        return UsuarioResponse.desde(cargar(id));
    }

    /** El cambio de rol se aplica en el próximo inicio de sesión (el rol viaja dentro del token). */
    public UsuarioResponse cambiarRol(long adminId, long id, String rol) {
        String valor = rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT);
        if (!ROLES.contains(valor)) {
            throw new ValidacionException(Map.of("rol", "El rol debe ser VECINO, COORDINADOR, DIRECTIVA o ADMINISTRADOR"));
        }
        if (adminId == id && !"ADMINISTRADOR".equals(valor)) {
            throw new ValidacionException(Map.of("rol", "No puedes quitarte a ti mismo el rol de administrador"));
        }
        cargar(id);
        usuarios.actualizarRol(id, valor);
        return UsuarioResponse.desde(cargar(id));
    }

    /** Recuperación de contraseña asistida: el administrador entrega la clave temporal en persona. */
    public ClaveTemporalResponse restablecerClave(long id) {
        cargar(id);
        String temporal = generarClaveTemporal();
        usuarios.actualizarClave(id, encoder.encode(temporal));
        return new ClaveTemporalResponse(temporal);
    }

    private Usuario cargar(long id) {
        return usuarios.buscarPorId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe"));
    }

    /** 10 caracteres con al menos una letra y un número, sin caracteres ambiguos. */
    private String generarClaveTemporal() {
        List<Character> caracteres = new ArrayList<>();
        caracteres.add(LETRAS.charAt(azar.nextInt(LETRAS.length())));
        caracteres.add(DIGITOS.charAt(azar.nextInt(DIGITOS.length())));
        String todo = LETRAS + DIGITOS;
        while (caracteres.size() < LARGO_TEMPORAL) {
            caracteres.add(todo.charAt(azar.nextInt(todo.length())));
        }
        Collections.shuffle(caracteres, azar);
        StringBuilder clave = new StringBuilder();
        caracteres.forEach(clave::append);
        return clave.toString();
    }
}