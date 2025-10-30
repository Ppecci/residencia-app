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
            Usuario u = usuariosDAO.login(usuario, clave);
            if (u == null) {
                errorEtiqueta.setText("Usuario o contraseña incorrectos.");
                return;
            }
            Sesion.setUsuario(u);

            switch (u.getRol()) {
                case "ADMIN" -> Navegacion.cambiar("/fxml/PanelAdmin.fxml");

                case "TRABAJADOR" -> {
                    // Cargamos manualmente el FXML del panel trabajador
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PanelTrabajador.fxml"));
                    Parent root = loader.load();

                    // Accedemos al controlador
                    PanelTrabajadorControlador ctrl = loader.getController();

                    // Pasamos el ID del trabajador
                    if (u.getTrabajadorId() != null) {
                        ctrl.setTrabajadorId(u.getTrabajadorId());

                        // El nombre para el título
                        var daoTrab = new TrabajadoresDAO();
                        String nombre = daoTrab.obtenerNombrePorId(u.getTrabajadorId()).orElse("Desconocido");
                        ctrl.setNombreTrabajador(nombre);
                    }

                    // Mostramos
                    Scene scene = accederBoton.getScene();
                    scene.setRoot(root);
                }

                case "FAMILIAR" -> Navegacion.cambiar("/fxml/PanelFamiliar.fxml");
                default -> errorEtiqueta.setText("Rol no reconocido: " + u.getRol());
            }

        } catch (Exception e) {
            errorEtiqueta.setText("Error en el acceso: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
