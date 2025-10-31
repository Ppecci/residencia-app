package es.tfg.residencias.ui.trabajador;

import dao.CitaMedicaDAO;
import dao.CitaMedicaDAO.CitaMedica;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class DialogoCitasControlador {

    @FXML private Label lblTitulo;

    @FXML private TableView<CitaMedica> tabla;
    @FXML private TableColumn<CitaMedica, String> colFecha, colEspecialidad, colLugar, colEstado, colNotas;

    @FXML private DatePicker inFecha;
    @FXML private TextField inHora, inEspecialidad, inLugar, inNotas;
    @FXML private ComboBox<String> cbEstado;

    private final CitaMedicaDAO dao = new CitaMedicaDAO();
    private final javafx.collections.ObservableList<CitaMedica> items =
            javafx.collections.FXCollections.observableArrayList();
    private int residenteId;

    @FXML
    public void initialize() {
        tabla.setItems(items);
        colFecha.setCellValueFactory(cd -> Bindings.createStringBinding(
                () -> CitaMedicaDAO.FMT.format(cd.getValue().fechaHora)));
        colEspecialidad.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().especialidad)));
        colLugar.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().lugar)));
        colEstado.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().estado));
        colNotas.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().notas)));

        cbEstado.getItems().setAll(dao.estados());
        cbEstado.getSelectionModel().selectFirst();

        inFecha.setValue(LocalDate.now());
        inHora.setText("10:00");
    }

    public void setResidente(int residenteId, String nombreCompleto) {
        this.residenteId = residenteId;
        lblTitulo.setText("Citas médicas — Residente: " + nombreCompleto + " (id=" + residenteId + ")");
        recargar();
    }

    @FXML private void cerrar() { ((Stage) lblTitulo.getScene().getWindow()).close(); }

    @FXML
    private void agregar() {
        var cm = leerFormulario(null);
        if (cm == null) return;
        try {
            int id = dao.insertar(cm);
            cm.id = id;
            limpiarFormulario();
            recargar();
            info("Cita creada.");
        } catch (Exception e) {
            error("No se pudo crear la cita", e.getMessage());
        }
    }

    @FXML
    private void editar() {
        var sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Selecciona una cita."); return; }

        inFecha.setValue(sel.fechaHora.toLocalDate());
        inHora.setText(String.format("%02d:%02d", sel.fechaHora.getHour(), sel.fechaHora.getMinute()));
        inEspecialidad.setText(nvl(sel.especialidad));
        inLugar.setText(nvl(sel.lugar));
        cbEstado.getSelectionModel().select(sel.estado);
        inNotas.setText(nvl(sel.notas));

        var mod = leerFormulario(sel.id);
        if (mod == null) return;

        try {
            dao.actualizar(mod);
            limpiarFormulario();
            recargar();
            info("Cita actualizada.");
        } catch (Exception e) {
            error("No se pudo actualizar", e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        var sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) { info("Selecciona una cita."); return; }
        if (!confirm("¿Eliminar la cita seleccionada?")) return;

        try {
            dao.eliminar(sel.id);
            recargar();
        } catch (Exception e) {
            error("No se pudo eliminar", e.getMessage());
        }
    }

    private void recargar() {
        try { items.setAll(dao.listarPorResidente(residenteId)); }
        catch (Exception e) { error("No se pudieron cargar las citas", e.getMessage()); }
    }

    private CitaMedica leerFormulario(Integer idExistente) {
        try {
            LocalDate f = inFecha.getValue();
            String[] hm = inHora.getText().trim().split(":");
            int h = Integer.parseInt(hm[0]), m = Integer.parseInt(hm[1]);
            LocalDateTime ldt = LocalDateTime.of(f, LocalTime.of(h, m));

            String esp = txt(inEspecialidad);
            String lug = txt(inLugar);
            String est = cbEstado.getValue();
            String notas = txt(inNotas);
            if (est == null || est.isBlank()) { info("Selecciona un estado."); return null; }

            return new CitaMedica(idExistente, residenteId, ldt, esp, lug, est, notas);
        } catch (Exception ex) {
            error("Datos inválidos", "Revisa fecha u hora (HH:mm).");
            return null;
        }
    }

    private void limpiarFormulario() {
        inFecha.setValue(LocalDate.now());
        inHora.setText("10:00");
        inEspecialidad.clear(); inLugar.clear(); inNotas.clear();
        cbEstado.getSelectionModel().selectFirst();
    }

    private String nvl(String s) { return s == null ? "" : s; }
    private String txt(TextField tf) { return tf.getText() == null ? "" : tf.getText().trim(); }

    private void info(String msg) { 
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
            a.getDialogPane().getStylesheets().add(es.tfg.residencias.ui.util.Navegacion.appCss());
            a.showAndWait(); }
    private void error(String titulo, String detalle) {
        Alert a = new Alert(Alert.AlertType.ERROR, detalle, ButtonType.OK);
        a.setHeaderText(titulo);
        a.getDialogPane().getStylesheets().add(es.tfg.residencias.ui.util.Navegacion.appCss());
        a.showAndWait();
    }
    private boolean confirm(String msg) {
        var a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.getDialogPane().getStylesheets().add(es.tfg.residencias.ui.util.Navegacion.appCss());
        var r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }
}

