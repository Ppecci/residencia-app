package modelo;

public class Familiar {
    private Integer id;
    private String nombre;
    private String usuario;
    private String email;

    private transient String passwordHashTemporal;

    public Familiar() {}

    public Familiar(Integer id, String nombre, String usuario, String email) {
        this.id = id; this.nombre = nombre; this.usuario = usuario; this.email = email;
    }

    public Integer getId() { return id; }     public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }  public void setNombre(String nombre) { this.nombre = nombre; }
    public String getUsuario() { return usuario; } public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getEmail() { return email; }    public void setEmail(String email) { this.email = email; }

    public String getPasswordHashTemporal() { return passwordHashTemporal; }
    public void setPasswordHashTemporal(String passwordHashTemporal) { this.passwordHashTemporal = passwordHashTemporal; }

    @Override public String toString() {
        String u = (usuario == null || usuario.isBlank()) ? "" : " (" + usuario + ")";
        return (nombre == null ? "" : nombre) + u;
    }
}
