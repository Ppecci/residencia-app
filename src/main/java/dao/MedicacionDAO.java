package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.*;

public class MedicacionDAO {

    public static class Medicacion {
        public final int id;
        public final String nombre;
        public final String forma;
        public final String fuerza;

        public Medicacion(int id, String nombre, String forma, String fuerza) {
            this.id = id; this.nombre = nombre; this.forma = forma; this.fuerza = fuerza;
        }
        @Override public String toString() {
            // se mostrará en el ComboBox
            String f1 = (forma  == null || forma.isBlank())  ? "" : " • " + forma;
            String f2 = (fuerza == null || fuerza.isBlank()) ? "" : " • " + fuerza;
            return nombre + f1 + f2;
        }

        // getters opcionales si los necesitas
        public int getId() { return id; }
        public String getNombre() { return nombre; }
        public String getForma() { return forma; }
        public String getFuerza() { return fuerza; }
    }

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
}
