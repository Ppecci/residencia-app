package modelo;

public class Usuario {
    private int id;
    private String username;
    private String rol; // ADMIN | TRABAJADOR | FAMILIAR
    private Integer trabajadorId; // puede ser null
    private Integer familiarId;   // puede ser null
    private boolean activo;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public Integer getTrabajadorId() { return trabajadorId; }
    public void setTrabajadorId(Integer trabajadorId) { this.trabajadorId = trabajadorId; }

    public Integer getFamiliarId() { return familiarId; }
    public void setFamiliarId(Integer familiarId) { this.familiarId = familiarId; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
