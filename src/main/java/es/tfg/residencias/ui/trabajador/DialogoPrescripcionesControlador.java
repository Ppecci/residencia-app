package es.tfg.residencias.ui.trabajador;

import dao.MedicacionDAO;
import dao.PrescripcionDAO;
import dao.PrescripcionDAO.PrescView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.List;


public class DialogoPrescripcionesControlador {

    // ---- UI ----
    @FXML private Label lblTitulo;
    @FXML private TabPane tabs;

    @FXML private TableView<PrescView> tablaActivas;
    @FXML private TableView<PrescView> tablaHist;

    @FXML private TableColumn<PrescView, String> colA_Med, colA_Dosis, colA_Frec, colA_Via, colA_Desde, colA_Notas;
    @FXML private TableColumn<PrescView, String> colH_Med, colH_Dosis, colH_Frec, colH_Via, colH_Desde, colH_Hasta, colH_Notas;

    @FXML private ComboBox<MedicacionDAO.Medicacion> cbMedicacion;
    @FXML private TextField inDosis, inFrecuencia, inVia, inDesde, inNotas;

    // ---- Estado / DAOs ----
    private final PrescripcionDAO presDAO = new PrescripcionDAO();
    private final MedicacionDAO   medDAO  = new MedicacionDAO();

    private final ObservableList<PrescView> act = FXCollections.observableArrayList();
    private final ObservableList<PrescView> his = FXCollections.observableArrayList();

    private Integer residenteId;
    @SuppressWarnings("unused")
    private String  residenteNombre; 

    private boolean  modoNuevo = false;

    @FXML
    public void initialize() {
        colA_Med.setCellValueFactory(   new PropertyValueFactory<>("medicamento"));
        colA_Dosis.setCellValueFactory( new PropertyValueFactory<>("dosis"));
        colA_Frec.setCellValueFactory(  new PropertyValueFactory<>("frecuencia"));
        colA_Via.setCellValueFactory(   new PropertyValueFactory<>("via"));
        colA_Desde.setCellValueFactory( new PropertyValueFactory<>("inicio"));
        colA_Notas.setCellValueFactory( new PropertyValueFactory<>("notas"));

        colH_Med.setCellValueFactory(   new PropertyValueFactory<>("medicamento"));
        colH_Dosis.setCellValueFactory( new PropertyValueFactory<>("dosis"));
        colH_Frec.setCellValueFactory(  new PropertyValueFactory<>("frecuencia"));
        colH_Via.setCellValueFactory(   new PropertyValueFactory<>("via"));
        colH_Desde.setCellValueFactory( new PropertyValueFactory<>("inicio"));
        colH_Hasta.setCellValueFactory( new PropertyValueFactory<>("fin"));
        colH_Notas.setCellValueFactory( new PropertyValueFactory<>("notas"));

        tablaActivas.setItems(act);
        tablaHist.setItems(his);

        tablaActivas.getSelectionModel().selectedItemProperty().addListener((o, old, cur) -> {
            if (cur != null) {
                cargarEnFormulario(cur);
                tabs.getSelectionModel().select(0);
            }
        });
        tablaHist.getSelectionModel().selectedItemProperty().addListener((o, old, cur) -> {
            if (cur != null) {
                cargarEnFormulario(cur);
                tabs.getSelectionModel().select(1);
            }
        });
    }

    
    public void setResidente(int residenteId, String nombreCompleto) {
        this.residenteId = residenteId;
        this.residenteNombre = nombreCompleto;
        lblTitulo.setText("Prescripciones — Residente: " + nombreCompleto + " (id=" + residenteId + ")");
        try {
            cbMedicacion.getItems().setAll(medDAO.listarTodas());
            refrescar();
        } catch (Exception e) {
            error("Error inicializando", e.getMessage());
        }
    }


    @FXML private void refrescar() {
        if (residenteId == null) return;
        try {
            List<PrescView> lAct = presDAO.listarActivas(residenteId);
            List<PrescView> lHis = presDAO.listarHistorico(residenteId);
            act.setAll(lAct.toArray(new PrescView[0]));
            his.setAll(lHis.toArray(new PrescView[0]));
            limpiarForm();
            modoNuevo = false;
        } catch (Exception e) {
            error("Error cargando prescripciones", e.getMessage());
        }
    }

    @FXML private void nueva() {
        modoNuevo = true;
        limpiarForm();
        inDesde.setText(LocalDate.now().toString()); // sugerimos hoy
        cbMedicacion.setDisable(false);
        inDesde.setDisable(false);
        tabs.getSelectionModel().select(0);
        cbMedicacion.requestFocus();
    }

    @FXML private void editar() {
        PrescView cur = getSeleccionActual();
        if (cur == null) { info("Selecciona una prescripción (activa o histórica)."); return; }
        if (!cur.isActiva()) { info("No se puede editar una prescripción finalizada."); return; }
        modoNuevo = false;
        cargarEnFormulario(cur);
        cbMedicacion.setDisable(true); // no cambiamos medicamento en edición
        inDesde.setDisable(true);      // ni la fecha de inicio
    }

