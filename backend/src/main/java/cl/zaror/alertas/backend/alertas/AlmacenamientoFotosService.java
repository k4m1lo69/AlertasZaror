package cl.zaror.alertas.backend.alertas;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import cl.zaror.alertas.backend.auth.AuthExcepciones.ValidacionException;

@Service
public class AlmacenamientoFotosService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^[a-f0-9-]{36}\\.(jpg|png)$");

    private final Path directorio;

    public AlmacenamientoFotosService(
            @Value("${alertas.fotos.directorio:./datos/fotos}") String ruta) {
        this.directorio = Path.of(ruta).toAbsolutePath().normalize();
    }

    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw error("Debes seleccionar una fotografía");
        }

        if (archivo.getSize() > MAX_BYTES) {
            throw error("La fotografía no puede superar los 5 MB");
        }

        try {
            byte[] bytes = archivo.getBytes();

            try (var entrada = ImageIO.createImageInputStream(
                    new ByteArrayInputStream(bytes))) {

                var lectores = ImageIO.getImageReaders(entrada);
                if (!lectores.hasNext()) {
                    throw error("El archivo no contiene una imagen válida");
                }

                var lector = lectores.next();
                try {
                    String formato = lector.getFormatName().toLowerCase(Locale.ROOT);

                    if (!formato.equals("jpeg") && !formato.equals("jpg")
                            && !formato.equals("png")) {
                        throw error("Solo se permiten fotografías JPEG o PNG");
                    }

                    lector.setInput(entrada);
                    int ancho = lector.getWidth(0);
                    int alto = lector.getHeight(0);

                    if (ancho > 4096 || alto > 4096) {
                        throw error("La imagen no puede superar 4096 píxeles por lado");
                    }

                    String extension = formato.equals("png") ? ".png" : ".jpg";
                    String nombre = UUID.randomUUID() + extension;

                    Files.createDirectories(directorio);
                    Files.write(directorio.resolve(nombre), bytes,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE);

                    return nombre;
                } finally {
                    lector.dispose();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar la fotografía", e);
        }
    }

    public FotoContenido leer(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new AlertaExcepciones.AlertaNoEncontradaException();
        }

        try {
            Path archivo = directorio.resolve(nombre).normalize();

            if (!archivo.getParent().equals(directorio)
                    || !Files.isRegularFile(archivo)) {
                throw new AlertaExcepciones.AlertaNoEncontradaException();
            }

            MediaType tipo = nombre.endsWith(".png")
                    ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;

            return new FotoContenido(Files.readAllBytes(archivo), tipo);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer la fotografía", e);
        }
    }

    public void eliminar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return;
        }

        try {
            Files.deleteIfExists(directorio.resolve(nombre).normalize());
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo eliminar la fotografía anterior", e);
        }
    }

    private ValidacionException error(String mensaje) {
        return new ValidacionException(java.util.Map.of("foto", mensaje));
    }

    public record FotoContenido(byte[] bytes, MediaType tipo) {
    }
}