package es.tfg.residencias.ui.trabajador;

import dao.TrabajadoresDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.TrabajadorResumenFila;

import java.util.List;

public class PanelTrabajadorControlador {

    // UI
    @FXML private Label lblTitulo;
    @FXML private TextField txtBuscar;

    @FXML private TableView<TrabajadorResumenFila> tabla;
    @FXML private TableColumn<TrabajadorResumenFila, Integer> colResidenteId, colHabId, colDietaId;
    @FXML private TableColumn<TrabajadorResumenFila, String>  colNombre, colApellidos, colHabNumero, colHabPlanta,
                                                              colMedicacion, colDietaNotas, colProximaCita;

    @FXML private Button btnEditarPrescripciones, btnEditarDieta, btnCitas;

    // Estado
    private final TrabajadoresDAO dao = new TrabajadoresDAO();
    private final ObservableList<TrabajadorResumenFila> datos = FXCollections.observableArrayList();

    private Integer trabajadorId;        
    private String  nombreTrabajador;    

    @FXML
    public void initialize() {
        colResidenteId.setCellValueFactory(new PropertyValueFactory<>("residenteId"));
        colNombre.setCellValueFactory(     new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(  new PropertyValueFactory<>("apellidos"));

        colHabId.setCellValueFactory(      new PropertyValueFactory<>("habId"));
        colHabNumero.setCellValueFactory(  new PropertyValueFactory<>("habNumero"));
        colHabPlanta.setCellValueFactory(  new PropertyValueFactory<>("habPlanta"));

        colMedicacion.setCellValueFactory( new PropertyValueFactory<>("medicacionResumen"));

        colDietaId.setCellValueFactory(    new PropertyValueFactory<>("dietaId"));
        colDietaNotas.setCellValueFactory( new PropertyValueFactory<>("dietaNotas"));

        colProximaCita.setCellValueFactory(new PropertyValueFactory<>("proximaCita"));

        tabla.setItems(datos);


        tabla.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean sel = (n != null);
            btnEditarPrescripciones.setDisable(!sel);
            btnEditarDieta.setDisable(!sel);
            btnCitas.setDisable(!sel);
        });

        btnEditarPrescripciones.setDisable(true);
        btnEditarDieta.setDisable(true);
        btnCitas.setDisable(true);

