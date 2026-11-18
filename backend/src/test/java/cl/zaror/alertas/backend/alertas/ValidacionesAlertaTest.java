package cl.zaror.alertas.backend.alertas;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidacionesAlertaTest {

    @Test
    void tipoVacioONuloDebeRechazarse() {
        assertThat(ValidacionesAlerta.validarTipo(null)).isNotNull();
        assertThat(ValidacionesAlerta.validarTipo("  ")).isNotNull();
    }

    @Test
    void tipoFueraDelCatalogoDebeRechazarse() {
        assertThat(ValidacionesAlerta.validarTipo("INCENDIO")).isEqualTo("El tipo de alerta no es válido");
    }

    @Test
    void tiposDelCatalogoDebenAceptarse() {
        assertThat(ValidacionesAlerta.validarTipo("EMERGENCIA")).isNull();
        assertThat(ValidacionesAlerta.validarTipo("FALLA_ALARMA")).isNull();
        assertThat(ValidacionesAlerta.validarTipo("SOLICITUD_APOYO")).isNull();
    }

    @Test
    void descripcionCortaOLargaDebeRechazarse() {
        assertThat(ValidacionesAlerta.validarDescripcion(null)).isNotNull();
        assertThat(ValidacionesAlerta.validarDescripcion("corta")).contains("al menos 10");
        assertThat(ValidacionesAlerta.validarDescripcion("x".repeat(241))).contains("240");
    }

    @Test
    void descripcionEnElLimiteDebeAceptarse() {
        assertThat(ValidacionesAlerta.validarDescripcion("x".repeat(10))).isNull();
        assertThat(ValidacionesAlerta.validarDescripcion("x".repeat(240))).isNull();
    }

    @Test
    void ubicacionVaciaOMuyLargaDebeRechazarse() {
        assertThat(ValidacionesAlerta.validarUbicacion(" ")).isNotNull();
        assertThat(ValidacionesAlerta.validarUbicacion("x".repeat(201))).isNotNull();
        assertThat(ValidacionesAlerta.validarUbicacion("Pasaje Uno 123")).isNull();
    }

    @Test
    void fotoOpcionalPeroConLargoMaximo() {
        assertThat(ValidacionesAlerta.validarFoto(null)).isNull();
        assertThat(ValidacionesAlerta.validarFoto("x".repeat(501))).isNotNull();
    }
}
