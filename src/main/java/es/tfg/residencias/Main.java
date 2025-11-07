package es.tfg.residencias;

import es.tfg.residencias.ui.util.Navegacion;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main extends Application {
    private static final Logger log = LoggerFactory.getLogger(Main.class);


    // Recursos del lock para liberar sin fugas
    private static RandomAccessFile lockRaf;
    private static FileChannel     lockChannel;
    private static FileLock        lock;

    @Override
    public void start(Stage stage) throws Exception {
        new File("logs").mkdirs();

        File lf = new File(System.getProperty("user.home"), ".residencias-app.lock");
        lockRaf     = new RandomAccessFile(lf, "rw");
        lockChannel = lockRaf.getChannel();
        lock        = lockChannel.tryLock();

            System.out.println("PID=" + ProcessHandle.current().pid());
            System.out.println("LOCK=" + (lock != null));

            if (lock == null) {
                System.err.println("La aplicación ya está en ejecución. Saliendo.");
                System.exit(0);
                return;
            }

            // Solo loguear si se ha adquirido el lock
            log.info("Aplicación iniciada. PID={}", ProcessHandle.current().pid());


        if (lock == null) {
            System.err.println("La aplicación ya está en ejecución. Saliendo.");
            System.exit(0);
            return;
        }

        Thread.setDefaultUncaughtExceptionHandler((t, e) ->
            org.slf4j.LoggerFactory.getLogger("Uncaught")
                .error("Excepción no capturada en hilo " + t.getName(), e)
        );

        stage.setOnCloseRequest(e -> {
            Platform.exit();
        });

        stage.getIcons().add(new Image(Main.class.getResource("/img/logo2.png").toExternalForm()));
        Navegacion.init(stage, "/fxml/AccesoVista.fxml");
    }

    @Override
    public void stop() throws Exception {
        log.info("Aplicación cerrada correctamente");
        try { if (lock != null) lock.release(); } catch (Exception ignored) {}
        try { if (lockChannel != null) lockChannel.close(); } catch (Exception ignored) {}
        try { if (lockRaf != null) lockRaf.close(); } catch (Exception ignored) {}
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
