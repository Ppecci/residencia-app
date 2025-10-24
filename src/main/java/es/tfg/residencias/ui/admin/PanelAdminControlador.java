package es.tfg.residencias.ui.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert;

public class PanelAdminControlador {

    @FXML private StackPane contenedorCentro;
    @FXML private Button btnResidentes;

    @FXML
    public void initialize() {
        // opcional: abrirResidentes();
    }

    @FXML
    private void abrirResidentes() {
        cargarEnCentro("/fxml/PanelResidentes.fxml");
    }

    @FXML
    private void abrirEmpleados() {
        // Carga un FXML real que YA tienes para verificar que cargarEnCentro funciona
        cargarEnCentro("/fxml/TrabajadoresVista.fxml");
    }

    @FXML
    private void abrirFamiliares() {
        // Usa el método que ya tienes, que hace todo el trabajo y maneja errores.
        cargarEnCentro("/fxml/PanelFamiliares.fxml");
    }
        


    @FXML
    private void abrirHabitaciones() {
        cargarEnCentro("/fxml/PanelHabitaciones.fxml");
     
    }

    @FXML
    private void cerrarSesion() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AccesoVista.fxml"));
            contenedorCentro.getScene().setRoot(root);
        } catch (Exception e) {
            mostrarError("No se pudo cargar /fxml/AccesoVista.fxml", e);
        }
    }

    /** Carga un FXML dentro del centro y muestra alerta con la CAUSA real si falla. */
    private void cargarEnCentro(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent vista = loader.load();
            contenedorCentro.getChildren().setAll(vista);
        } catch (Exception e) {
            // extrae causa encadenada para diagnosticar FXML (línea exacta, método faltante, fx:id, etc.)
            Throwable cause = e;
            StringBuilder sb = new StringBuilder(e.toString());
            while (cause.getCause() != null && cause.getCause() != cause) {
                cause = cause.getCause();
                sb.append("\nCAUSA: ").append(cause.toString());
            }
            mostrarError("No se pudo cargar " + rutaFxml + "\n" + sb, e);
        }
    }

    private void mostrarError(String msg, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, msg);

        a.setHeaderText("Error de carga");
        a.showAndWait();
    }
}
