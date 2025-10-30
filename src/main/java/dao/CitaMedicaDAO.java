package dao;

import bd.ConexionBD;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class CitaMedicaDAO {

    
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  
    public static class CitaMedica {
        public Integer id;                         // <- se asigna tras insertar
        public final int residenteId;
        public final LocalDateTime fechaHora;
        public final String especialidad;
        public final String lugar;
        public final String estado;               // PROGRAMADA | REALIZADA | CANCELADA
        public final String notas;

        public CitaMedica(Integer id, int residenteId, LocalDateTime fechaHora,
                          String especialidad, String lugar, String estado, String notas) {
            this.id = id;
            this.residenteId = residenteId;
            this.fechaHora = fechaHora;
            this.especialidad = especialidad;
            this.lugar = lugar;
            this.estado = estado;
            this.notas = notas;
        }
    }



    public List<CitaMedica> listarPorResidente(int residenteId) throws Exception {
        String sql = """
            SELECT id, residente_id, fecha_hora, especialidad, lugar, estado, notas
            FROM citas_medicas
            WHERE residente_id = ?
            ORDER BY datetime(fecha_hora)
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CitaMedica> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new CitaMedica(
                        rs.getInt("id"),
                        rs.getInt("residente_id"),
                        LocalDateTime.parse(rs.getString("fecha_hora"), FMT),
                        rs.getString("especialidad"),
                        rs.getString("lugar"),
                        rs.getString("estado"),
                        rs.getString("notas")
                    ));
                }
                return out;
            }
        }
    }

    /** Inserta una cita y devuelve el id */
    public int insertar(CitaMedica cta) throws Exception {
        String sql = """
            INSERT INTO citas_medicas (residente_id, fecha_hora, especialidad, lugar, estado, notas)
            VALUES (?,?,?,?,?,?)
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, cta.residenteId);
            ps.setString(2, FMT.format(cta.fechaHora));
            ps.setString(3, cta.especialidad);
            ps.setString(4, cta.lugar);
            ps.setString(5, cta.estado);
            ps.setString(6, cta.notas);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Actualiza  */
    public void actualizar(CitaMedica cta) throws Exception {
        String sql = """
            UPDATE citas_medicas
               SET residente_id=?, fecha_hora=?, especialidad=?, lugar=?, estado=?, notas=?
             WHERE id=?
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cta.residenteId);
            ps.setString(2, FMT.format(cta.fechaHora));
            ps.setString(3, cta.especialidad);
            ps.setString(4, cta.lugar);
            ps.setString(5, cta.estado);
            ps.setString(6, cta.notas);
            ps.setInt(7, cta.id);
            ps.executeUpdate();
        }
    }

    /** Elimina una cita por id */
    public void eliminar(int id) throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM citas_medicas WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    
    public List<String> estados() {
        return List.of("PROGRAMADA", "REALIZADA", "CANCELADA");
    }
}
