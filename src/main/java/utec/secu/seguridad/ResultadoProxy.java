package utec.secu.seguridad;

import utec.secu.dao.ResultadoDAO;
import utec.secu.modelo.ResultadoPostulacion;
import utec.secu.modelo.Usuario;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Patrón PROXY para CU11.
 * Intercepta cada llamada a {@link ResultadoDAO} y verifica que el usuario en sesión
 * esté activo y sea Administrador antes de delegar en la base de datos.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class ResultadoProxy extends ProxyBase {

    private final ResultadoDAO resultadoDAO;

    public ResultadoProxy() {
        this.resultadoDAO = new ResultadoDAO();
    }

    public Map<Long, String> listarConvocatorias(Usuario sesion) throws SQLException {
        validarPermisoAdmin(sesion, "consultar los resultados de las convocatorias");
        return resultadoDAO.listarConvocatorias();
    }

    public List<ResultadoPostulacion> listarResultados(Usuario sesion, long idConvocatoria) throws SQLException {
        validarPermisoAdmin(sesion, "consultar los resultados de las convocatorias");
        return resultadoDAO.listarResultados(idConvocatoria);
    }

    public void registrarOrdenCes(Usuario sesion, long idConvocatoria, List<ResultadoPostulacion> prelacion)
            throws SQLException {
        validarPermisoAdmin(sesion, "registrar el orden final CES");
        resultadoDAO.registrarOrdenCes(idConvocatoria, prelacion);
    }
}
