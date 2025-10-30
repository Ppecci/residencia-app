package dao;

import bd.ConexionBD;

import java.sql.*;
import java.util.Optional;

public class ResidenteDietaDAO {

    public static class DietaVigente {
        public final Integer dietaId;
        public final String nombre;
        public final String desde;
        public final String notas;

        public DietaVigente(Integer dietaId, String nombre, String desde, String notas) {
            this.dietaId = dietaId; this.nombre = nombre; this.desde = desde; this.notas = notas;
        }

        public Integer getDietaId() { return dietaId; }
        public String getNombre()   { return nombre; }
        public String getDesde()    { return desde; }
        public String getNotas()    { return notas; }
    }

    public Optional<DietaVigente> obtenerVigente(int residenteId) throws Exception {
        String sql = """
            SELECT rd.dieta_id AS dieta_id,
                   d.nombre    AS nombre,
                   rd.start_date AS desde,
                   IFNULL(rd.notas,'') AS notas
            FROM residente_dieta rd
            LEFT JOIN dietas d ON d.id = rd.dieta_id
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
                            rs.getInt("dieta_id"),
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
                // cerrar vigente (si la hay)
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE residente_dieta
                           SET end_date = ?
                         WHERE residente_id = ?
                           AND (end_date IS NULL OR end_date = '')
                    """)) {
                    ps.setString(1, startDate);
                    ps.setInt(2, residenteId);
                    ps.executeUpdate();
                }
                // abrir nueva
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO residente_dieta (residente_id, dieta_id, start_date, notas)
                        VALUES (?,?,?,?)
                    """)) {
                    ps.setInt(1, residenteId);
                    ps.setInt(2, nuevaDietaId);
                    ps.setString(3, startDate);
                    ps.setString(4, notas);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }
}
