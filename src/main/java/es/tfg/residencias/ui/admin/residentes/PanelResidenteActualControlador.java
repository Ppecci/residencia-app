package es.tfg.residencias.ui.admin.residentes;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import modelo.Residente;

import java.time.LocalDate;

import dao.HabitacionDAO;

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

        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
            "Habitación cambiada a " + nueva.numero + ".").showAndWait();

    } catch (Exception e) {
        e.printStackTrace();
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
            "No se pudo cambiar la habitación:\n" + e.getMessage()).showAndWait();
    }
}
}