package es.tfg.residencias.ui.admin.trabajadores;

import dao.TrabajadoresDAO;
import modelo.Trabajador;
import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import org.mindrot.jbcrypt.BCrypt;


public class TrabajadoresControlador {

    @FXML private TableView<Trabajador> tabla;
    @FXML private TableColumn<Trabajador, Integer> colId;
    @FXML private TableColumn<Trabajador, String>  colNombre, colUsuario, colEmail;
    @FXML private TableColumn<Trabajador, Boolean> colActivo;

    @FXML private TextField txtBuscar;
    @FXML private TextField inNombre, inUsuario, inEmail;
    @FXML private PasswordField inPassword; 
    @FXML private CheckBox  chkActivo;

    private final TrabajadoresDAO dao = new TrabajadoresDAO();
    private final ObservableList<Trabajador> datos = FXCollections.observableArrayList();
    private Trabajador seleccionado;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        tabla.setItems(datos);
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> seleccionado = n);
        refrescar();

        
        tabla.setRowFactory(tv -> {
            TableRow<modelo.Trabajador> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    seleccionado = row.getItem();
                    verDetalle();
                }
            });
            return row;
        });
    }

    @FXML private void refrescar() { cargar(null); }
    @FXML private void buscar()    { cargar(txtBuscar.getText()); }

    private void cargar(String filtro) {
        datos.clear();
        try { datos.addAll(dao.listar(filtro)); }
        catch (Exception e) { error("Error cargando trabajadores", e.getMessage()); }
    }

    @FXML
    private void nuevo() {
        seleccionado = null;
        inNombre.clear(); inUsuario.clear(); inEmail.clear();
        if (inPassword != null) inPassword.clear();
        if (chkActivo != null) chkActivo.setSelected(true);
    }

    @FXML
    private void editar() {
        if (seleccionado == null) { info("Selecciona un trabajador"); return; }
        inNombre.setText(seleccionado.getNombre());
        inUsuario.setText(seleccionado.getUsuario());
        inEmail.setText(seleccionado.getEmail());
        if (inPassword != null) inPassword.clear(); 
        if (chkActivo != null) chkActivo.setSelected(Boolean.TRUE.equals(seleccionado.getActivo()));
    }

    @FXML
    private void eliminar() {
        if (seleccionado == null) { info("Selecciona un trabajador"); return; }
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
        Trabajador t = seleccionado;
        if (t == null) {
            info("Selecciona un trabajador primero");
            return;
        }

        try {
            var url = getClass().getResource("/fxml/PanelTrabajadorDetalle.fxml");
            if (url == null) {
                error("No se encontró el FXML",
                      "Ruta inválida: /fxml/PanelTrabajadorDetalle.fxml.\n" +
                      "Asegúrate de que está en src/main/resources/fxml/ con ese nombre exacto.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent vistaDetalle = loader.load();

            TrabajadorDetalleControlador ctrl = loader.getController();
            ctrl.setTrabajador(t);


            StackPane centro = (StackPane) tabla.getScene().lookup("#contenedorCentro");
            if (centro == null) {
                error("No se encontró el contenedor central",
                      "Revisa fx:id=\"contenedorCentro\" en PanelAdmin.fxml");
                return;
            }
            centro.getChildren().setAll(vistaDetalle);

        } catch (Exception e) {
            e.printStackTrace();
            error("No se pudo abrir el detalle", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

   @FXML
private void guardar() {
    String nombre = inNombre.getText().trim();
    String usuario = inUsuario.getText().trim();
    String email   = inEmail.getText().trim();
    String pwd     = (inPassword == null ? null : inPassword.getText());
    boolean activo = (chkActivo == null) || chkActivo.isSelected();

    if (nombre.isBlank() || usuario.isBlank()) {
        info("Nombre y usuario son obligatorios"); return;
    }

    try {
        if (seleccionado == null || seleccionado.getId() == null) {
            
            if (pwd == null || pwd.isBlank()) {
                info("La contraseña es obligatoria al crear un trabajador"); return;
            }

            String hash = BCrypt.hashpw(pwd, BCrypt.gensalt(12));

            Trabajador t = new Trabajador(null, nombre, usuario, email, activo);
            t.setPasswordHashTemporal(hash); 
            dao.insertar(t);

        } else {
            seleccionado.setNombre(nombre);
            seleccionado.setUsuario(usuario);
            seleccionado.setEmail(email);
            seleccionado.setActivo(activo);
            dao.actualizar(seleccionado);

            if (pwd != null && !pwd.isBlank()) {
              
                String hash = BCrypt.hashpw(pwd, BCrypt.gensalt(12));
                dao.actualizarPassword(seleccionado.getId(), hash);
            }
        }

        nuevo();
        refrescar();

    } catch (Exception e) {
        error("No se pudo guardar", e.getMessage());
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
