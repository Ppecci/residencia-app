package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.*;

public class MedicacionDAO {

    /** 
     * DTO de medicación (tal como ya tenías). 
     * Se usa tanto en DAOs como en ComboBox del diálogo de prescripciones.
     */
    public static class Medicacion {
        public final int id;
        public final String nombre;
        public final String forma;
        public final String fuerza;

        public Medicacion(int id, String nombre, String forma, String fuerza) {
            this.id = id;
            this.nombre = nombre;
            this.forma = forma;
            this.fuerza = fuerza;
        }

        /** Devuelve una etiqueta amigable para mostrar en ComboBox o tablas. */
        @Override
        public String toString() {
            String f1 = (forma  == null || forma.isBlank())  ? "" : " · " + forma;
            String f2 = (fuerza == null || fuerza.isBlank()) ? "" : " · " + fuerza;
            return nombre + f1 + f2;
        }

        // Getters opcionales (por si se usan en PropertyValueFactory)
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getForma() { return forma; }
        public String getFuerza() { return fuerza; }
    }

    /**
     * Devuelve todas las medicaciones registradas, ordenadas alfabéticamente.
     * Se usa para llenar el ComboBox del diálogo de prescripciones.
     */
    public List<Medicacion> listarTodas() throws Exception {
        String sql = "SELECT id, nombre, forma, fuerza FROM medicaciones ORDER BY nombre";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Medicacion> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Medicacion(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("forma"),
                    rs.getString("fuerza")
                ));
            }
            return out;
        }
    }

    /**
     * Busca una medicación por su ID.
     * Puede usarse para mostrar detalles o recuperar el nombre al cargar una prescripción.
     */
    public Optional<Medicacion> obtenerPorId(int id) throws Exception {
        String sql = "SELECT id, nombre, forma, fuerza FROM medicaciones WHERE id = ?";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Medicacion(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("forma"),
                        rs.getString("fuerza")
                    ));
                }
                return Optional.empty();
            }
        }
    }

    /**
     * Inserta una nueva medicación en la base de datos.
     * (Opcional: puedes usarla desde el panel admin si luego habilitas CRUD de medicaciones)
     */
    public int crear(String nombre, String forma, String fuerza) throws Exception {
        String sql = "INSERT INTO medicaciones (nombre, forma, fuerza) VALUES (?,?,?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, forma);
            ps.setString(3, fuerza);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("No se pudo obtener el id de la medicación creada");
            }
        }
    }
}

