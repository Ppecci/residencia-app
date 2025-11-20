package es.tfg.residencias.ui.admin.trabajadores;

import dao.AsignacionesDAO;
import dao.TrabajadoresDAO;
import bd.ConexionBD;
import modelo.AsignacionVista;
import modelo.Trabajador;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.control.SelectionMode;  

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;

public class ReasignarAsignacionesControlador {

    @FXML private DialogPane dialogPane;  

    @FXML private Label lblTitulo;
    @FXML private TextField inSustituido;
    @FXML private ComboBox<Trabajador> cbSustituto;
    @FXML private DatePicker dpInicio, dpFin;
    @FXML private CheckBox chkTodos;
    @FXML private TableView<AsignacionVista> tablaVigentes;
    @FXML private TableColumn<AsignacionVista, String> colResidente, colInicio;

    private final AsignacionesDAO asignDAO = new AsignacionesDAO();
    private final TrabajadoresDAO trabDAO = new TrabajadoresDAO();
    private final ObservableList<AsignacionVista> datos = FXCollections.observableArrayList();

    private Trabajador trabajador; // el sustituido

    @FXML
    public void initialize() {
        colResidente.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getResidente()));
        colInicio.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().getInicio()));
        tablaVigentes.setItems(datos);

        tablaVigentes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        dpInicio.setValue(LocalDate.now());
        chkTodos.setOnAction(e -> tablaVigentes.setDisable(chkTodos.isSelected()));
    }

    public void setTrabajador(Trabajador t) throws Exception {
        this.trabajador = t;
        lblTitulo.setText("Reasignar residentes de: " + t.getNombre() + " (" + t.getUsuario() + ")");
        inSustituido.setText(t.getNombre() + " (" + t.getUsuario() + ")");

       
        cbSustituto.setItems(FXCollections.observableArrayList(trabDAO.listarActivosExcepto(t.getId())));
        datos.setAll(asignDAO.listarPorTrabajador(t.getId(), true));
    }

    @FXML
    private void cancelar() {
        ((Stage) dialogPane.getScene().getWindow()).close();
    }

    @FXML
    private void aceptar() {
        Trabajador sustituto = cbSustituto.getValue();
        LocalDate ini = dpInicio.getValue();
        LocalDate fin = dpFin.getValue();

        if (sustituto == null) { alerta("Selecciona un sustituto"); return; }
        if (ini == null) { alerta("Selecciona fecha de inicio"); return; }
        if (fin != null && fin.isBefore(ini)) { alerta("La fecha fin no puede ser anterior al inicio"); return; }

        List<AsignacionVista> seleccionadas = chkTodos.isSelected()
                ? datos
                : tablaVigentes.getSelectionModel().getSelectedItems();

        if (seleccionadas == null || seleccionadas.isEmpty()) {
            alerta("Selecciona al menos un residente (o marca 'Todos los vigentes')");
            return;
        }

        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE asignacion_trabajador SET end_date=? WHERE id=?")) {
                    for (AsignacionVista v : seleccionadas) {
                        ps.setString(1, ini.toString());
                        ps.setInt(2, v.getIdAsignacion());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO asignacion_trabajador(residente_id, trabajador_id, start_date, end_date, notas) " +
                        "VALUES (?,?,?,?,?)")) {
                    for (AsignacionVista v : seleccionadas) {
                        ps.setInt(1, v.getIdResidente());
                        ps.setInt(2, sustituto.getId());
                        ps.setString(3, ini.toString());
                        if (fin != null) {
                            ps.setString(4, fin.toString());
                        } else {
                            ps.setNull(4, Types.VARCHAR);
                        }
                        ps.setString(5, "Sustitución de " + trabajador.getNombre());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                c.commit();
                ((Stage) dialogPane.getScene().getWindow()).close();
            } catch (Exception ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            alerta("Error guardando: " + ex.getMessage());
        }
    }

    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
