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

public class Main extends Application {

    // Recursos del lock (para cerrar sin fugas)
    private static RandomAccessFile lockRaf;
    private static FileChannel     lockChannel;
    private static FileLock        lock;

    @Override
    public void start(Stage stage) throws Exception {
        System.out.println("PID=" + ProcessHandle.current().pid());
        System.out.println("LOCK=" + (lock != null));
        // --- Evitar segunda instancia ---
        File lf = new File(System.getProperty("user.home"), ".residencias-app.lock");
        lockRaf     = new RandomAccessFile(lf, "rw");
        lockChannel = lockRaf.getChannel();
        lock        = lockChannel.tryLock();
        if (lock == null) {
            System.err.println("La aplicación ya está en ejecución.");
            System.exit(0);
            return;
        }

        // Cerrar proceso al pulsar la X
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        // Icono y arranque normal
        stage.getIcons().add(new Image(Main.class.getResource("/img/logo2.png").toExternalForm()));
        Navegacion.init(stage, "/fxml/AccesoVista.fxml");
    }

    @Override
    public void stop() throws Exception {
        // Liberar recursos del lock (evita el warning y fugas)
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
