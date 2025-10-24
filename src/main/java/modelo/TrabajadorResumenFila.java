package modelo;

/**
 * Fila maestra del Panel Trabajador.
 * Solo lectura: se usa para poblar la TableView del trabajador.
 */
public class TrabajadorResumenFila {

    // Residente
    private final Integer residenteId;
    private final String  nombre;
    private final String  apellidos;

    // Habitación vigente
    private final Integer habId;     // puede ser null si no tiene habitación vigente
    private final String  habNumero;
    private final String  habPlanta;

    // Dieta vigente
    private final Integer dietaId;   // si no unes a 'dietas', mostramos el ID
    private final String  dietaNotas;

    // Medicación (resumen de prescripciones activas)
    private final String  medicacionResumen;

    // Próxima cita programada
    private final String  proximaCita; // formato ISO: "YYYY-MM-DD HH:MM" o null

    public TrabajadorResumenFila(
            Integer residenteId,
            String nombre,
            String apellidos,
            Integer habId,
            String habNumero,
            String habPlanta,
            Integer dietaId,
            String dietaNotas,
            String medicacionResumen,
            String proximaCita
    ) {
        this.residenteId = residenteId;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.habId = habId;
        this.habNumero = habNumero;
        this.habPlanta = habPlanta;
        this.dietaId = dietaId;
        this.dietaNotas = dietaNotas;
        this.medicacionResumen = medicacionResumen;
        this.proximaCita = proximaCita;
    }

    // Getters para TableView (PropertyValueFactory usa estos nombres)
    public Integer getResidenteId()       { return residenteId; }
    public String  getNombre()            { return nombre; }
    public String  getApellidos()         { return apellidos; }

    public Integer getHabId()             { return habId; }
    public String  getHabNumero()         { return habNumero; }
    public String  getHabPlanta()         { return habPlanta; }

    public Integer getDietaId()           { return dietaId; }
    public String  getDietaNotas()        { return dietaNotas; }

    public String  getMedicionResumen()   { return medicacionResumen; } // <- ojo typo común: "Medicion"
    public String  getMedicacionResumen() { return medicacionResumen; } // alias correcto para la columna

    public String  getProximaCita()       { return proximaCita; }
}
