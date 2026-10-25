package utec.secu.seguridad;

import utec.secu.dao.PostulacionDAO;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.FiltroPostulacion;
import utec.secu.modelo.Postulacion;
import utec.secu.modelo.Usuario;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Patrón PROXY para CU07.
 * Intercepta cada llamada a {@link PostulacionDAO} y verifica que el usuario en sesión
 * esté activo y sea Administrador antes de delegar en la base de datos.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class PostulacionProxy extends ProxyBase {

    private final PostulacionDAO postulacionDAO;

    public PostulacionProxy() {
        this.postulacionDAO = new PostulacionDAO();
    }

    public List<Postulacion> buscar(Usuario sesion, FiltroPostulacion filtro, String valor) throws SQLException {
        validarPermisoAdmin(sesion, "consultar postulaciones");
        return postulacionDAO.buscar(filtro, valor);
    }

    public List<Postulacion> buscarPorRango(Usuario sesion, LocalDate inicio, LocalDate fin) throws SQLException {
        validarPermisoAdmin(sesion, "consultar postulaciones");
        return postulacionDAO.buscarPorRango(inicio, fin);
    }

    public List<Postulacion> listarTodas(Usuario sesion) throws SQLException {
        validarPermisoAdmin(sesion, "consultar postulaciones");
        return postulacionDAO.listarTodas();
    }

    public Postulacion buscarPorId(Usuario sesion, long id) throws SQLException {
        validarPermisoAdmin(sesion, "consultar postulaciones");
        return postulacionDAO.buscarPorId(id);
    }

    public Map<Integer, String> listarDepartamentos(Usuario sesion) throws SQLException {
        validarPermisoAdmin(sesion, "modificar postulaciones");
        return postulacionDAO.listarDepartamentos();
    }

    public Map<Integer, String> listarSedes(Usuario sesion) throws SQLException {
        validarPermisoAdmin(sesion, "modificar postulaciones");
        return postulacionDAO.listarSedes();
    }

    public boolean actualizarDatos(Usuario sesion, Postulacion p) throws SQLException {
        validarPermisoAdmin(sesion, "modificar postulaciones");
        return postulacionDAO.actualizarDatos(p);
    }

    public boolean cambiarEstado(Usuario sesion, long id, EstadoPostulacion actual, EstadoPostulacion nuevo)
            throws SQLException {
        validarPermisoAdmin(sesion, "cambiar el estado de postulaciones");
        return postulacionDAO.cambiarEstado(id, actual, nuevo);
    }
}
