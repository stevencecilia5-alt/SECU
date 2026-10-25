package utec.secu.excepciones;

/**
 * Excepción de negocio lanzada cuando una operación válida en general no se puede
 * realizar por el estado actual de los datos (por ejemplo, cambiar una postulación
 * que ya está en un estado final). Su mensaje es el que se muestra al usuario.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class OperacionNoPermitidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
