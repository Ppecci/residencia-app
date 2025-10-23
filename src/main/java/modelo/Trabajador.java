package modelo;

public class Trabajador {
    private Integer id;
    private String nombre;
    private String usuario;
    private String email;     // puede ser null
    private Boolean activo;   // 1/0 en BBDD

    // campo temporal SOLO para alta/edición de password (no persistente en objeto)
    private transient String passwordHashTemporal;

    public Trabajador() {}

    public Trabajador(Integer id, String nombre, String usuario, String email, Boolean activo) {
        this.id = id; this.nombre = nombre; this.usuario = usuario; this.email = email; this.activo = activo;
    }

    public Integer getId() { return id; }                 public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }          public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUsuario() { return usuario; }        public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getEmail() { return email; }            public void setEmail(String email) { this.email = email; }
    public Boolean getActivo() { return activo; }         public void setActivo(Boolean activo) { this.activo = activo; }

    public String getPasswordHashTemporal() { return passwordHashTemporal; }
    public void setPasswordHashTemporal(String passwordHashTemporal) { this.passwordHashTemporal = passwordHashTemporal; }
}
