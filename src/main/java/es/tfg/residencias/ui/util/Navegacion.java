package es.tfg.residencias.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navegacion {
    private static Stage stage;

    public static void init(Stage primaryStage, String fxmlInicial) throws Exception {
        stage = primaryStage;
        cambiar(fxmlInicial);
        stage.setTitle("Gestor de Residencias");
        stage.show();
    }

    public static void cambiar(String rutaFxml) throws Exception {
        Parent root = FXMLLoader.load(Navegacion.class.getResource(rutaFxml));
        stage.setScene(new Scene(root));
    }
}