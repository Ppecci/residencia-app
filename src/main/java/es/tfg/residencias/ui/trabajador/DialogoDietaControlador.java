package es.tfg.residencias.ui.trabajador;

import dao.DietaDAO;
import dao.DietaDAO.Dieta;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;

public class DialogoDietaControlador {

    // UI
    @FXML private Label lblTitulo;
    @FXML private Label lblVigenteNombre, lblVigenteDesde, lblVigenteNotas;
    @FXML private ComboBox<Dieta> cbDieta;
    @FXML private TextField inDesde, inNotas;

    // Estado / DAO
    private final DietaDAO dietaDAO = new DietaDAO();
    private Integer residenteId;

    @FXML
    public void initialize() {
        // nada especial aquí
    }

    /** Llamar al abrir el diálogo. */
    public void setResidente(int residenteId, String nombreCompleto, Integer dietaActualId, String dietaNotasActuales) {
        this.residenteId = residenteId;


        lblTitulo.setText("Dieta — Residente: " + nombreCompleto + " (id=" + residenteId + ")");
        inDesde.setText(LocalDate.now().toString());

        try {
            // Catálogo para el combo (usa tu listarCatalogo)
            cbDieta.getItems().setAll(dietaDAO.listarCatalogo());

            // Dieta vigente desde BD (más fiable que la tabla)
            var vigenteOpt = dietaDAO.obtenerDietaVigente(residenteId);
            if (vigenteOpt.isPresent()) {
                var v = vigenteOpt.get();
                lblVigenteNombre.setText(v.nombre != null ? v.nombre : "(sin nombre)");
                lblVigenteDesde.setText(v.desde != null ? v.desde : "-");
                lblVigenteNotas.setText(v.notas != null ? v.notas : "-");

                // Preselecciona en el combo si existe en catálogo
                cbDieta.getItems().stream()
                        .filter(d -> d.id == v.idAsignacion /* ojo: idAsignacion es de residente_dieta, NO de dietas */)
                        .findAny(); // no seleccionar por esto; ver nota abajo
                // Nota: tu DietaVigente guarda idAsignacion (residente_dieta), no dieta_id.
                // Por eso NO podemos preseleccionar por id aquí. Mostramos la vigente en labels,
                // y el usuario elige la nueva en el combo.
            } else {
                lblVigenteNombre.setText("(sin dieta vigente)");
                lblVigenteDesde.setText("-");
                lblVigenteNotas.setText("-");
            }

        } catch (Exception e) {
            error("Error cargando dietas", e.getMessage());
        }
    }

    @FXML
    private void guardar() {
        var nueva = cbDieta.getValue();
        if (nueva == null) { info("Selecciona una nueva dieta."); return; }
        String desde = val(inDesde);
        String notas = val(inNotas);
        if (desde.isBlank()) { info("La fecha de inicio es obligatoria."); return; }

        try {
            dietaDAO.cambiarDieta(residenteId, nueva.id, desde, notas);
            info("Dieta cambiada correctamente.");
            cerrar();
        } catch (Exception e) {
            error("No se pudo cambiar la dieta", e.getMessage());
        }
    }

    @FXML
    private void cerrar() {
        Stage st = (Stage) lblTitulo.getScene().getWindow();
        st.close();
    }

    private String val(TextField tf) { return tf.getText() == null ? "" : tf.getText().trim(); }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }

    private void error(String titulo, String detalle) {
        Alert a = new Alert(Alert.AlertType.ERROR, detalle, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
}
