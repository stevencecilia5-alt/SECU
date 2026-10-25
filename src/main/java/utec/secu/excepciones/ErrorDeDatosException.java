package utec.secu.excepciones;

/**
 * Excepción lanzada cuando falla una operación con la base de datos: no hay conexión,
 * se cayó el contenedor, falló una sentencia, etc.
 * <p>
 * La lanzan los servicios (las fachadas) al atrapar la {@code SQLException} que sube del
 * Proxy y del DAO. Así la consola no depende de JDBC: solo atrapa esta excepción y muestra
 * su mensaje, que ya está escrito para el usuario. La {@code SQLException} original queda
 * guardada como causa ({@link #getCause()}) por si hace falta ver el detalle técnico.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class ErrorDeDatosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Mensaje que ve el usuario cuando falla la base de datos. */
    public static final String MENSAJE = "No se pudo completar la operación con la base de datos.";

    public ErrorDeDatosException(Throwable causa) {
        super(MENSAJE, causa);
    }
}
