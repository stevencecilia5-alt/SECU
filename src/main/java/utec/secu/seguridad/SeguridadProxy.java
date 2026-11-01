package utec.secu.seguridad;

import utec.secu.dao.UsuarioDAO;
import utec.secu.modelo.Usuario;

import java.sql.SQLException;
import java.util.List;

/**
 * Patrón PROXY para CU01 y CU03.
 * <p>
 * Intercepta las llamadas a {@link UsuarioDAO} y verifica sesión activa y rol antes de
 * delegar en la base de datos. Las operaciones previas al inicio de sesión (autenticar y
 * recuperar la contraseña) no exigen sesión.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 3.0
 */
public class SeguridadProxy extends ProxyBase {

    private final UsuarioDAO usuarioDAO;

    public SeguridadProxy() {
        this.usuarioDAO = new UsuarioDAO();
    }

    // =========================================================================
    // 1. AUTENTICACIÓN Y RECUPERACIÓN (sin sesión)
    // =========================================================================

    public Usuario autenticar(String correo, String contrasenia) throws SQLException {
        return usuarioDAO.autenticar(correo, contrasenia);
    }

    public String recuperarContrasenia(String correo) throws SQLException {
        return usuarioDAO.recuperarContrasenia(correo);
    }

    // =========================================================================
    // 2. CAMBIO DE CONTRASEÑA (con sesión)
    // =========================================================================

    /**
     * Comprueba la contraseña vigente del propio usuario en sesión (CU01, cambio de contraseña).
     * Busca por cédula, que no cambia, para que funcione aunque el usuario haya modificado su correo.
     */
    public boolean verificarContrasenia(Usuario usuarioSesion, String contrasenia) throws SQLException {
        validarSesionActiva(usuarioSesion);
        return usuarioDAO.verificarContrasenia(usuarioSesion.getCi(), contrasenia);
    }

    /** Cambia la contraseña del propio usuario en sesión (CU01). Nadie puede cambiar una contraseña ajena. */
    public boolean cambiarContrasenia(Usuario usuarioSesion, String nuevaContrasenia) throws SQLException {
        validarSesionActiva(usuarioSesion);
        return usuarioDAO.cambiarContrasenia(usuarioSesion.getCi(), nuevaContrasenia);
    }

    // =========================================================================
    // 3. GESTIÓN DE USUARIOS (CU03, solo Administrador)
    // =========================================================================

    public List<Usuario> listarTodosLosUsuarios(Usuario usuarioSesion) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "consultar el listado completo de usuarios");
        return usuarioDAO.listarTodos();
    }

    public Usuario buscarPorCi(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "buscar usuarios por cédula");
        return usuarioDAO.buscarPorCi(ci);
    }

    public boolean existeCi(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "crear usuarios");
        return usuarioDAO.existeCi(ci);
    }

    public boolean existeCorreo(Usuario usuarioSesion, String correo, String ciExcluida) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "crear o modificar usuarios");
        return usuarioDAO.existeCorreo(correo, ciExcluida);
    }

    public String crearUsuario(Usuario usuarioSesion, Usuario nuevo) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "crear usuarios");
        return usuarioDAO.crear(nuevo);
    }

    public boolean modificarUsuario(Usuario usuarioSesion, Usuario datos) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "modificar usuarios");
        return usuarioDAO.modificar(datos);
    }

    public boolean tieneAsignaciones(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "modificar usuarios");
        return usuarioDAO.tieneAsignaciones(ci);
    }

    public boolean tieneConvocatorias(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "modificar usuarios");
        return usuarioDAO.tieneConvocatorias(ci);
    }

    public String restablecerContrasenia(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "restablecer contraseñas de usuarios");
        return usuarioDAO.restablecerContrasenia(ci);
    }

    public boolean darDeBajaUsuario(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "dar de baja usuarios");
        return usuarioDAO.inactivar(ci);
    }

    public String reactivarUsuario(Usuario usuarioSesion, String ci) throws SQLException {
        validarPermisoAdmin(usuarioSesion, "reactivar usuarios");
        return usuarioDAO.reactivar(ci);
    }
}
