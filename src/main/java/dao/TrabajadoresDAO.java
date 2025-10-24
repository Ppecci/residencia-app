package dao;

import bd.ConexionBD;
import modelo.Trabajador;
import modelo.TrabajadorResumenFila; // <-- NUEVO import

import java.sql.*;
import java.util.*;

public class TrabajadoresDAO {

    public List<Trabajador> listar(String filtro) throws Exception {
        List<Trabajador> out = new ArrayList<>();
        String sql = "SELECT id, nombre, usuario, email, activo " +
                     "FROM trabajadores " +
                     (filtro == null || filtro.isBlank()
                        ? ""
                        : "WHERE nombre LIKE ? OR usuario LIKE ? OR email LIKE ? ") +
                     "ORDER BY nombre, usuario";

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (sql.contains("LIKE")) {
                String like = "%" + filtro + "%";
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
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

    public void insertar(Trabajador t) throws Exception {
        String sql = "INSERT INTO trabajadores (nombre, usuario, password_hash, email, activo) VALUES (?,?,?,?,?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.setString(2, t.getUsuario());
            ps.setString(3, t.getPasswordHashTemporal()); // debe venir ya hasheada
            ps.setString(4, t.getEmail());
            ps.setInt(5, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void actualizar(Trabajador t) throws Exception {
        String sql = "UPDATE trabajadores SET nombre=?, usuario=?, email=?, activo=? WHERE id=?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.setString(2, t.getUsuario());
            ps.setString(3, t.getEmail());
            ps.setInt(4, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        }
    }

    public void actualizarPassword(int idTrabajador, String nuevoHash) throws Exception {
        String sql = "UPDATE trabajadores SET password_hash=? WHERE id=?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, idTrabajador);
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM trabajadores WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // --- NUEVO: lista trabajadores activos excepto el dado ---
    public java.util.List<modelo.Trabajador> listarActivosExcepto(int idExcluir) throws Exception {
        String sql = "SELECT id, nombre, usuario, email, activo " +
                     "FROM trabajadores " +
                     "WHERE activo = 1 AND id <> ? " +
                     "ORDER BY nombre";
        java.util.List<modelo.Trabajador> out = new java.util.ArrayList<>();
        try (java.sql.Connection c = bd.ConexionBD.obtener();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idExcluir);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new modelo.Trabajador(
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

    // =========================================================
    // === NUEVO: método para el Panel Trabajador (listado maestro)
    // === Devuelve una fila por residente asignado al trabajador,
    // === con habitación/dieta vigentes, medicación resumida y próxima cita.
    // =========================================================
    public List<TrabajadorResumenFila> listarAsignados(int trabajadorId, String filtro) throws Exception {

        // WHERE dinámico para el filtro de texto
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

              -- resumen prescripciones activas (una sola línea)
              (
                SELECT GROUP_CONCAT(m.nombre || CASE WHEN IFNULL(p.dosis,'')<>'' THEN ' '||p.dosis ELSE '' END, ', ')
                FROM prescripciones p
                JOIN medicaciones m ON m.id = p.medicacion_id
                WHERE p.residente_id = r.id
                  AND (p.end_date IS NULL OR p.end_date = '')
              )               AS medicacion_resumen,

              -- próxima cita futura programada
              (
                SELECT MIN(c.fecha_hora)
                FROM citas_medicas c
                WHERE c.residente_id = r.id
                  AND c.estado = 'PROGRAMADA'
                  AND c.fecha_hora >= datetime('now')
              )               AS proxima_cita

            FROM asignacion_trabajador at
            JOIN residentes r ON r.id = at.residente_id

            -- Habitación vigente (si existe)
            LEFT JOIN (
                SELECT rh.residente_id,
                       h.id      AS hab_id,
                       h.numero  AS hab_numero,
                       h.planta  AS hab_planta
                  FROM residente_habitacion rh
                  JOIN habitaciones h ON h.id = rh.habitacion_id
                 WHERE (rh.end_date IS NULL OR rh.end_date = '')
            ) hv ON hv.residente_id = r.id

            -- Dieta vigente (si existe)
            LEFT JOIN (
                SELECT rd.residente_id,
                       rd.dieta_id    AS dieta_id,
                       rd.notas       AS dieta_notas
                  FROM residente_dieta rd
                 WHERE (rd.end_date IS NULL OR rd.end_date = '')
            ) dv ON dv.residente_id = r.id

            WHERE at.trabajador_id = ?
              AND (at.end_date IS NULL OR at.end_date = '')
            """ + where.toString() + """
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
    public java.util.Optional<String> obtenerNombrePorId(int idTrabajador) throws Exception {
    String sql = "SELECT nombre FROM trabajadores WHERE id=? AND activo=1";
    try (java.sql.Connection c = bd.ConexionBD.obtener();
         java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, idTrabajador);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return java.util.Optional.ofNullable(rs.getString("nombre"));
            return java.util.Optional.empty();
        }
    }
}
}
