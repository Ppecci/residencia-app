package es.tfg.residencias.ui.admin.trabajadores;

import dao.AsignacionesDAO;
import modelo.AsignacionVista;
import modelo.Trabajador;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
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

    private void abrirReasignacion(ActionEvent e) {
        // TODO: abrir diálogo real; por ahora no rompemos nada
        Alert a = new Alert(Alert.AlertType.INFORMATION, "Reasignación pendiente de implementar", ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void alertError(String h, String d) {
        Alert a = new Alert(Alert.AlertType.ERROR, d, ButtonType.OK);
        a.setHeaderText(h); a.showAndWait();
    }
}
