package sesion;

import modelo.Usuario;

public class Sesion {
    private static Usuario usuario;

    public static void setUsuario(Usuario u) { usuario = u; }
    public static Usuario getUsuario() { return usuario; }

    public static boolean esAdmin() { return usuario != null && "ADMIN".equals(usuario.getRol()); }
    public static boolean esTrabajador() { return usuario != null && "TRABAJADOR".equals(usuario.getRol()); }
    public static boolean esFamiliar() { return usuario != null && "FAMILIAR".equals(usuario.getRol()); }
}
