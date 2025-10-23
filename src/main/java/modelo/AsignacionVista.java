package modelo;

public class AsignacionVista {
    private Integer idAsignacion;
    private Integer idResidente;
    private String residente;   // nombre completo
    private String inicio;      // YYYY-MM-DD
    private String fin;         // puede ser null
    private String notas;       // puede ser null

    public AsignacionVista() {}

    public Integer getIdAsignacion() { return idAsignacion; }
    public void setIdAsignacion(Integer idAsignacion) { this.idAsignacion = idAsignacion; }

    public Integer getIdResidente() { return idResidente; }
    public void setIdResidente(Integer idResidente) { this.idResidente = idResidente; }

    public String getResidente() { return residente; }
    public void setResidente(String residente) { this.residente = residente; }

    public String getInicio() { return inicio; }
    public void setInicio(String inicio) { this.inicio = inicio; }

    public String getFin() { return fin; }
    public void setFin(String fin) { this.fin = fin; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
