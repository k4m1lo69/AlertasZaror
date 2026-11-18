package cl.zaror.alertas.backend.alertas;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private static final String AUTORIDAD_ADMIN = "ROLE_ADMINISTRADOR";

    private final AlertaService servicio;

    public AlertaController(AlertaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<AlertaResponse> crear(@AuthenticationPrincipal Jwt jwt,
            @RequestBody AlertaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(idDe(jwt), solicitud));
    }

    @GetMapping
    public List<AlertaResponse> listar(@AuthenticationPrincipal Jwt jwt, Authentication auth) {
        return servicio.listar(idDe(jwt), esAdmin(auth));
    }

    @GetMapping("/{id}")
    public AlertaResponse obtener(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id) {
        return servicio.obtener(id, idDe(jwt), esAdmin(auth));
    }

    @PatchMapping("/{id}/estado")
    public AlertaResponse cambiarEstado(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id, @RequestBody EstadoRequest solicitud) {
        return servicio.cambiarEstado(id, idDe(jwt), esAdmin(auth), solicitud.estado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@AuthenticationPrincipal Jwt jwt, Authentication auth,
            @PathVariable long id) {
        servicio.eliminar(id, idDe(jwt), esAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    private static long idDe(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private static boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> AUTORIDAD_ADMIN.equals(a.getAuthority()));
    }
}
