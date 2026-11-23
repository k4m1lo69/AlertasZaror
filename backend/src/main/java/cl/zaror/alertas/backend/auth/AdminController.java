package cl.zaror.alertas.backend.auth;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Solo ROLE_ADMINISTRADOR (ver SeguridadConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    public record AprobarRequest(Long cuadraId) {
    }

    public record RolRequest(String rol) {
    }

    private final AdminService servicio;

    public AdminController(AdminService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> usuarios(@RequestParam(required = false) String estado) {
        return servicio.listarPorEstado(estado);
    }

    @GetMapping("/cuadras")
    public List<CuadraResponse> cuadras() {
        return servicio.listarCuadras();
    }

    @PostMapping("/usuarios/{id}/aprobar")
    public UsuarioResponse aprobar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @RequestBody AprobarRequest solicitud) {
        return servicio.aprobar(Long.parseLong(jwt.getSubject()), id, solicitud.cuadraId());
    }

    @PostMapping("/usuarios/{id}/rechazar")
    public UsuarioResponse rechazar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return servicio.rechazar(Long.parseLong(jwt.getSubject()), id);
    }

    @PatchMapping("/usuarios/{id}/rol")
    public UsuarioResponse cambiarRol(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @RequestBody RolRequest solicitud) {
        return servicio.cambiarRol(Long.parseLong(jwt.getSubject()), id, solicitud.rol());
    }

    @PostMapping("/usuarios/{id}/restablecer-clave")
    public ResponseEntity<AdminService.ClaveTemporalResponse> restablecerClave(@PathVariable long id) {
        return ResponseEntity.ok().header("Cache-Control", "no-store").body(servicio.restablecerClave(id));
    }
}