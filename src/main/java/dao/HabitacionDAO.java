package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import modelo.HabitacionOcupacionVista;

public class HabitacionDAO {
    public enum Modo { TODAS, OCUPADAS, LIBRES }
   
    public static class HabitacionAsignada {
        public final String numero;
        public final String planta;
        public final String desde;   // start_date
        public final String notas;

        public HabitacionAsignada(String numero, String planta, String desde, String notas) {
            this.numero = numero;
            this.planta = planta;
            this.desde = desde;
            this.notas = notas;
        }
    }

    public Optional<HabitacionAsignada> obtenerHabitacionVigente(int residenteId) throws Exception {
        String sql = """
                SELECT h.numero, h.planta, rh.start_date AS desde, rh.notas
                FROM residente_habitacion rh
                JOIN habitaciones h ON h.id = rh.habitacion_id
                WHERE rh.residente_id = ?
                  AND (rh.end_date IS NULL OR rh.end_date = '')
                ORDER BY rh.start_date DESC
                LIMIT 1
                """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new HabitacionAsignada(
                            rs.getString("numero"),
                            rs.getString("planta"),
                            rs.getString("desde"),
                            rs.getString("notas")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public static class Habitacion {         
        public final int id;
        public final String numero;
        public final String planta;
        public Habitacion(int id, String numero, String planta) {
            this.id = id; this.numero = numero; this.planta = planta;
        }
        @Override public String toString() { 
            return numero + (planta != null && !planta.isBlank() ? " · " + planta : "");
        }
    }

    public List<Habitacion> listarDisponibles() throws Exception {
        String sql = """
            SELECT h.id, h.numero, h.planta
            FROM habitaciones h
            LEFT JOIN residente_habitacion rh
                   ON rh.habitacion_id = h.id AND (rh.end_date IS NULL OR rh.end_date = '')
            WHERE rh.id IS NULL               -- sin ocupación vigente
            ORDER BY h.numero
            """;
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            var out = new ArrayList<Habitacion>();
            while (rs.next()) {
                out.add(new Habitacion(rs.getInt("id"), rs.getString("numero"), rs.getString("planta")));
            }
            return out;
        }
    }

    public void cambiarHabitacion(int residenteId, int nuevaHabitacionId, String startDate, String notas) throws Exception {
        try (var c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                // cerrar vigente
                try (var ps = c.prepareStatement("""
                        UPDATE residente_habitacion
                        SET end_date = ?
                        WHERE residente_id = ? AND (end_date IS NULL OR end_date = '')
                    """)) {
                    ps.setString(1, startDate);
                    ps.setInt(2, residenteId);
                    ps.executeUpdate();
                }
                // crear nueva
                try (var ps = c.prepareStatement("""
                        INSERT INTO residente_habitacion (residente_id, habitacion_id, start_date, notas)
                        VALUES (?,?,?,?)
                    """)) {
                    ps.setInt(1, residenteId);
                    ps.setInt(2, nuevaHabitacionId);
                    ps.setString(3, startDate);
                    ps.setString(4, notas);
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


    public static class HistHab {
        public final String numero, planta, desde, hasta, notas;
        public HistHab(String numero, String planta, String desde, String hasta, String notas) {
            this.numero = numero; this.planta = planta; this.desde = desde; this.hasta = hasta; this.notas = notas;
        }
        public String getNumero() { return numero; }
        public String getPlanta() { return planta; }
        public String getDesde()  { return desde;  }
        public String getHasta()  { return hasta;  }
        public String getNotas()  { return notas;  }
    }

    public List<HistHab> listarHistorico(int residenteId) throws Exception {
        String sql = """
            SELECT h.numero, h.planta, rh.start_date AS desde, rh.end_date AS hasta, rh.notas
            FROM residente_habitacion rh
            JOIN habitaciones h ON h.id = rh.habitacion_id
            WHERE rh.residente_id = ?
            ORDER BY rh.start_date DESC
            """;
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (var rs = ps.executeQuery()) {
                var out = new ArrayList<HistHab>();
                while (rs.next()) {
                    out.add(new HistHab(
                        rs.getString("numero"),
                        rs.getString("planta"),
                        rs.getString("desde"),
                        rs.getString("hasta"),
                        rs.getString("notas")
                    ));
                }
                return out;
            }
        }
    }


    public java.util.List<modelo.Habitacion> listarCatalogo(String filtro) throws Exception {
        
        String where = "";
        if (filtro != null && !filtro.isBlank()) {
            where = "WHERE numero LIKE ? OR IFNULL(planta,'') LIKE ?";
        }
        String sql = "SELECT id, numero, planta FROM habitaciones " + where + " ORDER BY numero";

        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql)) {

            if (!where.isBlank()) {
                String like = "%" + filtro.trim() + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }

            var out = new java.util.ArrayList<modelo.Habitacion>();
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new modelo.Habitacion(
                            rs.getInt("id"),
                            rs.getString("numero"),
                            rs.getString("planta")
                    ));
                }
            }
            return out;
        }
    }

    /** Inserta habitación. Devuelve ID generado. (numero es UNIQUE) */
    public int insertarHabitacion(String numero, String planta) throws Exception {
        String sql = "INSERT INTO habitaciones (numero, planta) VALUES (?,?)";
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, numero);
            ps.setString(2, planta);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("No se pudo obtener el id de la habitación creada");
            }
        }
    }

    /** Actualiza habitación por id. */
    public void actualizarHabitacion(int id, String numero, String planta) throws Exception {
        String sql = "UPDATE habitaciones SET numero = ?, planta = ? WHERE id = ?";
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setString(2, planta);
            ps.setInt(3, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Habitación no encontrada");
        }
    }
    /** Elimina habitación por id. */
    public void eliminarHabitacion(int id) throws Exception {
        String sql = "DELETE FROM habitaciones WHERE id = ?";
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
    public java.util.List<HabitacionOcupacionVista> listarOcupacionActual(String filtro, Modo modo) throws Exception {

    StringBuilder sb = new StringBuilder();
    java.util.List<String> params = new java.util.ArrayList<>();

    // Filtro de texto (número/planta/id/nombre/apellidos)
    if (filtro != null && !filtro.isBlank()) {
        sb.append(" WHERE ( h.numero LIKE ? OR IFNULL(h.planta,'') LIKE ? ")
          .append(" OR CAST(r.id AS TEXT) LIKE ? OR IFNULL(r.nombre,'') LIKE ? OR IFNULL(r.apellidos,'') LIKE ? )");
        String like = "%" + filtro.trim() + "%";
        params.add(like); params.add(like); params.add(like); params.add(like); params.add(like);
    }

    // Filtro de modo
    String modoClause = switch (modo == null ? Modo.TODAS : modo) {
        case OCUPADAS -> (sb.length() == 0 ? " WHERE rh.id IS NOT NULL" : " AND rh.id IS NOT NULL");
        case LIBRES   -> (sb.length() == 0 ? " WHERE rh.id IS NULL"     : " AND rh.id IS NULL");
        default       -> "";
    };

    String sql = """
        SELECT
            h.id            AS hab_id,
            h.numero        AS numero,
            h.planta        AS planta,
            r.id            AS residente_id,
            r.nombre        AS nombre,
            r.apellidos     AS apellidos
        FROM habitaciones h
        LEFT JOIN residente_habitacion rh
               ON rh.habitacion_id = h.id
              AND (rh.end_date IS NULL OR rh.end_date = '')
        LEFT JOIN residentes r
               ON r.id = rh.residente_id
        """ + sb.toString() + modoClause + " ORDER BY h.numero";

    try (var c = ConexionBD.obtener();
         var ps = c.prepareStatement(sql)) {
        int idx = 1;
        for (String p : params) ps.setString(idx++, p);

        try (var rs = ps.executeQuery()) {
            var out = new java.util.ArrayList<HabitacionOcupacionVista>();
            while (rs.next()) {
                out.add(new HabitacionOcupacionVista(
                    rs.getInt("hab_id"),
                    rs.getString("numero"),
                    rs.getString("planta"),
                    (Integer) (rs.getObject("residente_id") == null ? null : rs.getInt("residente_id")),
                    rs.getString("nombre"),
                    rs.getString("apellidos")
                ));
            }
            return out;
        }
    }
}
}