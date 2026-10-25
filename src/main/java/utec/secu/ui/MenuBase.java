package utec.secu.ui;

import utec.secu.modelo.Usuario;

import java.util.Scanner;

/**
 * Base común de los menús de consola: guarda el Scanner y el usuario de la sesión,
 * y tiene los métodos para leer datos y pedir confirmaciones.
 * Cada submenú ({@link MenuUsuarios}, {@link MenuPostulaciones}, etc.) la extiende para no repetirlos.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 2.0
 */
public abstract class MenuBase {

    protected final Scanner sc;
    protected final Usuario sesion;

    protected MenuBase(Scanner sc, Usuario sesion) {
        this.sc = sc;
        this.sesion = sesion;
    }

    /** Muestra el mensaje y devuelve lo que escribió el usuario. */
    protected String leer(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine();
    }

    /** Pregunta (s/n) y devuelve {@code true} solo si la respuesta es "s". */
    protected boolean confirmar(String pregunta) {
        return leer(pregunta).trim().equalsIgnoreCase("s");
    }
}
