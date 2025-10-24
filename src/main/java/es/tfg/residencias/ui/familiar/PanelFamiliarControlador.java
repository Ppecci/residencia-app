package es.tfg.residencias.ui.familiar;

import dao.FamiliarDAO;
import modelo.FilaResumenFamiliar;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class PanelFamiliarControlador {

    // Top bar
    @FXML private Label lblFamiliar;
    @FXML private Label lblResidente;   // si hay varios, puedes dejarlo vacío o poner "Todos"
    @FXML private TextField txtBuscar;

    // Tabla
    @FXML private TableView<FilaResumenFamiliar> tabla;
    @FXML private TableColumn<FilaResumenFamiliar, Number> colIdResidente;
    @FXML private TableColumn<FilaResumenFamiliar, String> colNombre;
    @FXML private TableColumn<FilaResumenFamiliar, String> colApellidos;
    @FXML private TableColumn<FilaResumenFamiliar, Number> colIdHabitacion;
    @FXML private TableColumn<FilaResumenFamiliar, String> colNumero;
    @FXML private TableColumn<FilaResumenFamiliar, String> colPlanta;
    @FXML private TableColumn<FilaResumenFamiliar, String> colMedResumen;
    @FXML private TableColumn<FilaResumenFamiliar, String> colDietaNombre;
    @FXML private TableColumn<FilaResumenFamiliar, String> colDietaNotas;
    @FXML private TableColumn<FilaResumenFamiliar, String> colProximaCita;

    // Bottom status
    @FXML private Label lblEstado;

    // Dependencias
    private final FamiliarDAO familiarDAO = new FamiliarDAO();

    // Contexto de sesión (ajusta a tu proyecto)
    private int familiarId;        // lo rellenaremos en initialize()
    private String nombreFamiliar; // para mostrar en el encabezado

    private final ObservableList<FilaResumenFamiliar> datos = FXCollections.observableArrayList();
    private final DateTimeFormatter horaFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    public void initialize() {
        // 1) Cargar info de sesión (ADAPTA a tu clase real de sesión)
        // ----------------------------------------------------------------
        // Ejemplos típicos:
        //   this.familiarId = Sesion.getUsuarioActual().getFamiliarId();
        //   this.nombreFamiliar = Sesion.getUsuarioActual().getNombre();
        //
        // Para que puedas arrancar ya mismo, dejamos valores "dummy"
        // y en cuanto tengas tu clase de sesión, sustituyes estas 2 líneas.
        this.familiarId = obtenerFamiliarIdDesdeSesion();
        this.nombreFamiliar = obtenerNombreFamiliarDesdeSesion();

        lblFamiliar.setText(nombreFamiliar != null ? nombreFamiliar : "(Familiar)");

        // 2) Configurar columnas (solo lectura)
        // ----------------------------------------------------------------
        tabla.setEditable(false);
        tabla.setPlaceholder(new Label("Sin datos para mostrar"));

        colIdResidente.setCellValueFactory(c -> c.getValue().idResidenteProperty());
        colNombre.setCellValueFactory(c -> c.getValue().nombreProperty());
        colApellidos.setCellValueFactory(c -> c.getValue().apellidosProperty());
        colIdHabitacion.setCellValueFactory(c -> c.getValue().idHabitacionProperty());
        colNumero.setCellValueFactory(c -> c.getValue().numeroProperty());
        colPlanta.setCellValueFactory(c -> c.getValue().plantaProperty());
        colMedResumen.setCellValueFactory(c -> c.getValue().medicacionResumenProperty());
        colDietaNombre.setCellValueFactory(c -> c.getValue().dietaNombreProperty());
        colDietaNotas.setCellValueFactory(c -> c.getValue().dietaNotasProperty());
        colProximaCita.setCellValueFactory(c -> c.getValue().proximaCitaProperty());

        tabla.setItems(datos);

        // 3) Cargar datos iniciales
        // ----------------------------------------------------------------
        cargarTabla(null);
    }

    @FXML
    private void buscar() {
        String q = txtBuscar.getText();
        cargarTabla((q != null && !q.isBlank()) ? q.trim() : null);
    }

    @FXML
    private void refrescar() {
        txtBuscar.clear();
        cargarTabla(null);
    }

    @FXML
    private void cerrarSesion() {
        try {
            // Ajusta a tu util de navegación / escena de login:
            // Navegacion.cambiar("/fxml/Acceso.fxml");
            // Si no tienes Navegacion utilitario, deja este TODO y gestiona en tu app.
            System.out.println("Cerrar sesión solicitado (implementa tu navegación a login)");
            setEstado("Sesión cerrada (demo).");
        } catch (Exception ex) {
            ex.printStackTrace();
            setEstado("Error al cerrar sesión.");
        }
    }

    // ----------------------
    // Métodos auxiliares
    // ----------------------

    private void cargarTabla(String filtro) {
        try {
            List<FilaResumenFamiliar> lista = (filtro == null)
                    ? familiarDAO.obtenerResumenPanel(familiarId)
                    :familiarDAO.buscarEnPanel(familiarId, filtro);


            datos.setAll(lista);
            // Si el familiar solo tiene 1 residente, puedes mostrarlo aquí:
            lblResidente.setText(residenteTextoCabecera(lista));
            setEstado("Datos actualizados a las " + LocalDateTime.now().format(horaFmt));
        } catch (Exception ex) {
            ex.printStackTrace();
            setEstado("Error cargando datos.");
        }
    }

    private String residenteTextoCabecera(List<FilaResumenFamiliar> filas) {
        if (filas == null || filas.isEmpty()) return "(Sin residentes asignados)";
        // Si hay varios residentes, puedes dejar “Varios” o vacío
        Integer firstId = filas.get(0).getIdResidente();
        boolean todosMismo = filas.stream().allMatch(f -> Objects.equals(f.getIdResidente(), firstId));
        if (todosMismo) {
            // si todos son del mismo residente, muestra su nombre/apellidos
            var f = filas.get(0);
            return f.getNombre() + " " + f.getApellidos();
        }
        return "(Varios residentes)";
    }

    private void setEstado(String texto) {
        lblEstado.setText(texto != null ? texto : "");
    }

    // ----------------------
    // Simulación de sesión
    // (Sustituye por tu clase real de sesión/login)
    // ----------------------
    private int obtenerFamiliarIdDesdeSesion() {
        // TODO: Sustituir por: Sesion.getUsuarioActual().getFamiliarId()
        return 1; // valor de ejemplo
    }

    private String obtenerNombreFamiliarDesdeSesion() {
        // TODO: Sustituir por: Sesion.getUsuarioActual().getNombre()
        return "Familiar (demo)";
    }
}
