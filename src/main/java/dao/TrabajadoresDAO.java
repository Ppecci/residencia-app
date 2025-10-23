package dao;

import bd.ConexionBD;
import modelo.Trabajador;
import java.sql.*;
import java.util.*;

public class TrabajadoresDAO {

    public List<Trabajador> listar(String filtro) throws Exception {
        List<Trabajador> out = new ArrayList<>();
        String sql = "SELECT id, nombre, usuario, email, activo " +
                     "FROM trabajadores " +
                     (filtro == null || filtro.isBlank()
                        ? ""
                        : "WHERE nombre LIKE ? OR usuario LIKE ? OR email LIKE ? ") +
                     "ORDER BY nombre, usuario";

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            if (sql.contains("LIKE")) {
                String like = "%" + filtro + "%";
                ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new Trabajador(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email"),
                        rs.getInt("activo") == 1
                    ));
                }
            }
        }
        return out;
    }

    public void insertar(Trabajador t) throws Exception {
        String sql = "INSERT INTO trabajadores (nombre, usuario, password_hash, email, activo) VALUES (?,?,?,?,?)";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.setString(2, t.getUsuario());
            ps.setString(3, t.getPasswordHashTemporal()); // debe venir ya hasheada
            ps.setString(4, t.getEmail());
            ps.setInt(5, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void actualizar(Trabajador t) throws Exception {
        String sql = "UPDATE trabajadores SET nombre=?, usuario=?, email=?, activo=? WHERE id=?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNombre());
            ps.setString(2, t.getUsuario());
            ps.setString(3, t.getEmail());
            ps.setInt(4, Boolean.TRUE.equals(t.getActivo()) ? 1 : 0);
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        }
    }

    public void actualizarPassword(int idTrabajador, String nuevoHash) throws Exception {
        String sql = "UPDATE trabajadores SET password_hash=? WHERE id=?";
        try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, idTrabajador);
            ps.executeUpdate();
        }
    }

    public void eliminar(int id) throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM trabajadores WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
