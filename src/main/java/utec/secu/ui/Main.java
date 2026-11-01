package utec.secu.ui;

import utec.secu.modelo.Usuario;
import utec.secu.servicio.UsuarioService;

import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Punto de entrada de SECU. Solo coordina el ciclo de la aplicación: el inicio de sesión
 * ({@link MenuInicio}) y, si el ingreso es correcto, el menú principal según el rol ({@link MenuPrincipal}).
 * Un error que nadie previó no cierra la aplicación: se informa y se vuelve al inicio de sesión.
 *
 * @author Grupo 10 - ANGLO Deploy
 */
public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        UsuarioService servicio = new UsuarioService();
        MenuInicio inicio = new MenuInicio(sc, servicio);

        System.out.println("==============================");
        System.out.println("   SECU - Sistema de gestión   ");
        System.out.println("==============================");

        boolean seguirCorriendo = true;
        while (seguirCorriendo) {
            try {
                Usuario usuario = inicio.iniciarSesion();
                if (usuario != null) {
                    new MenuPrincipal(sc, usuario, servicio).mostrar();
                }
                System.out.print("\n¿Volver a la pantalla de inicio de sesión? (s/n): ");
                seguirCorriendo = sc.nextLine().trim().equalsIgnoreCase("s");
            } catch (NoSuchElementException ex) {
                seguirCorriendo = false;                             // se cerró la entrada (Ctrl+Z o Ctrl+D)
            } catch (RuntimeException ex) {
                System.out.println(MenuBase.MSG_ERROR_INESPERADO);   // vuelve al inicio de sesión
            }
        }
        System.out.println("\nHasta luego.");
    }
}