        if (trabajadorId != null) {
            refrescar();
        }
    }


   public void setTrabajadorId(int trabajadorId) {
    this.trabajadorId = trabajadorId;
                if (this.nombreTrabajador == null || this.nombreTrabajador.isBlank()) {
                    try {
                        this.nombreTrabajador = dao.obtenerNombrePorId(trabajadorId).orElse(null);
                    } catch (Exception e) {
                        System.err.println("No se pudo cargar el nombre del trabajador: " + e.getMessage());
                    }
                }

                if (tabla != null) refrescar();
                actualizarTitulo();
            }
    public void setNombreTrabajador(String nombre) {
        this.nombreTrabajador = nombre;
        actualizarTitulo();
    }

    private void actualizarTitulo() {
        if (lblTitulo == null) return;
        String base = "Trabajador: ";
        if (nombreTrabajador != null && !nombreTrabajador.isBlank()) {
            lblTitulo.setText(base + nombreTrabajador + " (id=" + (trabajadorId != null ? trabajadorId : "—") + ")");
        } else {
            lblTitulo.setText(base + (trabajadorId != null ? ("id=" + trabajadorId) : "—"));
        }
    }


    @FXML private void refrescar() { cargar(null); }

    @FXML private void buscar()    { cargar(txtBuscar != null ? txtBuscar.getText() : null); }

    private void cargar(String filtro) {
        if (trabajadorId == null) {
            info("Falta identificar al trabajador (setTrabajadorId).");
            return;
        }
        try {
            List<TrabajadorResumenFila> lista = dao.listarAsignados(trabajadorId, filtro);
            datos.setAll(lista.toArray(new TrabajadorResumenFila[0]));
        } catch (Exception e) {
            error("Error cargando datos", e.getMessage());
        }
    }

    @FXML
        private void editarPrescripciones() {
            var fila = tabla.getSelectionModel().getSelectedItem();
            if (fila == null) { info("Selecciona un residente."); return; }

            try {
                javafx.fxml.FXMLLoader loader =
                        new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/DialogoPrescripciones.fxml"));

                javafx.scene.Parent root = loader.load();

                es.tfg.residencias.ui.trabajador.DialogoPrescripcionesControlador ctrl = loader.getController();
                String nombreCompleto = fila.getNombre() + (fila.getApellidos() != null ? " " + fila.getApellidos() : "");
                ctrl.setResidente(fila.getResidenteId(), nombreCompleto);

                javafx.stage.Stage st = new javafx.stage.Stage();
                st.setTitle("Prescripciones — " + nombreCompleto);
                st.setScene(new javafx.scene.Scene(root));  
                st.initOwner(tabla.getScene().getWindow());
                st.showAndWait();

                refrescar();
            } catch (Exception e) {
                error("No se pudo abrir el diálogo de prescripciones", e.getMessage());
            }
        }

    @FXML
        private void editarDieta() {
                var fila = tabla.getSelectionModel().getSelectedItem();
                if (fila == null) { info("Selecciona un residente."); return; }

                try {
                    java.net.URL url = getClass().getResource("/fxml/DialogoDieta.fxml");
                    if (url == null) {
                        error("FXML no encontrado", "/fxml/DialogoDieta.fxml");
                        return;
                    }
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
                    javafx.scene.Parent root = loader.load();

                    var ctrl = (es.tfg.residencias.ui.trabajador.DialogoDietaControlador) loader.getController();
                    String nombreCompleto = fila.getNombre() + (fila.getApellidos() != null ? " " + fila.getApellidos() : "");
                    ctrl.setResidente(fila.getResidenteId(), nombreCompleto, fila.getDietaId(), fila.getDietaNotas());

                    javafx.stage.Stage st = new javafx.stage.Stage();
                    st.setTitle("Cambiar dieta — " + nombreCompleto);
                    st.setScene(new javafx.scene.Scene(root));
                    st.initOwner(tabla.getScene().getWindow());
                    st.showAndWait();

                    refrescar();

                } catch (Exception e) {
                    error("No se pudo abrir el diálogo de dieta", e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            @FXML
            private void gestionarCitas() {
                var fila = tabla.getSelectionModel().getSelectedItem();
                if (fila == null) { 
                    info("Selecciona un residente."); 
                    return; 
                }

                try {
                    java.net.URL url = getClass().getResource("/fxml/DialogoCitas.fxml");
                    if (url == null) {
                        error("FXML no encontrado", "/fxml/DialogoCitas.fxml");
                        return;
                    }

                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
                    javafx.scene.Parent root = loader.load();

                    es.tfg.residencias.ui.trabajador.DialogoCitasControlador ctrl =
                            loader.getController();

                    String nombreCompleto = fila.getNombre() + 
                            (fila.getApellidos() != null ? " " + fila.getApellidos() : "");

                    ctrl.setResidente(fila.getResidenteId(), nombreCompleto);

                    
                    javafx.stage.Stage st = new javafx.stage.Stage();
                    st.setTitle("Citas médicas — " + nombreCompleto);
                    st.setScene(new javafx.scene.Scene(root));
                    st.initOwner(tabla.getScene().getWindow());
                    st.initModality(javafx.stage.Modality.WINDOW_MODAL);
                    st.showAndWait();

                    refrescar();

                } catch (Exception e) {
                    error("No se pudo abrir el diálogo de citas", e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }


           @FXML
            private void cerrarSesion(javafx.event.ActionEvent e) {
                javafx.scene.control.Alert alerta = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.CONFIRMATION,
                        "¿Seguro que quieres cerrar sesión?",
                        javafx.scene.control.ButtonType.YES,
                        javafx.scene.control.ButtonType.NO
                );
                alerta.setHeaderText("Cerrar sesión");
                alerta.setTitle("Confirmar");

                java.util.Optional<javafx.scene.control.ButtonType> resultado = alerta.showAndWait();
                if (resultado.isEmpty() || resultado.get() != javafx.scene.control.ButtonType.YES) {
                    return;
                }

                try {
                    trabajadorId = null;
                    nombreTrabajador = null;
                    datos.clear();

                    java.net.URL url = getClass().getResource("/fxml/AccesoVista.fxml");
                    if (url == null) {
                        error("FXML no encontrado", "/fxml/AccesoVista.fxml");
                        return;
                    }

                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(url);
                    javafx.scene.Parent root = loader.load();

                    javafx.stage.Stage stage = (javafx.stage.Stage)
                            ((javafx.scene.Node) e.getSource()).getScene().getWindow();

                    stage.setScene(new javafx.scene.Scene(root));
                    stage.setTitle("Acceso al sistema");
                    stage.show();

                } catch (Exception ex) {
                    error("No se pudo volver al login",
                        ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }




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
