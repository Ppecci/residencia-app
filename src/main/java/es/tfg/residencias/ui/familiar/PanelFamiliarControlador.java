package es.tfg.residencias.ui.familiar;

import dao.FamiliarDAO;
import modelo.FilaResumenFamiliar;
import modelo.Usuario;
import sesion.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import es.tfg.residencias.ui.util.Navegacion;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class PanelFamiliarControlador {

    private static final Logger log = LoggerFactory.getLogger(PanelFamiliarControlador.class);


    @FXML private Label lblFamiliar;
    @FXML private Label lblResidente;   
    @FXML private TextField txtBuscar;

    @FXML private TableView<FilaResumenFamiliar> tabla;
    @FXML private TableColumn<FilaResumenFamiliar, Number> colIdResidente;
    @FXML private TableColumn<FilaResumenFamiliar, String> colNombre;
    @FXML private TableColumn<FilaResumenFamiliar, String> colApellidos;
    @FXML private TableColumn<FilaResumenFamiliar, Number> colIdHabitacion;
    @FXML private TableColumn<FilaResumenFamiliar, String> colNumero;
    @FXML private TableColumn<FilaResumenFamiliar, String> colPlanta;
    @FXML private TableColumn<FilaResumenFamiliar, String> colMedResumen;
    @FXML private TableColumn<FilaResumenFamiliar, String> colDietaNombre;
    @FXML private TableColumn<FilaResumenFamiliar, String> colDietaNotas;
    @FXML private TableColumn<FilaResumenFamiliar, String> colProximaCita;

   
    @FXML private Label lblEstado;

    private final FamiliarDAO familiarDAO = new FamiliarDAO();

    private int familiarId;        
    private String nombreFamiliar; 

    private final ObservableList<FilaResumenFamiliar> datos = FXCollections.observableArrayList();
    private final DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    @FXML private javafx.scene.image.ImageView logoImagen;


   @FXML
        public void initialize() {

            if (Sesion.getUsuario() == null) {
                log.warn("Acceso a PanelFamiliar sin sesión → redirigir a Acceso");
                try {
                    Navegacion.cambiar("/fxml/AccesoVista.fxml");
                } catch (Exception e) {
                    log.warn("No se pudo redirigir a AccesoVista.fxml: {}", e.getMessage(), e);
                }
                return;
            }

            if (!Sesion.esFamiliar()) {
                var u = Sesion.getUsuario();
                log.warn("ACCESO_DENEGADO accion=VER_PANEL_FAMILIAR rol={} userId={}",
                        (u != null ? u.getRol() : "—"), (u != null ? u.getId() : -1));
                try {
                    Navegacion.cambiar("/fxml/AccesoVista.fxml");
                } catch (Exception e) {
                    log.warn("No se pudo redirigir a AccesoVista.fxml tras denegar acceso: {}", e.getMessage(), e);
                }
                return; 
            }

            var u = Sesion.getUsuario();
            log.info("Acceso a PanelFamiliar por userId={} rol={}", u.getId(), u.getRol());

            this.familiarId = obtenerFamiliarIdDesdeSesion();
            this.nombreFamiliar = obtenerNombreFamiliarDesdeSesion();
            lblFamiliar.setText(nombreFamiliar != null ? nombreFamiliar : "(Familiar)");

            tabla.setEditable(false);
            tabla.setPlaceholder(new Label("Sin datos para mostrar"));

            colIdResidente.setCellValueFactory(c -> c.getValue().idResidenteProperty());
            colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
            colApellidos.setCellValueFactory(c -> c.getValue().apellidosProperty());
            colIdHabitacion.setCellValueFactory(c -> c.getValue().idHabitacionProperty());
            colNumero.setCellValueFactory(c -> c.getValue().numeroProperty());
            colPlanta.setCellValueFactory(c -> c.getValue().plantaProperty());
            colMedResumen.setCellValueFactory(c -> c.getValue().medicacionResumenProperty());
            colDietaNombre.setCellValueFactory(c -> c.getValue().dietaNombreProperty());
            colDietaNotas.setCellValueFactory(c -> c.getValue().dietaNotasProperty());
            colProximaCita.setCellValueFactory(c -> c.getValue().proximaCitaProperty());

            tabla.setItems(datos);

            cargarTabla(null);

            try {
                var url = getClass().getResource("/img/logo-png.png");
                if (url != null) {
                    if (logoImagen != null) {
                        logoImagen.setImage(new javafx.scene.image.Image(url.toExternalForm(), true));
                        logoImagen.setFitHeight(100);
                        logoImagen.setPreserveRatio(true);
                        logoImagen.setSmooth(false);
                    }
                    log.info("Logo de PanelFamiliar cargado correctamente");
                } else {
                    log.warn("No se encontró /img/logo-png.png (classpath)");
                }
            } catch (Throwable t) {
                log.error("Error cargando logo en PanelFamiliar", t);
            }
        }

    @FXML
    private void buscar() {
        String q = txtBuscar.getText();
        cargarTabla((q != null && !q.isBlank()) ? q.trim() : null);
    }

    @FXML
    private void refrescar() {
        txtBuscar.clear();
        cargarTabla(null);
    }
@FXML private javafx.scene.control.Button btnCerrarSesion;

@FXML
private void cerrarSesion() {
    var alerta = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION,
            "¿Seguro que quieres cerrar sesión?",
            javafx.scene.control.ButtonType.YES,
            javafx.scene.control.ButtonType.NO
    );
    alerta.setHeaderText("Cerrar sesión");
    alerta.setTitle("Confirmación");
    Navegacion.aplicarCss(alerta);

    var res = alerta.showAndWait();
    if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.YES) return;

    // LOGOUT en log
    if (Sesion.getUsuario() != null) {
        log.info("LOGOUT userId={} rol={}", Sesion.getUsuario().getId(), Sesion.getUsuario().getRol());
    } else {
        log.info("LOGOUT sin usuario en sesión (flujo atípico)");
    }

    try {
        Navegacion.cambiar("/fxml/AccesoVista.fxml");
    } catch (Exception e) {
        log.warn("No se pudo cargar /fxml/AccesoVista.fxml: {}", e.getMessage(), e);
        mostrarError("No se pudo cargar /fxml/AccesoVista.fxml", e);
    }
}

    private void mostrarError(String msg, Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, msg);

        a.setHeaderText("Error de carga");
        a.showAndWait();
    }

    private void cargarTabla(String filtro) {
        try {
            List<FilaResumenFamiliar> lista = (filtro == null)
                    ? familiarDAO.obtenerResumenPanel(familiarId)
                    :familiarDAO.buscarEnPanel(familiarId, filtro);


            datos.setAll(lista);
            lblResidente.setText(residenteTextoCabecera(lista));
            setEstado("Datos actualizados a las " + LocalDateTime.now().format(horaFmt));
        } catch (Exception ex) {
            ex.printStackTrace();
            setEstado("Error cargando datos.");
        }
    }

    private String residenteTextoCabecera(List<FilaResumenFamiliar> filas) {
        if (filas == null || filas.isEmpty()) return "(Sin residentes asignados)";
        Integer firstId = filas.get(0).getIdResidente();
        boolean todosMismo = filas.stream().allMatch(f -> Objects.equals(f.getIdResidente(), firstId));
        if (todosMismo) {
            var f = filas.get(0);
            return f.getNombre() + " " + f.getApellidos();
        }
        return "(Varios residentes)";
    }

    private void setEstado(String texto) {
        lblEstado.setText(texto != null ? texto : "");
    }


   private int obtenerFamiliarIdDesdeSesion() {
    Usuario usuario = Sesion.getUsuario();
    if (usuario != null && usuario.getFamiliarId() != 0) {
        return usuario.getFamiliarId();
    } else {
        throw new IllegalStateException("No hay usuario familiar activo en la sesión");
    }
}

private String obtenerNombreFamiliarDesdeSesion() {
    Usuario usuario = Sesion.getUsuario();
    return (usuario != null) ? usuario.getUsername() : "Familiar desconocido";
}
}
