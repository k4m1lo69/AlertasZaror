package cl.zaror.alertas.backend.alertas;

/** Mismas reglas que la app Android (Validaciones.kt), validadas también en el servidor. */
public final class ValidacionesAlerta {

    private ValidacionesAlerta() {
    }

    public static String validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return "Debes seleccionar un tipo de alerta";
        }
        if (!Alerta.TIPOS.contains(tipo)) {
            return "El tipo de alerta no es válido";
        }
        return null;
    }

    public static String validarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return "La descripción es obligatoria";
        }
        if (descripcion.length() < 10) {
            return "La descripción debe tener al menos 10 caracteres";
        }
        if (descripcion.length() > 240) {
            return "La descripción no puede superar 240 caracteres";
        }
        return null;
    }

    public static String validarUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            return "La ubicación es obligatoria";
        }
        if (ubicacion.length() > 200) {
            return "La ubicación no puede superar 200 caracteres";
        }
        return null;
    }

    public static String validarFoto(String fotoUri) {
        if (fotoUri != null && fotoUri.length() > 500) {
            return "La referencia de la foto es demasiado larga";
        }
        return null;
    }
}
