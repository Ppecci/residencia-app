package dao;

import bd.ConexionBD;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitaMedicaDAO {

    private static final Logger log = LoggerFactory.getLogger(CitaMedicaDAO.class);

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static class CitaMedica {
        public Integer id;                        
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
        long t0 = System.nanoTime();
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
                long ms = (System.nanoTime() - t0) / 1_000_000;
                log.info("CitaMedicaDAO.listarPorResidente(residenteId={}) -> {} citas en {} ms",
                        residenteId, out.size(), ms);
                return out;
            }
        } catch (SQLException e) {
            log.error("Error SQL en listarPorResidente(residenteId={}): {}", residenteId, e.getMessage(), e);
            throw e;
        }
    }

    public int insertar(CitaMedica cta) throws Exception {
        long t0 = System.nanoTime();
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
                if (keys.next()) {
                    int id = keys.getInt(1);
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    log.info("CitaMedicaDAO.insertar -> id={} residenteId={} fechaHora={} en {} ms",
                            id, cta.residenteId, cta.fechaHora, ms);
                    return id;
                } else {
                    log.warn("CitaMedicaDAO.insertar -> no se devolvió id generado (residenteId={}, fechaHora={})",
                            cta.residenteId, cta.fechaHora);
                    return -1;
                }
            }
        } catch (SQLException e) {
            log.error("Error SQL en insertar(residenteId={}, fechaHora={}): {}",
                    cta.residenteId, cta.fechaHora, e.getMessage(), e);
            throw e;
        }
    }

    public void actualizar(CitaMedica cta) throws Exception {
        long t0 = System.nanoTime();
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
            int filas = ps.executeUpdate();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (filas == 0) {
                log.warn("CitaMedicaDAO.actualizar(id={}) -> cita no encontrada ({} ms)", cta.id, ms);
            } else {
                log.info("CitaMedicaDAO.actualizar(id={}) -> filas={} en {} ms", cta.id, filas, ms);
            }
        } catch (SQLException e) {
            log.error("Error SQL en actualizar(id={}): {}", cta.id, e.getMessage(), e);
            throw e;
        }
    }

    public void eliminar(int id) throws Exception {
        String sql = "DELETE FROM citas_medicas WHERE id=?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                log.warn("CitaMedicaDAO.eliminar(id={}) -> cita no encontrada", id);
            } else {
                log.info("CitaMedicaDAO.eliminar(id={}) -> filas={}", id, filas);
            }
        } catch (SQLException e) {
            log.error("Error SQL en eliminar(id={}): {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public List<String> estados() {
        return List.of("PROGRAMADA", "REALIZADA", "CANCELADA");
    }
}
