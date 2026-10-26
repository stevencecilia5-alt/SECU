package utec.secu.servicio;

import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Criterio;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.Postulacion;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.EvaluacionProxy;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón FACADE para CU08 (asignación de evaluadores) y CU09 (registro de evaluaciones).
 * <p>
 * Aplica las reglas de negocio antes de delegar en {@link EvaluacionProxy}, que controla
 * el acceso: dos evaluadores por postulación, solo Evaluadores externos activos, puntajes
 * enteros de 1 a 5 y una evaluación Completada no se modifica.
 * </p>
 * <p>
 * Si falla la base de datos, cada método atrapa la {@code SQLException} del Proxy y lanza
 * {@link ErrorDeDatosException}: la consola no depende de JDBC.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class EvaluacionService {

    public static final int EVALUADORES_POR_POSTULACION = 2;

    public static final String MSG_DOS_EVALUADORES = "La postulación ya tiene dos evaluadores asignados.";
    public static final String MSG_NO_EN_REVISION = "Solo se pueden asignar evaluadores a una postulación En revisión.";
    public static final String MSG_USUARIO_NO_EXISTE = "No existe un usuario con ese correo.";
    public static final String MSG_NO_EVALUADOR = "El usuario no tiene rol Evaluador externo.";
    public static final String MSG_INACTIVO = "El evaluador no se encuentra activo.";
    public static final String MSG_YA_ASIGNADO = "Ese evaluador ya está asignado a la postulación.";
    public static final String MSG_NO_ASIGNADO = "Ese evaluador no está asignado a la postulación.";
    public static final String MSG_YA_REGISTRO = "No se puede cambiar el evaluador: ya registró su evaluación.";
    public static final String MSG_JUICIO_VACIO = "El juicio global es obligatorio.";
    public static final String MSG_NO_EVALUADA =
            "Solo se puede registrar el juicio global de una postulación ya evaluada (Evaluada, Aprobada o Rechazada).";
    public static final String MSG_COMPLETADA = "La evaluación ya fue completada y no puede modificarse.";
    public static final String MSG_NO_EVALUABLE = "La postulación ya no admite evaluaciones.";
    public static final String MSG_PUNTAJE = "El Puntaje no es válido: debe ser un número entero de 1 a 5.";
    public static final String MSG_FALTAN_CRITERIOS = "No se puede finalizar: faltan criterios por puntuar.";

    private static final String ROL_EVALUADOR = "Evaluador externo";

    private final EvaluacionProxy proxy;

    public EvaluacionService() {
        this.proxy = new EvaluacionProxy();
    }

    // =========================================================================
    // 1. ASIGNACIÓN DE EVALUADORES (CU08)
    // =========================================================================

    /**
     * Asigna un evaluador a una postulación En revisión. La postulación pasa sola a En evaluación
     * cuando tiene sus dos evaluadores y al menos uno empezó su evaluación: al asignar
     * el segundo, si alguno ya empezó, o cuando uno de los dos guarde su evaluación.
     *
     * @return Cantidad de evaluadores asignados después de la operación (1 o 2).
     * @throws DatoInvalidoException         Si la postulación o el usuario no existen, o el usuario no es evaluador.
     * @throws OperacionNoPermitidaException Si ya tiene dos evaluadores, no está En revisión,
     *                                       el evaluador está inactivo o ya está asignado.
     */
    public int asignarEvaluador(Usuario sesion, long idPostulacion, String correo) {
        try {
            EstadoPostulacion estado = obtenerEstado(sesion, idPostulacion);
            List<Evaluacion> asignadas = proxy.listarPorPostulacion(sesion, idPostulacion);
            if (asignadas.size() >= EVALUADORES_POR_POSTULACION) {
                throw new OperacionNoPermitidaException(MSG_DOS_EVALUADORES);
            }
            if (estado != EstadoPostulacion.EN_REVISION) {
                throw new OperacionNoPermitidaException(MSG_NO_EN_REVISION);
            }
            Usuario evaluador = obtenerEvaluador(sesion, correo);
            verificarActivoYNoAsignado(evaluador, asignadas);
            return proxy.asignar(sesion, idPostulacion, evaluador.getCi());
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * @return El estado de la postulación.
     * @throws DatoInvalidoException Si no existe una postulación con ese identificador.
     */
    public EstadoPostulacion obtenerEstado(Usuario sesion, long idPostulacion) {
        try {
            return PostulacionService.verificarExiste(proxy.buscarEstadoPostulacion(sesion, idPostulacion));
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @return Los evaluadores asignados a la postulación, con el estado de cada evaluación. */
    public List<Evaluacion> listarEvaluadores(Usuario sesion, long idPostulacion) {
        try {
            obtenerEstado(sesion, idPostulacion);
            return proxy.listarPorPostulacion(sesion, idPostulacion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @return Las postulaciones asignadas al evaluador con ese correo. */
    public List<Evaluacion> listarPostulacionesDe(Usuario sesion, String correo) {
        try {
            return proxy.listarPorEvaluador(sesion, obtenerEvaluador(sesion, correo).getCi());
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Busca la asignación que se quiere reemplazar.
     *
     * @throws DatoInvalidoException         Si ese evaluador no está asignado a la postulación.
     * @throws OperacionNoPermitidaException Si ya registró su evaluación.
     */
    public Evaluacion obtenerParaReemplazar(Usuario sesion, long idPostulacion, String correoActual) {
        for (Evaluacion e : listarEvaluadores(sesion, idPostulacion)) {
            if (e.getCorreoEvaluador().equalsIgnoreCase(correoActual.trim())) {
                if (e.estaRegistrada()) {
                    throw new OperacionNoPermitidaException(MSG_YA_REGISTRO);
                }
                return e;
            }
        }
        throw new DatoInvalidoException(MSG_NO_ASIGNADO);
    }

    /**
     * Verifica que el nuevo evaluador pueda asignarse a la postulación.
     *
     * @return El nuevo evaluador.
     */
    public Usuario verificarNuevoEvaluador(Usuario sesion, long idPostulacion, String correoNuevo) {
        try {
            Usuario nuevo = obtenerEvaluador(sesion, correoNuevo);
            verificarActivoYNoAsignado(nuevo, proxy.listarPorPostulacion(sesion, idPostulacion));
            return nuevo;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** Reemplaza a un evaluador que todavía no registró su evaluación. */
    public void reemplazarEvaluador(Usuario sesion, long idPostulacion, String correoActual, String correoNuevo) {
        try {
            Evaluacion actual = obtenerParaReemplazar(sesion, idPostulacion, correoActual);
            Usuario nuevo = verificarNuevoEvaluador(sesion, idPostulacion, correoNuevo);
            proxy.reemplazar(sesion, actual.getIdAsignacion(), nuevo.getCi());
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Verifica que la postulación admita el juicio global y devuelve el que ya tiene.
     *
     * @return El juicio global actual, o {@code null} si todavía no tiene.
     * @throws DatoInvalidoException         Si la postulación no existe.
     * @throws OperacionNoPermitidaException Si la postulación todavía no fue evaluada.
     */
    public String obtenerJuicioGlobal(Usuario sesion, long idPostulacion) {
        verificarAdmiteJuicio(sesion, idPostulacion);
        try {
            return proxy.buscarJuicioGlobal(sesion, idPostulacion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Registra el juicio global del Administrador sobre una postulación ya evaluada.
     *
     * @throws DatoInvalidoException         Si el juicio está vacío o la postulación no existe.
     * @throws OperacionNoPermitidaException Si la postulación todavía no fue evaluada.
     */
    public void registrarJuicioGlobal(Usuario sesion, long idPostulacion, String juicio) {
        try {
            String texto = juicio == null ? "" : juicio.trim();
            if (texto.isEmpty()) {
                throw new DatoInvalidoException(MSG_JUICIO_VACIO);
            }
            verificarAdmiteJuicio(sesion, idPostulacion);
            proxy.registrarJuicioGlobal(sesion, idPostulacion, texto);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 2. REGISTRO DE EVALUACIONES (CU09)
    // =========================================================================

    /** @return Las postulaciones asignadas al evaluador en sesión. */
    public List<Evaluacion> listarMisEvaluaciones(Usuario sesion) {
        try {
            return proxy.listarMisAsignaciones(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Obtiene la evaluación del evaluador en sesión para completarla o continuarla.
     *
     * @throws OperacionNoPermitidaException Si ya está Completada o la postulación ya no se evalúa.
     */
    public Evaluacion obtenerParaEvaluar(Usuario sesion, long idPostulacion) {
        Evaluacion e = obtenerMiEvaluacion(sesion, idPostulacion);
        if (e.estaCompletada()) {
            throw new OperacionNoPermitidaException(MSG_COMPLETADA);
        }
        EstadoPostulacion estado = e.getEstadoPostulacion();
        if (estado != EstadoPostulacion.EN_REVISION && estado != EstadoPostulacion.EN_EVALUACION) {
            throw new OperacionNoPermitidaException(MSG_NO_EVALUABLE);
        }
        return e;
    }

    /** @return La evaluación del evaluador en sesión, con los criterios y sus puntajes (solo lectura). */
    public Evaluacion obtenerMiEvaluacion(Usuario sesion, long idPostulacion) {
        try {
            return proxy.buscarMiEvaluacion(sesion, idPostulacion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @return Los datos de una postulación asignada al evaluador en sesión. */
    public Postulacion obtenerPostulacionAsignada(Usuario sesion, long idPostulacion) {
        try {
            return proxy.buscarPostulacionAsignada(sesion, idPostulacion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Guarda la evaluación como borrador: queda En proceso, aunque falten criterios.
     * Si la postulación está En revisión y ya tiene sus dos evaluadores, pasa a En evaluación.
     */
    public void guardarBorrador(Usuario sesion, Evaluacion e) {
        e.setEstado(Evaluacion.EN_PROCESO);
        e.setPuntajeTotal(null);
        guardar(sesion, e);
    }

    /**
     * Finaliza la evaluación y calcula su puntaje total. Si la postulación está En revisión y ya
     * tiene sus dos evaluadores, pasa a En evaluación; con las dos Completadas, pasa a Evaluada.
     *
     * @return {@code true} si, al quedar las dos evaluaciones Completadas, la postulación pasó a Evaluada.
     * @throws OperacionNoPermitidaException Si faltan criterios por puntuar; en ese caso se guarda como borrador.
     */
    public boolean finalizar(Usuario sesion, Evaluacion e) {
        if (!e.todosPuntuados()) {
            guardarBorrador(sesion, e);
            throw new OperacionNoPermitidaException(MSG_FALTAN_CRITERIOS);
        }
        e.setEstado(Evaluacion.COMPLETADA);
        e.setPuntajeTotal(calcularPuntajeTotal(e.getCriterios()));
        return guardar(sesion, e);
    }

    private boolean guardar(Usuario sesion, Evaluacion e) {
        try {
            return proxy.guardarEvaluacion(sesion, e);
        } catch (SQLException ex) {
            throw new ErrorDeDatosException(ex);
        }
    }

    // =========================================================================
    // VALIDACIONES Y CÁLCULOS (también los usa el menú)
    // =========================================================================

    /**
     * @return El puntaje entre 1 y 5, o {@code null} si el texto está vacío (criterio sin puntuar).
     * @throws DatoInvalidoException Si no es un número entero de 1 a 5.
     */
    public static Integer parsearPuntaje(String texto) {
        String valor = texto == null ? "" : texto.trim();
        if (valor.isEmpty()) {
            return null;
        }
        try {
            int puntaje = Integer.parseInt(valor);
            if (puntaje >= 1 && puntaje <= 5) {
                return puntaje;
            }
        } catch (NumberFormatException ex) {
            // "3.5", letras, etc.: se informa abajo con el mismo mensaje
        }
        throw new DatoInvalidoException(MSG_PUNTAJE);
    }

    /**
     * Puntaje total ponderado: el promedio de cada sección multiplicado por su peso
     * (los pesos suman 100%), redondeado a dos decimales. Da un valor entre 1 y 5.
     *
     * @param criterios Los criterios de la rúbrica, todos con puntaje.
     */
    public static double calcularPuntajeTotal(List<Criterio> criterios) {
        Map<String, double[]> secciones = new LinkedHashMap<>();   // sección -> {suma, cantidad, peso}
        for (Criterio c : criterios) {
            double[] datos = secciones.get(c.getSeccion());
            if (datos == null) {
                datos = new double[]{0, 0, c.getPesoSeccion()};
                secciones.put(c.getSeccion(), datos);
            }
            datos[0] += c.getPuntaje();
            datos[1]++;
        }
        double total = 0;
        for (double[] datos : secciones.values()) {
            total += (datos[0] / datos[1]) * datos[2] / 100;
        }
        return Math.round(total * 100) / 100.0;
    }

    /** @return Las secciones de la rúbrica con su peso, en orden (sección -> peso %). */
    public static Map<String, Double> seccionesDe(List<Criterio> criterios) {
        Map<String, Double> secciones = new LinkedHashMap<>();
        for (Criterio c : criterios) {
            secciones.putIfAbsent(c.getSeccion(), c.getPesoSeccion());
        }
        return secciones;
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================

    /** @throws DatoInvalidoException Si no existe un usuario con ese correo o no es Evaluador externo. */
    private Usuario obtenerEvaluador(Usuario sesion, String correo) throws SQLException {
        String texto = correo == null ? "" : correo.trim();
        Usuario u = texto.isEmpty() ? null : proxy.buscarUsuarioPorCorreo(sesion, texto);
        if (u == null) {
            throw new DatoInvalidoException(MSG_USUARIO_NO_EXISTE);
        }
        if (!ROL_EVALUADOR.equalsIgnoreCase(u.getRol())) {
            throw new DatoInvalidoException(MSG_NO_EVALUADOR);
        }
        return u;
    }

    /** @throws OperacionNoPermitidaException Si la postulación no está Evaluada, Aprobada ni Rechazada. */
    private void verificarAdmiteJuicio(Usuario sesion, long idPostulacion) {
        EstadoPostulacion estado = obtenerEstado(sesion, idPostulacion);
        if (estado != EstadoPostulacion.EVALUADA && estado != EstadoPostulacion.APROBADA
                && estado != EstadoPostulacion.RECHAZADA) {
            throw new OperacionNoPermitidaException(MSG_NO_EVALUADA);
        }
    }

    /** @throws OperacionNoPermitidaException Si el evaluador está inactivo o ya está asignado. */
    private static void verificarActivoYNoAsignado(Usuario evaluador, List<Evaluacion> asignadas) {
        if (!"Activo".equalsIgnoreCase(evaluador.getEstado())) {
            throw new OperacionNoPermitidaException(MSG_INACTIVO);
        }
        for (Evaluacion e : asignadas) {
            if (e.getCorreoEvaluador().equalsIgnoreCase(evaluador.getCorreo())) {
                throw new OperacionNoPermitidaException(MSG_YA_ASIGNADO);
            }
        }
    }
}
