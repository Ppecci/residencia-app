package es.tfg.residencias.ui.admin.residentes;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import modelo.Residente;

import java.time.LocalDate;

import dao.HabitacionDAO;
import dao.PrescripcionDAO;
import dao.MedicacionDAO;
import dao.MedicacionDAO.Medicacion;

public class PanelResidenteActualControlador {
    @FXML private Label lblTitulo;

    // HABITACIÓN
    @FXML private Label lblHabNumero, lblHabPlanta, lblHabDesde, lblHabNotas;

    private Residente residente;
    private final HabitacionDAO habitacionDAO = new HabitacionDAO();

    public void setResidente(Residente r) {
        this.residente = r;
        if (lblTitulo != null && r != null) {
            lblTitulo.setText("Detalle: " + r.getNombre() + " " + r.getApellidos());
        }
        // Nada más recibir el residente, cargamos la pestaña Habitación
        cargarHabitacion();
        cargarHistorico();
        cargarPrescripciones();
        
    }

    private void cargarHabitacion() {
        if (residente == null) return;
        // valores por defecto
        setHabLabels("—", "—", "—", "—");
        try {
            var opt = habitacionDAO.obtenerHabitacionVigente(residente.getId());
            if (opt.isPresent()) {
                var h = opt.get();
                setHabLabels(
                        safe(h.numero),
                        safe(h.planta),
                        safe(h.desde),
                        safe(h.notas)
                );
            }
        } catch (Exception e) {
            // si hay error, mostramos algo legible
            setHabLabels("Error", "Error", "Error", e.getMessage());
            e.printStackTrace();
        }
    }

    private void setHabLabels(String numero, String planta, String desde, String notas) {
        if (lblHabNumero != null) lblHabNumero.setText(numero);
        if (lblHabPlanta != null) lblHabPlanta.setText(planta);
        if (lblHabDesde  != null) lblHabDesde.setText(desde);
        if (lblHabNotas  != null) lblHabNotas.setText(notas);
    }

    private String safe(String s) { return (s == null || s.isBlank()) ? "—" : s; }


    @FXML
        private void cambiarHabitacion() {
            if (residente == null) { return; }

            try {
                // 1) Traer habitaciones disponibles
                var disponibles = habitacionDAO.listarDisponibles();
                if (disponibles.isEmpty()) {
                    // no hay libres
                    if (lblHabNumero != null) lblHabNumero.setText("—");
                    new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                        "No hay habitaciones disponibles ahora mismo.").showAndWait();
                    return;
                }

        // 2) Mostrar selector
        var dialog = new ChoiceDialog<>(disponibles.get(0), disponibles);
        dialog.setTitle("Cambiar habitación");
        dialog.setHeaderText("Selecciona la nueva habitación");
        dialog.setContentText("Habitación:");

        var elegido = dialog.showAndWait();
        if (elegido.isEmpty()) return; // cancelado

        var nueva = elegido.get();
        // (opcional) pedir notas
        var notasInput = new javafx.scene.control.TextInputDialog("");
        notasInput.setTitle("Cambiar habitación");
        notasInput.setHeaderText("Notas (opcional)");
        notasInput.setContentText("Motivo/observaciones:");
        var notas = notasInput.showAndWait().orElse("");

        // 3) Ejecutar cambio (cierra vigente y crea nueva)
        String hoy = LocalDate.now().toString(); // YYYY-MM-DD
        habitacionDAO.cambiarHabitacion(residente.getId(), nueva.id, hoy, notas);

        // 4) Refrescar vista
        cargarHabitacion(); // vuelve a leer y pinta número/planta/desde/notas
        cargarHistorico();

        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
            "Habitación cambiada a " + nueva.numero + ".").showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
            "No se pudo cambiar la habitación:\n" + e.getMessage()).showAndWait();
    }
}


@FXML private TableView<HabitacionDAO.HistHab> tablaHistHab;
@FXML private TableColumn<HabitacionDAO.HistHab, String> colHistNumero, colHistPlanta, colHistDesde, colHistHasta, colHistNotas;

private final ObservableList<HabitacionDAO.HistHab> datosHist = FXCollections.observableArrayList();

