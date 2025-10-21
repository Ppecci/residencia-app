package dao;

import bd.ConexionBD;
import modelo.Residente;
import java.sql.*;
import java.util.*;

public class ResidentesDAO {

    public List<Residente> listar(String filtro) throws Exception {
        List<Residente> out = new ArrayList<>();
        String sql = "SELECT id, nombre, apellidos, fecha_nacimiento, notas, activo " +
                     "FROM residentes " +
                     (filtro == null || filtro.isBlank() ? "" : "WHERE nombre LIKE ? OR apellidos LIKE ? ") +
                     "ORDER BY apellidos, nombre";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (sql.contains("LIKE")) {
                String like = "%" + filtro + "%";
                ps.setString(1, like); ps.setString(2, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Residente(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("apellidos"),
                        rs.getString("fecha_nacimiento"),
                        rs.getString("notas"),
                        rs.getInt("activo") == 1
                    ));
                }
            }
        }
        return out;
    }

    public void insertar(Residente r) throws Exception {
        String sql = "INSERT INTO residentes (nombre, apellidos, fecha_nacimiento, notas, activo) " +
                     "VALUES (?,?,?,?,?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            ps.setString(2, r.getApellidos());
            ps.setString(3, r.getFechaNacimiento());
            ps.setString(4, r.getNotas());
            ps.setInt(5, Boolean.TRUE.equals(r.getActivo()) ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void actualizar(Residente r) throws Exception {
        String sql = "UPDATE residentes SET nombre=?, apellidos=?, fecha_nacimiento=?, notas=?, activo=? WHERE id=?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getNombre());
            ps.setString(2, r.getApellidos());
            ps.setString(3, r.getFechaNacimiento());
            ps.setString(4, r.getNotas());
            ps.setInt(5, Boolean.TRUE.equals(r.getActivo()) ? 1 : 0);
            ps.setInt(6, r.getId());
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM residentes WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
