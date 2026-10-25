package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.UsuarioService;

import java.util.List;
import java.util.Scanner;

/**
 * Menú de consola de CU03: Gestión de usuarios.
 * <p>
 * Pide cada dato con {@link #pedirDato(String, String, String)}, que lo valida apenas se
 * ingresa y lo vuelve a pedir si es inválido. Las reglas están en {@link UsuarioService};
 * este menú solo lee y muestra.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 2.0
 */
public class MenuUsuarios extends MenuBase {

    private static final String LISTA_NACIONALIDADES =
            "Nacionalidad: 1) Uruguaya 2) Argentina 3) Brasileña 4) Chilena 5) Paraguaya";
    private static final String LISTA_ROLES = "Rol: 1) Administrador 2) Evaluador externo";
    private static final String LISTA_ESTADOS = "Estado: 1) Activo 2) Inactivo";

    private final UsuarioService servicio;

    /** Cédula del usuario que se está modificando, o null en un alta (para no compararlo consigo mismo). */
    private String ciEnEdicion;

    public MenuUsuarios(Scanner sc, Usuario sesion, UsuarioService servicio) {
        super(sc, sesion);
        this.servicio = servicio;
    }

    /** Muestra el submenú hasta que el usuario elige Volver. */
    public void mostrar() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Gestión de usuarios ---");
            System.out.println("1. Listar usuarios");
            System.out.println("2. Crear usuario");
            System.out.println("3. Modificar usuario");
            System.out.println("4. Restablecer contraseña de un usuario");
            System.out.println("5. Dar de baja usuario");
            System.out.println("6. Reactivar usuario");
            System.out.println("7. Volver");
            String opcion = leer("Elegí una opción: ").trim();

            // Si una opción falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (opcion) {
                    case "1": listar(); break;
                    case "2": crear(); break;
                    case "3": modificar(); break;
                    case "4": restablecer(); break;
                    case "5": darDeBaja(); break;
                    case "6": reactivar(); break;
                    case "7": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void listar() {
        List<Usuario> usuarios = servicio.listarTodosLosUsuarios(sesion);
        if (usuarios.isEmpty()) {
            System.out.println("No hay usuarios registrados.");
            return;
        }
        System.out.printf("%n%-10s %-26s %-32s %-18s %s%n", "Cédula", "Nombre", "Correo", "Rol", "Estado");
        System.out.println("-".repeat(96));
        for (Usuario u : usuarios) {
            System.out.printf("%-10s %-26s %-32s %-18s %s%n", u.getCi(), u.getNombre() + " " + u.getApellido(),
                    u.getCorreo(), u.getRol(), u.getEstado());
        }
    }

    private void crear() {
        System.out.println("\n--- Nuevo usuario ---");
        ciEnEdicion = null;
        String nombre = pedirDato("Nombre: ", "nombre", null);
        String apellido = pedirDato("Apellido: ", "apellido", null);
        String ci = pedirDato("Cédula: ", "cedula", null);
        String correo = pedirDato("Correo: ", "correo", null);
        String telefono = pedirDato("Teléfono (opcional): ", "telefono", null);
        System.out.println(LISTA_NACIONALIDADES);
        String nacionalidad = pedirDato("Elegí una opción: ", "nacionalidad", null);
        String calle = pedirDato("Calle (opcional): ", "calle", null);
        String numero = pedirDato("Número (opcional): ", "numero", null);
        System.out.println(LISTA_ROLES);
        String rol = pedirDato("Elegí una opción: ", "rol", null);
        System.out.println(LISTA_ESTADOS);
        String estado = pedirDato("Elegí una opción: ", "estado", null);

        Usuario nuevo = new Usuario(ci, correo, nombre, apellido, rol, estado, true,
                nacionalidad, calle, numero, telefono);
        boolean seEnvioContrasenia = servicio.crearUsuario(sesion, nuevo);
        if (seEnvioContrasenia) {
            System.out.println("Usuario creado correctamente. Se envió la contraseña temporal al correo registrado.");
        } else {
            System.out.println("Usuario creado correctamente.");
        }
    }

