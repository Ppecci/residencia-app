package es.tfg.residencias.ui.admin.residentes;

import dao.ResidentesDAO;
import modelo.Residente;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class PanelResidentesControlador {

    @FXML private TableView<Residente> tabla;
    @FXML private TableColumn<Residente, Integer> colId;
    @FXML private TableColumn<Residente, String>  colNombre, colApellidos, colFecha, colNotas;
    @FXML private TableColumn<Residente, Boolean> colActivo;

    @FXML private TextField txtBuscar;
    @FXML private TextField inNombre, inApellidos, inFecha;
    @FXML private TextArea  inNotas;
    @FXML private CheckBox  chkActivo;

    private final ResidentesDAO dao = new ResidentesDAO();
    private final ObservableList<Residente> datos = FXCollections.observableArrayList();
    private Residente seleccionado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(new PropertyValueFactory<>("apellidos"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaNacimiento"));
        colNotas.setCellValueFactory(new PropertyValueFactory<>("notas"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> seleccionado = n);
        refrescar();
    }

    @FXML private void refrescar() { cargar(null); }
    @FXML private void buscar()    { cargar(txtBuscar.getText()); }

    private void cargar(String filtro) {
        datos.clear();
        try { datos.addAll(dao.listar(filtro)); }
        catch (Exception e) { error("Error cargando residentes", e.getMessage()); }
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        inNombre.clear(); inApellidos.clear(); inFecha.clear();
        if (inNotas != null) inNotas.clear();
        if (chkActivo != null) chkActivo.setSelected(true);
    }

    @FXML
    private void editar() {
        if (seleccionado == null) { info("Selecciona un residente"); return; }
        inNombre.setText(seleccionado.getNombre());
        inApellidos.setText(seleccionado.getApellidos());
        inFecha.setText(seleccionado.getFechaNacimiento());
        if (inNotas != null) inNotas.setText(seleccionado.getNotas());
        if (chkActivo != null) chkActivo.setSelected(Boolean.TRUE.equals(seleccionado.getActivo()));
    }

    @FXML
    private void eliminar() {
        if (seleccionado == null) { info("Selecciona un residente"); return; }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar a " + seleccionado.getNombre() + "?", ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText("Confirmar eliminación");
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try { dao.eliminar(seleccionado.getId()); refrescar(); }
                catch (Exception e) { error("No se pudo eliminar", e.getMessage()); }
            }
        });
    }

    @FXML
    private void guardar() {
        String nombre = inNombre.getText();
        String apellidos = inApellidos.getText();
        String fecha = inFecha.getText();
        String notas = (inNotas == null ? null : inNotas.getText());
        boolean activo = (chkActivo == null) || chkActivo.isSelected();

        if (nombre.isBlank() || apellidos.isBlank() || fecha.isBlank()) {
            info("Nombre, apellidos y fecha son obligatorios"); return;
        }

        try {
            if (seleccionado == null || seleccionado.getId() == null) {
                dao.insertar(new Residente(null, nombre, apellidos, fecha, notas, activo));
            } else {
                seleccionado.setNombre(nombre);
                seleccionado.setApellidos(apellidos);
                seleccionado.setFechaNacimiento(fecha);
                seleccionado.setNotas(notas);
                seleccionado.setActivo(activo);
                dao.actualizar(seleccionado);
            }
            nuevo(); refrescar();
        } catch (Exception e) { error("No se pudo guardar", e.getMessage()); }
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null); a.showAndWait();
    }
    private void error(String titulo, String detalle) {
        Alert a = new Alert(Alert.AlertType.ERROR, detalle, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
}