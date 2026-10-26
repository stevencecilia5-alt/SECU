package utec.secu.servicio;

import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.FiltroPostulacion;
import utec.secu.modelo.Postulacion;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.PostulacionProxy;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;

/**
 * Patrón FACADE para CU07: Gestión y revisión de postulaciones.
 * <p>
 * Es el único punto de entrada del menú a las postulaciones. Aplica las reglas de
 * negocio (formato y orden de fechas, estados editables, transiciones permitidas,
 * datos válidos) antes de delegar en {@link PostulacionProxy}, que controla el acceso.
 * </p>
 * <p>
 * Si falla la base de datos, cada método atrapa la {@code SQLException} del Proxy y lanza
 * {@link ErrorDeDatosException}: la consola no depende de JDBC.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class PostulacionService {

    public static final String MSG_NO_ENCONTRADA = "No se encontró una postulación con ese identificador.";
    public static final String MSG_RANGO = "El rango de fechas no es válido: la fecha de inicio debe ser "
            + "anterior o igual a la de fin.";
    public static final String MSG_FECHA = "La fecha no es válida: usá el formato dd/mm/aaaa.";
    public static final String MSG_ESTADO_FINAL = "La postulación está en un estado final y no admite cambios.";
    public static final String MSG_NO_MODIFICABLE = "Solo se pueden modificar postulaciones Registradas o En revisión.";

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final PostulacionProxy proxy;

    public PostulacionService() {
        this.proxy = new PostulacionProxy();
    }

    // =========================================================================
    // 1. BÚSQUEDA Y CONSULTA
    // =========================================================================

    /**
     * Busca postulaciones por un filtro de texto (búsqueda parcial, sin distinguir mayúsculas).
     *
     * @throws DatoInvalidoException Si el valor está vacío.
     */
    public List<Postulacion> buscar(Usuario sesion, FiltroPostulacion filtro, String valor) {
        try {
            String texto = valor == null ? "" : valor.trim();
            if (texto.isEmpty()) {
                throw new DatoInvalidoException("Ingresá un valor para buscar.");
            }
            return proxy.buscar(sesion, filtro, texto);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** Busca las postulaciones que están en un estado. */
    public List<Postulacion> buscarPorEstado(Usuario sesion, EstadoPostulacion estado) {
        try {
            return proxy.buscar(sesion, FiltroPostulacion.ESTADO, estado.getValorBD());
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Busca por fecha de solicitud, con ambos extremos incluidos.
     *
     * @throws DatoInvalidoException Si la fecha de inicio es posterior a la de fin.
     */
    public List<Postulacion> buscarPorRango(Usuario sesion, LocalDate inicio, LocalDate fin) {
        try {
            validarRango(inicio, fin);
            return proxy.buscarPorRango(sesion, inicio, fin);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    public List<Postulacion> listarTodas(Usuario sesion) {
        try {
            return proxy.listarTodas(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Obtiene el detalle completo de una postulación.
     *
     * @throws DatoInvalidoException Si no existe una postulación con ese identificador.
     */
    public Postulacion obtenerDetalle(Usuario sesion, long id) {
        try {
            Postulacion p = proxy.buscarPorId(sesion, id);
            if (p == null) {
                throw new DatoInvalidoException(MSG_NO_ENCONTRADA);
            }
            return p;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 2. MODIFICACIÓN DE DATOS
    // =========================================================================

    /**
     * Obtiene una postulación para modificarla.
     *
     * @throws OperacionNoPermitidaException Si no está Registrada ni En revisión.
     */
    public Postulacion obtenerParaModificar(Usuario sesion, long id) {
        Postulacion p = obtenerDetalle(sesion, id);
        if (!p.getEstado().admiteModificarDatos()) {
            throw new OperacionNoPermitidaException(MSG_NO_MODIFICABLE);
        }
        return p;
    }

    public Map<Integer, String> listarDepartamentos(Usuario sesion) {
        try {
            return proxy.listarDepartamentos(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    public Map<Integer, String> listarSedes(Usuario sesion) {
        try {
            return proxy.listarSedes(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Guarda el departamento y la sede de la postulación. Son los únicos datos que el
     * Administrador puede modificar: el resto es la propuesta del postulante (supuesto de CU07).
     *
     * @throws OperacionNoPermitidaException Si la postulación dejó de ser modificable.
     */
    public void guardarCambios(Usuario sesion, Postulacion p) {
        try {
            if (!proxy.actualizarDatos(sesion, p)) {
                throw new OperacionNoPermitidaException(MSG_NO_MODIFICABLE);
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 3. CAMBIO DE ESTADO
    // =========================================================================

    /**
     * Obtiene una postulación para cambiar su estado.
     *
     * @throws OperacionNoPermitidaException Si está en un estado final.
     */
    public Postulacion obtenerParaCambiarEstado(Usuario sesion, long id) {
        Postulacion p = obtenerDetalle(sesion, id);
        if (p.getEstado().esFinal()) {
            throw new OperacionNoPermitidaException(MSG_ESTADO_FINAL);
        }
        return p;
    }

    /**
     * Cambia el estado de una postulación a uno de los permitidos desde su estado actual.
     * "En evaluación" y "Evaluada" nunca se aceptan: los asigna el sistema.
     *
     * @throws OperacionNoPermitidaException Si el estado es final, la transición no está
     *                                       permitida o el estado cambió en otra sesión.
     */
    public void cambiarEstado(Usuario sesion, long id, EstadoPostulacion nuevo) {
        try {
            Postulacion p = obtenerParaCambiarEstado(sesion, id);
            EstadoPostulacion actual = p.getEstado();
            if (!actual.transicionesManuales().contains(nuevo)) {
                throw new OperacionNoPermitidaException("Opción inválida.");
            }
            if (!proxy.cambiarEstado(sesion, id, actual, nuevo)) {
                throw new OperacionNoPermitidaException(
                        "El estado de la postulación cambió mientras se editaba. Volvé a intentarlo.");
            }
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // VALIDACIONES DE DATOS (también las usa el menú al pedir cada dato)
    // =========================================================================

    /** @throws DatoInvalidoException Si el texto no es una fecha válida dd/mm/aaaa. */
    public static LocalDate parsearFecha(String texto) {
        try {
            return LocalDate.parse(texto.trim(), FORMATO_FECHA);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new DatoInvalidoException(MSG_FECHA);
        }
    }

    /** @throws DatoInvalidoException Si la fecha de inicio es posterior a la de fin. */
    public static void validarRango(LocalDate inicio, LocalDate fin) {
        if (inicio.isAfter(fin)) {
            throw new DatoInvalidoException(MSG_RANGO);
        }
    }

    /**
     * Controla que se haya encontrado la postulación al buscar su estado (CU06 y CU08-09).
     *
     * @return El mismo estado.
     * @throws DatoInvalidoException Si el estado es {@code null}: no existe una postulación con ese identificador.
     */
    public static EstadoPostulacion verificarExiste(EstadoPostulacion estado) {
        if (estado == null) {
            throw new DatoInvalidoException(MSG_NO_ENCONTRADA);
        }
        return estado;
    }

    /** @throws DatoInvalidoException Si el texto no es un identificador numérico. */
    public static long parsearId(String texto) {
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException | NullPointerException ex) {
            throw new DatoInvalidoException(MSG_NO_ENCONTRADA);
        }
    }
}
