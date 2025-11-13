package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.*;

import modelo.Familiar;              // panel de administración
import modelo.FilaResumenFamiliar;   // Panel Familiar

public class FamiliarDAO {

    public static class FamiliarAsignado {
        public final int idRelacion;
        public final int idFamiliar;
        public final String nombre;
        public final String usuario;
        public final String email;
        public final String parentesco;

        public FamiliarAsignado(int idRelacion, int idFamiliar, String nombre, String usuario,
                                String email, String parentesco) {
            this.idRelacion = idRelacion;
            this.idFamiliar = idFamiliar;
            this.nombre = nombre;
            this.usuario = usuario;
            this.email = email;
            this.parentesco = parentesco;
        }

        public int getIdRelacion()  { return idRelacion; }
        public int getIdFamiliar()  { return idFamiliar; }
        public String getNombre()   { return nombre; }
        public String getUsuario()  { return usuario; }
        public String getEmail()    { return email; }
        public String getParentesco(){ return parentesco; }
    }

    public static class ComboFamiliar {
        public final int id;
        public final String nombre;
        public final String usuario;
        public final String email;

        public ComboFamiliar(int id, String nombre, String usuario, String email) {
            this.id = id;
            this.nombre = nombre;
            this.usuario = usuario;
            this.email = email;
        }

        @Override public String toString() {
            String u = (usuario != null && !usuario.isBlank()) ? " - " + usuario : "";
            String e = (email != null && !email.isBlank())     ? " - " + email   : "";
            return nombre + u + e;
        }
    }

