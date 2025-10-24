package modelo;

public class FamiliarResidenteVista {
    private Integer idRelacion;
    private Integer idResidente;
    private String residente;   // nombre + apellidos
    private String parentesco;  // hijo/a, tutor, etc.

    public Integer getIdRelacion() { return idRelacion; }  public void setIdRelacion(Integer idRelacion) { this.idRelacion = idRelacion; }
    public Integer getIdResidente() { return idResidente; } public void setIdResidente(Integer idResidente) { this.idResidente = idResidente; }
    public String getResidente() { return residente; }     public void setResidente(String residente) { this.residente = residente; }
    public String getParentesco() { return parentesco; }   public void setParentesco(String parentesco) { this.parentesco = parentesco; }
}
