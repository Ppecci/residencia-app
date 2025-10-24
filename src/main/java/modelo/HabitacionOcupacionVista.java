package modelo;

public class HabitacionOcupacionVista {
    private final Integer habId;
    private final String numero;
    private final String planta;
    private final Integer residenteId;  // null si libre
    private final String nombre;        // null si libre
    private final String apellidos;     // null si libre

    public HabitacionOcupacionVista(Integer habId, String numero, String planta,
                                    Integer residenteId, String nombre, String apellidos) {
        this.habId = habId;
        this.numero = numero;
        this.planta = planta;
        this.residenteId = residenteId;
        this.nombre = nombre;
        this.apellidos = apellidos;
    }

    public Integer getHabId()      { return habId; }
    public String  getNumero()     { return numero; }
    public String  getPlanta()     { return planta; }
    public Integer getResidenteId(){ return residenteId; }
    public String  getNombre()     { return nombre; }
    public String  getApellidos()  { return apellidos; }
}
