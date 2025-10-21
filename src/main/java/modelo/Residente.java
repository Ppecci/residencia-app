package modelo;

public class Residente {
    private Integer id;
    private String nombre;
    private String apellidos;
    private String fechaNacimiento; // yyyy-MM-dd
    private String notas;           // puede ser null
    private Boolean activo;         // 1/0 en BBDD

    public Residente() {}

    public Residente(Integer id, String nombre, String apellidos,
                     String fechaNacimiento, String notas, Boolean activo) {
        this.id = id; this.nombre = nombre; this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento; this.notas = notas; this.activo = activo;
    }

    public Integer getId() { return id; }                   public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }            public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidos() { return apellidos; }      public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getNotas() { return notas; }              public void setNotas(String notas) { this.notas = notas; }
    public Boolean getActivo() { return activo; }           public void setActivo(Boolean activo) { this.activo = activo; }
}