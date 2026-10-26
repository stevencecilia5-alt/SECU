package utec.secu.servicio;

import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.ResultadoPostulacion;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.ResultadoProxy;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Patrón FACADE para CU11 (registro del orden final CES).
 * <p>
 * Arma el orden final CES a partir de la lista de prelación. El Administrador mueve una
 * postulación a otro puesto y las demás se corren; la movida lleva el motivo del CES y las que
 * quedan corridas reciben una observación automática. El CES también puede agregar una
 * observación a cualquier postulación, aunque quede en su orden calculado.
 * </p>
 * <p>
 * Si falla la base de datos, cada método atrapa la {@code SQLException} del Proxy y lanza
 * {@link ErrorDeDatosException}: la consola no depende de JDBC.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class ResultadoService {

    public static final String MSG_SIN_PRELACION =
            "La convocatoria no tiene postulaciones Evaluadas: todavía no hay lista de prelación.";
    public static final String MSG_POSICION = "La posición no es válida: debe ser un número entero de 1 a ";
    public static final String MSG_OBSERVACION =
            "La observación es obligatoria: escribí el motivo de la decisión del CES.";

    private final ResultadoProxy proxy;

    public ResultadoService() {
        this.proxy = new ResultadoProxy();
    }

    // =========================================================================
    // 1. CONSULTAS
    // =========================================================================

    /** @return Las convocatorias (id y nombre con su estado). */
    public Map<Long, String> listarConvocatorias(Usuario sesion) {
        try {
            return proxy.listarConvocatorias(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * @return La lista de prelación: las postulaciones con puntaje consolidado, en el orden calculado (CU11).
     * @throws OperacionNoPermitidaException Si ninguna postulación de la convocatoria está Evaluada.
     */
    public List<ResultadoPostulacion> listarPrelacion(Usuario sesion, long idConvocatoria) {
        try {
            List<ResultadoPostulacion> prelacion = new ArrayList<>();
            for (ResultadoPostulacion r : proxy.listarResultados(sesion, idConvocatoria)) {
                if (r.getPuntajeConsolidado() != null) {
                    prelacion.add(r);
                }
            }
            if (prelacion.isEmpty()) {
                throw new OperacionNoPermitidaException(MSG_SIN_PRELACION);
            }
            return prelacion;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 2. ORDEN FINAL CES (CU11)
    // =========================================================================

    /** @return {@code true} si ya hay un orden final CES registrado para esa lista. */
    public static boolean tieneOrdenCes(List<ResultadoPostulacion> prelacion) {
        for (ResultadoPostulacion r : prelacion) {
            if (r.getOrdenCes() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prepara la lista para registrar un orden CES nuevo: numera el orden calculado de 1 a N
     * (el mismo que SECU guarda al registrar) y borra las posiciones y observaciones CES anteriores.
     */
    public static void prepararRegistro(List<ResultadoPostulacion> prelacion) {
        for (int i = 0; i < prelacion.size(); i++) {
            ResultadoPostulacion r = prelacion.get(i);
            r.setOrdenCalculado(i + 1);
            r.setOrdenCes(null);
            r.setObservacionCes(null);
            r.setNotaCes(null);
        }
    }

    /**
     * @return La observación sin espacios sobrantes.
     * @throws DatoInvalidoException Si está vacía.
     */
    public static String validarObservacion(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new DatoInvalidoException(MSG_OBSERVACION);
        }
        return observacion.trim();
    }

    /**
     * Agrega una observación del CES a una postulación del orden que se está armando, aunque quede
     * en su orden calculado. Si se vuelve a agregar, vale la última.
     *
     * @param orden       Orden que se está armando, del puesto 1 al N.
     * @param puesto      Puesto de la postulación (1 a N).
     * @param observacion Observación del CES.
     * @throws DatoInvalidoException Si el puesto no está entre 1 y N o la observación está vacía.
     */
    public static void anotar(List<ResultadoPostulacion> orden, int puesto, String observacion) {
        String nota = validarObservacion(observacion);
        if (puesto < 1 || puesto > orden.size()) {
            throw new DatoInvalidoException(MSG_POSICION + orden.size() + ".");
        }
        orden.get(puesto - 1).setNotaCes(nota);
    }

    /**
     * Mueve una postulación a otro puesto del orden que se está armando: las que están en el medio
     * se corren un lugar. La postulación movida guarda el motivo escrito por el CES; si se vuelve
     * a mover, vale el último motivo.
     *
     * @param orden        Orden que se está armando, del puesto 1 al N.
     * @param puestoActual Puesto donde está la postulación (1 a N).
     * @param puestoNuevo  Puesto al que se mueve (1 a N).
     * @param observacion  Motivo de la decisión del CES.
     * @throws DatoInvalidoException Si un puesto no está entre 1 y N o la observación está vacía.
     */
    public static void mover(List<ResultadoPostulacion> orden, int puestoActual, int puestoNuevo,
                             String observacion) {
        String motivo = validarObservacion(observacion);
        int cantidad = orden.size();
        if (puestoActual < 1 || puestoActual > cantidad || puestoNuevo < 1 || puestoNuevo > cantidad) {
            throw new DatoInvalidoException(MSG_POSICION + cantidad + ".");
        }
        ResultadoPostulacion movida = orden.remove(puestoActual - 1);
        orden.add(puestoNuevo - 1, movida);
        movida.setObservacionCes(motivo);
    }

    /**
     * Cierra el orden armado antes de confirmarlo: le da a cada postulación su posición final CES
     * (de 1 a N) y completa las observaciones. La que quedó en su orden calculado no lleva
     * observación; la que movió el CES conserva el motivo escrito; la que quedó corrida por esos
     * movimientos recibe una observación automática que nombra las decisiones del CES. La observación
     * que agregó el CES ({@link #anotar}) se suma a la que corresponda, o queda sola si no hay otra.
     */
    public static void cerrarOrdenCes(List<ResultadoPostulacion> orden) {
        // 1) Posición final de cada una y decisiones del CES que cambiaron el orden
        StringBuilder decisiones = new StringBuilder();
        for (int i = 0; i < orden.size(); i++) {
            ResultadoPostulacion r = orden.get(i);
            r.setOrdenCes(i + 1);
            if (r.getOrdenCes().equals(r.getOrdenCalculado())) {
                r.setObservacionCes(null);
            } else if (r.getObservacionCes() != null) {
                if (decisiones.length() > 0) {
                    decisiones.append(", ");
                }
                decisiones.append("postulación ").append(r.getIdPostulacion()).append(" al puesto ").append(i + 1);
            }
        }
        // 2) Observación automática para las que quedaron corridas
        for (ResultadoPostulacion r : orden) {
            if (r.getObservacionCes() == null && !r.getOrdenCes().equals(r.getOrdenCalculado())) {
                r.setObservacionCes("Quedó en el puesto " + r.getOrdenCes() + " (orden calculado: "
                        + r.getOrdenCalculado() + ") por las decisiones del CES: " + decisiones + ".");
            }
        }
        // 3) Observación que agregó el CES: se suma a la que ya tenga
        for (ResultadoPostulacion r : orden) {
            if (r.getNotaCes() != null) {
                r.setObservacionCes(r.getObservacionCes() == null ? r.getNotaCes()
                        : r.getObservacionCes() + " / " + r.getNotaCes());
            }
        }
    }

    /**
     * Registra el orden final CES de la convocatoria, ya cerrado con {@link #cerrarOrdenCes}.
     * El orden calculado por SECU se guarda aparte y no se sobrescribe.
     */
    public void registrarOrdenCes(Usuario sesion, long idConvocatoria, List<ResultadoPostulacion> orden) {
        try {
            proxy.registrarOrdenCes(sesion, idConvocatoria, orden);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }
}