    public List<FamiliarAsignado> listarAsignados(int residenteId) throws Exception {
        String sql = """
            SELECT 
                rf.id AS id_rel,
                f.id  AS id_fam,
                f.nombre,
                u.username AS usuario,              -- AHORA VIENE DE usuarios
                f.email,
                rf.parentesco
            FROM residente_familiar rf
            JOIN familiares f ON f.id = rf.familiar_id
            LEFT JOIN usuarios u
                ON u.familiar_id = f.id
                AND u.rol = 'FAMILIAR'
                AND u.activo = 1
            WHERE rf.residente_id = ?
            ORDER BY f.nombre
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<FamiliarAsignado> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new FamiliarAsignado(
                        rs.getInt("id_rel"),
                        rs.getInt("id_fam"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email"),
                        rs.getString("parentesco")
                    ));
                }
                return out;
            }
        }
    }

    public List<ComboFamiliar> listarNoAsignados(int residenteId) throws Exception {
        String sql = """
            SELECT 
                f.id,
                f.nombre,
                u.username AS usuario,              -- de usuarios
                f.email
            FROM familiares f
            LEFT JOIN usuarios u
                ON u.familiar_id = f.id
                AND u.rol = 'FAMILIAR'
                AND u.activo = 1
            WHERE NOT EXISTS (
                SELECT 1 FROM residente_familiar rf
                WHERE rf.residente_id = ? AND rf.familiar_id = f.id
            )
            ORDER BY f.nombre
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ComboFamiliar> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ComboFamiliar(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email")
                    ));
                }
                return out;
            }
        }
    }

    public void borrarAsignacion(int idRelacion) throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM residente_familiar WHERE id = ?")) {
            ps.setInt(1, idRelacion);
            ps.executeUpdate();
        }
    }

    public void insertarAsignacion(int residenteId, int familiarId, String parentesco) throws Exception {
        String sql = "INSERT INTO residente_familiar (residente_id, familiar_id, parentesco) VALUES (?,?,?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            ps.setInt(2, familiarId);
            ps.setString(3, parentesco);
            ps.executeUpdate();
        }
    }

    public void insertarAsignacion(Integer residenteId, int familiarId, String parentesco) throws Exception {
        insertarAsignacion(residenteId.intValue(), familiarId, parentesco);
    }

    public int crearFamiliar(String nombre, String usuario, String passwordHash, String email) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                if (existeUsernameEnUsuarios(usuario) || existeUsuarioEnFamiliares(usuario)) {
                    throw new SQLException("El usuario ya existe");
                }

                int familiarId;

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO familiares (nombre, usuario, password_hash, email) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {

                    ps.setString(1, nombre);
                    ps.setString(2, usuario);
                    ps.setString(3, passwordHash);
                    ps.setString(4, (email == null || email.isBlank()) ? null : email);

                    int n = ps.executeUpdate();
                    if (n != 1) throw new SQLException("No se insertó el familiar (n=" + n + ")");

                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No se pudo obtener el id del familiar creado");
                        familiarId = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO usuarios (username, password_hash, rol, familiar_id, activo, created_at) " +
                        "VALUES (?,?,?,?,1, datetime('now'))")) {

                    ps.setString(1, usuario);
                    ps.setString(2, passwordHash);
                    ps.setString(3, "FAMILIAR");
                    ps.setInt(4, familiarId);

                    int n = ps.executeUpdate();
                    if (n != 1) throw new SQLException("No se insertó el usuario (n=" + n + ")");
                }

                c.commit();
                return familiarId;

            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public boolean existeUsernameEnUsuarios(String username) throws SQLException {
        String sql = "SELECT 1 FROM usuarios WHERE LOWER(username)=LOWER(?) LIMIT 1";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean existeUsuarioEnFamiliares(String usuario) throws SQLException {
        String sql = "SELECT 1 FROM familiares WHERE LOWER(usuario)=LOWER(?) LIMIT 1";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void actualizarAsignado(int idRelacion, int idFamiliar,
                                   String nombre, String usuario, String email, String parentesco) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE familiares
                           SET nombre = ?, email = ?
                         WHERE id = ?
                    """)) {
                    ps.setString(1, nombre);
                    ps.setString(2, email);
                    ps.setInt(3, idFamiliar);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE residente_familiar
                           SET parentesco = ?
                         WHERE id = ?
                    """)) {
                    ps.setString(1, parentesco);
                    ps.setInt(2, idRelacion);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                       "UPDATE usuarios SET username = ? WHERE familiar_id = ? AND rol = 'FAMILIAR'")) {
                    ps.setString(1, usuario);
                    ps.setInt(2, idFamiliar);
                    ps.executeUpdate();
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public List<Familiar> listar(String filtro) throws Exception {
        String base = """
            SELECT 
                f.id,
                f.nombre,
                u.username AS usuario,
                f.email
            FROM familiares f
            LEFT JOIN usuarios u
                   ON u.familiar_id = f.id
                  AND u.rol = 'FAMILIAR'
                  AND u.activo = 1
        """;

        String sql = base + (filtro != null && !filtro.isBlank()
                ? " WHERE f.nombre LIKE ? ORDER BY f.nombre"
                : " ORDER BY f.nombre");

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (filtro != null && !filtro.isBlank()) {
                ps.setString(1, "%" + filtro + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<Familiar> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Familiar(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email")
                    ));
                }
                return out;
            }
        }
    }

    public void insertar(Familiar familiar) throws Exception {
        crearFamiliar(
            familiar.getNombre(),
            familiar.getUsuario(),
            familiar.getPasswordHashTemporal(),
            familiar.getEmail()
        );
    }

    public void actualizarBasico(Familiar familiar) throws Exception {
        String sql = "UPDATE familiares SET nombre = ?, email = ? WHERE id = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, familiar.getNombre());
            ps.setString(2, familiar.getEmail());
            ps.setInt(3, familiar.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(Integer id) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM residente_familiar WHERE familiar_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

               
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM usuarios WHERE familiar_id = ? AND rol = 'FAMILIAR'")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM familiares WHERE id = ?")) {
                    ps.setInt(1, id);
                    int n = ps.executeUpdate();
                    if (n != 1) throw new SQLException("No se encontró el familiar (id=" + id + ")");
                }

                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private static final String SELECT_PANEL_FAMILIAR_BASE = """
        WITH res_vinc AS (
            SELECT r.id AS residente_id, r.nombre, r.apellidos
            FROM residentes r
            JOIN residente_familiar rf ON rf.residente_id = r.id
            WHERE rf.familiar_id = ?   -- :fid
        ),
        -- Habitación actual (vigente y última por start_date)
        hab_actual AS (
            SELECT rh1.residente_id, h1.id AS habitacion_id, h1.numero, h1.planta
            FROM residente_habitacion rh1
            JOIN habitaciones h1 ON h1.id = rh1.habitacion_id
            WHERE (rh1.end_date IS NULL OR date(rh1.end_date) >= date('now'))
              AND NOT EXISTS (
                SELECT 1 FROM residente_habitacion rh2
                WHERE rh2.residente_id = rh1.residente_id
                  AND (rh2.end_date IS NULL OR date(rh2.end_date) >= date('now'))
                  AND date(rh2.start_date) > date(rh1.start_date)
              )
        ),
        -- Dieta vigente (última por start_date)
        dieta_vig AS (
            SELECT rd1.residente_id, d1.nombre AS dieta_nombre, rd1.notas AS dieta_notas
            FROM residente_dieta rd1
            JOIN dietas d1 ON d1.id = rd1.dieta_id
            WHERE (rd1.end_date IS NULL OR date(rd1.end_date) >= date('now'))
              AND NOT EXISTS (
                SELECT 1 FROM residente_dieta rd2
                WHERE rd2.residente_id = rd1.residente_id
                  AND (rd2.end_date IS NULL OR date(rd2.end_date) >= date('now'))
                  AND date(rd2.start_date) > date(rd1.start_date)
              )
        ),
        -- Medicación activa resumida por residente
        med_resumen AS (
            SELECT p.residente_id,
                   GROUP_CONCAT(
                     TRIM(
                       COALESCE(m.nombre,'') || ' ' ||
                       COALESCE(p.dosis,'')  ||
                       CASE WHEN p.frecuencia IS NOT NULL AND p.frecuencia<>'' THEN ' '||p.frecuencia ELSE '' END
                     ),
                     '; '
                   ) AS meds
            FROM prescripciones p
            JOIN medicaciones m ON m.id = p.medicacion_id
            WHERE (p.end_date IS NULL OR date(p.end_date) >= date('now'))
            GROUP BY p.residente_id
        ),
        -- Próxima cita: MIN(fecha_hora) futura por residente
        prox_min AS (
            SELECT c.residente_id, MIN(datetime(c.fecha_hora)) AS min_fecha
            FROM citas_medicas c
            WHERE datetime(c.fecha_hora) >= datetime('now')
              AND c.estado = 'PROGRAMADA'
            GROUP BY c.residente_id
        ),
        prox_cita AS (
            SELECT c.residente_id, c.especialidad, c.fecha_hora, c.lugar, c.notas
            FROM prox_min pm
            JOIN citas_medicas c
              ON c.residente_id = pm.residente_id
             AND datetime(c.fecha_hora) = pm.min_fecha
        )
        SELECT
          rv.residente_id,
          rv.nombre,
          rv.apellidos,
          ha.habitacion_id,
          ha.numero,
          ha.planta,
          mr.meds,
          dv.dieta_nombre,
          dv.dieta_notas,
          CASE
            WHEN pc.especialidad IS NULL THEN ''
            ELSE pc.especialidad || ' - ' || pc.fecha_hora ||
                 CASE WHEN pc.lugar IS NOT NULL AND pc.lugar<>'' THEN ' - '||pc.lugar ELSE '' END ||
                 CASE WHEN pc.notas IS NOT NULL AND pc.notas<>'' THEN ' - '||pc.notas ELSE '' END
          END AS proxima_cita
        FROM res_vinc rv
        LEFT JOIN hab_actual ha ON ha.residente_id = rv.residente_id
        LEFT JOIN med_resumen mr ON mr.residente_id = rv.residente_id
        LEFT JOIN dieta_vig dv   ON dv.residente_id = rv.residente_id
        LEFT JOIN prox_cita pc   ON pc.residente_id = rv.residente_id
        ORDER BY rv.apellidos, rv.nombre
        """;

    private static final String SELECT_PANEL_FAMILIAR_BUSCAR = """
        WITH res_vinc AS (
            SELECT r.id AS residente_id, r.nombre, r.apellidos
            FROM residentes r
            JOIN residente_familiar rf ON rf.residente_id = r.id
            WHERE rf.familiar_id = ?   -- :fid
              AND (
                   r.nombre    LIKE '%' || ? || '%'
                OR r.apellidos LIKE '%' || ? || '%'
                OR EXISTS (
                    SELECT 1
                    FROM residente_habitacion rh
                    JOIN habitaciones h ON h.id = rh.habitacion_id
                    WHERE rh.residente_id = r.id
                      AND (rh.end_date IS NULL OR date(rh.end_date) >= date('now'))
                      AND h.numero LIKE '%' || ? || '%'
                )
              )
        ),
        hab_actual AS (
            SELECT rh1.residente_id, h1.id AS habitacion_id, h1.numero, h1.planta
            FROM residente_habitacion rh1
            JOIN habitaciones h1 ON h1.id = rh1.habitacion_id
            WHERE (rh1.end_date IS NULL OR date(rh1.end_date) >= date('now'))
              AND NOT EXISTS (
                SELECT 1 FROM residente_habitacion rh2
                WHERE rh2.residente_id = rh1.residente_id
                  AND (rh2.end_date IS NULL OR date(rh2.end_date) >= date('now'))
                  AND date(rh2.start_date) > date(rh1.start_date)
              )
        ),
        dieta_vig AS (
            SELECT rd1.residente_id, d1.nombre AS dieta_nombre, rd1.notas AS dieta_notas
            FROM residente_dieta rd1
            JOIN dietas d1 ON d1.id = rd1.dieta_id
            WHERE (rd1.end_date IS NULL OR date(rd1.end_date) >= date('now'))
              AND NOT EXISTS (
                SELECT 1 FROM residente_dieta rd2
                WHERE rd2.residente_id = rd1.residente_id
                  AND (rd2.end_date IS NULL OR date(rd2.end_date) >= date('now'))
                  AND date(rd2.start_date) > date(rd1.start_date)
              )
        ),
        med_resumen AS (
            SELECT p.residente_id,
                   GROUP_CONCAT(
                     TRIM(
                       COALESCE(m.nombre,'') || ' ' ||
                       COALESCE(p.dosis,'')  ||
                       CASE WHEN p.frecuencia IS NOT NULL AND p.frecuencia<>'' THEN ' '||p.frecuencia ELSE '' END
                     ),
                     '; '
                   ) AS meds
            FROM prescripciones p
            JOIN medicaciones m ON m.id = p.medicacion_id
            WHERE (p.end_date IS NULL OR date(p.end_date) >= date('now'))
            GROUP BY p.residente_id
        ),
        prox_min AS (
            SELECT c.residente_id, MIN(datetime(c.fecha_hora)) AS min_fecha
            FROM citas_medicas c
            WHERE datetime(c.fecha_hora) >= datetime('now')
              AND c.estado = 'PROGRAMADA'
            GROUP BY c.residente_id
        ),
        prox_cita AS (
            SELECT c.residente_id, c.especialidad, c.fecha_hora, c.lugar, c.notas
            FROM prox_min pm
            JOIN citas_medicas c
              ON c.residente_id = pm.residente_id
             AND datetime(c.fecha_hora) = pm.min_fecha
        )
        SELECT
          rv.residente_id,
          rv.nombre,
          rv.apellidos,
          ha.habitacion_id,
          ha.numero,
          ha.planta,
          mr.meds,
          dv.dieta_nombre,
          dv.dieta_notas,
          CASE
            WHEN pc.especialidad IS NULL THEN ''
            ELSE pc.especialidad || ' - ' || pc.fecha_hora ||
                 CASE WHEN pc.lugar IS NOT NULL AND pc.lugar<>'' THEN ' - '||pc.lugar ELSE '' END ||
                 CASE WHEN pc.notas IS NOT NULL AND pc.notas<>'' THEN ' - '||pc.notas ELSE '' END
          END AS proxima_cita
        FROM res_vinc rv
        LEFT JOIN hab_actual ha ON ha.residente_id = rv.residente_id
        LEFT JOIN med_resumen mr ON mr.residente_id = rv.residente_id
        LEFT JOIN dieta_vig dv   ON dv.residente_id = rv.residente_id
        LEFT JOIN prox_cita pc   ON pc.residente_id = rv.residente_id
        ORDER BY rv.apellidos, rv.nombre
        """;

    public List<FilaResumenFamiliar> obtenerResumenPanel(int familiarId) {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_PANEL_FAMILIAR_BASE)) {

            ps.setInt(1, familiarId);

            try (ResultSet rs = ps.executeQuery()) {
                return mapearResumenPanel(rs);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error obteniendo resumen del panel familiar", e);
        }
    }

    public List<FilaResumenFamiliar> buscarEnPanel(int familiarId, String texto) {
        if (texto == null) texto = "";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(SELECT_PANEL_FAMILIAR_BUSCAR)) {

            ps.setInt(1, familiarId);
            ps.setString(2, texto);
            ps.setString(3, texto);
            ps.setString(4, texto);

            try (ResultSet rs = ps.executeQuery()) {
                return mapearResumenPanel(rs);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error buscando en el panel familiar", e);
        }
    }

    private List<FilaResumenFamiliar> mapearResumenPanel(ResultSet rs) throws SQLException {
        List<FilaResumenFamiliar> out = new ArrayList<>();
        while (rs.next()) {
            int residenteId   = rs.getInt("residente_id");
            String nombre     = rs.getString("nombre");
            String apellidos  = rs.getString("apellidos");

            Integer idHab     = getNullableInt(rs, "habitacion_id");
            String numero     = rs.getString("numero");
            String planta     = rs.getString("planta");

            String meds       = rs.getString("meds");
            String dietaNom   = rs.getString("dieta_nombre");
            String dietaNot   = rs.getString("dieta_notas");
            String prox       = rs.getString("proxima_cita");

            out.add(new FilaResumenFamiliar(
                residenteId, nombre, apellidos,
                idHab, numero, planta,
                meds, dietaNom, dietaNot, prox
            ));
        }
        return out;
    }

    private Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    public void actualizarPasswordFamiliar(int idFamiliar, String nuevoHash) throws Exception {
        String sql = "UPDATE usuarios SET password_hash=? WHERE familiar_id=? AND rol='FAMILIAR'";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, idFamiliar);
            ps.executeUpdate();
        }
    }
}
