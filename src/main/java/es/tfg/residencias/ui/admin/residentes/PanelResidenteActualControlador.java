package es.tfg.residencias.ui.admin.residentes;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import modelo.Residente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.sql.Types;

import org.mindrot.jbcrypt.BCrypt;

import bd.ConexionBD;
import dao.HabitacionDAO;
import dao.PrescripcionDAO;
import es.tfg.residencias.ui.util.Navegacion;
import dao.MedicacionDAO;
import dao.MedicacionDAO.Medicacion;
import dao.DietaDAO;
import dao.FamiliarDAO;
import dao.FamiliarDAO.FamiliarAsignado;

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
        cargarHabitacion();
        cargarHistorico();
        cargarPrescripciones();
        cargarDieta();
        cargarHistoricoDieta();
        cargarFamilia();
        this.residenteId = (r != null ? r.getId() : 0);
        initializeTrabajadoresTab();
        cargarCuidadores();
    
    }

    private void cargarHabitacion() {
        if (residente == null) return;
        
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
        var disponibles = habitacionDAO.listarDisponibles();
        if (disponibles.isEmpty()) {
            // No hay libres
            if (lblHabNumero != null) lblHabNumero.setText("—");

            var alertaNoLibres = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION,
                    "No hay habitaciones disponibles ahora mismo."
            );
            Navegacion.aplicarCss(alertaNoLibres);
            alertaNoLibres.showAndWait();
            return;
        }

        var dialog = new ChoiceDialog<>(disponibles.get(0), disponibles);
        dialog.setTitle("Cambiar habitación");
        dialog.setHeaderText("Selecciona la nueva habitación");
        dialog.setContentText("Habitación:");

        Navegacion.aplicarCss(dialog);

        var elegido = dialog.showAndWait();
        if (elegido.isEmpty()) return;

        var nueva = elegido.get();

        var notasInput = new javafx.scene.control.TextInputDialog("");
        notasInput.setTitle("Cambiar habitación");
        notasInput.setHeaderText("Notas (opcional)");
        notasInput.setContentText("Motivo/observaciones:");

        Navegacion.aplicarCss(notasInput);

        var notas = notasInput.showAndWait().orElse("");

        String hoy = LocalDate.now().toString(); // YYYY-MM-DD
        habitacionDAO.asignarOActualizar(residente.getId(), nueva.id, hoy, notas);

        cargarHabitacion();
        cargarHistorico();

       
        var alertaOk = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Habitación cambiada a " + nueva.numero + "."
        );
        Navegacion.aplicarCss(alertaOk);
        alertaOk.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();

       
        var alertaError = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "No se pudo cambiar la habitación:\n" + e.getMessage()
        );
        Navegacion.aplicarCss(alertaError);
        alertaError.showAndWait();
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
    }
    }

            @FXML
private void anadirPrescripcion() {
    if (residente == null) return;

    try {
        var meds = medicacionDAO.listarTodas();
        if (meds.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "No hay medicamentos en el catálogo. Crea alguno en 'medicaciones'.");
            Navegacion.aplicarCss(a); 
            a.showAndWait();
            return;
        }

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

        Navegacion.aplicarCss(dialog);

        var btnOk = dialog.getDialogPane().lookupButton(guardarBtn);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (cbMed.getValue() == null || inDosis.getText().isBlank() || inFreq.getText().isBlank()) {
                Alert w = new Alert(Alert.AlertType.WARNING,
                        "Medicamento, dosis y frecuencia son obligatorios.");
                Navegacion.aplicarCss(w); 
                w.showAndWait();
                ev.consume();
            }
        });

        var res = dialog.showAndWait();
        if (res.isEmpty() || res.get() != guardarBtn) return;

        var medSel = cbMed.getValue();
        String dosis  = inDosis.getText().trim();
        String freq   = inFreq.getText().trim();
        String via    = inVia.getText().trim();
        String inicio = (dpInicio.getValue() != null ? dpInicio.getValue().toString() : LocalDate.now().toString());
        String notas  = inNotas.getText();

        boolean yaActiva = prescDAO.existeActivaMismoMedicamento(residente.getId(), medSel.id);
        if (yaActiva) {
            Alert errDup = new Alert(Alert.AlertType.ERROR,
                    "Ya existe una prescripción ACTIVA para \"" + medSel.nombre + "\".\n" +
                    "No se puede duplicar la misma medicación activa.");
            Navegacion.aplicarCss(errDup); 
            errDup.showAndWait();
            return;
        }

        prescDAO.insertar(residente.getId(), medSel.id, dosis, freq, via, inicio, notas);

        cargarPrescripciones();

        Alert ok = new Alert(Alert.AlertType.INFORMATION,
                "Prescripción añadida correctamente.");
        Navegacion.aplicarCss(ok); 
        ok.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        Alert err = new Alert(Alert.AlertType.ERROR,
                "Error al añadir prescripción:\n" + e.getMessage());
        Navegacion.aplicarCss(err); 
        err.showAndWait();
    }
}

    @FXML
