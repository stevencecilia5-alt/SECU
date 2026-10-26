package utec.secu.servicio;

import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.SeguridadProxy;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Patrón FACADE para CU01 (acceso) y CU03 (gestión de usuarios).
 * <p>
 * Es el único punto de entrada de la consola a los usuarios. Aplica todas las reglas de
 * negocio (formato de cada dato, duplicados, estados, política de contraseñas) antes de
 * delegar en {@link SeguridadProxy}, y envía por correo simulado las contraseñas
 * temporales, que nunca se muestran en consola.
 * </p>
 * <p>
 * Si falla la base de datos, cada método atrapa la {@code SQLException} del Proxy y lanza
 * {@link ErrorDeDatosException}: la consola no depende de JDBC.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 2.0
 */
public class UsuarioService {

    // ---------- Mensajes (los mismos que esperan los casos de prueba) ----------
    public static final String MSG_OBLIGATORIOS = "El correo y la contraseña son campos obligatorios.";
    public static final String MSG_MINIMO_CLAVE = "La contraseña debe tener al menos 6 caracteres.";
    public static final String MSG_NO_COINCIDEN = "Las contraseñas no coinciden.";
    public static final String MSG_RECUPERACION = "Si el correo corresponde a un usuario activo, se generó "
            + "una nueva contraseña temporal y se envió al correo registrado.";
    public static final String MSG_NOMBRE = "El Nombre no es válido: debe tener entre 2 y 50 caracteres "
            + "y solo letras, espacios, apóstrofes o guiones.";
    public static final String MSG_APELLIDO = "El Apellido no es válido: debe tener entre 2 y 50 caracteres "
            + "y solo letras, espacios, apóstrofes o guiones.";
    public static final String MSG_CEDULA = "La Cédula no es válida: debe tener 7 u 8 dígitos, sin puntos ni "
            + "guion, y un dígito verificador correcto.";
    public static final String MSG_CEDULA_DUPLICADA = "Ya existe un usuario registrado con esa cédula.";
    public static final String MSG_CORREO = "El Correo no es válido.";
    public static final String MSG_CORREO_DUPLICADO = "Ya existe un usuario registrado con ese correo.";
    public static final String MSG_TELEFONO = "El Teléfono no es válido: solo números, con prefijo + opcional, "
            + "entre 6 y 20 dígitos.";
    public static final String MSG_NACIONALIDAD = "La Nacionalidad no es válida: elegí una opción de 1 a 5.";
    public static final String MSG_CALLE = "La Calle no es válida: debe tener entre 2 y 40 caracteres.";
    public static final String MSG_NUMERO = "El Número no es válido: debe tener hasta 8 caracteres.";
    public static final String MSG_ROL = "El Rol no es válido: elegí 1 o 2.";
    public static final String MSG_ESTADO = "El Estado no es válido: elegí 1 o 2.";
    public static final String MSG_NO_EXISTE = "No se encontró un usuario con esa cédula.";

    /** Valores permitidos, en el orden en que se muestran (supuestos documentados según la FAQ). */
    public static final List<String> NACIONALIDADES =
            List.of("Uruguaya", "Argentina", "Brasileña", "Chilena", "Paraguaya");
    public static final List<String> ROLES = List.of("Administrador", "Evaluador externo");
    public static final List<String> ESTADOS = List.of("Activo", "Inactivo");

