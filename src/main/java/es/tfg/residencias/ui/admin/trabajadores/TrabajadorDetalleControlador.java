package es.tfg.residencias.ui.admin.trabajadores;

import dao.AsignacionesDAO;
import modelo.AsignacionVista;
import modelo.Trabajador;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class TrabajadorDetalleControlador {

    @FXML private Label lblTitulo;
    @FXML private CheckBox chkSoloVigentes;
    @FXML private Button btnReasignar;
    @FXML private TableView<AsignacionVista> tabla;
    @FXML private TableColumn<AsignacionVista, String> colResidente, colInicio, colFin, colNotas;

    private final AsignacionesDAO asignDAO = new AsignacionesDAO();
    private final ObservableList<AsignacionVista> datos = FXCollections.observableArrayList();
    private Trabajador trabajador;

    @FXML
    public void initialize() {
        // Enlaces de columnas
        colResidente.setCellValueFactory(new PropertyValueFactory<>("residente"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("fin"));
        colNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
        tabla.setItems(datos);

        // Handlers asignados en código (evita LoadException por onAction)
        if (chkSoloVigentes != null) {
            chkSoloVigentes.setOnAction(this::toggleVigentes);
        }
        if (btnReasignar != null) {
            btnReasignar.setOnAction(this::abrirReasignacion);
        }
    }

    /** Llamado por el padre tras cargar el FXML */
    public void setTrabajador(Trabajador t) {
        this.trabajador = t;
        lblTitulo.setText("Trabajador: " + t.getNombre() + " (" + t.getUsuario() + ")");
        cargar();
    }

    private void cargar() {
        if (trabajador == null) return;
        try {
            boolean soloVigentes = (chkSoloVigentes != null) && chkSoloVigentes.isSelected();
            datos.setAll(asignDAO.listarPorTrabajador(trabajador.getId(), soloVigentes));
        } catch (Exception e) {
            e.printStackTrace();
            alertError("Error cargando asignaciones", e.getMessage());
        }
    }

    private void toggleVigentes(ActionEvent e) { cargar(); }

  @FXML
private void abrirReasignacion(javafx.event.ActionEvent e) {
    try {
        final String RUTA = "/fxml/ReasignarAsignaciones.fxml";
        System.out.println("[DEBUG] Cargando FXML: " + RUTA);

        var url = getClass().getResource(RUTA);
        if (url == null) {
            alertError("FXML no encontrado",
                    "Ruta: " + RUTA + "\nUbícalo en src/main/resources/fxml/ReasignarAsignaciones.fxml");
            return;
        }
        System.out.println("[DEBUG] URL: " + url);

        FXMLLoader loader = new FXMLLoader(url);
        DialogPane root = loader.load(); // IMPORTANTE: DialogPane como root

        Object ctrl = loader.getController();
        System.out.println("[DEBUG] Controller: " + (ctrl == null ? "null" : ctrl.getClass().getName()));

        if (!(ctrl instanceof ReasignarAsignacionesControlador)) {
            alertError("Controller inesperado",
                    "Esperaba ReasignarAsignacionesControlador, obtuve: "
                    + (ctrl == null ? "null" : ctrl.getClass().getName()));
            return;
        }

        ReasignarAsignacionesControlador c = (ReasignarAsignacionesControlador) ctrl;
        c.setTrabajador(trabajador); // pasa el trabajador actual

        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Reasignar residentes");
        dlg.setDialogPane(root);
        dlg.initOwner(lblTitulo.getScene().getWindow());
        dlg.showAndWait();

        cargar(); // refrescar al cerrar

    } catch (Exception ex) {
        ex.printStackTrace(); // verás la causa real en la consola
        alertError("No se pudo abrir el diálogo", String.valueOf(ex));
    }
}

private void alertError(String h, String d) {
    Alert a = new Alert(Alert.AlertType.ERROR, d, ButtonType.OK);
    a.setHeaderText(h); a.showAndWait();
}
}