package cl.zaror.alertas.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registro(@RequestBody RegistroRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud));
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest solicitud) {
        return servicio.login(solicitud);
    }

    /** Devuelve el usuario dueño del token. Sirve para comprobar que el token funciona. */
    @GetMapping("/yo")
    public UsuarioResponse yo(@AuthenticationPrincipal Jwt jwt) {
        return servicio.perfil(Long.parseLong(jwt.getSubject()));
    }

    /** Edita nombre y dirección del propio usuario. */
    @PatchMapping("/perfil")
    public UsuarioResponse actualizarPerfil(@AuthenticationPrincipal Jwt jwt, @RequestBody PerfilRequest solicitud) {
        return servicio.actualizarPerfil(Long.parseLong(jwt.getSubject()), solicitud);
    }

    /** Cambia la contraseña del propio usuario (pide la actual). */
    @PostMapping("/cambiar-clave")
    public ResponseEntity<Void> cambiarClave(@AuthenticationPrincipal Jwt jwt, @RequestBody CambiarClaveRequest solicitud) {
        servicio.cambiarClave(Long.parseLong(jwt.getSubject()), solicitud);
        return ResponseEntity.noContent().build();
    }
}
