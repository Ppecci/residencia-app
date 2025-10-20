package bd;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ConexionBD {
    private static final String DB_PATH =
            Paths.get("data", "residenciaSauces.db").toAbsolutePath().toString();
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    public static Connection obtener() throws Exception {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return c;
    }
}

