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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DialogoCitasControlador {

    private static final Logger log = LoggerFactory.getLogger(DialogoCitasControlador.class);

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
    private Integer editandoId = null;

    @FXML
    public void initialize() {
        log.debug("DialogoCitasControlador.initialize()");
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
        log.info("DialogoCitas abierto para residenteId={} ({})", residenteId, nombreCompleto);
        recargar();
    }

    @FXML private void cerrar() {
        log.info("DialogoCitas cerrado para residenteId={}", residenteId);
        ((Stage) lblTitulo.getScene().getWindow()).close();
    }

    @FXML
    private void agregar() {
        log.info("Agregar cita -> residenteId={}", residenteId);
        var cm = leerFormulario(null);
        if (cm == null) {
            log.warn("Agregar cita cancelado por formulario inválido (residenteId={})", residenteId);
            return;
        }
        try {
            int id = dao.insertar(cm);
            cm.id = id;
            limpiarFormulario();
            recargar();
            log.info("Cita creada id={} residenteId={} fechaHora={}", id, cm.residenteId, cm.fechaHora);
            info("Cita creada.");
        } catch (Exception e) {
            log.error("Error al crear cita para residenteId={}: {}", residenteId, e.getMessage(), e);
            error("No se pudo crear la cita", e.getMessage());
        }
    }

    @FXML
    private void editar() {
        var sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            log.warn("Editar cita -> sin selección (residenteId={})", residenteId);
            info("Selecciona una cita.");
            return;
        }

        if (editandoId == null || !sel.id.equals(editandoId)) {
            log.info("Editar cita (modo edición) -> cargando cita id={} residenteId={}", sel.id, sel.residenteId);
            inFecha.setValue(sel.fechaHora.toLocalDate());
            inHora.setText(String.format("%02d:%02d", sel.fechaHora.getHour(), sel.fechaHora.getMinute()));
            inEspecialidad.setText(nvl(sel.especialidad));
            inLugar.setText(nvl(sel.lugar));
            cbEstado.getSelectionModel().select(sel.estado);
            inNotas.setText(nvl(sel.notas));
            editandoId = sel.id;
            info("Modifica los campos y vuelve a pulsar Editar para guardar.");
            return;
        }

        var mod = leerFormulario(editandoId);
        if (mod == null) {
            log.warn("Guardar edición de cita id={} cancelado por formulario inválido", editandoId);
            return;
        }

        try {
            dao.actualizar(mod);
            log.info("Cita actualizada id={} residenteId={} fechaHora={}", mod.id, mod.residenteId, mod.fechaHora);
            editandoId = null;
            limpiarFormulario();
            recargar();
            info("Cita actualizada.");
        } catch (Exception e) {
            log.error("Error al actualizar cita id={}: {}", mod.id, e.getMessage(), e);
            error("No se pudo actualizar", e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        var sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) {
            log.warn("Eliminar cita -> sin selección (residenteId={})", residenteId);
            info("Selecciona una cita.");
            return;
        }
        if (!confirm("¿Eliminar la cita seleccionada?")) {
            log.info("Eliminar cita cancelado por el usuario (id={} residenteId={})", sel.id, sel.residenteId);
            return;
        }

        try {
            dao.eliminar(sel.id);
            log.info("Cita eliminada id={} residenteId={}", sel.id, sel.residenteId);
            recargar();
        } catch (Exception e) {
            log.error("Error al eliminar cita id={}: {}", sel.id, e.getMessage(), e);
            error("No se pudo eliminar", e.getMessage());
        }
    }

    private void recargar() {
        try {
            log.debug("Recargando citas de residenteId={}", residenteId);
            items.setAll(dao.listarPorResidente(residenteId));
        } catch (Exception e) {
            log.error("Error recargando citas de residenteId={}: {}", residenteId, e.getMessage(), e);
            error("No se pudieron cargar las citas", e.getMessage());
        }
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
            if (est == null || est.isBlank()) {
                log.warn("Formulario cita inválido: estado vacío (residenteId={})", residenteId);
                info("Selecciona un estado.");
                return null;
            }

            return new CitaMedica(idExistente, residenteId, ldt, esp, lug, est, notas);
        } catch (Exception ex) {
            log.warn("Formulario cita inválido (fecha/hora) para residenteId={}: {}", residenteId, ex.getMessage());
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
        a.showAndWait();
    }

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

