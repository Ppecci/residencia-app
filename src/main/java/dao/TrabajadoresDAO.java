package dao;

import bd.ConexionBD;
import modelo.Trabajador;
import modelo.TrabajadorResumenFila;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TrabajadoresDAO {

    // ---------------------------------------------------------------------
    // LISTAR (con preferencia por username activo en 'usuarios')
    // ---------------------------------------------------------------------
    public List<Trabajador> listar(String filtro) throws Exception {
        List<Trabajador> out = new ArrayList<>();

        String base = """
            SELECT
                t.id,
                t.nombre,
                CASE
                  WHEN u.username IS NOT NULL AND u.activo = 1 THEN u.username
                  ELSE t.usuario
                END AS usuario,
                t.email,
                t.activo
            FROM trabajadores t
            LEFT JOIN usuarios u
                   ON u.trabajador_id = t.id
                  AND u.rol = 'TRABAJADOR'
            """;

        String sql = base + (
                (filtro == null || filtro.isBlank())
                        ? " ORDER BY t.nombre, usuario"
                        : " WHERE t.nombre LIKE ? OR COALESCE(u.username, t.usuario) LIKE ? OR t.email LIKE ? " +
                          " ORDER BY t.nombre, usuario"
        );

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (!(filtro == null || filtro.isBlank())) {
                String like = "%" + filtro.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Trabajador(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("usuario"),
                            rs.getString("email"),
                            rs.getInt("activo") == 1
                    ));
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // INSERTAR (crea en 'trabajadores' y en 'usuarios' en una transacción)
    //  - t.getPasswordHashTemporal() debe venir relleno desde el controlador
    // ---------------------------------------------------------------------
    public void insertar(Trabajador t) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                int trabajadorId;

                // 1) trabajadores (guardamos también password_hash por NOT NULL legacy)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO trabajadores (nombre, usuario, email, activo, password_hash) VALUES (?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, t.getNombre());
                    ps.setString(2, t.getUsuario());
                    ps.setString(3, t.getEmail());
                    ps.setInt(4, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
                    ps.setString(5, t.getPasswordHashTemporal());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("No se pudo obtener id de trabajador");
                        trabajadorId = rs.getInt(1);
                    }
                }

                // 2) usuarios (fuente de verdad para login)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO usuarios (username, password_hash, rol, trabajador_id, activo, created_at) " +
                        "VALUES (?,?,?,?,?, datetime('now'))")) {
                    ps.setString(1, t.getUsuario());
                    ps.setString(2, t.getPasswordHashTemporal());
                    ps.setString(3, "TRABAJADOR");
                    ps.setInt(4, trabajadorId);
                    ps.setInt(5, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
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

    // ---------------------------------------------------------------------
    // ACTUALIZAR datos del trabajador y reflejo en 'usuarios'
    //  (no toca el password aquí)
    // ---------------------------------------------------------------------
    public void actualizar(Trabajador t) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                // trabajadores
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE trabajadores SET nombre=?, usuario=?, email=?, activo=? WHERE id=?")) {
                    ps.setString(1, t.getNombre());
                    ps.setString(2, t.getUsuario());
                    ps.setString(3, t.getEmail());
                    ps.setInt(4, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
                    ps.setInt(5, t.getId());
                    ps.executeUpdate();
                }

                // usuarios
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE usuarios SET username=?, activo=? WHERE trabajador_id=? AND rol='TRABAJADOR'")) {
                    ps.setString(1, t.getUsuario());
                    ps.setInt(2, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
                    ps.setInt(3, t.getId());
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

    // ---------------------------------------------------------------------
    // ACTUALIZAR PASSWORD (solo en 'usuarios')
    // ---------------------------------------------------------------------
    public void actualizarPassword(int idTrabajador, String nuevoHash) throws Exception {
        String sql = "UPDATE usuarios SET password_hash=? WHERE trabajador_id=? AND rol='TRABAJADOR'";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, idTrabajador);
            ps.executeUpdate();
        }
    }

    // ---------------------------------------------------------------------
    // ELIMINAR (borra primero en 'usuarios', después en 'trabajadores')
    //  - Si hay dependencias (asignacion_trabajador sin CASCADE) puede fallar por FK
    // ---------------------------------------------------------------------
    public void eliminar(int id) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                // usuarios
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM usuarios WHERE trabajador_id=? AND rol='TRABAJADOR'")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }

                // trabajadores
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM trabajadores WHERE id=?")) {
                    ps.setInt(1, id);
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

    // ---------------------------------------------------------------------
    // LISTAR ACTIVOS (excluyendo uno), mostrando username preferente
    // ---------------------------------------------------------------------
    public List<Trabajador> listarActivosExcepto(int idExcluir) throws Exception {
        String sql = """
            SELECT
                t.id,
                t.nombre,
                CASE
                  WHEN u.username IS NOT NULL AND u.activo = 1 THEN u.username
                  ELSE t.usuario
                END AS usuario,
                t.email,
                t.activo
            FROM trabajadores t
            LEFT JOIN usuarios u
                   ON u.trabajador_id = t.id
                  AND u.rol = 'TRABAJADOR'
            WHERE t.activo = 1 AND t.id <> ?
            ORDER BY t.nombre
            """;

        List<Trabajador> out = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Trabajador(
                            rs.getInt("id"),
                            rs.getString("nombre"),
                            rs.getString("usuario"),
                            rs.getString("email"),
                            rs.getInt("activo") == 1
                    ));
                }
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------
    // LISTAR RESIDENTES asignados a un trabajador (vista resumen)
    //   - Solo vigentes: end_date NULL
    // ---------------------------------------------------------------------
    public List<TrabajadorResumenFila> listarAsignados(int trabajadorId, String filtro) throws Exception {
        StringBuilder where = new StringBuilder();
        List<String> paramsLike = new ArrayList<>();

        if (filtro != null && !filtro.isBlank()) {
            where.append(" AND ( r.nombre LIKE ? OR r.apellidos LIKE ? OR IFNULL(hv.hab_numero,'') LIKE ? ) ");
            String like = "%" + filtro.trim() + "%";
            paramsLike.add(like);
            paramsLike.add(like);
            paramsLike.add(like);
        }

        String sql = """
            SELECT
              r.id            AS residente_id,
              r.nombre        AS nombre,
              r.apellidos     AS apellidos,

              hv.hab_id       AS hab_id,
              hv.hab_numero   AS hab_numero,
              hv.hab_planta   AS hab_planta,

              dv.dieta_id     AS dieta_id,
              dv.dieta_notas  AS dieta_notas,

              (
                SELECT GROUP_CONCAT(m.nombre || CASE WHEN IFNULL(p.dosis,'')<>'' THEN ' '||p.dosis ELSE '' END, ', ')
                FROM prescripciones p
                JOIN medicaciones m ON m.id = p.medicacion_id
                WHERE p.residente_id = r.id
                  AND (p.end_date IS NULL OR p.end_date = '')
              )               AS medicacion_resumen,

              (
                SELECT MIN(c.fecha_hora)
                FROM citas_medicas c
                WHERE c.residente_id = r.id
                  AND c.estado = 'PROGRAMADA'
                  AND c.fecha_hora >= datetime('now')
              )               AS proxima_cita

            FROM asignacion_trabajador at
            JOIN residentes r ON r.id = at.residente_id

            LEFT JOIN (
                SELECT rh.residente_id,
                       h.id      AS hab_id,
                       h.numero  AS hab_numero,
                       h.planta  AS hab_planta
                  FROM residente_habitacion rh
                  JOIN habitaciones h ON h.id = rh.habitacion_id
                 WHERE (rh.end_date IS NULL OR rh.end_date = '')
            ) hv ON hv.residente_id = r.id

            LEFT JOIN (
                SELECT rd.residente_id,
                       rd.dieta_id    AS dieta_id,
                       rd.notas       AS dieta_notas
                  FROM residente_dieta rd
                 WHERE (rd.end_date IS NULL OR rd.end_date = '')
            ) dv ON dv.residente_id = r.id

            WHERE at.trabajador_id = ?
              AND (at.end_date IS NULL OR at.end_date = '')
            """ + where + """
            ORDER BY r.apellidos, r.nombre
            """;

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            int idx = 1;
            ps.setInt(idx++, trabajadorId);

            for (String p : paramsLike) {
                ps.setString(idx++, p);
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<TrabajadorResumenFila> out = new ArrayList<>();
                while (rs.next()) {
                    Integer habId   = (rs.getObject("hab_id")   == null) ? null : rs.getInt("hab_id");
                    Integer dietaId = (rs.getObject("dieta_id") == null) ? null : rs.getInt("dieta_id");

                    out.add(new TrabajadorResumenFila(
                            rs.getInt("residente_id"),
                            rs.getString("nombre"),
                            rs.getString("apellidos"),
                            habId,
                            rs.getString("hab_numero"),
                            rs.getString("hab_planta"),
                            dietaId,
                            rs.getString("dieta_notas"),
                            rs.getString("medicacion_resumen"),
                            rs.getString("proxima_cita")
                    ));
                }
                return out;
            }
        }
    }

    // ---------------------------------------------------------------------
    // OBTENER NOMBRE por id (solo activos)
    // ---------------------------------------------------------------------
    public Optional<String> obtenerNombrePorId(int idTrabajador) throws Exception {
        String sql = "SELECT nombre FROM trabajadores WHERE id=? AND activo=1";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTrabajador);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString("nombre"));
                return Optional.empty();
            }
        }
    }
}
