package dao;

import bd.ConexionBD;
import java.sql.*;
import java.util.*;
import modelo.Familiar;

public class FamiliarDAO {

    /** Vista para la tabla: familiar asignado a un residente */
    public static class FamiliarAsignado {
        public final int idRelacion;   // id en residente_familiar
        public final int idFamiliar;   // id en familiares
        public final String nombre;
        public final String usuario;
        public final String email;
        public final String parentesco;

        public FamiliarAsignado(int idRelacion, int idFamiliar, String nombre, String usuario,
                                String email, String parentesco) {
            this.idRelacion = idRelacion;
            this.idFamiliar = idFamiliar;
            this.nombre = nombre;
            this.usuario = usuario;
            this.email = email;
            this.parentesco = parentesco;
        }

        // getters para TableView
        public int getIdRelacion() { return idRelacion; }
        public int getIdFamiliar() { return idFamiliar; }
        public String getNombre() { return nombre; }
        public String getUsuario() { return usuario; }
        public String getEmail() { return email; }
        public String getParentesco() { return parentesco; }
    }

    /** Lista SOLO  de los familiares asignados */
    public List<FamiliarAsignado> listarAsignados(int residenteId) throws Exception {
        String sql = """
            SELECT rf.id AS id_rel, f.id AS id_fam, f.nombre, f.usuario, f.email, rf.parentesco
            FROM residente_familiar rf
            JOIN familiares f ON f.id = rf.familiar_id
            WHERE rf.residente_id = ?
            ORDER BY f.nombre
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<FamiliarAsignado> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new FamiliarAsignado(
                        rs.getInt("id_rel"),
                        rs.getInt("id_fam"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email"),
                        rs.getString("parentesco")
                    ));
                }
                return out;
            }
        }
    }

    /** Lista familiares */
    public List<ComboFamiliar> listarNoAsignados(int residenteId) throws Exception {
        String sql = """
            SELECT f.id, f.nombre, f.usuario, f.email
            FROM familiares f
            WHERE NOT EXISTS (
                SELECT 1 FROM residente_familiar rf
                 WHERE rf.residente_id = ? AND rf.familiar_id = f.id
            )
            ORDER BY f.nombre
        """;
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ComboFamiliar> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new ComboFamiliar(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email")
                    ));
                }
                return out;
            }
        }
    }

    /** Familiares existentes */
    public static class ComboFamiliar {
        public final int id;
        public final String nombre;
        public final String usuario;
        public final String email;
        public ComboFamiliar(int id, String nombre, String usuario, String email) {
            this.id = id; this.nombre = nombre; this.usuario = usuario; this.email = email;
        }
        @Override public String toString() {
            String u = (usuario != null && !usuario.isBlank()) ? " · " + usuario : "";
            String e = (email != null && !email.isBlank())     ? " · " + email   : "";
            return nombre + u + e;
        }
    }

    /** Crea un nuevo familiar */
    public int crearFamiliar(String nombre, String usuario, String passwordHash, String email) throws Exception {
        String sql = "INSERT INTO familiares (nombre, usuario, password_hash, email) VALUES (?,?,?,?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setString(2, usuario);
            ps.setString(3, passwordHash);
            ps.setString(4, email);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("No se pudo obtener el id del familiar creado");
            }
        }
    }

    /** Vincula un familiar existente a un residente */
    public void insertarAsignacion(int residenteId, int familiarId, String parentesco) throws Exception {
        String sql = "INSERT INTO residente_familiar (residente_id, familiar_id, parentesco) VALUES (?,?,?)";
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, residenteId);
            ps.setInt(2, familiarId);
            ps.setString(3, parentesco);
            ps.executeUpdate();
        }
    }

    /** Actualiza datos del familiar*/
    public void actualizarAsignado(int idRelacion, int idFamiliar,
                                   String nombre, String usuario, String email, String parentesco) throws Exception {
        try (Connection c = ConexionBD.obtener()) {
            c.setAutoCommit(false);
            try {
                // familiares
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE familiares
                           SET nombre = ?, usuario = ?, email = ?
                         WHERE id = ?
                    """)) {
                    ps.setString(1, nombre);
                    ps.setString(2, usuario);
                    ps.setString(3, email);
                    ps.setInt(4, idFamiliar);
                    ps.executeUpdate();
                }
                // relación
                try (PreparedStatement ps = c.prepareStatement("""
                        UPDATE residente_familiar
                           SET parentesco = ?
                         WHERE id = ?
                    """)) {
                    ps.setString(1, parentesco);
                    ps.setInt(2, idRelacion);
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    /** Elimina */
    public void borrarAsignacion(int idRelacion) throws Exception {
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement("DELETE FROM residente_familiar WHERE id = ?")) {
            ps.setInt(1, idRelacion);
            ps.executeUpdate();
        }
    }
    /* ==================================================================
     * MÉTODOS CRUD PARA EL PANEL DE ADMINISTRACIÓN DE FAMILIARES
     * (Estos son los métodos que te faltaban)
     * ================================================================== */

    /**
     * Lista TODOS los familiares para el panel de admin, con filtro por nombre.
     * @param filtro Texto para buscar por nombre (si es null o vacío, los trae todos)
     * @return Lista de objetos Familiar
     */
    public List<Familiar> listar(String filtro) throws Exception {
        // El modelo Familiar debe tener un constructor que acepte (id, nombre, usuario, email)
        String sql = "SELECT id, nombre, usuario, email FROM familiares";
        
        // Añadir filtro si existe
        if (filtro != null && !filtro.isBlank()) {
            sql += " WHERE nombre LIKE ?";
        }
        sql += " ORDER BY nombre";

        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            if (filtro != null && !filtro.isBlank()) {
                ps.setString(1, "%" + filtro + "%");
            }

            try (ResultSet rs = ps.executeQuery()) {
                List<Familiar> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new Familiar(
                        rs.getInt("id"),
                        rs.getString("nombre"),
                        rs.getString("usuario"),
                        rs.getString("email")
                        // Nota: No se carga el hash de la contraseña por seguridad
                    ));
                }
                return out;
            }
        }
    }

    /**
     * Inserta un nuevo familiar en la base de datos.
     * @param familiar Objeto Familiar con los datos (debe incluir el passwordHashTemporal)
     */
    public void insertar(Familiar familiar) throws Exception {
        // Asume que la tabla tiene 'password_hash' como viste en tu método crearFamiliar
        String sql = "INSERT INTO familiares (nombre, usuario, email, password_hash) VALUES (?, ?, ?, ?)";
        
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, familiar.getNombre());
            ps.setString(2, familiar.getUsuario());
            ps.setString(3, familiar.getEmail());
            // El controlador pone la contraseña (hasheada o no) aquí:
            ps.setString(4, familiar.getPasswordHashTemporal()); 
            
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza los datos básicos (Nombre, Email) de un familiar existente.
     * NO actualiza ni usuario ni contraseña.
     * @param familiar Objeto Familiar con el ID y los nuevos datos
     */
    public void actualizarBasico(Familiar familiar) throws Exception {
        String sql = "UPDATE familiares SET nombre = ?, email = ? WHERE id = ?";
        
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setString(1, familiar.getNombre());
            ps.setString(2, familiar.getEmail());
            ps.setInt(3, familiar.getId());
            
            ps.executeUpdate();
        }
    }

    /**
     * Elimina un familiar de la base de datos usando su ID.
     * @param id ID del familiar a eliminar
     */
    public void eliminar(Integer id) throws Exception {
        String sql = "DELETE FROM familiares WHERE id = ?";
        
        try (Connection c = ConexionBD.obtener();
             PreparedStatement ps = c.prepareStatement(sql)) {
            
            ps.setInt(1, id);
            ps.executeUpdate();
            
            // NOTA: Esto puede fallar si el familiar está asignado a un residente
            // (error de Foreign Key). Tu controlador lo capturará y mostrará el error,
            // lo cual es correcto. La alternativa sería borrar asignaciones primero.
        }
    }
}
