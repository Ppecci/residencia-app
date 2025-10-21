package es.tfg.residencias.ui.admin.residentes;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import modelo.Residente;

public class PanelResidenteActualControlador {
    @FXML private Label lblTitulo;
    private Residente residente;

    public void setResidente(Residente r) {
        this.residente = r;
        if (lblTitulo != null && r != null) {
            lblTitulo.setText("Detalle: " + this.residente.getNombre() + " " + this.residente.getApellidos());
        }
    }
}