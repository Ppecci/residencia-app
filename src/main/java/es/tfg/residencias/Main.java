package es.tfg.residencias;

import javafx.application.Application;
import javafx.stage.Stage;
import es.tfg.residencias.ui.util.Navegacion;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Inicializa Navegacion con el Stage y carga la primera vista
        Navegacion.init(stage, "/fxml/AccesoVista.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}