private void finalizarPrescripcion() {
    if (tablaPresc == null || tablaPresc.getSelectionModel().getSelectedItem() == null) {
        Alert info = new Alert(Alert.AlertType.INFORMATION,
                "Selecciona una prescripción activa primero.");
        Navegacion.aplicarCss(info);
        info.showAndWait();
        return;
    }

    var seleccionada = tablaPresc.getSelectionModel().getSelectedItem();

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Finalizar prescripción");
    confirm.setHeaderText("¿Finalizar la medicación seleccionada?");
    confirm.setContentText("Medicamento: " + seleccionada.getMedicamento());

    Navegacion.aplicarCss(confirm);

    var res = confirm.showAndWait();
    if (res.isEmpty() || res.get() != ButtonType.OK) return;

    try {
        String hoy = java.time.LocalDate.now().toString();
        prescDAO.finalizar(seleccionada.getId(), hoy);
        cargarPrescripciones();

        Alert ok = new Alert(Alert.AlertType.INFORMATION,
                "Prescripción finalizada correctamente.");
        Navegacion.aplicarCss(ok); 
        ok.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        Alert err = new Alert(Alert.AlertType.ERROR,
                "Error al finalizar prescripción:\n" + e.getMessage());
        Navegacion.aplicarCss(err); 
        err.showAndWait();
    }
}
// DIETA
    @FXML private Label lblDietaActual, lblDietaDesde, lblDietaNotas;

    private final DietaDAO dietaDAO = new DietaDAO();

    private void cargarDieta() {
        if (residente == null) return;

        setDietaLabels("—", "—", "—");

        try {
            var opt = dietaDAO.obtenerDietaVigente(residente.getId());
            if (opt.isPresent()) {
                var d = opt.get(); 
                setDietaLabels(safe(d.nombre), safe(d.desde), safe(d.notas));
            }
        } catch (Exception e) {
            e.printStackTrace();
            setDietaLabels("Error", "Error", e.getMessage());
        }
    }

    private void setDietaLabels(String nombre, String desde, String notas) {
        if (lblDietaActual != null) lblDietaActual.setText(nombre);
        if (lblDietaDesde  != null) lblDietaDesde.setText(desde);
        if (lblDietaNotas  != null) lblDietaNotas.setText(notas);
    }

    @FXML
