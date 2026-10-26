package utec.secu.seguridad;

import utec.secu.dao.EvaluacionDAO;
import utec.secu.dao.PostulacionDAO;
import utec.secu.dao.UsuarioDAO;
import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.Postulacion;
import utec.secu.modelo.Usuario;

import java.sql.SQLException;
import java.util.List;

/**
 * Patrón PROXY para CU08 y CU09.
 * <p>
 * Las operaciones de asignación (CU08) solo las puede hacer un Administrador activo.
 * Las de evaluación (CU09) solo un Evaluador externo activo, y únicamente sobre las
 * postulaciones que tiene asignadas: el Proxy lo comprueba antes de delegar en los DAO.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class EvaluacionProxy extends ProxyBase {

    public static final String MSG_NO_ASIGNADA = "No tenés asignada esa postulación.";
    private static final String ROL_EVALUADOR = "Evaluador externo";

    private final EvaluacionDAO evaluacionDAO;
    private final UsuarioDAO usuarioDAO;
    private final PostulacionDAO postulacionDAO;

    public EvaluacionProxy() {
        this.evaluacionDAO = new EvaluacionDAO();
        this.usuarioDAO = new UsuarioDAO();
        this.postulacionDAO = new PostulacionDAO();
    }

    // =========================================================================
    // 1. ASIGNACIÓN DE EVALUADORES (CU08, solo Administrador)
    // =========================================================================

    public EstadoPostulacion buscarEstadoPostulacion(Usuario sesion, long idPostulacion) throws SQLException {
        validarPermisoAdmin(sesion, "asignar evaluadores");
        return postulacionDAO.buscarEstado(idPostulacion);
    }

    public Usuario buscarUsuarioPorCorreo(Usuario sesion, String correo) throws SQLException {
        validarPermisoAdmin(sesion, "asignar evaluadores");
        return usuarioDAO.buscarPorCorreo(correo);
    }

    public List<Evaluacion> listarPorPostulacion(Usuario sesion, long idPostulacion) throws SQLException {
        validarPermisoAdmin(sesion, "asignar evaluadores");
        return evaluacionDAO.listarPorPostulacion(idPostulacion);
    }

    public List<Evaluacion> listarPorEvaluador(Usuario sesion, String ciEvaluador) throws SQLException {
        validarPermisoAdmin(sesion, "asignar evaluadores");
        return evaluacionDAO.listarPorEvaluador(ciEvaluador);
    }

    public int asignar(Usuario sesion, long idPostulacion, String ciEvaluador) throws SQLException {
        validarPermisoAdmin(sesion, "asignar evaluadores");
        return evaluacionDAO.asignar(idPostulacion, ciEvaluador);
    }

    public void reemplazar(Usuario sesion, long idAsignacion, String ciNuevo) throws SQLException {
        validarPermisoAdmin(sesion, "asignar evaluadores");
        evaluacionDAO.reemplazar(idAsignacion, ciNuevo);
    }

    public String buscarJuicioGlobal(Usuario sesion, long idPostulacion) throws SQLException {
        validarPermisoAdmin(sesion, "registrar el juicio global");
        return evaluacionDAO.buscarJuicioGlobal(idPostulacion);
    }

    public void registrarJuicioGlobal(Usuario sesion, long idPostulacion, String juicio) throws SQLException {
        validarPermisoAdmin(sesion, "registrar el juicio global");
        evaluacionDAO.registrarJuicioGlobal(idPostulacion, juicio);
    }

    // =========================================================================
    // 2. EVALUACIONES (CU09, solo el Evaluador externo asignado)
    // =========================================================================

    public List<Evaluacion> listarMisAsignaciones(Usuario sesion) throws SQLException {
        validarEvaluador(sesion);
        return evaluacionDAO.listarPorEvaluador(sesion.getCi());
    }

    /** @throws AccesoDenegadoException Si el evaluador no tiene asignada la postulación. */
    public Evaluacion buscarMiEvaluacion(Usuario sesion, long idPostulacion) throws SQLException {
        validarEvaluador(sesion);
        Evaluacion e = evaluacionDAO.buscarAsignacion(idPostulacion, sesion.getCi());
        if (e == null) {
            throw new AccesoDenegadoException(MSG_NO_ASIGNADA);
        }
        return e;
    }

    /** @throws AccesoDenegadoException Si el evaluador no tiene asignada la postulación. */
    public Postulacion buscarPostulacionAsignada(Usuario sesion, long idPostulacion) throws SQLException {
        buscarMiEvaluacion(sesion, idPostulacion);
        return postulacionDAO.buscarPorId(idPostulacion);
    }

    /**
     * @return {@code true} si la postulación pasó a Evaluada.
     * @throws AccesoDenegadoException Si la evaluación no es del evaluador en sesión.
     */
    public boolean guardarEvaluacion(Usuario sesion, Evaluacion e) throws SQLException {
        Evaluacion propia = buscarMiEvaluacion(sesion, e.getIdPostulacion());
        if (propia.getIdAsignacion() != e.getIdAsignacion()) {
            throw new AccesoDenegadoException(MSG_NO_ASIGNADA);
        }
        return evaluacionDAO.guardar(e);
    }

    /** @throws AccesoDenegadoException Si el usuario no es un Evaluador externo activo. */
    private void validarEvaluador(Usuario sesion) {
        validarSesionActiva(sesion);
        if (!ROL_EVALUADOR.equalsIgnoreCase(sesion.getRol())) {
            throw new AccesoDenegadoException("Acceso denegado: Solo un Evaluador externo puede registrar evaluaciones.");
        }
    }
}
