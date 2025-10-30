package es.tfg.residencias.ui.admin.familiares;

import dao.FamiliarDAO; 

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import modelo.Familiar;

public class FamiliaresControlador {

    @FXML private TableView<Familiar> tabla;
    @FXML private TableColumn<Familiar, Integer> colId;
    @FXML private TableColumn<Familiar, String>  colNombre, colUsuario, colEmail;

    @FXML private TextField txtBuscar;
    @FXML private TextField inNombre, inUsuario, inEmail;
    @FXML private PasswordField inPassword;

   
    private final FamiliarDAO dao = new FamiliarDAO();

    private final ObservableList<Familiar> datos = FXCollections.observableArrayList();
    private Familiar seleccionado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> seleccionado = n);

        tabla.setRowFactory(tv -> {
            TableRow<Familiar> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    seleccionado = row.getItem();
                    verDetalle();
                }
            });
            return row;
        });

        refrescar();
    }

    @FXML private void refrescar() { cargar(null); }
    @FXML private void buscar()    { cargar(txtBuscar.getText()); }

    private void cargar(String filtro) {
        datos.clear();
        try { datos.addAll(dao.listar(filtro)); }
        catch (Exception e) { error("Error cargando familiares", e.getMessage()); }
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        inNombre.clear(); inUsuario.clear(); inEmail.clear();
        if (inPassword != null) inPassword.clear();
        inUsuario.setDisable(false);
        if (inPassword != null) inPassword.setDisable(false);
    }

    @FXML
    private void editar() {
        if (seleccionado == null) { info("Selecciona un familiar"); return; }
        inNombre.setText(seleccionado.getNombre());
        inUsuario.setText(seleccionado.getUsuario());
        inEmail.setText(seleccionado.getEmail());
        // No permitir cambiar usuario ni contraseña en edición
        inUsuario.setDisable(true);
        if (inPassword != null) { inPassword.clear(); inPassword.setDisable(true); }
    }

    @FXML
    private void guardar() {
        String nombre = inNombre.getText();
        String usuario = inUsuario.getText();
        String email   = inEmail.getText();
        String pass    = (inPassword == null ? null : inPassword.getText());

        if (nombre.isBlank()) { info("Nombre es obligatorio"); return; }

        try {
            if (seleccionado == null || seleccionado.getId() == null) {
                // Alta: usuario + password obligatorios
                if (usuario.isBlank() || pass == null || pass.isBlank()) {
                    info("Usuario y contraseña son obligatorios en el alta"); return;
                }
                Familiar f = new Familiar(null, nombre, usuario, email);
                f.setPasswordHashTemporal(pass);
                dao.insertar(f);
            } else {
                seleccionado.setNombre(nombre);
                seleccionado.setEmail(email);
                dao.actualizarBasico(seleccionado);
            }
            nuevo(); refrescar();
        } catch (Exception e) {
            e.printStackTrace();
            error("No se pudo guardar", e.getMessage());
        }
    }

    @FXML
    private void eliminar() {
        if (seleccionado == null) { info("Selecciona un familiar"); return; }
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
    private void verDetalle() {
        Familiar f = seleccionado;
        if (f == null) { info("Selecciona un familiar primero"); return; }
        try {
            var url = getClass().getResource("/fxml/PanelFamiliarDetalle.fxml");
            if (url == null) { error("FXML no encontrado", "/fxml/PanelFamiliarDetalle.fxml"); return; }
            FXMLLoader loader = new FXMLLoader(url);
            Parent vistaDetalle = loader.load();
            FamiliarDetalleControlador ctrl = loader.getController();
            ctrl.setFamiliar(f);

            StackPane centro = (StackPane) tabla.getScene().lookup("#contenedorCentro");
            if (centro == null) { error("No se encontró el contenedor central", "fx:id=contenedorCentro"); return; }
            centro.getChildren().setAll(vistaDetalle);
        } catch (Exception e) {
            e.printStackTrace();
            error("No se pudo abrir el detalle", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
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
