package es.tfg.residencias.ui.acceso;

import dao.TrabajadoresDAO;
import dao.UsuariosDAO;
import es.tfg.residencias.ui.trabajador.PanelTrabajadorControlador;
import es.tfg.residencias.ui.util.Navegacion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import modelo.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sesion.Sesion;

public class AccesoControlador {

    private static final Logger log = LoggerFactory.getLogger(AccesoControlador.class);

    @FXML private TextField usuarioCampo;
    @FXML private PasswordField claveCampo;
    @FXML private Button accederBoton;
    @FXML private Label errorEtiqueta;
    @FXML private ImageView logoImagen;

    private final UsuariosDAO usuariosDAO = new UsuariosDAO();

    // Airbag anti doble-disparo
    private boolean loginEnCurso = false;

    @FXML
    public void initialize() {
        try {
            var url = getClass().getResource("/img/logo-png.png");
            if (url == null) {
                log.warn("No se encuentra recurso del logo: /img/logo-png.png (classpath)");
            } else {
                logoImagen.setImage(new javafx.scene.image.Image(url.toExternalForm(), true));
                log.info("Logo cargado desde: {}", url);
            }
        } catch (Throwable t) {
            log.error("Error cargando logo", t);
        }
    }

    @FXML
    private void alAcceder(javafx.event.ActionEvent e) {
        // Diagnóstico de quién dispara el evento
        log.debug("alAcceder() disparado por {}",
            (e != null && e.getSource() != null) ? e.getSource().getClass().getName() : "desconocido");

        // Airbag anti-doble-disparo
        if (loginEnCurso) return;
        loginEnCurso = true;

        try {
            errorEtiqueta.setText("");
            String usuario = usuarioCampo.getText() == null ? "" : usuarioCampo.getText().trim();
            String clave   = claveCampo.getText() == null ? "" : claveCampo.getText();

            log.info("Intento de login usuario='{}'", usuario);

            if (usuario.isEmpty() || clave.isEmpty()) {
                errorEtiqueta.setText("Introduce usuario y contraseña.");
                log.warn("Formulario incompleto (usuario/clave vacíos)");
                return;
            }

            // 1) Buscar usuario por username
            Usuario u = usuariosDAO.buscarPorUsername(usuario);

            // 2) Validaciones
            if (u == null) {
                errorEtiqueta.setText("Usuario o contraseña incorrectos.");
                log.warn("Login fallido usuario='{}' (usuario no encontrado)", usuario);
                return;
            }

            // 3) Verificar bcrypt
            boolean ok = BCrypt.checkpw(clave, u.getPasswordHash());
            if (!ok) {
                errorEtiqueta.setText("Usuario o contraseña incorrectos.");
                log.warn("Login fallido usuario='{}' (credenciales no válidas)", usuario);
                return;
            }

            // 4) Login correcto → sesión + navegación
            Sesion.setUsuario(u);
            log.info("Login OK. usuarioId={} rol={}", u.getId(), u.getRol());

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

        } catch (Exception ex) {
            log.error("Error técnico en login usuario='{}'", usuarioCampo.getText(), ex);
            errorEtiqueta.setText("Error en el acceso: " + ex.getMessage());
        } finally {
            loginEnCurso = false;
        }
    }
}
