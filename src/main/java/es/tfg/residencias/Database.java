package es.tfg.residencias;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;

public class Database {

    private static final String DB_RELATIVE_PATH = "data/residenciaSauces.db";

    public static Connection getConnection() throws Exception {
        String absolutePath = Paths.get(DB_RELATIVE_PATH).toAbsolutePath().toString();

        String url = "jdbc:sqlite:" + absolutePath;

        return DriverManager.getConnection(url);
    }
}