    @FXML private void finalizar() {
        PrescView cur = tablaActivas.getSelectionModel().getSelectedItem();
        if (cur == null) { info("Selecciona una prescripción ACTIVA para finalizar."); return; }
        TextInputDialog dlg = new TextInputDialog(LocalDate.now().toString());
        dlg.setTitle("Finalizar prescripción");
        dlg.setHeaderText("Fecha fin (YYYY-MM-DD)");
        dlg.setContentText("Fecha fin:");
        dlg.getDialogPane().getStylesheets().add(es.tfg.residencias.ui.util.Navegacion.appCss());

        dlg.showAndWait().ifPresent(fecha -> {
            try {
                presDAO.finalizar(cur.getId(), fecha);
                refrescar();
                tabs.getSelectionModel().select(1);
            } catch (Exception e) {
                error("No se pudo finalizar", e.getMessage());
            }
        });
    }

    @FXML private void guardar() {
        try {
            if (modoNuevo) {
                var med = cbMedicacion.getValue();
                String dosis = val(inDosis), frec = val(inFrecuencia), via = val(inVia),
                        desde = val(inDesde), notas = val(inNotas);

                if (med == null) { info("Selecciona una medicación."); return; }
                if (dosis.isBlank() || frec.isBlank() || desde.isBlank()) {
                    info("Dosis, frecuencia e inicio son obligatorios."); return;
                }
                if (presDAO.existeActivaMismoMedicamento(residenteId, med.id)) {
                    info("Ya existe una prescripción ACTIVA de ese medicamento para este residente.");
                    return;
                }

                int nuevoId = presDAO.insertar(residenteId, med.id, dosis, frec, via, desde, notas);
                refrescar();
                tablaActivas.getItems().stream()
                        .filter(p -> p.getId() == nuevoId)
                        .findFirst()
                        .ifPresent(p -> tablaActivas.getSelectionModel().select(p));

            } else {
                PrescView cur = getSeleccionActual();
                if (cur == null) { info("Selecciona una prescripción."); return; }
                if (!cur.isActiva()) {
                    info("No se puede modificar una prescripción finalizada."); return;
                }
                String dosis = val(inDosis), frec = val(inFrecuencia), via = val(inVia), notas = val(inNotas);
                if (dosis.isBlank() || frec.isBlank()) {
                    info("Dosis y frecuencia son obligatorias."); return;
                }
                presDAO.actualizar(cur.getId(), dosis, frec, via, notas);
                refrescar();
                tabs.getSelectionModel().select(0);
            }
        } catch (Exception e) {
            error("No se pudo guardar", e.getMessage());
        }
    }

    @FXML private void cerrar() {
        Stage st = (Stage) lblTitulo.getScene().getWindow();
        st.close();
    }

    private PrescView getSeleccionActual() {
        var selTab = tabs.getSelectionModel().getSelectedItem();
        if (selTab == null) return null;
        if ("Activas".equals(selTab.getText())) {
            return tablaActivas.getSelectionModel().getSelectedItem();
        } else {
            return tablaHist.getSelectionModel().getSelectedItem();
        }
    }

    private void limpiarForm() {
        cbMedicacion.getSelectionModel().clearSelection();
        inDosis.clear();
        inFrecuencia.clear();
        inVia.clear();
        inDesde.clear();
        inNotas.clear();
        cbMedicacion.setDisable(false);
        inDesde.setDisable(false);
    }

    private void cargarEnFormulario(PrescView p) {
        int medId = buscarMedicacionIdEnEtiqueta(p);
        if (medId != -1) {
            cbMedicacion.getItems().stream()
                .filter(mi -> mi.id == medId)
                .findFirst()
                .ifPresent(mi -> cbMedicacion.getSelectionModel().select(mi));
        } else {
            cbMedicacion.getSelectionModel().clearSelection();
        }

        inDosis.setText(nvl(p.getDosis()));
        inFrecuencia.setText(nvl(p.getFrecuencia()));
        inVia.setText(nvl(p.getVia()));
        inDesde.setText(nvl(p.getInicio()));
        inNotas.setText(nvl(p.getNotas()));

        cbMedicacion.setDisable(!modoNuevo);
        inDesde.setDisable(!modoNuevo);
    }

    private int buscarMedicacionIdEnEtiqueta(PrescView p) {
        String base = (p.getMedicamento() == null) ? "" : p.getMedicamento();
        StringBuilder sb = new StringBuilder(base);
        if (p.getForma()  != null && !p.getForma().isBlank())  sb.append(" · ").append(p.getForma());
        if (p.getFuerza() != null && !p.getFuerza().isBlank()) sb.append(" · ").append(p.getFuerza());
        final String etiqueta = sb.toString(); 

        return cbMedicacion.getItems().stream()
                .filter(mi -> etiqueta.equals(mi.toString()))
                .map(mi -> mi.id)
                .findFirst()
                .orElse(-1);
    }

    private String val(TextField tf) { return tf.getText() == null ? "" : tf.getText().trim(); }
    private String nvl(String s)     { return (s == null) ? "" : s; }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void error(String titulo, String detalle) {
        Alert a = new Alert(Alert.AlertType.ERROR, detalle, ButtonType.OK);
        a.setHeaderText(titulo);
        a.showAndWait();
    }
}
