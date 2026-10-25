package utec.secu.seguridad;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.modelo.Usuario;

/**
 * Base común de los Proxy de seguridad.
 * Concentra la verificación de sesión activa y de rol para que cada Proxy
 * ({@link SeguridadProxy}, {@link PostulacionProxy}) la reutilice sin duplicarla.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public abstract class ProxyBase {

    /**
     * Verifica que exista un usuario en sesión y que esté activo.
     *
     * @param usuario Usuario de la sesión.
     * @throws AccesoDenegadoException Si no hay sesión o el usuario no está activo.
     */
    protected void validarSesionActiva(Usuario usuario) {
        if (usuario == null) {
            throw new AccesoDenegadoException("Acceso denegado: Debe iniciar sesión previamente.");
        }
        if (!"Activo".equalsIgnoreCase(usuario.getEstado())) {
            throw new AccesoDenegadoException("Acceso denegado: El usuario de sesión no se encuentra activo.");
        }
    }

    /**
     * Verifica que el usuario en sesión esté activo y tenga rol Administrador.
     *
     * @param usuario   Usuario de la sesión.
     * @param operacion Descripción de la operación, para el mensaje de error.
     * @throws AccesoDenegadoException Si no cumple alguna de las dos condiciones.
     */
    protected void validarPermisoAdmin(Usuario usuario, String operacion) {
        validarSesionActiva(usuario);
        if (!esAdmin(usuario)) {
            throw new AccesoDenegadoException("Acceso denegado: Se requieren permisos de Administrador para " + operacion + ".");
        }
    }

    /**
     * @param usuario Usuario a evaluar.
     * @return {@code true} si el usuario tiene rol Administrador.
     */
    protected boolean esAdmin(Usuario usuario) {
        return usuario != null && "Administrador".equalsIgnoreCase(usuario.getRol());
    }
}
