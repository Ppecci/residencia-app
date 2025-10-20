package es.tfg.residencias.ui.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Navegacion {
    private static Stage principal;

    public static void iniciar(Stage stage, String fxmlInicial) throws Exception {
        principal = stage;
        cambiarVista(fxmlInicial);
        principal.setTitle("Gestor de Residencias");
        principal.show();
    }

    public static void cambiarVista(String rutaFxml) throws Exception {
        Parent root = FXMLLoader.load(Navegacion.class.getResource(rutaFxml));
        principal.setScene(new Scene(root));
    }
}