package es.tfg.residencias.ui.admin.trabajadores;

import dao.AsignacionesDAO;
import modelo.AsignacionVista;
import modelo.Trabajador;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class TrabajadorDetalleControlador {

    @FXML private Label lblTitulo;
    @FXML private CheckBox chkSoloVigentes;
    @FXML private TableView<AsignacionVista> tabla;
    @FXML private TableColumn<AsignacionVista, String> colResidente, colInicio, colFin, colNotas;

    private final AsignacionesDAO asignDAO = new AsignacionesDAO();
    private final ObservableList<AsignacionVista> datos = FXCollections.observableArrayList();
    private Trabajador trabajador;

    @FXML
    public void initialize() {
        colResidente.setCellValueFactory(new PropertyValueFactory<>("residente"));
        colInicio.setCellValueFactory(new PropertyValueFactory<>("inicio"));
        colFin.setCellValueFactory(new PropertyValueFactory<>("fin"));
        colNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
        tabla.setItems(datos);
    }

    /** Llamado desde el controlador padre tras cargar el FXML */
    public void setTrabajador(Trabajador t) {
        this.trabajador = t;
        lblTitulo.setText("Trabajador: " + t.getNombre() + "  (" + t.getUsuario() + ")");
        cargar();
    }

    @FXML
    private void toggleVigentes() {
        cargar();
    }

    private void cargar() {
        if (trabajador == null) return;
        try {
            datos.setAll(asignDAO.listarPorTrabajador(trabajador.getId(), chkSoloVigentes.isSelected()));
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            a.setHeaderText("Error cargando asignaciones"); a.showAndWait();
        }
    }
}