    private void modificar() {
        listar();
        Usuario actual = servicio.obtenerUsuario(sesion, leer("\nCédula: "));
        ciEnEdicion = actual.getCi();
        System.out.println("(Presioná Enter para conservar el valor actual)");

        String nombre = pedirDato("Nombre [" + actual.getNombre() + "]: ", "nombre", actual.getNombre());
        String apellido = pedirDato("Apellido [" + actual.getApellido() + "]: ", "apellido", actual.getApellido());
        String correo = pedirDato("Correo [" + actual.getCorreo() + "]: ", "correo", actual.getCorreo());
        String telefono = pedirDato("Teléfono [" + textoOVacio(actual.getTelefono()) + "]: ",
                "telefono", actual.getTelefono());
        System.out.println(LISTA_NACIONALIDADES);
        String nacionalidad = pedirDato("Nacionalidad [" + actual.getNacionalidad() + "]: ",
                "nacionalidad", actual.getNacionalidad());
        String calle = pedirDato("Calle [" + textoOVacio(actual.getCalle()) + "]: ", "calle", actual.getCalle());
        String numero = pedirDato("Número [" + textoOVacio(actual.getNumDomicilio()) + "]: ",
                "numero", actual.getNumDomicilio());
        System.out.println(LISTA_ROLES);
        String rol = pedirDato("Rol [" + actual.getRol() + "]: ", "rol", actual.getRol());

        if (!confirmar("¿Confirmás los cambios? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        Usuario datos = new Usuario(actual.getCi(), correo, nombre, apellido, rol, actual.getEstado(),
                actual.isRequiereCambioClave(), nacionalidad, calle, numero, telefono);
        servicio.modificarUsuario(sesion, datos);
        System.out.println("Usuario modificado correctamente.");
    }

    private void restablecer() {
        listar();
        Usuario u = servicio.obtenerUsuario(sesion, leer("\nCédula: "));
        UsuarioService.verificarPuedeRestablecer(u);
        servicio.restablecerContrasenia(sesion, u.getCi());
        System.out.println("Contraseña restablecida. Se envió la contraseña temporal al correo registrado.");
    }

    private void darDeBaja() {
        listar();
        Usuario u = servicio.obtenerUsuario(sesion, leer("\nCédula: "));
        UsuarioService.verificarPuedeDarDeBaja(u);
        if (!confirmar("¿Confirmás la baja del usuario? (s/n): ")) {
            System.out.println("Operación cancelada.");
            return;
        }
        servicio.darDeBaja(sesion, u.getCi());
        System.out.println("Usuario dado de baja correctamente.");
    }

    private void reactivar() {
        listar();
        Usuario u = servicio.obtenerUsuario(sesion, leer("\nCédula: "));
        UsuarioService.verificarPuedeReactivar(u);
        if (!confirmar("¿Confirmás la reactivación del usuario? (s/n): ")) {
            System.out.println("Operación cancelada.");
            return;
        }
        servicio.reactivar(sesion, u.getCi());
        System.out.println("Usuario reactivado correctamente. Se envió la contraseña temporal al correo registrado.");
    }

    // =========================================================================
    // PEDIR UN DATO HASTA QUE SEA VÁLIDO
    // =========================================================================

    /**
     * Pide un dato y lo vuelve a pedir hasta que sea válido. Según el campo, usa la
     * validación que corresponde; si el dato es inválido, muestra el mensaje del error
     * (y, si el campo se elige de una lista, vuelve a mostrar la lista).
     *
     * @param mensaje Texto que se muestra al pedir el dato.
     * @param campo   Qué dato es: "nombre", "apellido", "cedula", "correo", "telefono",
     *                "calle", "numero", "nacionalidad", "rol" o "estado".
     * @param actual  Valor actual al modificar (Enter lo conserva), o null en un alta.
     * @return El dato válido.
     */
    private String pedirDato(String mensaje, String campo, String actual) {
        while (true) {
            String valor = leer(mensaje);
            if (actual != null && valor.isBlank()) {
                return actual;                  // al modificar, Enter conserva el valor actual
            }
            try {
                switch (campo) {
                    case "nombre":
                        return UsuarioService.validarNombre(valor);
                    case "apellido":
                        return UsuarioService.validarApellido(valor);
                    case "cedula":
                        String ci = UsuarioService.validarCedula(valor);
                        servicio.verificarCedulaDisponible(sesion, ci);              // que no esté repetida
                        return ci;
                    case "correo":
                        String correo = UsuarioService.validarCorreo(valor);
                        servicio.verificarCorreoDisponible(sesion, correo, ciEnEdicion); // que no esté repetido
                        return correo;
                    case "telefono":
                        return UsuarioService.validarTelefono(valor);
                    case "calle":
                        return UsuarioService.validarCalle(valor);
                    case "numero":
                        return UsuarioService.validarNumero(valor);
                    case "nacionalidad":
                        return UsuarioService.elegirDeLista(valor, UsuarioService.NACIONALIDADES,
                                UsuarioService.MSG_NACIONALIDAD);
                    case "rol":
                        return UsuarioService.elegirDeLista(valor, UsuarioService.ROLES, UsuarioService.MSG_ROL);
                    case "estado":
                        return UsuarioService.elegirDeLista(valor, UsuarioService.ESTADOS, UsuarioService.MSG_ESTADO);
                    default:
                        // Si se escribe mal el nombre del campo, el programa avisa enseguida al probarlo
                        throw new IllegalArgumentException("Campo desconocido: " + campo);
                }
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
                String lista = listaDelCampo(campo);
                if (lista != null) {
                    System.out.println(lista);  // si se eligió mal de una lista, se vuelve a mostrar
                }
            }
        }
    }

    /** @return La lista de opciones del campo, o null si el campo no se elige de una lista. */
    private static String listaDelCampo(String campo) {
        switch (campo) {
            case "nacionalidad": return LISTA_NACIONALIDADES;
            case "rol":          return LISTA_ROLES;
            case "estado":       return LISTA_ESTADOS;
            default:             return null;
        }
    }

    /** Muestra un dato que puede no existir (teléfono, calle) sin escribir "null". */
    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto;
    }
}
