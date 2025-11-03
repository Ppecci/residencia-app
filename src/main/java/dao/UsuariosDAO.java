package dao;

import bd.ConexionBD;
import modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuariosDAO {

    
    public Usuario buscarPorUsername(String username) throws Exception {
        String sql = """
            SELECT id, username, rol, trabajador_id, familiar_id, activo, password_hash
            FROM usuarios
            WHERE username = ? AND activo = 1
        """;

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Usuario u = new Usuario();
                u.setId(rs.getInt("id"));
                u.setUsername(rs.getString("username"));
                u.setRol(rs.getString("rol"));

                int t = rs.getInt("trabajador_id");
                u.setTrabajadorId(rs.wasNull() ? null : t);

                int f = rs.getInt("familiar_id");
                u.setFamiliarId(rs.wasNull() ? null : f);

                u.setActivo(rs.getInt("activo") == 1);
                u.setPasswordHash(rs.getString("password_hash")); // <-- necesario para BCrypt
                return u;
            }
        }
    }
}
