package cl.zaror.alertas.backend.alertas;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AlertaRepository {

    private static final String CONSULTA_BASE =
            "SELECT a.ID, a.USUARIO_ID, a.TIPO, a.DESCRIPCION, a.UBICACION, a.FOTO_URI, a.ESTADO, "
                    + "a.FECHA_HORA, a.ATENDIDA_POR, a.FECHA_ATENCION, u.NOMBRE AS REPORTADA_POR "
                    + "FROM ALERTAS a JOIN USUARIOS u ON u.ID = a.USUARIO_ID ";

    private static final RowMapper<Alerta> MAPEADOR = (rs, fila) -> {
        Timestamp atencion = rs.getTimestamp("FECHA_ATENCION");
        long atendidaPor = rs.getLong("ATENDIDA_POR");
        Long atendida = rs.wasNull() ? null : atendidaPor;
        return new Alerta(
                rs.getLong("ID"),
                rs.getLong("USUARIO_ID"),
                rs.getString("TIPO"),
                rs.getString("DESCRIPCION"),
                rs.getString("UBICACION"),
                rs.getString("FOTO_URI"),
                rs.getString("ESTADO"),
                rs.getTimestamp("FECHA_HORA").toInstant(),
                atendida,
                atencion == null ? null : atencion.toInstant(),
                rs.getString("REPORTADA_POR"));
    };

    private final JdbcTemplate jdbc;

    public AlertaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Crea una alerta ACTIVA (estado y fecha los pone la base) y devuelve su id. */
    public long crear(long usuarioId, String tipo, String descripcion, String ubicacion, String fotoUri) {
        KeyHolder llaves = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO ALERTAS (USUARIO_ID, TIPO, DESCRIPCION, UBICACION, FOTO_URI) VALUES (?, ?, ?, ?, ?)",
                    new String[] {"ID"});
            ps.setLong(1, usuarioId);
            ps.setString(2, tipo);
            ps.setString(3, descripcion);
            ps.setString(4, ubicacion);
            if (fotoUri == null) {
                ps.setNull(5, Types.VARCHAR);
            } else {
                ps.setString(5, fotoUri);
            }
            return ps;
        }, llaves);
        return llaves.getKey().longValue();
    }

    public Optional<Alerta> buscarPorId(long id) {
        List<Alerta> lista = jdbc.query(
                CONSULTA_BASE + "WHERE a.ID = ?", MAPEADOR, id);
        return lista.stream().findFirst();
    }

    public List<Alerta> listarTodas() {
        return jdbc.query(CONSULTA_BASE + "ORDER BY a.FECHA_HORA DESC", MAPEADOR);
    }

    public List<Alerta> listarDeUsuario(long usuarioId) {
        return jdbc.query(
                CONSULTA_BASE + "WHERE a.USUARIO_ID = ? ORDER BY a.FECHA_HORA DESC",
                MAPEADOR, usuarioId);
    }

    /** Pasa la alerta a otro estado. atendidaPor y fechaAtencion solo se usan al marcarla ATENDIDA. */
    public void actualizarEstado(long id, String estado, Long atendidaPor, Instant fechaAtencion) {
        jdbc.update(
                "UPDATE ALERTAS SET ESTADO = ?, ATENDIDA_POR = ?, FECHA_ATENCION = ? WHERE ID = ?",
                ps -> {
                    ps.setString(1, estado);
                    if (atendidaPor == null) {
                        ps.setNull(2, Types.NUMERIC);
                    } else {
                        ps.setLong(2, atendidaPor);
                    }
                    if (fechaAtencion == null) {
                        ps.setNull(3, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(3, Timestamp.from(fechaAtencion));
                    }
                    ps.setLong(4, id);
                });
    }

    /** Actualiza la URI de la fotografía asociada a una alerta. */
    public void actualizarFotoUri(long id, String fotoUri) {
        jdbc.update(
                "UPDATE ALERTAS SET FOTO_URI = ? WHERE ID = ?",
                ps -> {
                    ps.setString(1, fotoUri);
                    ps.setLong(2, id);
                });
    }
    public void eliminar(long id) {
        jdbc.update("DELETE FROM ALERTAS WHERE ID = ?", id);
    }
}
