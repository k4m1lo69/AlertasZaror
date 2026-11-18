package cl.zaror.alertas.data

import cl.zaror.alertas.data.remote.AlertaResponse
import cl.zaror.alertas.data.remote.MapeoAlertas
import cl.zaror.alertas.data.remote.MapeoAlertas.aEntidad
import org.junit.Assert.assertEquals
import org.junit.Test

class MapeoAlertasTest {

    private fun respuesta(
        tipo: String = "EMERGENCIA",
        estado: String = "ACTIVA",
        fecha: String? = "2026-11-12T12:00:00Z",
        reportadaPor: String? = "Vecino Prueba"
    ) = AlertaResponse(
        id = 7L,
        usuarioId = 99L,
        tipo = tipo,
        descripcion = "Humo en el pasaje",
        ubicacion = "Pasaje Uno 123",
        fotoUri = null,
        estado = estado,
        fechaHora = fecha,
        atendidaPor = null,
        fechaAtencion = null,
        reportadaPor = reportadaPor
    )

    @Test
    fun tiposDeLaAppSeTraducenAlServidor() {
        assertEquals("EMERGENCIA", MapeoAlertas.tipoAServidor("Emergencia"))
        assertEquals("FALLA_ALARMA", MapeoAlertas.tipoAServidor("Falla de alarma"))
        assertEquals("SOLICITUD_APOYO", MapeoAlertas.tipoAServidor("Solicitud de apoyo"))
    }

    @Test
    fun tiposDelServidorSeTraducenALaApp() {
        assertEquals("Emergencia", MapeoAlertas.tipoDesdeServidor("EMERGENCIA"))
        assertEquals("Falla de alarma", MapeoAlertas.tipoDesdeServidor("FALLA_ALARMA"))
        assertEquals("Solicitud de apoyo", MapeoAlertas.tipoDesdeServidor("SOLICITUD_APOYO"))
    }

    @Test
    fun tipoDesconocidoSeDejaIgual() {
        assertEquals("OTRO", MapeoAlertas.tipoDesdeServidor("OTRO"))
    }

    @Test
    fun estadosDelServidorSeTraducenALaApp() {
        assertEquals("Activa", MapeoAlertas.estadoDesdeServidor("ACTIVA"))
        assertEquals("Atendida", MapeoAlertas.estadoDesdeServidor("ATENDIDA"))
        assertEquals("Cancelada", MapeoAlertas.estadoDesdeServidor("CANCELADA"))
    }

    @Test
    fun fechaIsoSeConvierteAMilisegundos() {
        assertEquals(1_794_484_800_000L, MapeoAlertas.fechaAMilisegundos("2026-11-12T12:00:00Z", ahora = 5L))
    }

    @Test
    fun fechaConFraccionDeSegundoSeConvierte() {
        assertEquals(
            1_794_484_800_123L,
            MapeoAlertas.fechaAMilisegundos("2026-11-12T12:00:00.123Z", ahora = 5L)
        )
    }

    @Test
    fun fechaEnSegundosNumericosSeConvierte() {
        assertEquals(1_794_484_800_000L, MapeoAlertas.fechaAMilisegundos("1794484800.0", ahora = 5L))
    }

    @Test
    fun fechaVaciaOInvalidaUsaElMomentoActual() {
        assertEquals(5L, MapeoAlertas.fechaAMilisegundos(null, ahora = 5L))
        assertEquals(5L, MapeoAlertas.fechaAMilisegundos("  ", ahora = 5L))
        assertEquals(5L, MapeoAlertas.fechaAMilisegundos("no es fecha", ahora = 5L))
    }

    @Test
    fun respuestaDelServidorSeConvierteEnFilaLocal() {
        val entidad = respuesta().aEntidad(usuarioLocalId = 3)

        assertEquals(7, entidad.id)
        assertEquals("Emergencia", entidad.tipo)
        assertEquals("Activa", entidad.estado)
        assertEquals("Vecino Prueba", entidad.reportadaPor)
        assertEquals(3, entidad.usuarioId)
        assertEquals(1_794_484_800_000L, entidad.fechaHora)
    }

    @Test
    fun sinNombreDelReportanteSeUsaVecino() {
        assertEquals("Vecino", respuesta(reportadaPor = null).aEntidad(3).reportadaPor)
    }
}
