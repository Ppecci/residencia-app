package es.tfg.residencias.ui.admin.familiares;

import dao.FamiliarRelacionesDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.Familiar;
import modelo.FamiliarResidenteVista;

public class FamiliarDetalleControlador {

    @FXML private Label lblTitulo;
    @FXML private TableView<FamiliarResidenteVista> tabla;
    @FXML private TableColumn<FamiliarResidenteVista, String> colResidente, colParentesco;

    private final FamiliarRelacionesDAO relDAO = new FamiliarRelacionesDAO();
    private final ObservableList<FamiliarResidenteVista> datos = FXCollections.observableArrayList();
    private Familiar familiar;

    @FXML
    public void initialize() {
        colResidente.setCellValueFactory(new PropertyValueFactory<>("residente"));
        colParentesco.setCellValueFactory(new PropertyValueFactory<>("parentesco"));
        tabla.setItems(datos);
    }

    public void setFamiliar(Familiar f) {
        this.familiar = f;
        lblTitulo.setText("Familiar: " + f.getNombre() + " (" + f.getUsuario() + ")");
        cargar();
    }

    private void cargar() {
        if (familiar == null) return;
        try { datos.setAll(relDAO.listarPorFamiliar(familiar.getId())); }
        catch (Exception e) { error("Error cargando relaciones", e.getMessage()); }
    }

    private void error(String h, String d) {
        Alert a = new Alert(Alert.AlertType.ERROR, d, ButtonType.OK);
        a.setHeaderText(h); a.showAndWait();
    }
}
