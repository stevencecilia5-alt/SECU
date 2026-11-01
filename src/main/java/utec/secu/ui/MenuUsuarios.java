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
    private static final String MSG_ALTA_CANCELADA = "Alta cancelada: no se guardó el usuario.";

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

    /**
     * Alta de un usuario (CU03). Pide todos los datos; Enter en cualquiera cancela el alta. Al final muestra
     * los datos y deja elegir: confirmar (se guarda), editar (los vuelve a pedir, y Enter conserva cada uno)
     * o cancelar.
     */
    private void crear() {
        System.out.println("\n--- Nuevo usuario ---");
        System.out.println("(Presioná Enter en cualquier dato para cancelar el alta)");
        ciEnEdicion = null;
        Usuario nuevo = pedirDatosNuevo(null);
        while (true) {
            imprimirDatos("Datos del nuevo usuario", nuevo);
            int opcion = elegirAlTerminar();
            if (opcion == 1) {
                boolean seEnvioContrasenia = servicio.crearUsuario(sesion, nuevo);
                if (seEnvioContrasenia) {
                    System.out.println("Usuario creado correctamente. Se envió la contraseña temporal al correo registrado.");
                } else {
                    System.out.println("Usuario creado correctamente.");
                }
                return;
            }
            if (opcion == 3) {
                System.out.println(MSG_ALTA_CANCELADA);
                return;
            }
            System.out.println("(Presioná Enter para conservar el valor actual)");
            nuevo = pedirDatosNuevo(nuevo);
        }
    }

    /**
     * Pide los datos de un usuario nuevo. En el primer recorrido ({@code previo} null) Enter cancela el alta;
     * al editar, cada dato muestra el valor ya ingresado y Enter lo conserva.
     */
    private Usuario pedirDatosNuevo(Usuario previo) {
        boolean editando = previo != null;
        String nombre = pedirDato("Nombre", "nombre", editando ? previo.getNombre() : null);
        String apellido = pedirDato("Apellido", "apellido", editando ? previo.getApellido() : null);
        String ci = pedirDato("Cédula", "cedula", editando ? previo.getCi() : null);
        String correo = pedirDato("Correo", "correo", editando ? previo.getCorreo() : null);
        String telefono = pedirDato("Teléfono", "telefono", editando ? previo.getTelefono() : null);
        System.out.println(LISTA_NACIONALIDADES);
        String nacionalidad = pedirDato(editando ? "Nacionalidad" : "Elegí una opción", "nacionalidad",
                editando ? previo.getNacionalidad() : null);
        String calle = pedirDato("Calle", "calle", editando ? previo.getCalle() : null);
        String numero = pedirDato("Número", "numero", editando ? previo.getNumDomicilio() : null);
        System.out.println(LISTA_ROLES);
        String rol = pedirDato(editando ? "Rol" : "Elegí una opción", "rol", editando ? previo.getRol() : null);
        System.out.println(LISTA_ESTADOS);
        String estado = pedirDato(editando ? "Estado" : "Elegí una opción", "estado", editando ? previo.getEstado() : null);
        return new Usuario(ci, correo, nombre, apellido, rol, estado, true, nacionalidad, calle, numero, telefono);
    }

    /**
     * Modificación de un usuario (CU03): Enter conserva cada valor. Al final muestra los datos y deja elegir
     * confirmar, editar o cancelar. La cédula no se modifica y el estado cambia solo con la baja o la reactivación.
     */
    private void modificar() {
        listar();
        Usuario original = servicio.obtenerUsuario(sesion, leer("\nCédula: "));
        ciEnEdicion = original.getCi();
        System.out.println("(Presioná Enter para conservar el valor actual)");
        Usuario datos = pedirDatosModificacion(original, original);
        while (true) {
            imprimirDatos("Datos del usuario", datos);
            int opcion = elegirAlTerminar();
            if (opcion == 1) {
                servicio.modificarUsuario(sesion, datos);
                System.out.println("Usuario modificado correctamente.");
                return;
            }
            if (opcion == 3) {
                System.out.println("Cambios descartados.");
                return;
            }
            System.out.println("(Presioná Enter para conservar el valor actual)");
            datos = pedirDatosModificacion(original, datos);
        }
    }

    /** Pide los datos modificables; cada uno muestra el valor de {@code previo}, que Enter conserva. */
    private Usuario pedirDatosModificacion(Usuario original, Usuario previo) {
        String nombre = pedirDato("Nombre", "nombre", previo.getNombre());
        String apellido = pedirDato("Apellido", "apellido", previo.getApellido());
        String correo = pedirDato("Correo", "correo", previo.getCorreo());
        String telefono = pedirDato("Teléfono", "telefono", textoOVacio(previo.getTelefono()));
        System.out.println(LISTA_NACIONALIDADES);
        String nacionalidad = pedirDato("Nacionalidad", "nacionalidad", previo.getNacionalidad());
        String calle = pedirDato("Calle", "calle", textoOVacio(previo.getCalle()));
        String numero = pedirDato("Número", "numero", textoOVacio(previo.getNumDomicilio()));
        System.out.println(LISTA_ROLES);
        String rol = pedirRol(original, previo.getRol());
        return new Usuario(original.getCi(), correo, nombre, apellido, rol, original.getEstado(),
                original.isRequiereCambioClave(), nacionalidad, calle, numero, telefono);
    }

    /** Muestra los datos del formulario antes de guardarlos. */
    private static void imprimirDatos(String titulo, Usuario u) {
        System.out.println("\n--- " + titulo + " ---");
        System.out.println("Nombre:        " + u.getNombre());
        System.out.println("Apellido:      " + u.getApellido());
        System.out.println("Cédula:        " + u.getCi());
        System.out.println("Correo:        " + u.getCorreo());
        System.out.println("Teléfono:      " + u.getTelefono());
        System.out.println("Nacionalidad:  " + u.getNacionalidad());
        System.out.println("Calle:         " + u.getCalle());
        System.out.println("Número:        " + u.getNumDomicilio());
        System.out.println("Rol:           " + u.getRol());
        System.out.println("Estado:        " + u.getEstado());
    }

    /** @return La opción elegida al terminar un formulario: 1 (confirmar), 2 (editar) o 3 (cancelar). */
    private int elegirAlTerminar() {
        while (true) {
            System.out.println("1. Confirmar");
            System.out.println("2. Editar");
            System.out.println("3. Cancelar");
            int opcion = convertirOpcion(leer("Elegí una opción: ").trim(), 3);
            if (opcion > 0) {
                return opcion;
            }
            System.out.println("Opción inválida.");
        }
    }

    /**
     * Pide el rol nuevo. Si no se puede cambiar (CU03), muestra el motivo y lo vuelve a pedir, sin perder
     * los datos ya ingresados (Mantis #0044248). Con Enter se conserva {@code rolPrevio}; el rol del usuario
     * en la base siempre se permite.
     */
    private String pedirRol(Usuario original, String rolPrevio) {
        while (true) {
            String rol = pedirDato("Rol", "rol", rolPrevio);
            try {
                servicio.verificarCambioDeRol(sesion, original, rol);   // aviso temprano; el servicio vuelve a controlar
                return rol;
            } catch (OperacionNoPermitidaException ex) {
                System.out.println(ex.getMessage());
            }
        }
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
        UsuarioService.verificarPuedeDarDeBaja(sesion, u);
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
     * @param etiqueta Nombre del dato que se muestra al pedirlo, por ejemplo "Nombre".
     * @param campo    Qué dato es: "nombre", "apellido", "cedula", "correo", "telefono",
     *                 "calle", "numero", "nacionalidad", "rol" o "estado".
     * @param actual   Valor actual al editar o modificar (se muestra entre corchetes y Enter lo conserva),
     *                 o null en el primer recorrido del alta, donde Enter cancela el alta.
     * @return El dato válido.
     * @throws OperacionNoPermitidaException Si en el alta se presiona Enter: el menú informa que se canceló.
     */
    private String pedirDato(String etiqueta, String campo, String actual) {
        while (true) {
            String valor = leer(actual == null ? etiqueta + ": " : etiqueta + " [" + actual + "]: ");
            if (valor.isBlank() && actual == null) {
                throw new OperacionNoPermitidaException(MSG_ALTA_CANCELADA);   // el menú muestra el mensaje y vuelve
            }
            if (valor.isBlank() && !actual.isBlank()) {
                return actual;                  // al editar o modificar, Enter conserva el valor actual
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

    /** Muestra un dato que puede faltar en un usuario cargado antes de que fuera obligatorio, sin escribir "null". */
    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto;
    }
}
