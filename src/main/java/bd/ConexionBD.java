package bd;

import java.sql.*;

public class ConexionBD {
    private static final String URL = "jdbc:sqlite:ruta/a/tu_base.sqlite"; // <-- CAMBIA ESTA RUTA

    public static Connection obtener() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys=ON;");
        }
        return c;
    }
}
