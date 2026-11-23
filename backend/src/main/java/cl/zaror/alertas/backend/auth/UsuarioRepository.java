package cl.zaror.alertas.backend.auth;

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
public class UsuarioRepository {

    private static final String COLUMNAS =
            "ID, NOMBRE, TELEFONO, DIRECCION, CLAVE_HASH, ROL, ESTADO, INTENTOS_FALLIDOS, BLOQUEADO_HASTA";

    private static final RowMapper<Usuario> MAPEADOR = (rs, fila) -> {
        Timestamp bloqueo = rs.getTimestamp("BLOQUEADO_HASTA");
        return new Usuario(
                rs.getLong("ID"),
                rs.getString("NOMBRE"),
                rs.getString("TELEFONO"),
                rs.getString("DIRECCION"),
                rs.getString("CLAVE_HASH"),
                rs.getString("ROL"),
                rs.getString("ESTADO"),
                rs.getInt("INTENTOS_FALLIDOS"),
                bloqueo == null ? null : bloqueo.toInstant());
    };

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> buscarPorTelefono(String telefono) {
        List<Usuario> lista = jdbc.query(
                "SELECT " + COLUMNAS + " FROM USUARIOS WHERE TELEFONO = ?", MAPEADOR, telefono);
        return lista.stream().findFirst();
    }

    public Optional<Usuario> buscarPorId(long id) {
        List<Usuario> lista = jdbc.query(
                "SELECT " + COLUMNAS + " FROM USUARIOS WHERE ID = ?", MAPEADOR, id);
        return lista.stream().findFirst();
    }

    /** Crea un vecino PENDIENTE (los valores por defecto los pone la base) y devuelve su id. */
    public long crear(String nombre, String telefono, String direccion, String claveHash) {
        KeyHolder llaves = new GeneratedKeyHolder();
        jdbc.update(conexion -> {
            PreparedStatement ps = conexion.prepareStatement(
                    "INSERT INTO USUARIOS (NOMBRE, TELEFONO, DIRECCION, CLAVE_HASH) VALUES (?, ?, ?, ?)",
                    new String[] {"ID"});
            ps.setString(1, nombre);
            ps.setString(2, telefono);
            ps.setString(3, direccion);
            ps.setString(4, claveHash);
            return ps;
        }, llaves);
        return llaves.getKey().longValue();
    }

    public void actualizarIntentos(long id, int intentosFallidos, Instant bloqueadoHasta) {
        jdbc.update(
                "UPDATE USUARIOS SET INTENTOS_FALLIDOS = ?, BLOQUEADO_HASTA = ? WHERE ID = ?",
                ps -> {
                    ps.setInt(1, intentosFallidos);
                    if (bloqueadoHasta == null) {
                        ps.setNull(2, Types.TIMESTAMP);
                    } else {
                        ps.setTimestamp(2, Timestamp.from(bloqueadoHasta));
                    }
                    ps.setLong(3, id);
                });
    }

    // ---- Perfil, clave y administración de usuarios ----

    public void actualizarPerfil(long id, String nombre, String direccion) {
        jdbc.update("UPDATE USUARIOS SET NOMBRE = ?, DIRECCION = ? WHERE ID = ?", nombre, direccion, id);
    }

    /** Cambia el hash y limpia intentos fallidos y bloqueo. */
    public void actualizarClave(long id, String claveHash) {
        jdbc.update(
                "UPDATE USUARIOS SET CLAVE_HASH = ?, INTENTOS_FALLIDOS = 0, BLOQUEADO_HASTA = NULL WHERE ID = ?",
                claveHash, id);
    }

    public List<Usuario> listarPorEstado(String estado) {
        return jdbc.query(
                "SELECT " + COLUMNAS + " FROM USUARIOS WHERE ESTADO = ? ORDER BY FECHA_REGISTRO", MAPEADOR, estado);
    }

    public boolean existeCuadra(long id) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM CUADRAS WHERE ID = ?", Integer.class, id);
        return total != null && total > 0;
    }

    public List<CuadraResponse> listarCuadras() {
        return jdbc.query(
                "SELECT ID, CALLE, TRAMO, SECTOR FROM CUADRAS ORDER BY CALLE, TRAMO",
                (rs, fila) -> new CuadraResponse(
                        rs.getLong("ID"), rs.getString("CALLE"), rs.getString("TRAMO"), rs.getString("SECTOR")));
    }

    /** Aprueba un usuario pendiente o rechazado y le asigna su cuadra. */
    public boolean aprobar(long id, long cuadraId, long aprobadoPor) {
        return jdbc.update(
                "UPDATE USUARIOS SET ESTADO = 'APROBADO', CUADRA_ID = ?, APROBADO_POR = ?, "
                        + "FECHA_APROBACION = SYSTIMESTAMP WHERE ID = ? AND ESTADO IN ('PENDIENTE', 'RECHAZADO')",
                cuadraId, aprobadoPor, id) > 0;
    }

    public boolean rechazar(long id, long rechazadoPor) {
        return jdbc.update(
                "UPDATE USUARIOS SET ESTADO = 'RECHAZADO', APROBADO_POR = ?, "
                        + "FECHA_APROBACION = SYSTIMESTAMP WHERE ID = ? AND ESTADO = 'PENDIENTE'",
                rechazadoPor, id) > 0;
    }

    public void actualizarRol(long id, String rol) {
        jdbc.update("UPDATE USUARIOS SET ROL = ? WHERE ID = ?", rol, id);
    }
}