private void cambiarDieta() {
    if (residente == null) return;

    try {
        var catalogo = dietaDAO.listarCatalogo();
        if (catalogo.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "No hay dietas en el catálogo. Crea alguna en 'Dietas'.");
            Navegacion.aplicarCss(a); 
            a.showAndWait();
            return;
        }

        var dialog = new ChoiceDialog<>(catalogo.get(0), catalogo);
        dialog.setTitle("Cambiar dieta");
        dialog.setHeaderText("Selecciona la nueva dieta");
        dialog.setContentText("Dieta:");

        Navegacion.aplicarCss(dialog); 
        var elegido = dialog.showAndWait();
        if (elegido.isEmpty()) return;

        var nueva = elegido.get();

     
        var notasInput = new TextInputDialog("");
        notasInput.setTitle("Cambiar dieta");
        notasInput.setHeaderText("Notas (opcional)");
        notasInput.setContentText("Motivo/observaciones:");

        Navegacion.aplicarCss(notasInput); 
        var notas = notasInput.showAndWait().orElse("");

       
        var dp = new DatePicker(LocalDate.now());
        var dDialog = new Dialog<ButtonType>();
        dDialog.setTitle("Fecha de inicio de la dieta");
        dDialog.setHeaderText("Selecciona la fecha desde la que aplica la dieta");
        var ok = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        dDialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);

        var gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new Insets(10));
        gp.addRow(0, new Label("Desde:"), dp);
        dDialog.getDialogPane().setContent(gp);

        Navegacion.aplicarCss(dDialog); 
        var fechaRes = dDialog.showAndWait();
        if (fechaRes.isEmpty() || fechaRes.get() != ok) return;

        String desde = (dp.getValue() != null ? dp.getValue().toString() : LocalDate.now().toString());

        dietaDAO.cambiarDieta(residente.getId(), nueva.id, desde, notas);

        cargarDieta();
        cargarHistoricoDieta();

        Alert okAlert = new Alert(Alert.AlertType.INFORMATION,
                "Dieta cambiada a \"" + nueva.nombre + "\".");
        Navegacion.aplicarCss(okAlert); 
        okAlert.showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        Alert err = new Alert(Alert.AlertType.ERROR,
                "No se pudo cambiar la dieta:\n" + e.getMessage());
        Navegacion.aplicarCss(err); 
        err.showAndWait();
    }
}

@FXML private TableView<DietaDAO.HistDieta> tablaHistDieta;
@FXML private TableColumn<DietaDAO.HistDieta, String> colDHNombre, colDHDesde, colDHHasta, colDHNotas;

private final ObservableList<DietaDAO.HistDieta> datosHistDieta = FXCollections.observableArrayList();
private boolean dietaHistInit = false;

private void initHistDietaIfNeeded() {
    if (dietaHistInit || tablaHistDieta == null) return;
    colDHNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
    colDHDesde.setCellValueFactory(new PropertyValueFactory<>("desde"));
    colDHHasta.setCellValueFactory(new PropertyValueFactory<>("hasta"));
    colDHNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
    tablaHistDieta.setItems(datosHistDieta);
    dietaHistInit = true;
}


