package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.*;

public class DietaDAO {


    public static class Dieta {
        public final int id;
        public final String nombre;
        public final String descripcion;

        public Dieta(int id, String nombre, String descripcion) {
            this.id = id;
            this.nombre = nombre;
            this.descripcion = descripcion;
        }

        @Override public String toString() {
            return nombre + (descripcion != null && !descripcion.isBlank() ? " — " + descripcion : "");
        }
    }

    // --- Dieta
    public static class DietaVigente {
        public final int idAsignacion; // id fila en residente_dieta (opcional para ti)
        public final String nombre;
        public final String desde;     // start_date
        public final String notas;

        public DietaVigente(int idAsignacion, String nombre, String desde, String notas) {
            this.idAsignacion = idAsignacion;
            this.nombre = nombre;
            this.desde = desde;
            this.notas = notas;
        }
    }

    /** Lista el catálogo */
    public List<Dieta> listarCatalogo() throws Exception {
        String sql = """
            SELECT id, nombre, descripcion
            FROM dietas
            ORDER BY nombre
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Dieta> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Dieta(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("descripcion")
                ));
            }
            return out;
        }
    }

    /** Devuelve la dieta */
    public Optional<DietaVigente> obtenerDietaVigente(int residenteId) throws Exception {
        String sql = """
            SELECT rd.id AS id_asign, d.nombre, rd.start_date AS desde, rd.notas
            FROM residente_dieta rd
            JOIN dietas d ON d.id = rd.dieta_id
            WHERE rd.residente_id = ?
              AND (rd.end_date IS NULL OR rd.end_date = '')
            ORDER BY rd.start_date DESC
            LIMIT 1
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new DietaVigente(
                        rs.getInt("id_asign"),
                        rs.getString("nombre"),
                        rs.getString("desde"),
                        rs.getString("notas")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    public void cambiarDieta(int residenteId, int nuevaDietaId, String startDate, String notas) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                // 1) Cierra dieta actual, si hay
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE residente_dieta
                        SET end_date = ?
                        WHERE residente_id = ? AND (end_date IS NULL OR end_date = '')
                    """)) {
                    ps.setString(1, startDate);
                    ps.setInt(2, residenteId);
                    ps.executeUpdate();
                }

                // Abre nueva dieta
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO residente_dieta (residente_id, dieta_id, start_date, notas)
                        VALUES (?,?,?,?)
                    """)) {
                    ps.setInt(1, residenteId);
                    ps.setInt(2, nuevaDietaId);
                    ps.setString(3, startDate);
                    ps.setString(4, notas != null ? notas : "");
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
    // --- DTO para el histórico de dietas
public static class HistDieta {
    public final String nombre;   // nombre de la dieta
    public final String desde;    // start_date
    public final String hasta;    // end_date (puede ser null)
    public final String notas;

    public HistDieta(String nombre, String desde, String hasta, String notas) {
        this.nombre = nombre; this.desde = desde; this.hasta = hasta; this.notas = notas;
    }
    // getters para TableView
    public String getNombre() { return nombre; }
    public String getDesde()  { return desde;  }
    public String getHasta()  { return hasta;  }
    public String getNotas()  { return notas;  }
}

/** Histórico completo de dietas del residente (vigentes y finalizadas). */
public java.util.List<HistDieta> listarHistorico(int residenteId) throws Exception {
    String sql = """
        SELECT d.nombre, rd.start_date AS desde, rd.end_date AS hasta, rd.notas
        FROM residente_dieta rd
        JOIN dietas d ON d.id = rd.dieta_id
        WHERE rd.residente_id = ?
        ORDER BY rd.start_date DESC
    """;
    try (var c = ConexionBD.obtener();
         var ps = c.prepareStatement(sql)) {
        ps.setInt(1, residenteId);
        try (var rs = ps.executeQuery()) {
            var out = new java.util.ArrayList<HistDieta>();
            while (rs.next()) {
                out.add(new HistDieta(
                    rs.getString("nombre"),
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
