package es.tfg.residencias.ui.familiar;

import dao.FamiliarDAO;
import modelo.FilaResumenFamiliar;
import modelo.Usuario;
import sesion.Sesion;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class PanelFamiliarControlador {

    // Top bar
    @FXML private Label lblFamiliar;
    @FXML private Label lblResidente;   
    @FXML private TextField txtBuscar;

    // Tabla
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

    // Dependencias
    private final FamiliarDAO familiarDAO = new FamiliarDAO();

    private int familiarId;        
    private String nombreFamiliar; 

    private final ObservableList<FilaResumenFamiliar> datos = FXCollections.observableArrayList();
    private final DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm:ss");
    @FXML private javafx.scene.image.ImageView logoImagen;


    @FXML
    public void initialize() {
        
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
            var url = getClass().getResource("/img/logo-png.png"); // o el nombre real de tu logo
            if (url != null) {
                logoImagen.setImage(new javafx.scene.image.Image(url.toExternalForm(), true));
                logoImagen.setFitHeight(100);
                logoImagen.setPreserveRatio(true);
                logoImagen.setSmooth(false); // para que no se vea borroso
            } else {
                System.err.println("[DIAG] No se encontró /img/logo-png.png");
            }
        } catch (Throwable t) {
            System.err.println("[DIAG] Error cargando logo en PanelFamiliar:");
            t.printStackTrace();
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
    alerta.getDialogPane().getStylesheets().add(es.tfg.residencias.ui.util.Navegacion.appCss());


    var res = alerta.showAndWait();
    if (res.isEmpty() || res.get() != javafx.scene.control.ButtonType.YES) return;

    try {
        javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/fxml/AccesoVista.fxml"));
        btnCerrarSesion.getScene().setRoot(root);
    } catch (Exception e) {
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