private void cargarHistoricoDieta() {
    if (residente == null || tablaHistDieta == null) return;
    initHistDietaIfNeeded();
    datosHistDieta.clear();
    try {
        var lista = dietaDAO.listarHistorico(residente.getId());
        for (var d : lista) {
            
            var hasta = (d.hasta == null || d.hasta.isBlank()) ? "—" : d.hasta;
            datosHistDieta.add(new DietaDAO.HistDieta(d.nombre, d.desde, hasta, d.notas));
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
@FXML private TableView<FamiliarAsignado> tablaFamilia;
@FXML private TableColumn<FamiliarAsignado, String> colFamNombre, colFamUsuario, colFamParentesco, colFamEmail;

private final FamiliarDAO familiarDAO = new FamiliarDAO();
private final ObservableList<FamiliarAsignado> datosFamilia = FXCollections.observableArrayList();
private boolean famInit = false;

private void initFamiliaTableIfNeeded() {
    if (famInit || tablaFamilia == null) return;
    colFamNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
    colFamUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
    colFamParentesco.setCellValueFactory(new PropertyValueFactory<>("parentesco"));
    colFamEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
    tablaFamilia.setItems(datosFamilia);
    famInit = true;
}

private void cargarFamilia() {
    if (residente == null || tablaFamilia == null) return;
    initFamiliaTableIfNeeded();
    datosFamilia.clear();
    try {
        var lista = familiarDAO.listarAsignados(residente.getId());
        datosFamilia.addAll(lista);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@FXML
private void anadirFamiliar() {
    if (residente == null) return;


    var tipo = new ChoiceDialog<>(
        "Vincular existente", FXCollections.observableArrayList("Vincular existente", "Crear nuevo")
    );
    tipo.setTitle("Añadir familiar");
    tipo.setHeaderText("Elige cómo quieres añadir");
    tipo.setContentText("Acción:");
    Navegacion.aplicarCss(tipo);
    var tipoRes = tipo.showAndWait();
    if (tipoRes.isEmpty()) return;

    if (tipoRes.get().equals("Vincular existente")) {
        vincularExistente();
    } else {
        crearNuevoYVincular();
    }
}

private void vincularExistente() {
    try {
        var noAsignados = familiarDAO.listarNoAsignados(residente.getId());
        if (noAsignados.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay familiares disponibles para vincular.").showAndWait();
            return;
        }

        var choice = new ChoiceDialog<>(noAsignados.get(0), noAsignados);
        choice.setTitle("Vincular familiar existente");
        choice.setHeaderText("Selecciona el familiar");
        choice.setContentText("Familiar:");
        Navegacion.aplicarCss(choice);

        var elegido = choice.showAndWait();
        if (elegido.isEmpty()) return;

        TextInputDialog inp = new TextInputDialog("");
        inp.setTitle("Parentesco");
        inp.setHeaderText("Indica el parentesco");
        inp.setContentText("Parentesco:");
        String parentesco = inp.showAndWait().orElse("");
        familiarDAO.insertarAsignacion(residente.getId(), elegido.get().id, parentesco);

        cargarFamilia();
        new Alert(Alert.AlertType.INFORMATION, "Familiar vinculado.").showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error al vincular:\n" + e.getMessage()).showAndWait();
    }
}

private void crearNuevoYVincular() {
    try {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Crear nuevo familiar");
        dlg.setHeaderText("Introduce los datos del nuevo familiar");
        ButtonType guardar = new ButtonType("Crear y vincular", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(Navegacion.appCss());

        TextField inNombre = new TextField();
        TextField inUsuario = new TextField();
        PasswordField inPass = new PasswordField();
        TextField inEmail = new TextField();
        TextField inParentesco = new TextField();

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new Label("Nombre:"), inNombre);
        gp.addRow(1, new Label("Usuario:"), inUsuario);
        gp.addRow(2, new Label("Contraseña:"), inPass);
        gp.addRow(3, new Label("Email:"), inEmail);
        gp.addRow(4, new Label("Parentesco:"), inParentesco);
        dlg.getDialogPane().setContent(gp);

        var btnOk = dlg.getDialogPane().lookupButton(guardar);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (inNombre.getText().isBlank() || inUsuario.getText().isBlank() || inPass.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Nombre, usuario y contraseña son obligatorios.").showAndWait();
                ev.consume();
            }
        });

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != guardar) return;

        String passPlano = inPass.getText();
        String hash = BCrypt.hashpw(passPlano, BCrypt.gensalt(12)); 

        int nuevoId = familiarDAO.crearFamiliar(
            inNombre.getText().trim(),
            inUsuario.getText().trim(),
            hash, 
            inEmail.getText().trim()
        );
        familiarDAO.insertarAsignacion(residente.getId(), nuevoId, inParentesco.getText().trim());

        cargarFamilia();
        new Alert(Alert.AlertType.INFORMATION, "Familiar creado y vinculado.").showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error al crear/vincular:\n" + e.getMessage()).showAndWait();
    }
}

@FXML
private void editarFamiliar() {
    if (tablaFamilia == null || tablaFamilia.getSelectionModel().getSelectedItem() == null) {
        new Alert(Alert.AlertType.INFORMATION, "Selecciona un familiar primero.").showAndWait();
        return;
    }
    var sel = tablaFamilia.getSelectionModel().getSelectedItem();
    try {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Editar familiar");
        dlg.setHeaderText("Modifica los datos del familiar y el parentesco");
        ButtonType guardar = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        dlg.getDialogPane().getStylesheets().add(Navegacion.appCss());

        TextField inNombre = new TextField(sel.getNombre());
        TextField inUsuario = new TextField(sel.getUsuario());
        TextField inEmail = new TextField(sel.getEmail());
        TextField inParentesco = new TextField(sel.getParentesco());

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10); gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new Label("Nombre:"), inNombre);
        gp.addRow(1, new Label("Usuario:"), inUsuario);
        gp.addRow(2, new Label("Email:"), inEmail);
        gp.addRow(3, new Label("Parentesco:"), inParentesco);
        dlg.getDialogPane().setContent(gp);

        var btnOk = dlg.getDialogPane().lookupButton(guardar);
        btnOk.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            if (inNombre.getText().isBlank() || inUsuario.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Nombre y usuario son obligatorios.").showAndWait();
                ev.consume();
            }
        });

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != guardar) return;

        familiarDAO.actualizarAsignado(
            sel.getIdRelacion(),
            sel.getIdFamiliar(),
            inNombre.getText().trim(),
            inUsuario.getText().trim(),
            inEmail.getText().trim(),
            inParentesco.getText().trim()
        );

        cargarFamilia();
        new Alert(Alert.AlertType.INFORMATION, "Familiar actualizado.").showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error al actualizar:\n" + e.getMessage()).showAndWait();
    }
}
@FXML
private void borrarFamiliar() {
    if (tablaFamilia == null || tablaFamilia.getSelectionModel().getSelectedItem() == null) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION, "Selecciona un familiar primero.");
        alerta.getDialogPane().getStylesheets().add(Navegacion.appCss());
        alerta.showAndWait();
        return;
    }
    var sel = tablaFamilia.getSelectionModel().getSelectedItem();

    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
    confirm.setTitle("Eliminar asignación");
    confirm.setHeaderText("¿Quitar este familiar del residente?");
    confirm.setContentText(sel.getNombre() + " (" + sel.getParentesco() + ")");
    confirm.getDialogPane().getStylesheets().add(Navegacion.appCss());
    var r = confirm.showAndWait();
    if (r.isEmpty() || r.get() != ButtonType.OK) return;

    try {
        familiarDAO.borrarAsignacion(sel.getIdRelacion());
        cargarFamilia();
        new Alert(Alert.AlertType.INFORMATION, "Familiar desasignado.").showAndWait();
    } catch (Exception e) {
        e.printStackTrace();
        new Alert(Alert.AlertType.ERROR, "Error al desasignar:\n" + e.getMessage()).showAndWait();
    }
}

