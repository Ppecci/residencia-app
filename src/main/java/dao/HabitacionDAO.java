package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.Optional;

public class HabitacionDAO {

    // DTO simple para devolver los datos de la habitación vigente
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
    public static class Habitacion {        // catálogo de habitaciones
    public final int id;
    public final String numero;
    public final String planta;
    public Habitacion(int id, String numero, String planta) {
        this.id = id; this.numero = numero; this.planta = planta;
    }
    @Override public String toString() { // se usa en el ChoiceDialog
        return numero + (planta != null && !planta.isBlank() ? " · " + planta : "");
    }
}

/** Lista habitaciones NO ocupadas (sin asignación vigente). */
public java.util.List<Habitacion> listarDisponibles() throws Exception {
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
        var out = new java.util.ArrayList<Habitacion>();
        while (rs.next()) {
            out.add(new Habitacion(rs.getInt("id"), rs.getString("numero"), rs.getString("planta")));
        }
        return out;
    }
}

/** Cambia la habitación en una transacción: cierra la vigente (si existe) y abre la nueva. */
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
// DTO para el histórico
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

public java.util.List<HistHab> listarHistorico(int residenteId) throws Exception {
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
            var out = new java.util.ArrayList<HistHab>();
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

}