package es.tfg.residencias;

import javafx.application.Application;
import javafx.stage.Stage;
import es.tfg.residencias.ui.util.Navegacion;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Navegacion.init(stage, "/fxml/AccesoVista.fxml");
        stage.getIcons().add(
    new javafx.scene.image.Image(Main.class.getResourceAsStream("/img/logo2.png"))
);
    }

    public static void main(String[] args) {
        launch(args);
    }
}