@FXML private Tab tabTrabajadores;
@FXML private Button btnAsignarTrab, btnCerrarTrab;
@FXML private CheckBox chkSoloVigentesTrab;
@FXML private TableView<CuidadorFila> tablaCuidadores;
@FXML private TableColumn<CuidadorFila, String> colTrabajador, colUsuario, colDesde, colHasta, colNotas;


private int residenteId;


public static class CuidadorFila {
    private final Integer idAsignacion; 
    private final String trabajador;
    private final String usuario;
    private final String desde;
    private final String hasta;
    private final String notas;

    public CuidadorFila(Integer idAsignacion, String trabajador, String usuario,
                        String desde, String hasta, String notas) {
        this.idAsignacion = idAsignacion;
        this.trabajador = trabajador;
        this.usuario = usuario;
        this.desde = desde;
        this.hasta = hasta;
        this.notas = notas;
    }
    public Integer getIdAsignacion() { return idAsignacion; }
    public String getTrabajador() { return trabajador; }
    public String getUsuario() { return usuario; }
    public String getDesde() { return desde; }
    public String getHasta() { return hasta; }
    public String getNotas() { return notas; }
}

private final ObservableList<CuidadorFila> cuidadores = FXCollections.observableArrayList();

@FXML
private void initializeTrabajadoresTab() {
    if (tablaCuidadores == null) return; 
    colTrabajador.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getTrabajador()));
    colUsuario.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getUsuario()));
    colDesde.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getDesde()));
    colHasta.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getHasta()));
    colNotas.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().getNotas()));
    tablaCuidadores.setItems(cuidadores);

    if (chkSoloVigentesTrab != null) chkSoloVigentesTrab.setSelected(true);
}

