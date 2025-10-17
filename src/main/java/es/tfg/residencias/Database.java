package es.tfg.residencias;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    // Ruta relativa al proyecto: ./data/residenciaSauces.db
    private static final String DB_RELATIVE_PATH = "data/residenciaSauces.db";

    public static Connection getConnection() throws Exception {
        // Resuelve ruta absoluta por si ejecutas desde otro directorio
        String absolutePath = Paths.get(DB_RELATIVE_PATH).toAbsolutePath().toString();

        // URL JDBC para SQLite
        String url = "jdbc:sqlite:" + absolutePath;

        // Desde SQLite JDBC modernos NO hace falta Class.forName()
        return DriverManager.getConnection(url);
    }
}