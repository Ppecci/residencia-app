package modelo;

import javafx.beans.property.*;

public class FilaResumenFamiliar {

    // Campos (solo lectura en la UI)
    private final IntegerProperty idResidente = new SimpleIntegerProperty();
    private final StringProperty  nombre       = new SimpleStringProperty();
    private final StringProperty  apellidos    = new SimpleStringProperty();
    private final IntegerProperty idHabitacion = new SimpleIntegerProperty();
    private final StringProperty  numero       = new SimpleStringProperty();
    private final StringProperty  planta       = new SimpleStringProperty();
    private final StringProperty  medicacionResumen = new SimpleStringProperty();
    private final StringProperty  dietaNombre       = new SimpleStringProperty();
    private final StringProperty  dietaNotas        = new SimpleStringProperty();
    private final StringProperty  proximaCita       = new SimpleStringProperty();

    public FilaResumenFamiliar(
            int idResidente,
            String nombre,
            String apellidos,
            Integer idHabitacion,   // puede venir null de la BD si no hay habitación vigente
            String numero,
            String planta,
            String medicacionResumen,
            String dietaNombre,
            String dietaNotas,
            String proximaCita
    ) {
        this.idResidente.set(idResidente);
        this.nombre.set(nullToEmpty(nombre));
        this.apellidos.set(nullToEmpty(apellidos));
        this.idHabitacion.set(idHabitacion != null ? idHabitacion : 0);
        this.numero.set(nullToEmpty(numero));
        this.planta.set(nullToEmpty(planta));
        this.medicacionResumen.set(nullToEmpty(medicacionResumen));
        this.dietaNombre.set(nullToEmpty(dietaNombre));
        this.dietaNotas.set(nullToEmpty(dietaNotas));
        this.proximaCita.set(nullToEmpty(proximaCita));
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    // Getters “JavaFX-friendly” (para TableView)
    public IntegerProperty idResidenteProperty() { return idResidente; }
    public StringProperty  nombreProperty() { return nombre; }
    public StringProperty  apellidosProperty() { return apellidos; }
    public IntegerProperty idHabitacionProperty() { return idHabitacion; }
    public StringProperty  numeroProperty() { return numero; }
    public StringProperty  plantaProperty() { return planta; }
    public StringProperty  medicacionResumenProperty() { return medicacionResumen; }
    public StringProperty  dietaNombreProperty() { return dietaNombre; }
    public StringProperty  dietaNotasProperty() { return dietaNotas; }
    public StringProperty  proximaCitaProperty() { return proximaCita; }

    // Getters simples (por comodidad en lógica)
    public Integer getIdResidente() { return idResidente.get(); }
    public String  getNombre() { return nombre.get(); }
    public String  getApellidos() { return apellidos.get(); }
    public Integer getIdHabitacion() { return idHabitacion.get(); }
    public String  getNumero() { return numero.get(); }
    public String  getPlanta() { return planta.get(); }
    public String  getMedicacionResumen() { return medicacionResumen.get(); }
    public String  getDietaNombre() { return dietaNombre.get(); }
    public String  getDietaNotas() { return dietaNotas.get(); }
    public String  getProximaCita() { return proximaCita.get(); }

    // Sin setters para reforzar “solo lectura” en UI.
}