private boolean histInit = false;
private void initHistoricoTableIfNeeded() {
    if (histInit || tablaHistHab == null) return;
    colHistNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));
    colHistPlanta.setCellValueFactory(new PropertyValueFactory<>("planta"));
    colHistDesde.setCellValueFactory(new PropertyValueFactory<>("desde"));
    colHistHasta.setCellValueFactory(new PropertyValueFactory<>("hasta"));
    colHistNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
    tablaHistHab.setItems(datosHist);
    histInit = true;
}
private void cargarHistorico() {
    if (residente == null || tablaHistHab == null) return;
    initHistoricoTableIfNeeded();
    datosHist.clear();
    try {
        var lista = habitacionDAO.listarHistorico(residente.getId());
        // pintar "—" cuando hasta sea NULL/"" (vigente)
        for (var h : lista) {
            var hasta = (h.hasta == null || h.hasta.isBlank()) ? "—" : h.hasta;
            datosHist.add(new HabitacionDAO.HistHab(h.numero, h.planta, h.desde, hasta, h.notas));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@FXML
    private TableView<PrescripcionDAO.PrescView> tablaPresc;
@FXML private TableColumn<PrescripcionDAO.PrescView, String> colPrescMed, colPrescForma, colPrescFuerza,
        colPrescDosis, colPrescFreq, colPrescVia,  colPrescInicio, colPrescFin, colPrescNotas;

private final PrescripcionDAO prescDAO = new PrescripcionDAO();
private final MedicacionDAO medicacionDAO = new MedicacionDAO();
private final ObservableList<PrescripcionDAO.PrescView> datosPresc = FXCollections.observableArrayList();
private boolean prescInit = false;

private void initPrescTableIfNeeded() {
    if (prescInit || tablaPresc == null) return;
    colPrescMed.setCellValueFactory(new PropertyValueFactory<>("medicamento"));
    colPrescForma.setCellValueFactory(new PropertyValueFactory<>("forma"));
    colPrescFuerza.setCellValueFactory(new PropertyValueFactory<>("fuerza"));
    colPrescDosis.setCellValueFactory(new PropertyValueFactory<>("dosis"));
    colPrescFreq.setCellValueFactory(new PropertyValueFactory<>("frecuencia"));
    colPrescVia.setCellValueFactory(new PropertyValueFactory<>("via"));
    colPrescInicio.setCellValueFactory(new PropertyValueFactory<>("inicio"));
    colPrescFin.setCellValueFactory(new PropertyValueFactory<>("fin"));
    colPrescNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
    tablaPresc.setItems(datosPresc);
    prescInit = true;
}
private void cargarPrescripciones() {
    if (residente == null || tablaPresc == null) return;
    initPrescTableIfNeeded();
    datosPresc.clear();
    try {
        var lista = prescDAO.listarActivas(residente.getId());
        datosPresc.addAll(lista);
    } catch (Exception e) {
        e.printStackTrace();
        // opcional: mostrar alerta amigable
    }
    }
    @FXML
    private void anadirPrescripcion() {
    if (residente == null) return;

    try {
        // 1) Medicamentos para el combo
        var meds = medicacionDAO.listarTodas();
        if (meds.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                "No hay medicamentos en el catálogo. Crea alguno en 'medicaciones'.").showAndWait();
            return;
        }

        // 2) Diálogo de alta
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nueva prescripción");
        dialog.setHeaderText("Completa los datos de la nueva prescripción");

        ButtonType guardarBtn = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(guardarBtn, ButtonType.CANCEL);

        ComboBox<Medicacion> cbMed = new ComboBox<>();
        cbMed.getItems().addAll(meds);
        cbMed.getSelectionModel().selectFirst();

        TextField inDosis = new TextField();
        TextField inFreq  = new TextField();
        TextField inVia   = new TextField();
        DatePicker dpInicio = new DatePicker(LocalDate.now());
        TextArea inNotas  = new TextArea(); inNotas.setPrefRowCount(3);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Medicamento:"), cbMed);
        gp.addRow(1, new Label("Dosis:"), inDosis);
        gp.addRow(2, new Label("Frecuencia:"), inFreq);
        gp.addRow(3, new Label("Vía:"), inVia);
        gp.addRow(4, new Label("Inicio:"), dpInicio);
        gp.addRow(5, new Label("Notas:"), inNotas);
        dialog.getDialogPane().setContent(gp);

        // Validación básica
        var btnOk = dialog.getDialogPane().lookupButton(guardarBtn);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbMed.getValue() == null || inDosis.getText().isBlank() || inFreq.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING,
                    "Medicamento, dosis y frecuencia son obligatorios.").showAndWait();
                ev.consume();
            }
        });

        var res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != guardarBtn) return; // cancelado

        var medSel = cbMed.getValue();
        String dosis  = inDosis.getText().trim();
        String freq   = inFreq.getText().trim();
        String via    = inVia.getText().trim();
        String inicio = (dpInicio.getValue() != null ? dpInicio.getValue().toString() : LocalDate.now().toString());
        String notas  = inNotas.getText();

        // 3) Regla: NO permitir duplicado activo del mismo medicamento
        boolean yaActiva = prescDAO.existeActivaMismoMedicamento(residente.getId(), medSel.id);
        if (yaActiva) {
            new Alert(Alert.AlertType.ERROR,
                "Ya existe una prescripción ACTIVA para \"" + medSel.nombre + "\".\n" +
                "No se puede duplicar la misma medicación activa.").showAndWait();
            return;
        }

        // 4) Insertar
        prescDAO.insertar(residente.getId(), medSel.id, dosis, freq, via, inicio, notas);

        // 5) Refrescar tabla
        cargarPrescripciones();

        new Alert(Alert.AlertType.INFORMATION,
            "Prescripción añadida correctamente.").showAndWait();
    } catch (Exception e) {
        e.printStackTrace();    
        new Alert(Alert.AlertType.ERROR,
            "Error al añadir prescripción:\n" + e.getMessage()).showAndWait();
        }
}
@FXML
private void finalizarPrescripcion() {
    if (tablaPresc == null || tablaPresc.getSelectionModel().getSelectedItem() == null) {
        new Alert(Alert.AlertType.INFORMATION, "Selecciona una prescripción activa primero.").showAndWait();
        return;
    }

    var seleccionada = tablaPresc.getSelectionModel().getSelectedItem();

    // Confirmación antes de cerrar
    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Finalizar prescripción");
    confirm.setHeaderText("¿Finalizar la medicación seleccionada?");
    confirm.setContentText("Medicamento: " + seleccionada.getMedicamento());
    var res = confirm.showAndWait();

    if (res.isEmpty() || res.get() != ButtonType.OK) return; // cancelado

    try {
        String hoy = java.time.LocalDate.now().toString(); // formato YYYY-MM-DD
        prescDAO.finalizar(seleccionada.getId(), hoy);
        cargarPrescripciones();
        new Alert(Alert.AlertType.INFORMATION, "Prescripción finalizada correctamente.").showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error al finalizar prescripción:\n" + e.getMessage()).showAndWait();
    }
}
}

