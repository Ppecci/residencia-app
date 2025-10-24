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
        abrirResidentes();
    }

    @FXML
    private void abrirResidentes() {
        cargarEnCentro("/fxml/PanelResidentes.fxml");
    }

    @FXML
    private void abrirEmpleados() {
        cargarEnCentro("/fxml/TrabajadoresVista.fxml");
    }

    @FXML
    private void abrirFamiliares() {
        cargarEnCentro("/fxml/PanelFamiliares.fxml");
    }
        


    @FXML
    private void abrirHabitaciones() {
        cargarEnCentro("/fxml/PanelHabitaciones.fxml");
     
    }

    @FXML
        private void cerrarSesion() {

            javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.CONFIRMATION,
                    "¿Seguro que quieres cerrar sesión?",
                    javafx.scene.control.ButtonType.YES,
                    javafx.scene.control.ButtonType.NO
            );
            alerta.setHeaderText("Cerrar sesión");
            alerta.setTitle("Confirmación");

            var res = alerta.showAndWait();
            if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.YES) {
                return; // el usuario cancela
            }
            
            try {
                javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                        getClass().getResource("/fxml/AccesoVista.fxml"));
                contenedorCentro.getScene().setRoot(root);
            } catch (Exception e) {
                mostrarError("No se pudo cargar /fxml/AccesoVista.fxml", e);
            }
        }


    private void cargarEnCentro(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent vista = loader.load();
            contenedorCentro.getChildren().setAll(vista);
        } catch (Exception e) {
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
