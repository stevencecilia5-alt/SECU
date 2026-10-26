package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.UsuarioService;

import java.util.Scanner;

/**
 * Menú de inicio (CU01): ingresar, recuperar la contraseña y, si se ingresa con una
 * contraseña temporal, definir una nueva antes de continuar.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuInicio extends MenuBase {

    private static final String MSG_SIN_BASE = "No se pudo conectar con la base de datos. ¿Está Docker corriendo?";

    private final UsuarioService servicio;

    public MenuInicio(Scanner sc, UsuarioService servicio) {
        super(sc, null);   // todavía no hay sesión
        this.servicio = servicio;
    }

    /**
     * Muestra el menú de inicio hasta que el usuario intenta ingresar.
     * "Olvidé mi contraseña" vuelve a este mismo menú al terminar.
     *
     * @return El usuario que ingresó, o {@code null} si el ingreso falló.
     */
    public Usuario iniciarSesion() {
        while (true) {
            System.out.println("\n--- Iniciar sesión ---");
            System.out.println("1. Ingresar");
            System.out.println("2. Olvidé mi contraseña");
            switch (leer("Elegí una opción: ").trim()) {
                case "1": return ingresar();
                case "2": recuperarContrasenia(); break;
                default: System.out.println("Opción inválida.");
            }
        }
    }

    private Usuario ingresar() {
        String correo = leer("Correo: ");
        String contrasenia = leerContrasenia("Contraseña: ");
        try {
            Usuario usuario = servicio.autenticar(correo, contrasenia);
            if (usuario == null) {
                System.out.println("Credenciales inválidas o usuario inactivo.");
                return null;
            }
            if (usuario.isRequiereCambioClave()) {
                forzarCambioClave(usuario);
            }
            return usuario;
        } catch (DatoInvalidoException ex) {
            System.out.println(ex.getMessage());
        } catch (ErrorDeDatosException ex) {
            System.out.println(MSG_SIN_BASE);
        }
        return null;
    }

    /** Si el correo es de un usuario activo, su contraseña se reemplaza por una temporal que llega al correo. */
    private void recuperarContrasenia() {
        String correo = leer("Correo electrónico: ");
        try {
            servicio.recuperarContrasenia(correo);
            System.out.println(UsuarioService.MSG_RECUPERACION);
        } catch (OperacionNoPermitidaException ex) {
            System.out.println(ex.getMessage());
        } catch (ErrorDeDatosException ex) {
            System.out.println(MSG_SIN_BASE);
        }
    }

    /** Cambio obligatorio tras ingresar con una contraseña temporal: se repite hasta que sea válida. */
    private void forzarCambioClave(Usuario usuario) {
        System.out.println("\nIngresaste con una contraseña temporal. Debés definir una contraseña nueva para continuar.");
        while (true) {
            try {
                servicio.cambiarContrasenia(usuario, pedirNuevaContraseniaConfirmada());
                usuario.setRequiereCambioClave(false);
                System.out.println("¡Contraseña actualizada con éxito!");
                return;
            } catch (AccesoDenegadoException | DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
            } catch (ErrorDeDatosException ex) {
                System.out.println("Error al actualizar la contraseña: " + ex.getMessage());
            }
        }
    }

    /** Pide la nueva contraseña y su confirmación, validando cada una apenas se ingresa. */
    private String pedirNuevaContraseniaConfirmada() {
        while (true) {
            String nueva = leerContrasenia("Nueva contraseña (mínimo 6 caracteres): ");
            try {
                UsuarioService.validarNuevaContrasenia(nueva);
                UsuarioService.validarConfirmacion(nueva, leerContrasenia("Confirmar contraseña: "));
                return nueva;
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
