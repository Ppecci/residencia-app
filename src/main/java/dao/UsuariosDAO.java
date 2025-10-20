package dao;

import bd.ConexionBD;
import modelo.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UsuariosDAO {

    public Usuario login(String username, String passwordHash) throws Exception {
        String sql = """
            SELECT id, username, rol, trabajador_id, familiar_id, activo
            FROM usuarios
            WHERE username = ? AND password_hash = ? AND activo = 1
        """;

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash); // De momento comparamos tal cual (HASH_PROVISIONAL)
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
                return u;
            }
        }       
    }
}