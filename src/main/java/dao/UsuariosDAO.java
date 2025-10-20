package dao;

import bd.ConexionBD;
import modelo.Usuario;

import java.sql.*;

public class UsuariosDAO {

    public Usuario login(String username, String passwordHashProvisional) throws SQLException {
        String sql = """
            SELECT id, username, rol, trabajador_id, familiar_id, activo
            FROM usuarios
            WHERE username = ? AND password_hash = ? AND activo = 1
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHashProvisional); // luego cambiaremos a verificación de hash real
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setRol(rs.getString("rol"));
                int trab = rs.getInt("trabajador_id");
                u.setTrabajadorId(rs.wasNull() ? null : trab);
                int fam = rs.getInt("familiar_id");
                u.setFamiliarId(rs.wasNull() ? null : fam);
                u.setActivo(rs.getInt("activo") == 1);
                return u;
            }
        }
    }
}