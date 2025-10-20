package es.tfg.residencias;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Carga la vista del acceso (login)
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/AccesoVista.fxml"));

        Scene scene = new Scene(root);
        stage.setTitle("Gestor de Residencias");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
