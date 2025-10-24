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

    private Integer trabajadorId;        // se inyecta tras el login
    private String  nombreTrabajador;    // opcional, para el título

    @FXML
    public void initialize() {
        // Mapea columnas a getters del DTO
        colResidenteId.setCellValueFactory(new PropertyValueFactory<>("residenteId"));
        colNombre.setCellValueFactory(     new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(  new PropertyValueFactory<>("apellidos"));

        colHabId.setCellValueFactory(      new PropertyValueFactory<>("habId"));
        colHabNumero.setCellValueFactory(  new PropertyValueFactory<>("habNumero"));
        colHabPlanta.setCellValueFactory(  new PropertyValueFactory<>("habPlanta"));

        // IMPORTANTE: usar "medicacionResumen" (el DTO también tiene alias getMedicionResumen())
        colMedicacion.setCellValueFactory( new PropertyValueFactory<>("medicacionResumen"));

        colDietaId.setCellValueFactory(    new PropertyValueFactory<>("dietaId"));
        colDietaNotas.setCellValueFactory( new PropertyValueFactory<>("dietaNotas"));

        colProximaCita.setCellValueFactory(new PropertyValueFactory<>("proximaCita"));

        tabla.setItems(datos);

        // Doble clic: ver detalle (opcional)
        tabla.setRowFactory(tv -> {
            TableRow<TrabajadorResumenFila> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) verDetalle();
            });
            return row;
        });

        // Deshabilita botones de edición si no hay selección
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean sel = (n != null);
            btnEditarPrescripciones.setDisable(!sel);
            btnEditarDieta.setDisable(!sel);
            btnCitas.setDisable(!sel);
        });

        btnEditarPrescripciones.setDisable(true);
        btnEditarDieta.setDisable(true);
        btnCitas.setDisable(true);

        // Si por cualquier razón ya está el trabajadorId seteado antes de initialize, refrescamos.
        if (trabajadorId != null) {
            refrescar();
        }
    }

    /* ==== Métodos de inicialización desde el login ==== */

    /** Llamar tras el login del trabajador. */
   public void setTrabajadorId(int trabajadorId) {
    this.trabajadorId = trabajadorId;
                // Si no nos pasaron el nombre desde el login, lo cargamos de BD
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
    /** (Opcional) Para mostrar el nombre en la barra superior. */
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

    /* ==== Acciones UI ==== */

    @FXML private void refrescar() { cargar(null); }

    @FXML private void buscar()    { cargar(txtBuscar != null ? txtBuscar.getText() : null); }

    private void cargar(String filtro) {
        if (trabajadorId == null) {
            info("Falta identificar al trabajador (setTrabajadorId).");
            return;
        }
        try {
            List<TrabajadorResumenFila> lista = dao.listarAsignados(trabajadorId, filtro);
            // Compatibilidad con JavaFX que solo admite varargs en setAll/addAll:
            datos.setAll(lista.toArray(new TrabajadorResumenFila[0]));
        } catch (Exception e) {
            error("Error cargando datos", e.getMessage());
        }
    }

    @FXML
    private void editarPrescripciones() {
        TrabajadorResumenFila fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) { info("Selecciona un residente."); return; }
        // TODO: abrir diálogo CRUD de prescripciones para fila.getResidenteId()
        info("Abrir diálogo de Prescripciones para residente id=" + fila.getResidenteId());
    }

    @FXML
    private void editarDieta() {
        TrabajadorResumenFila fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) { info("Selecciona un residente."); return; }
        // TODO: abrir diálogo de cambio de dieta (cerrar vigente y abrir nueva)
        info("Abrir diálogo de Dieta para residente id=" + fila.getResidenteId());
    }

    @FXML
    private void gestionarCitas() {
        TrabajadorResumenFila fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) { info("Selecciona un residente."); return; }
        // TODO: abrir diálogo de citas médicas (listado + CRUD)
        info("Abrir diálogo de Citas para residente id=" + fila.getResidenteId());
    }

    @FXML
    private void verDetalle() {
        TrabajadorResumenFila fila = tabla.getSelectionModel().getSelectedItem();
        if (fila == null) { info("Selecciona un residente."); return; }
        // TODO: navegar a una vista de detalle del residente con históricos
        info("Abrir detalle de residente id=" + fila.getResidenteId());
    }

    @FXML
    private void cerrarSesion() {
        // TODO: implementar navegación de vuelta al login/selector
        info("Cerrar sesión (implementar navegación a login).");
    }

    /* ==== Helpers ==== */

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
