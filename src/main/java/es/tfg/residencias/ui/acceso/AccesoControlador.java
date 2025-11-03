package es.tfg.residencias.ui.acceso;

import dao.UsuariosDAO;
import dao.TrabajadoresDAO; 
import es.tfg.residencias.ui.trabajador.PanelTrabajadorControlador;
import es.tfg.residencias.ui.util.Navegacion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import modelo.Usuario;
import sesion.Sesion;
import org.mindrot.jbcrypt.BCrypt;

public class AccesoControlador {

    @FXML private TextField usuarioCampo;
    @FXML private PasswordField claveCampo;
    @FXML private Button accederBoton;
    @FXML private Label errorEtiqueta;

    private final UsuariosDAO usuariosDAO = new UsuariosDAO();

    @FXML
            private void alAcceder() {
                errorEtiqueta.setText("");
                String usuario = usuarioCampo.getText() == null ? "" : usuarioCampo.getText().trim();
                String clave = claveCampo.getText() == null ? "" : claveCampo.getText();

                if (usuario.isEmpty() || clave.isEmpty()) {
                    errorEtiqueta.setText("Introduce usuario y contraseña.");
                    return;
                }

                try {
            // 1) Traer usuario activo por username
            Usuario u = usuariosDAO.buscarPorUsername(usuario); // ← la DAO ya no recibe password

            // 2) Validaciones
            if (u == null) {
                errorEtiqueta.setText("Usuario o contraseña incorrectos.");
                return;
            }
            // 3) Verificar bcrypt: clave tecleada vs hash guardado
            boolean ok = BCrypt.checkpw(clave, u.getPasswordHash());
            if (!ok) {
                errorEtiqueta.setText("Usuario o contraseña incorrectos.");
                return;
            }

            // 4) Login correcto → guardar sesión y navegar
            Sesion.setUsuario(u);

            switch (u.getRol()) {
                case "ADMIN" -> {
                    Navegacion.cambiar("/fxml/PanelAdmin.fxml");
                    Navegacion.maximizar();
                }
                case "TRABAJADOR" -> {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PanelTrabajador.fxml"));
                    Parent root = loader.load();

                    PanelTrabajadorControlador ctrl = loader.getController();

                    if (u.getTrabajadorId() != null) {
                        var daoTrab = new TrabajadoresDAO();
                        ctrl.setTrabajadorId(u.getTrabajadorId());
                        String nombre = daoTrab.obtenerNombrePorId(u.getTrabajadorId()).orElse("Desconocido");
                        ctrl.setNombreTrabajador(nombre);
                    }

                    Scene scene = accederBoton.getScene();
                    scene.setRoot(root);
                    Navegacion.maximizar();
                }
                case "FAMILIAR" -> {
                    Navegacion.cambiar("/fxml/PanelFamiliar.fxml");
                    Navegacion.maximizar();
                }
                default -> errorEtiqueta.setText("Rol no reconocido: " + u.getRol());
            }

        } catch (Exception e) {
            errorEtiqueta.setText("Error en el acceso: " + e.getMessage());
            e.printStackTrace();
        }
}
}
