package es.tfg.residencias.ui.admin.habitaciones;

import dao.HabitacionDAO;
import dao.HabitacionDAO.Modo; 
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import modelo.HabitacionOcupacionVista;

public class HabitacionesControlador {

    @FXML private TableView<HabitacionOcupacionVista> tabla;
    @FXML private TableColumn<HabitacionOcupacionVista, Integer> colHabId, colResidenteId;
    @FXML private TableColumn<HabitacionOcupacionVista, String>  colNumero, colPlanta, colNombre, colApellidos;

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<Modo> cbModo;
    @FXML private TextField inNumero;
    @FXML private TextField inPlanta;

    private final HabitacionDAO dao = new HabitacionDAO();
    private final ObservableList<HabitacionOcupacionVista> datos = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colHabId.setCellValueFactory(      new PropertyValueFactory<>("habId"));
        colNumero.setCellValueFactory(     new PropertyValueFactory<>("numero"));
        colPlanta.setCellValueFactory(     new PropertyValueFactory<>("planta"));
        colResidenteId.setCellValueFactory(new PropertyValueFactory<>("residenteId"));
        colNombre.setCellValueFactory(     new PropertyValueFactory<>("nombre"));
        colApellidos.setCellValueFactory(  new PropertyValueFactory<>("apellidos"));

        tabla.setItems(datos);

        cbModo.getItems().setAll(Modo.TODAS, Modo.OCUPADAS, Modo.LIBRES);
        cbModo.getSelectionModel().select(Modo.TODAS);
        cbModo.setOnAction(e -> refrescar());

        tabla.setRowFactory(tv -> {
            TableRow<HabitacionOcupacionVista> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) {
                    HabitacionOcupacionVista item = row.getItem();
                    if (item.getResidenteId() != null) {                    }
                }
            });
            return row;
        });

        refrescar();
    }

    @FXML private void refrescar() { cargar(null); }
    @FXML private void buscar()    { cargar(txtBuscar.getText()); }

    @FXML
        private void anadirHabitacion() {
            String numero = (inNumero.getText() == null) ? "" : inNumero.getText().trim();
            String planta = (inPlanta.getText() == null) ? "" : inPlanta.getText().trim();

            if (numero.isBlank()) {
                error("Datos incompletos", "El número de habitación es obligatorio.");
                inNumero.requestFocus();
                return;
            }

            try {
                int id = dao.insertarHabitacion(numero, planta.isBlank() ? null : planta);
                // Limpieza + refresco
                inNumero.clear();
                inPlanta.clear();
                refrescar();

                Alert ok = new Alert(Alert.AlertType.INFORMATION, "Habitación creada (id=" + id + ").");
                ok.setHeaderText("Éxito");
                ok.showAndWait();
                inNumero.requestFocus();
            } catch (Exception e) {
                // Mensaje “bonito” si viola UNIQUE de 'numero'
                String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
                if (msg.contains("unique") || msg.contains("constraint")) {
                    error("Número duplicado", "Ya existe una habitación con ese número.");
                } else {
                    error("Error al crear habitación", e.getMessage());
                }
            }
        }

    private void cargar(String filtro) {
        try {
            Modo modo = (cbModo.getValue() == null) ? Modo.TODAS : cbModo.getValue();
            var lista = dao.listarOcupacionActual(filtro, modo);
            // En tu JavaFX, setAll/addAll aceptan varargs: pasamos un array
            datos.setAll(lista.toArray(new HabitacionOcupacionVista[0]));
        } catch (Exception e) {
            error("Error cargando habitaciones", e.getMessage());
        }
    }

    private void error(String titulo, String detalle) {
        Alert a = new Alert(Alert.AlertType.ERROR, detalle, ButtonType.OK);
        a.setHeaderText(titulo);
        a.showAndWait();
    }
}
