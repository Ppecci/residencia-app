package es.tfg.residencias.ui.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import java.io.IOException;

public class PanelAdminControlador {

    @FXML private StackPane contenedorCentro;
    @FXML private Button btnResidentes;

    @FXML
    public void initialize() {
        // Si quieres abrir residentes por defecto:
        // abrirResidentes();
    }

    @FXML
    private void abrirResidentes() {
        cargarEnCentro("/fxml/ResidenteVista.fxml");
    }

    @FXML
    private void abrirEmpleados() {
        // placeholder hasta crear la vista
        contenedorCentro.getChildren().setAll(new javafx.scene.control.Label("Pendiente de implementar (Empleados)"));
    }

    @FXML
private void abrirFamiliares() {
    try {
        Parent vista = FXMLLoader.load(getClass().getResource("/fxml/PanelFamiliar.fxml"));
        contenedorCentro.getChildren().setAll(vista);
    } catch (IOException e) {
        e.printStackTrace();
        new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR,
            "No se pudo cargar /fxml/PanelFamiliar.fxml\n" + e.getMessage()
        ).showAndWait();
    }
    }

    @FXML
    private void abrirHabitaciones() {
        contenedorCentro.getChildren().setAll(new javafx.scene.control.Label("Pendiente de implementar (Habitaciones)"));
    }

    @FXML
    private void cerrarSesion() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AccesoVista.fxml"));
            contenedorCentro.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cargarEnCentro(String fxml) {
        try {
            Node vista = FXMLLoader.load(getClass().getResource(fxml));
            contenedorCentro.getChildren().setAll(vista);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
