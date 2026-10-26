package utec.secu.ui;

import utec.secu.modelo.Usuario;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
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

    /** Lector de JLine para las contraseñas; se crea la primera vez que se pide una. */
    private static LineReader lectorContrasenias;

    protected MenuBase(Scanner sc, Usuario sesion) {
        this.sc = sc;
        this.sesion = sesion;
    }

    /** Muestra el mensaje y devuelve lo que escribió el usuario. */
    protected String leer(String mensaje) {
        System.out.print(mensaje);
        return sc.nextLine();
    }

    /**
     * Pide una contraseña y muestra asteriscos mientras se escribe (librería JLine). Solo funciona
     * en una terminal real, como CMD o PowerShell al ejecutar el JAR; en la consola Run de IntelliJ
     * no hay terminal y la contraseña se lee como cualquier otro dato.
     */
    protected String leerContrasenia(String mensaje) {
        if (System.console() == null) {
            return leer(mensaje);
        }
        try {
            if (lectorContrasenias == null) {
                lectorContrasenias = LineReaderBuilder.builder()
                        .terminal(TerminalBuilder.builder().system(true).build())
                        .build();
            }
            return lectorContrasenias.readLine(mensaje, '*');
        } catch (IOException e) {
            return leer(mensaje);     // no se pudo abrir la terminal: se lee sin asteriscos
        } catch (UserInterruptException | EndOfFileException e) {
            return "";                // Ctrl+C o fin de entrada: queda vacía y la validación lo informa
        }
    }

    /** Pregunta (s/n) y devuelve {@code true} solo si la respuesta es "s". */
    protected boolean confirmar(String pregunta) {
        return leer(pregunta).trim().equalsIgnoreCase("s");
    }

    /**
     * Pide un número de una lista numerada hasta que sea válido.
     *
     * @return La opción elegida (de 1 a {@code cantidad}), o 0 si el usuario presionó Enter
     *         o la lista está vacía.
     */
    protected int elegir(String mensaje, int cantidad) {
        if (cantidad == 0) {
            return 0;
        }
        while (true) {
            String entrada = leer(mensaje).trim();
            if (entrada.isEmpty()) {
                return 0;
            }
            if (entrada.matches("\\d{1,3}")) {
                int opcion = Integer.parseInt(entrada);
                if (opcion >= 1 && opcion <= cantidad) {
                    return opcion;
                }
            }
            System.out.println("Opción inválida.");
        }
    }
}
