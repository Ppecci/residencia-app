package modelo;

public class TrabajadorResumenFila {

    // Residente
    private final Integer residenteId;
    private final String  nombre;
    private final String  apellidos;

    // Habitación 
    private final Integer habId;    
    private final String  habNumero;
    private final String  habPlanta;

    // Dieta 
    private final Integer dietaId;   
    private final String  dietaNotas;

    // Medicación 
    private final String  medicacionResumen;

    // Próxima cita 
    private final String  proximaCita; 

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
