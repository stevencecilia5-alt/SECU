package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.UsuarioService;

import java.util.Scanner;

/**
 * Menú principal: muestra las opciones según el rol del usuario en sesión y abre el
 * menú de cada módulo. También permite cambiar la propia contraseña y cerrar sesión.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuPrincipal extends MenuBase {

    private final UsuarioService servicio;

    public MenuPrincipal(Scanner sc, Usuario sesion, UsuarioService servicio) {
        super(sc, sesion);
        this.servicio = servicio;
    }

    /** Muestra el menú hasta que el usuario cierra sesión. */
    public void mostrar() {
        boolean sesionActiva = true;
        while (sesionActiva) {
            System.out.println("\n--- Menú principal ---");
            System.out.println("Bienvenido/a, " + sesion.getNombre() + " " + sesion.getApellido()
                    + " (" + sesion.getRol() + ")");
            sesionActiva = "Administrador".equalsIgnoreCase(sesion.getRol())
                    ? opcionesAdministrador() : opcionesEvaluador();
        }
    }

    /** @return {@code false} si el usuario eligió cerrar sesión. */
    private boolean opcionesAdministrador() {
        System.out.println("1. Gestión de usuarios");
        System.out.println("2. Gestión y revisión de postulaciones");
        System.out.println("3. Asignación de evaluadores (próximamente)");
        System.out.println("4. Cambiar mi contraseña");
        System.out.println("5. Cerrar sesión");
        switch (leer("Elegí una opción: ").trim()) {
            case "1": new MenuUsuarios(sc, sesion, servicio).mostrar(); break;
            case "2": new MenuPostulaciones(sc, sesion).mostrar(); break;
            case "3": System.out.println("Módulo en desarrollo."); break;
            case "4": cambiarMiContrasenia(); break;
            case "5": return false;
            default: System.out.println("Opción inválida.");
        }
        return true;
    }

    /** @return {@code false} si el usuario eligió cerrar sesión. */
    private boolean opcionesEvaluador() {
        System.out.println("1. Mis evaluaciones (próximamente)");
        System.out.println("2. Cambiar mi contraseña");
        System.out.println("3. Cerrar sesión");
        switch (leer("Elegí una opción: ").trim()) {
            case "1": System.out.println("Módulo en desarrollo."); break;
            case "2": cambiarMiContrasenia(); break;
            case "3": return false;
            default: System.out.println("Opción inválida.");
        }
        return true;
    }

    /** Ante cualquier error, informa y vuelve al menú sin cambios. */
    private void cambiarMiContrasenia() {
        System.out.println("\n--- Cambiar mi contraseña ---");
        try {
            if (!servicio.verificarContraseniaActual(sesion, leer("Contraseña actual: "))) {
                System.out.println("La contraseña actual no es correcta.");
                return;
            }
            String nueva = leer("Nueva contraseña (mínimo 6 caracteres): ");
            UsuarioService.validarNuevaContrasenia(nueva);
            UsuarioService.validarConfirmacion(nueva, leer("Confirmar contraseña: "));
            servicio.cambiarContrasenia(sesion, nueva);
            System.out.println("¡Contraseña actualizada con éxito!");
        } catch (AccesoDenegadoException | DatoInvalidoException | ErrorDeDatosException ex) {
            System.out.println(ex.getMessage());
        }
    }
}
