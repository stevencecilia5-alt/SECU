package utec.secu.excepciones;

/**
 * Excepción personalizada de negocio lanzada cuando un usuario intenta realizar
 * una operación sin los permisos requeridos.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class AccesoDenegadoException extends SecurityException {

    private static final long serialVersionUID = 1L;

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}