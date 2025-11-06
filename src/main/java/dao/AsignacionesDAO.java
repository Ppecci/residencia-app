package dao;

import bd.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class AsignacionesDAO {

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static class AsignacionDTO {
        public Integer id;            
        public final int residenteId;
        public final int trabajadorId;
        public final LocalDate startDate; 
        public final LocalDate endDate;   
        public final String notas;

        public AsignacionDTO(Integer id, int residenteId, int trabajadorId,
                             LocalDate startDate, LocalDate endDate, String notas) {
            this.id = id;
            this.residenteId = residenteId;
            this.trabajadorId = trabajadorId;
            this.startDate = Objects.requireNonNull(startDate, "startDate requerido");
            this.endDate = endDate;
            this.notas = notas;
        }
    }

   
    public List<modelo.AsignacionVista> listarPorTrabajador(int idTrabajador, boolean soloVigentes) throws Exception {
        String sql = """
           SELECT a.id AS asignacion_id,
                  r.id AS residente_id,
                  r.nombre || ' ' || IFNULL(r.apellidos,'') AS residente,
                  a.start_date AS inicio,
                  a.end_date   AS fin,
                  a.notas
           FROM asignacion_trabajador a
           JOIN residentes r ON r.id = a.residente_id
           WHERE a.trabajador_id = ?
        """;
        if (soloVigentes) sql += " AND (a.end_date IS NULL OR date(a.end_date) > date('now')) ";
        sql += " ORDER BY r.apellidos, r.nombre";

        List<modelo.AsignacionVista> out = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idTrabajador);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    var v = new modelo.AsignacionVista();
                    v.setIdAsignacion(rs.getInt("asignacion_id"));
                    v.setIdResidente(rs.getInt("residente_id"));
                    v.setResidente(rs.getString("residente"));
                    v.setInicio(rs.getString("inicio"));
                    v.setFin(rs.getString("fin"));
                    v.setNotas(rs.getString("notas"));
                    out.add(v);
                }
            }
        }
        return out;
    }

    public List<Map<String, Object>> listarCuidadoresVigentesDeResidente(int residenteId) throws Exception {
        String sql = """
            SELECT t.id   AS trabajador_id,
                   t.nombre || ' ' || IFNULL(t.apellidos,'') AS trabajador,
                   a.start_date, a.end_date
            FROM asignacion_trabajador a
            JOIN trabajadores t ON t.id = a.trabajador_id
            WHERE a.residente_id = ?
              AND a.end_date IS NULL
              AND t.activo = 1
            ORDER BY t.apellidos, t.nombre
        """;
        List<Map<String, Object>> out = new ArrayList<>();
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("trabajador_id", rs.getInt("trabajador_id"));
                    row.put("trabajador", rs.getString("trabajador"));
                    row.put("start_date", rs.getString("start_date"));
                    row.put("end_date", rs.getString("end_date"));
                    out.add(row);
                }
            }
        }
        return out;
    }

    public int contarVigentesPorTrabajador(int trabajadorId) throws Exception {
        String sql = """
            SELECT COUNT(*)
            FROM asignacion_trabajador a
            JOIN trabajadores t ON t.id = a.trabajador_id
            WHERE a.trabajador_id = ?
              AND a.end_date IS NULL
              AND t.activo = 1
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, trabajadorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

   
    public int insertar(AsignacionDTO a) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                if (a.endDate == null) {
                    
                    exigirTrabajadorActivo(c, a.trabajadorId);
                }
                String sql = """
                    INSERT INTO asignacion_trabajador
                        (residente_id, trabajador_id, start_date, end_date, notas)
                    VALUES (?, ?, ?, ?, ?)
                """;
                try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, a.residenteId);
                    ps.setInt(2, a.trabajadorId);
                    ps.setString(3, a.startDate.format(FMT));
                    if (a.endDate == null) ps.setNull(4, Types.VARCHAR);
                    else ps.setString(4, a.endDate.format(FMT));
                    ps.setString(5, a.notas);
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        int id = keys.next() ? keys.getInt(1) : -1;
                        c.commit();
                        return id;
                    }
                }
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public int cerrarAsignacion(int asignacionId, LocalDate endDate) throws Exception {
        String sql = "UPDATE asignacion_trabajador SET end_date = ? WHERE id = ? AND end_date IS NULL";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, endDate.format(FMT));
            ps.setInt(2, asignacionId);
            return ps.executeUpdate();
        }
    }

    public int actualizarFechasYNotas(AsignacionDTO a) throws Exception {
        Objects.requireNonNull(a.id, "id requerido para actualizar");
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                if (a.endDate == null) {
                    exigirTrabajadorActivo(c, a.trabajadorId);
                }
                String sql = """
                    UPDATE asignacion_trabajador
                       SET start_date = ?, end_date = ?, notas = ?
                     WHERE id = ?
                """;
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, a.startDate.format(FMT));
                    if (a.endDate == null) ps.setNull(2, Types.VARCHAR);
                    else ps.setString(2, a.endDate.format(FMT));
                    ps.setString(3, a.notas);
                    ps.setInt(4, a.id);
                    int n = ps.executeUpdate();
                    c.commit();
                    return n;
                }
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public int cerrarAsignacionesDeTrabajador(int trabajadorId, LocalDate endDate) throws Exception {
        String sql = """
            UPDATE asignacion_trabajador
               SET end_date = ?
             WHERE trabajador_id = ?
               AND end_date IS NULL
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, endDate.format(FMT));
            ps.setInt(2, trabajadorId);
            return ps.executeUpdate();
        }
    }

    private void exigirTrabajadorActivo(Connection c, int trabajadorId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT activo FROM trabajadores WHERE id = ?")) {
            ps.setInt(1, trabajadorId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("activo") == 0) {
                    throw new IllegalStateException(
                            "No se puede asignar un residente a un trabajador inactivo.");
                }
            }
        }
    }

    public boolean residenteTieneCuidadorVigente(int residenteId) throws Exception {
        String sql = """
            SELECT EXISTS(
                SELECT 1
                FROM asignacion_trabajador a
                JOIN trabajadores t ON t.id = a.trabajador_id
                WHERE a.residente_id = ?
                  AND a.end_date IS NULL
                  AND t.activo = 1
                LIMIT 1
            )
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }
        }
    }
}
