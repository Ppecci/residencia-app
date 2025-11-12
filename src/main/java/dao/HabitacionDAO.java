package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import modelo.HabitacionOcupacionVista;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HabitacionDAO {

    private static final Logger log = LoggerFactory.getLogger(HabitacionDAO.class);

    public enum Modo { TODAS, OCUPADAS, LIBRES }

    public static class HabitacionAsignada {
        public final String numero;
        public final String planta;
        public final String desde;
        public final String notas;

        public HabitacionAsignada(String numero, String planta, String desde, String notas) {
            this.numero = numero;
            this.planta = planta;
            this.desde = desde;
            this.notas = notas;
        }
    }

    public Optional<HabitacionAsignada> obtenerHabitacionVigente(int residenteId) throws Exception {
        long t0 = System.nanoTime();
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
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    log.info("HabitacionDAO.obtenerHabitacionVigente(residenteId={}) → OK en {} ms", residenteId, ms);
                    return Optional.of(new HabitacionAsignada(
                            rs.getString("numero"),
                            rs.getString("planta"),
                            rs.getString("desde"),
                            rs.getString("notas")
                    ));
                }
                long ms = (System.nanoTime() - t0) / 1_000_000;
                log.warn("HabitacionDAO.obtenerHabitacionVigente(residenteId={}) → sin vigente ({} ms)", residenteId, ms);
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Error SQL en obtenerHabitacionVigente(residenteId={}): {}", residenteId, e.getMessage(), e);
            throw e;
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
        long t0 = System.nanoTime();
        String sql = """
            SELECT h.id, h.numero, h.planta
            FROM habitaciones h
            LEFT JOIN residente_habitacion rh
                   ON rh.habitacion_id = h.id AND (rh.end_date IS NULL OR rh.end_date = '')
            WHERE rh.id IS NULL
            ORDER BY h.numero
            """;
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql);
             var rs = ps.executeQuery()) {
            var out = new ArrayList<Habitacion>();
            while (rs.next()) {
                out.add(new Habitacion(rs.getInt("id"), rs.getString("numero"), rs.getString("planta")));
            }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            log.info("HabitacionDAO.listarDisponibles → {} filas en {} ms", out.size(), ms);
            return out;
        } catch (SQLException e) {
            log.error("Error SQL en listarDisponibles: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void cambiarHabitacion(int residenteId, int nuevaHabitacionId, String startDate, String notas) throws Exception {
        log.info("HabitacionDAO.cambiarHabitacion → residenteId={} nuevaHabitacionId={} startDate={}", residenteId, nuevaHabitacionId, startDate);
        try (var c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                if (ocupadaPorOtro(nuevaHabitacionId, residenteId)) {
                    log.warn("cambiarHabitacion → habitación {} ocupada por otro residente", nuevaHabitacionId);
                    throw new SQLException("La habitación seleccionada ya está ocupada.");
                }

                try (var ps = c.prepareStatement("""
                        UPDATE residente_habitacion
                           SET end_date = ?
                         WHERE residente_id = ?
                           AND (end_date IS NULL OR end_date = '')
                    """)) {
                    ps.setString(1, startDate);
                    ps.setInt(2, residenteId);
                    int filas = ps.executeUpdate();
                    log.debug("cambiarHabitacion → cierre de anterior asignación: filas={}", filas);
                }

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
                log.info("cambiarHabitacion → OK residenteId={} nuevaHabitacionId={}", residenteId, nuevaHabitacionId);
            } catch (Exception e) {
                c.rollback();
                log.error("cambiarHabitacion → rollback por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void asignarHabitacionInicial(int residenteId, int habitacionId, String startDate, String notas) throws Exception {
        log.info("HabitacionDAO.asignarHabitacionInicial → residenteId={} habitacionId={} startDate={}", residenteId, habitacionId, startDate);
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                if (tieneAsignacionActiva(residenteId)) {
                    log.warn("asignarHabitacionInicial → residente {} ya tiene habitación activa", residenteId);
                    throw new SQLException("El residente ya tiene una habitación activa. Use 'cambiarHabitacion'.");
                }
                if (estaOcupada(habitacionId)) {
                    log.warn("asignarHabitacionInicial → habitación {} ocupada", habitacionId);
                    throw new SQLException("La habitación seleccionada ya está ocupada.");
                }

                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO residente_habitacion (residente_id, habitacion_id, start_date, notas)
                        VALUES (?,?,?,?)
                    """)) {
                    ps.setInt(1, residenteId);
                    ps.setInt(2, habitacionId);
                    ps.setString(3, startDate);
                    ps.setString(4, notas);
                    ps.executeUpdate();
                }

                c.commit();
                log.info("asignarHabitacionInicial → OK residenteId={} habitacionId={}", residenteId, habitacionId);
            } catch (Exception e) {
                c.rollback();
                log.error("asignarHabitacionInicial → rollback por error: {}", e.getMessage(), e);
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    public void asignarOActualizar(int residenteId, int habitacionId, String startDate, String notas) throws Exception {
        boolean activa = tieneAsignacionActiva(residenteId);
        log.debug("HabitacionDAO.asignarOActualizar → residenteId={} activa={}", residenteId, activa);
        if (activa) {
            cambiarHabitacion(residenteId, habitacionId, startDate, notas);
        } else {
            asignarHabitacionInicial(residenteId, habitacionId, startDate, notas);
        }
    }

    private boolean estaOcupada(int habitacionId) throws SQLException {
        String sql = """
            SELECT 1
            FROM residente_habitacion
            WHERE habitacion_id = ?
              AND (end_date IS NULL OR end_date = '')
            LIMIT 1
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, habitacionId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean ocupada = rs.next();
                log.debug("estaOcupada(habitacionId={}) → {}", habitacionId, ocupada);
                return ocupada;
            }
        } catch (SQLException e) {
            log.error("Error SQL en estaOcupada(habitacionId={}): {}", habitacionId, e.getMessage(), e);
            throw e;
        }
    }

    private boolean tieneAsignacionActiva(int residenteId) throws SQLException {
        String sql = """
            SELECT 1
            FROM residente_habitacion
            WHERE residente_id = ?
              AND (end_date IS NULL OR end_date = '')
            LIMIT 1
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean activa = rs.next();
                log.debug("tieneAsignacionActiva(residenteId={}) → {}", residenteId, activa);
                return activa;
            }
        } catch (SQLException e) {
            log.error("Error SQL en tieneAsignacionActiva(residenteId={}): {}", residenteId, e.getMessage(), e);
            throw e;
        }
    }

    private boolean ocupadaPorOtro(int habitacionId, int residenteId) throws SQLException {
        String sql = """
            SELECT 1
            FROM residente_habitacion
            WHERE habitacion_id = ?
              AND residente_id <> ?
              AND (end_date IS NULL OR end_date = '')
            LIMIT 1
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, habitacionId);
            ps.setInt(2, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                boolean ocupada = rs.next();
                log.debug("ocupadaPorOtro(habitacionId={}, residenteId={}) → {}", habitacionId, residenteId, ocupada);
                return ocupada;
            }
        } catch (SQLException e) {
            log.error("Error SQL en ocupadaPorOtro(habitacionId={}, residenteId={}): {}", habitacionId, residenteId, e.getMessage(), e);
            throw e;
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
        long t0 = System.nanoTime();
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
                long ms = (System.nanoTime() - t0) / 1_000_000;
                log.info("HabitacionDAO.listarHistorico(residenteId={}) → {} filas en {} ms", residenteId, out.size(), ms);
                return out;
            }
        } catch (SQLException e) {
            log.error("Error SQL en listarHistorico(residenteId={}): {}", residenteId, e.getMessage(), e);
            throw e;
        }
    }

    public java.util.List<modelo.Habitacion> listarCatalogo(String filtro) throws Exception {
        long t0 = System.nanoTime();
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
            long ms = (System.nanoTime() - t0) / 1_000_000;
            log.info("HabitacionDAO.listarCatalogo(filtro='{}') → {} filas en {} ms",
                    (filtro == null ? "" : filtro), out.size(), ms);
            return out;
        } catch (SQLException e) {
            log.error("Error SQL en listarCatalogo(filtro='{}'): {}", filtro, e.getMessage(), e);
            throw e;
        }
    }

    public int insertarHabitacion(String numero, String planta) throws Exception {
        long t0 = System.nanoTime();
        String sql = "INSERT INTO habitaciones (numero, planta) VALUES (?,?)";
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, numero);
            ps.setString(2, planta);
            ps.executeUpdate();
            try (var rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    log.info("HabitacionDAO.insertarHabitacion → id={} en {} ms", id, ms);
                    return id;
                }
                log.warn("HabitacionDAO.insertarHabitacion → no se devolvió id generado");
                throw new SQLException("No se pudo obtener el id de la habitación creada");
            }
        } catch (SQLException e) {
            log.error("Error SQL en insertarHabitacion(numero='{}', planta='{}'): {}", numero, planta, e.getMessage(), e);
            throw e;
        }
    }

    public void actualizarHabitacion(int id, String numero, String planta) throws Exception {
        long t0 = System.nanoTime();
        String sql = "UPDATE habitaciones SET numero = ?, planta = ? WHERE id = ?";
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql)) {
            ps.setString(1, numero);
            ps.setString(2, planta);
            ps.setInt(3, id);
            int filas = ps.executeUpdate();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            if (filas == 0) {
                log.warn("HabitacionDAO.actualizarHabitacion(id={}) → no encontrada ({} ms)", id, ms);
                throw new SQLException("Habitación no encontrada");
            } else {
                log.info("HabitacionDAO.actualizarHabitacion(id={}) → filas={} en {} ms", id, filas, ms);
            }
        } catch (SQLException e) {
            log.error("Error SQL en actualizarHabitacion(id={}): {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public void eliminarHabitacion(int id) throws Exception {
        String sql = "DELETE FROM habitaciones WHERE id = ?";
        try (var c = ConexionBD.obtener();
             var ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            int filas = ps.executeUpdate();
            if (filas == 0) {
                log.warn("HabitacionDAO.eliminarHabitacion(id={}) → no encontrada", id);
            } else {
                log.info("HabitacionDAO.eliminarHabitacion(id={}) → filas={}", id, filas);
            }
        } catch (SQLException e) {
            log.error("Error SQL en eliminarHabitacion(id={}): {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public java.util.List<HabitacionOcupacionVista> listarOcupacionActual(String filtro, Modo modo) throws Exception {
        long t0 = System.nanoTime();

        StringBuilder sb = new StringBuilder();
        java.util.List<String> params = new java.util.ArrayList<>();

        if (filtro != null && !filtro.isBlank()) {
            sb.append(" WHERE ( h.numero LIKE ? OR IFNULL(h.planta,'') LIKE ? ")
              .append(" OR CAST(r.id AS TEXT) LIKE ? OR IFNULL(r.nombre,'') LIKE ? OR IFNULL(r.apellidos,'') LIKE ? )");
            String like = "%" + filtro.trim() + "%";
            params.add(like); params.add(like); params.add(like); params.add(like); params.add(like);
        }

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
                long ms = (System.nanoTime() - t0) / 1_000_000;
                log.info("HabitacionDAO.listarOcupacionActual(modo={}, filtro='{}') → {} filas en {} ms",
                        (modo == null ? Modo.TODAS : modo), (filtro == null ? "" : filtro), out.size(), ms);
                return out;
            }
        } catch (SQLException e) {
            log.error("Error SQL en listarOcupacionActual(modo={}, filtro='{}'): {}", modo, filtro, e.getMessage(), e);
            throw e;
        }
    }
}
