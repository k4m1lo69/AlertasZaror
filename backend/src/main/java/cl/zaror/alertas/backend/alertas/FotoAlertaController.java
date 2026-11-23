package cl.zaror.alertas.backend.alertas;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import cl.zaror.alertas.backend.alertas.AlmacenamientoFotosService.FotoContenido;

@RestController
@RequestMapping("/api/alertas")
public class FotoAlertaController {

    private static final String AUTORIDAD_ADMIN = "ROLE_ADMINISTRADOR";

    private final FotoAlertaService servicio;

    public FotoAlertaController(FotoAlertaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> subir(
            @AuthenticationPrincipal Jwt jwt,
            Authentication auth,
            @PathVariable long id,
            @RequestPart("foto") MultipartFile foto) {

        String uri = servicio.subir(id, idDe(jwt), esAdmin(auth), foto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("fotoUri", uri));
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> obtener(
            @AuthenticationPrincipal Jwt jwt,
            Authentication auth,
            @PathVariable long id) {

        FotoContenido foto = servicio.obtener(id, idDe(jwt), esAdmin(auth));

        return ResponseEntity.ok()
                .contentType(foto.tipo())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(foto.bytes());
    }

    private static long idDe(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private static boolean esAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> AUTORIDAD_ADMIN.equals(a.getAuthority()));
    }
}