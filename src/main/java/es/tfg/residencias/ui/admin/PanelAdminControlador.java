package es.tfg.residencias.ui.admin;

import es.tfg.residencias.ui.util.Navegacion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Alert;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sesion.Sesion;

public class PanelAdminControlador {

    private static final Logger log = LoggerFactory.getLogger(PanelAdminControlador.class);

    @FXML private StackPane contenedorCentro;
    @FXML private Button btnResidentes;
    @FXML private ImageView logoImagen;

   @FXML
        public void initialize() {

            if (Sesion.getUsuario() == null) {
                log.warn("Acceso a PanelAdmin sin sesión → redirigir a Acceso");
                try {
                    Navegacion.cambiar("/fxml/AccesoVista.fxml");
                } catch (Exception e) {
                    log.warn("No se pudo redirigir a AccesoVista.fxml: {}", e.getMessage(), e);
                }
                return;
            }

            if (!Sesion.esAdmin()) {
                var u = Sesion.getUsuario();
                log.warn("ACCESO_DENEGADO accion=VER_PANEL_ADMIN rol={} userId={}",
                        (u != null ? u.getRol() : "—"), (u != null ? u.getId() : -1));

                try {
                    Navegacion.cambiar("/fxml/AccesoVista.fxml");
                } catch (Exception e) {
                    log.warn("No se pudo redirigir a AccesoVista.fxml tras denegar acceso: {}", e.getMessage(), e);
                }
                return; 
            }

            var u = Sesion.getUsuario();
            log.info("Acceso a PanelAdmin por userId={} rol={}", u.getId(), u.getRol());

            try {
                var url = getClass().getResource("/img/logo-transparent-png.png");
                if (url == null) {
                    log.warn("No se encontró /img/logo-transparent-png.png (classpath)");
                } else {
                    logoImagen.setImage(new javafx.scene.image.Image(url.toExternalForm()));
                    logoImagen.setFitHeight(210);
                    logoImagen.setPreserveRatio(true);
                    log.info("Logo de PanelAdmin cargado correctamente");
                }
            } catch (Throwable t) {
                log.error("Error cargando logo en PanelAdmin", t);
            }
        }

    @FXML
    private void abrirResidentes() { cargarEnCentro("/fxml/PanelResidentes.fxml"); }

    @FXML
    private void abrirEmpleados() { cargarEnCentro("/fxml/TrabajadoresVista.fxml"); }

    @FXML
    private void abrirFamiliares() { cargarEnCentro("/fxml/PanelFamiliares.fxml"); }

    @FXML
    private void abrirHabitaciones() { cargarEnCentro("/fxml/PanelHabitaciones.fxml"); }

    @FXML
    private void cerrarSesion() {
        var alerta = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que quieres cerrar sesión?",
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
        );
        alerta.setHeaderText("Cerrar sesión");
        alerta.setTitle("Confirmación");
        alerta.getDialogPane().getStylesheets().add(Navegacion.appCss());

        var res = alerta.showAndWait();
        if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.YES) return;

    
        if (Sesion.getUsuario() != null) {
            log.info("LOGOUT userId={} rol={}", Sesion.getUsuario().getId(), Sesion.getUsuario().getRol());
        } else {
            log.info("LOGOUT sin usuario en sesión");
        }

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/AccesoVista.fxml"));
            contenedorCentro.getScene().setRoot(root);
        } catch (Exception e) {
            log.warn("No se pudo cargar /fxml/AccesoVista.fxml: {}", e.getMessage(), e);
            mostrarError("No se pudo cargar /fxml/AccesoVista.fxml", e);
        }
    }

    private void cargarEnCentro(String rutaFxml) {
        try {
            log.info("Cargando vista central: {}", rutaFxml);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent vista = loader.load();
            contenedorCentro.getChildren().setAll(vista);
        } catch (Exception e) {
            log.warn("No se pudo cargar {}: {}", rutaFxml, e.getMessage(), e);
            mostrarError("No se pudo cargar " + rutaFxml, e);
        }
    }

    private void mostrarError(String msg, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg);
        a.setHeaderText("Error de carga");
        a.showAndWait();
    }
}
