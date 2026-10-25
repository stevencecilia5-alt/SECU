package utec.secu.excepciones;

/**
 * Excepción de negocio lanzada cuando un dato ingresado no cumple sus reglas
 * (formato, longitud, rango o existencia). Su mensaje es el que se muestra al usuario.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class DatoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DatoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
