package es.tfg.residencias.ui.util;

import es.tfg.residencias.Main;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;

import java.util.Objects;

public class Navegacion {

    private static Stage stage;
    private static Scene scene;

    private static final String APP_CSS = Objects.requireNonNull(
            Main.class.getResource("/css/app.css"), "No se encontró /css/app.css"
    ).toExternalForm();

    public static void init(Stage primaryStage, String fxmlInicial) throws Exception {
        stage = primaryStage;

        Parent root = FXMLLoader.load(Navegacion.class.getResource(fxmlInicial));
        scene = new Scene(root);
        if (!scene.getStylesheets().contains(APP_CSS)) {
            scene.getStylesheets().add(APP_CSS);
        }

        stage.setTitle("Gestor de Residencias");
        stage.setScene(scene);
        stage.setMaximized(true); // o maximizar();
        stage.show();
    }

    public static void cambiar(String rutaFxml) throws Exception {
        Parent nuevoRoot = FXMLLoader.load(Navegacion.class.getResource(rutaFxml));
        // Mantiene tamaño, posición y estado del Stage
        scene.setRoot(nuevoRoot);
    }

    public static void aplicarCss(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        if (!pane.getStylesheets().contains(APP_CSS)) {
            pane.getStylesheets().add(APP_CSS);
        }
        if (!pane.getStyleClass().contains("dialog")) {
            pane.getStyleClass().add("dialog");
        }
    }

    public static void maximizar() { stage.setMaximized(true); }
    public static String appCss() { return APP_CSS; }
}