public void setResidenteId(int id) {
    this.residenteId = id;
    initializeTrabajadoresTab(); 
    cargarCuidadores();
}

@FXML
private void toggleVigentesTrab() { cargarCuidadores(); }

private void cargarCuidadores() {
    if (tablaCuidadores == null) return;
    boolean soloVigentes = (chkSoloVigentesTrab != null) && chkSoloVigentesTrab.isSelected();
    cuidadores.setAll(consultarCuidadores(residenteId, soloVigentes));
}

private java.util.List<CuidadorFila> consultarCuidadores(int resId, boolean soloVigentes) {
    String sql = """
        SELECT
          a.id                AS asignacion_id,
          t.nombre AS trabajador,
          COALESCE(u.username, t.usuario)           AS usuario,
          a.start_date       AS desde,
          a.end_date         AS hasta,
          IFNULL(a.notas,'') AS notas
        FROM asignacion_trabajador a
        JOIN trabajadores t  ON t.id = a.trabajador_id
        LEFT JOIN usuarios u ON u.trabajador_id = t.id AND u.rol='TRABAJADOR'
        WHERE a.residente_id = ?
    """ + (soloVigentes ? " AND a.end_date IS NULL AND t.activo = 1 " : "") + """
        ORDER BY (a.end_date IS NULL) DESC, a.start_date DESC, trabajador
    """;

    java.util.List<CuidadorFila> lista = new java.util.ArrayList<>();
    try (Connection c = ConexionBD.obtener();
         PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, resId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new CuidadorFila(
                    rs.getInt("asignacion_id"),
                    rs.getString("trabajador"),
                    rs.getString("usuario"),
                    rs.getString("desde"),
                    rs.getString("hasta"),
                    rs.getString("notas")
                ));
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        a.setHeaderText("Error cargando cuidadores");
        a.showAndWait();
    }
    return lista;
}

@FXML
private void cerrarAsignacionTrab() {
    CuidadorFila sel = tablaCuidadores.getSelectionModel().getSelectedItem();
    if (sel == null) {
        new Alert(Alert.AlertType.INFORMATION, "Selecciona una asignación para cerrar.", ButtonType.OK).showAndWait();
        return;
    }
    if (sel.getHasta() != null && !sel.getHasta().isBlank()) {
        new Alert(Alert.AlertType.INFORMATION, "La asignación ya está cerrada.", ButtonType.OK).showAndWait();
        return;
    }
    if (!confirm("¿Cerrar la asignación seleccionada a fecha de hoy?")) return;

    try (Connection c = ConexionBD.obtener();
         PreparedStatement ps = c.prepareStatement("UPDATE asignacion_trabajador SET end_date = date('now') WHERE id=? AND end_date IS NULL")) {
        ps.setInt(1, sel.getIdAsignacion());
        int n = ps.executeUpdate();
        if (n == 0) {
            new Alert(Alert.AlertType.WARNING, "No se pudo cerrar (puede que ya esté cerrada).", ButtonType.OK).showAndWait();
        }
        cargarCuidadores();
    } catch (Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        a.setHeaderText("Error al cerrar");
        a.showAndWait();
    }
}

private static class TrabItem {
    final int id;
    final String etiqueta; 
    TrabItem(int id, String etiqueta) { this.id = id; this.etiqueta = etiqueta; }
    @Override public String toString() { return etiqueta; }
}