    private static final Pattern NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ '-]{2,50}$");
    private static final Pattern CEDULA = Pattern.compile("^\\d{7,8}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern TELEFONO = Pattern.compile("^\\+?\\d{6,20}$");
    private static final int[] COEFICIENTES_CI = {2, 9, 8, 7, 6, 3, 4};

    private final SeguridadProxy seguridadProxy;

    public UsuarioService() {
        this(new SeguridadProxy());
    }

    /**
     * Constructor para las pruebas unitarias: recibe el Proxy que se va a usar. Así una prueba
     * le pasa un Proxy simulado con Mockito y prueba las reglas sin base de datos.
     * La aplicación usa el constructor público, que crea el Proxy real.
     */
    UsuarioService(SeguridadProxy seguridadProxy) {
        this.seguridadProxy = seguridadProxy;
    }

    // =========================================================================
    // 1. ACCESO Y CONTRASEÑAS (CU01)
    // =========================================================================

    /**
     * @return El usuario autenticado, o {@code null} si las credenciales no son válidas o está Inactivo.
     * @throws DatoInvalidoException Si falta el correo o la contraseña.
     */
    public Usuario autenticar(String correo, String contrasenia) {
        try {
            if (correo == null || correo.isBlank() || contrasenia == null || contrasenia.isBlank()) {
                throw new DatoInvalidoException(MSG_OBLIGATORIOS);
            }
            return seguridadProxy.autenticar(correo.trim(), contrasenia);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @return {@code true} si la contraseña indicada es la vigente del usuario en sesión. */
    public boolean verificarContraseniaActual(Usuario sesion, String actual) {
        try {
            return actual != null && !actual.isEmpty() && seguridadProxy.autenticar(sesion.getCorreo(), actual) != null;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** Cambia la contraseña del usuario en sesión, aplicando la política de contraseñas. */
    public void cambiarContrasenia(Usuario sesion, String nueva) {
        try {
            validarNuevaContrasenia(nueva);
            seguridadProxy.cambiarContrasenia(sesion, nueva);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Recuperación de contraseña: si el correo es de un usuario Activo, escribe una contraseña
     * temporal sobre la anterior y la envía por correo. Si no lo es, no hace nada: la consola
     * muestra siempre el mismo mensaje, para no revelar qué correos existen.
     */
    public void recuperarContrasenia(String correo) {
        try {
            if (correo == null || correo.isBlank()) {
                return;
            }
            String temporal = seguridadProxy.recuperarContrasenia(correo.trim());
            if (temporal != null) {
                enviarContraseniaTemporal(correo.trim(), temporal, "Pediste recuperar tu contraseña de SECU.");
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 2. GESTIÓN DE USUARIOS (CU03)
    // =========================================================================

    public List<Usuario> listarTodosLosUsuarios(Usuario sesion) {
        try {
            return seguridadProxy.listarTodosLosUsuarios(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * @return El usuario con esa cédula.
     * @throws DatoInvalidoException Si no existe.
     */
    public Usuario obtenerUsuario(Usuario sesion, String ci) {
        try {
            Usuario u = seguridadProxy.buscarPorCi(sesion, ci == null ? "" : ci.trim());
            if (u == null) {
                throw new DatoInvalidoException(MSG_NO_EXISTE);
            }
            return u;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @throws DatoInvalidoException Si ya hay un usuario con esa cédula. */
    public void verificarCedulaDisponible(Usuario sesion, String ci) {
        try {
            if (seguridadProxy.existeCi(sesion, ci)) {
                throw new DatoInvalidoException(MSG_CEDULA_DUPLICADA);
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * @param ciPropia Cédula del usuario que se modifica, o {@code null} en un alta.
     * @throws DatoInvalidoException Si otro usuario ya tiene ese correo.
     */
    public void verificarCorreoDisponible(Usuario sesion, String correo, String ciPropia) {
        try {
            if (seguridadProxy.existeCorreo(sesion, correo, ciPropia)) {
                throw new DatoInvalidoException(MSG_CORREO_DUPLICADO);
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Registra un usuario. Si es Activo, le envía la contraseña temporal por correo.
     *
     * @return {@code true} si se envió la contraseña temporal (usuario Activo).
     */
    public boolean crearUsuario(Usuario sesion, Usuario nuevo) {
        try {
            validarDatosPersonales(nuevo);
            if (!validarCedula(nuevo.getCi()).equals(nuevo.getCi())) {
                throw new DatoInvalidoException(MSG_CEDULA);   // la cédula llega sin espacios ni puntos
            }
            if (!NACIONALIDADES.contains(nuevo.getNacionalidad())) {
                throw new DatoInvalidoException(MSG_NACIONALIDAD);
            }
            if (!ROLES.contains(nuevo.getRol())) {
                throw new DatoInvalidoException(MSG_ROL);
            }
            if (!ESTADOS.contains(nuevo.getEstado())) {
                throw new DatoInvalidoException(MSG_ESTADO);
            }
            verificarCedulaDisponible(sesion, nuevo.getCi());
            verificarCorreoDisponible(sesion, nuevo.getCorreo(), null);

            String temporal = seguridadProxy.crearUsuario(sesion, nuevo);
            if (temporal == null) {
                return false;
            }
            enviarContraseniaTemporal(nuevo.getCorreo(), temporal, "Se creó tu usuario en SECU.");
            return true;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Modifica los datos de un usuario existente. La cédula no se modifica y el estado
     * se cambia solo con la baja o la reactivación.
     */
    public void modificarUsuario(Usuario sesion, Usuario datos) {
        try {
            Usuario actual = obtenerUsuario(sesion, datos.getCi());
            validarDatosPersonales(datos);
            if (!NACIONALIDADES.contains(datos.getNacionalidad())
                    && !datos.getNacionalidad().equals(actual.getNacionalidad())) {
                throw new DatoInvalidoException(MSG_NACIONALIDAD);
            }
            if (!ROLES.contains(datos.getRol())) {
                throw new DatoInvalidoException(MSG_ROL);
            }
            verificarCorreoDisponible(sesion, datos.getCorreo(), datos.getCi());
            if (!seguridadProxy.modificarUsuario(sesion, datos)) {
                throw new DatoInvalidoException(MSG_NO_EXISTE);
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** Genera una contraseña temporal para un usuario Activo y se la envía por correo. */
    public void restablecerContrasenia(Usuario sesion, String ci) {
        try {
            Usuario u = obtenerUsuario(sesion, ci);
            verificarPuedeRestablecer(u);
            String temporal = seguridadProxy.restablecerContrasenia(sesion, u.getCi());
            if (temporal == null) {
                throw new OperacionNoPermitidaException("Solo se puede restablecer la contraseña de un usuario activo.");
            }
            enviarContraseniaTemporal(u.getCorreo(), temporal, "Se restableció tu contraseña de SECU.");
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** Baja lógica: pasa un usuario Activo a Inactivo. */
    public void darDeBaja(Usuario sesion, String ci) {
        try {
            Usuario u = obtenerUsuario(sesion, ci);
            verificarPuedeDarDeBaja(u);
            if (!seguridadProxy.darDeBajaUsuario(sesion, u.getCi())) {
                throw new OperacionNoPermitidaException("Solo se puede dar de baja a un usuario activo.");
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** Pasa un usuario Inactivo a Activo y le envía una contraseña temporal nueva. */
    public void reactivar(Usuario sesion, String ci) {
        try {
            Usuario u = obtenerUsuario(sesion, ci);
            verificarPuedeReactivar(u);
            String temporal = seguridadProxy.reactivarUsuario(sesion, u.getCi());
            if (temporal == null) {
                throw new OperacionNoPermitidaException("Solo se puede reactivar a un usuario inactivo.");
            }
            enviarContraseniaTemporal(u.getCorreo(), temporal, "Se reactivó tu usuario de SECU.");
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @throws OperacionNoPermitidaException Si el usuario no está Activo. */
    public static void verificarPuedeRestablecer(Usuario u) {
        if (!"Activo".equals(u.getEstado())) {
            throw new OperacionNoPermitidaException("Solo se puede restablecer la contraseña de un usuario activo.");
        }
    }

    /** @throws OperacionNoPermitidaException Si el usuario no está Activo. */
    public static void verificarPuedeDarDeBaja(Usuario u) {
        if (!"Activo".equals(u.getEstado())) {
            throw new OperacionNoPermitidaException("Solo se puede dar de baja a un usuario activo.");
        }
    }

    /** @throws OperacionNoPermitidaException Si el usuario no está Inactivo. */
    public static void verificarPuedeReactivar(Usuario u) {
        if (!"Inactivo".equals(u.getEstado())) {
            throw new OperacionNoPermitidaException("Solo se puede reactivar a un usuario inactivo.");
        }
    }

    // =========================================================================
    // VALIDACIONES DE DATOS (también las usa la consola al pedir cada dato)
    // =========================================================================

    public static void validarNuevaContrasenia(String nueva) {
        if (nueva == null || nueva.isBlank() || nueva.length() < 6) {
            throw new DatoInvalidoException(MSG_MINIMO_CLAVE);
        }
    }

    public static void validarConfirmacion(String nueva, String confirmacion) {
        if (!nueva.equals(confirmacion)) {
            throw new DatoInvalidoException(MSG_NO_COINCIDEN);
        }
    }

    public static String validarNombre(String texto) {
        return validar(texto, NOMBRE, MSG_NOMBRE);
    }

    public static String validarApellido(String texto) {
        return validar(texto, NOMBRE, MSG_APELLIDO);
    }

    public static String validarCedula(String texto) {
        String ci = texto == null ? "" : texto.trim();
        if (!CEDULA.matcher(ci).matches() || !digitoVerificadorCorrecto(ci)) {
            throw new DatoInvalidoException(MSG_CEDULA);
        }
        return ci;
    }

    public static String validarCorreo(String texto) {
        String correo = validar(texto, CORREO, MSG_CORREO);
        if (correo.length() > 100) {
            throw new DatoInvalidoException(MSG_CORREO);
        }
        return correo;
    }

    /** @return El teléfono, o cadena vacía si se dejó en blanco (es opcional). */
    public static String validarTelefono(String texto) {
        String tel = texto == null ? "" : texto.trim();
        return tel.isEmpty() ? "" : validar(tel, TELEFONO, MSG_TELEFONO);
    }

    /** @return La calle, o cadena vacía si se dejó en blanco (es opcional). */
    public static String validarCalle(String texto) {
        String calle = texto == null ? "" : texto.trim();
        if (!calle.isEmpty() && (calle.length() < 2 || calle.length() > 40)) {
            throw new DatoInvalidoException(MSG_CALLE);
        }
        return calle;
    }

    /** @return El número de puerta, o cadena vacía si se dejó en blanco (es opcional). */
    public static String validarNumero(String texto) {
        String numero = texto == null ? "" : texto.trim();
        if (numero.length() > 8) {
            throw new DatoInvalidoException(MSG_NUMERO);
        }
        return numero;
    }

    /**
     * Elige un valor de una lista numerada desde 1.
     *
     * @throws DatoInvalidoException Con el mensaje indicado si la opción no existe.
     */
    public static String elegirDeLista(String opcion, List<String> valores, String mensaje) {
        try {
            int n = Integer.parseInt(opcion == null ? "" : opcion.trim());
            if (n >= 1 && n <= valores.size()) {
                return valores.get(n - 1);
            }
        } catch (NumberFormatException ex) {
            // se informa abajo
        }
        throw new DatoInvalidoException(mensaje);
    }

    /**
     * Dígito verificador de la cédula uruguaya: los 7 dígitos (completando con 0 a la
     * izquierda) se multiplican por 2, 9, 8, 7, 6, 3 y 4; el verificador es lo que le falta
     * a la suma para llegar a la siguiente decena.
     */
    private static boolean digitoVerificadorCorrecto(String ci) {
        String ocho = String.format("%8s", ci).replace(' ', '0');
        int suma = 0;
        for (int i = 0; i < 7; i++) {
            suma += (ocho.charAt(i) - '0') * COEFICIENTES_CI[i];
        }
        return (10 - suma % 10) % 10 == ocho.charAt(7) - '0';
    }

    private static String validar(String texto, Pattern patron, String mensaje) {
        String valor = texto == null ? "" : texto.trim();
        if (!patron.matcher(valor).matches()) {
            throw new DatoInvalidoException(mensaje);
        }
        return valor;
    }

    private static void validarDatosPersonales(Usuario u) {
        u.setNombre(validarNombre(u.getNombre()));
        u.setApellido(validarApellido(u.getApellido()));
        u.setCorreo(validarCorreo(u.getCorreo()));
        u.setTelefono(validarTelefono(u.getTelefono()));
        u.setCalle(validarCalle(u.getCalle()));
        u.setNumDomicilio(validarNumero(u.getNumDomicilio()));
    }

    private static void enviarContraseniaTemporal(String correo, String temporal, String motivo) {
        ServicioCorreo.enviar(correo, "SECU - Contraseña temporal",
                motivo + System.lineSeparator()
                + "Tu contraseña temporal es: " + temporal + System.lineSeparator()
                + "Al ingresar vas a tener que definir una contraseña nueva.");
    }
}
