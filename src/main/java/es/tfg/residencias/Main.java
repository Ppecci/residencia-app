package es.tfg.residencias;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("Gestor de Residencias – JavaFX listo ✅");
        Scene scene = new Scene(label, 500, 300);
        stage.setScene(scene);
        stage.setTitle("Residencias App (Prueba)");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}