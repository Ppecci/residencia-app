package bd;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConexionBD {

    private static final Logger log = LoggerFactory.getLogger(ConexionBD.class);

    private static final String DB_PATH =
            Paths.get("data", "residenciaSauces.db").toAbsolutePath().toString();
    private static final String URL = "jdbc:sqlite:" + DB_PATH;

    // Flag para mostrar solo la primera conexión en nivel INFO
    private static boolean primeraConexion = true;

    /**
     * Obtiene una conexión a la base de datos SQLite.
     * Registra la apertura y cualquier fallo.
     */
    public static Connection obtener() throws SQLException {
        try {
            log.debug("Intentando abrir conexión SQLite en ruta: {}", DB_PATH);
            Connection c = DriverManager.getConnection(URL);

            // Primera vez en INFO, las demás en DEBUG
            if (primeraConexion) {
                log.info("Conexión abierta a SQLite (ruta={})", DB_PATH);
                primeraConexion = false;
            } else {
                log.debug("Conexión abierta a SQLite (ruta={})", DB_PATH);
            }

            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
                log.debug("PRAGMA foreign_keys=ON aplicado correctamente");
            }

            return c;

        } catch (SQLException e) {
            log.error("Fallo conectando a BD: {}", e.getMessage(), e);
            throw e; // repropagar para que el DAO lo gestione
        }
    }

    /**
     * Cierra de forma segura una conexión y deja trazas.
     */
    public static void cerrar(Connection c) {
        if (c != null) {
            try {
                c.close();
                log.info("Conexión a BD cerrada correctamente");
            } catch (SQLException e) {
                log.error("Error cerrando conexión BD: {}", e.getMessage(), e);
            }
        }
    }
}