@FXML
private void asignarTrabajador() {
    if (residente == null || residenteId == 0) {
        new Alert(Alert.AlertType.INFORMATION, "Primero selecciona un residente.").showAndWait();
        return;
    }

    try {
        var activos = cargarTrabajadoresActivos();
        if (activos.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "No hay trabajadores activos para asignar.").showAndWait();
            return;
        }

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Asignar cuidador");
        dlg.setHeaderText("Selecciona trabajador y fecha de inicio");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<TrabItem> cbTrab = new ComboBox<>(FXCollections.observableArrayList(activos));
        cbTrab.getSelectionModel().selectFirst();

        DatePicker dpInicio = new DatePicker(LocalDate.now());
        TextArea taNotas = new TextArea();
        taNotas.setPromptText("Notas (opcional)");
        taNotas.setPrefRowCount(3);

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(8); gp.setPadding(new javafx.geometry.Insets(10));
        gp.addRow(0, new Label("Trabajador:"), cbTrab);
        gp.addRow(1, new Label("Inicio:"),     dpInicio);
        gp.addRow(2, new Label("Notas:"),      taNotas);
        dlg.getDialogPane().setContent(gp);

        var res = dlg.showAndWait();
        if (res.isEmpty() || res.get() != ButtonType.OK) return;

        TrabItem sel = cbTrab.getValue();
        LocalDate ini = dpInicio.getValue();
        String notas = taNotas.getText() == null ? "" : taNotas.getText().trim();

        if (sel == null) { new Alert(Alert.AlertType.INFORMATION, "Selecciona un trabajador.").showAndWait(); return; }
        if (ini == null) { new Alert(Alert.AlertType.INFORMATION, "Selecciona la fecha de inicio.").showAndWait(); return; }

        if (existeAsignacionVigente(residenteId, sel.id)) {
            new Alert(Alert.AlertType.INFORMATION, "Ese trabajador ya está asignado actualmente a este residente.").showAndWait();
            return;
        }

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO asignacion_trabajador(residente_id, trabajador_id, start_date, end_date, notas) " +
                 "VALUES (?,?,?,?,?)")) {
            ps.setInt(1, residenteId);
            ps.setInt(2, sel.id);
            ps.setString(3, ini.toString());
            ps.setNull(4, Types.VARCHAR);
            ps.setString(5, notas);
            ps.executeUpdate();
        }

        cargarCuidadores();
        new Alert(Alert.AlertType.INFORMATION, "Asignación creada.").showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
        a.setHeaderText("No se pudo asignar"); a.showAndWait();
    }
}

private java.util.List<TrabItem> cargarTrabajadoresActivos() throws Exception {
    String sql = """
        SELECT t.id,
               t.nombre AS nombre,
               COALESCE(u.username, t.usuario) AS usuario
        FROM trabajadores t
        LEFT JOIN usuarios u ON u.trabajador_id = t.id AND u.rol='TRABAJADOR'
        WHERE t.activo = 1
        ORDER BY t.nombre, usuario
    """;
    var lista = new java.util.ArrayList<TrabItem>();
    try (Connection c = ConexionBD.obtener();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
            String etiqueta = rs.getString("nombre") + " (" +
                    (rs.getString("usuario") == null ? "—" : rs.getString("usuario")) + ")";
            lista.add(new TrabItem(rs.getInt("id"), etiqueta));
        }
    }
    return lista;
}

private boolean existeAsignacionVigente(int resId, int trabId) throws Exception {
    String sql = """
        SELECT EXISTS(
            SELECT 1
            FROM asignacion_trabajador
            WHERE residente_id=? AND trabajador_id=? AND end_date IS NULL
            LIMIT 1
        )
    """;
    try (Connection c = ConexionBD.obtener(); PreparedStatement ps = c.prepareStatement(sql)) {
        ps.setInt(1, resId);
        ps.setInt(2, trabId);
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() && rs.getInt(1) == 1;
        }
    }
}
private boolean confirm(String msg) {
    var r = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO).showAndWait();
    return r.isPresent() && r.get() == ButtonType.YES;
}
}



