package modelo;

public class Habitacion {
    private Integer id;
    private String numero;
    private String planta;

    public Habitacion(Integer id, String numero, String planta) {
        this.id = id; this.numero = numero; this.planta = planta;
    }

    public Integer getId() { return id; }
    public String getNumero() { return numero; }
    public String getPlanta() { return planta; }

    public void setId(Integer id) { this.id = id; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setPlanta(String planta) { this.planta = planta; }

    @Override public String toString() {
        return numero + (planta != null && !planta.isBlank() ? " · " + planta : "");
    }
}
