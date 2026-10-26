package utec.secu.seguridad;

import utec.secu.dao.DocumentoDAO;
import utec.secu.dao.PostulacionDAO;
import utec.secu.modelo.DocumentoAdjunto;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.TipoDocumento;
import utec.secu.modelo.Usuario;

import java.sql.SQLException;
import java.util.List;

/**
 * Patrón PROXY para CU06.
 * Intercepta cada llamada a {@link DocumentoDAO} y verifica que el usuario en sesión
 * esté activo y sea Administrador antes de delegar en la base de datos.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class DocumentoProxy extends ProxyBase {

    private static final String OPERACION = "gestionar documentos adjuntos";

    private final DocumentoDAO documentoDAO;
    private final PostulacionDAO postulacionDAO;

    public DocumentoProxy() {
        this.documentoDAO = new DocumentoDAO();
        this.postulacionDAO = new PostulacionDAO();
    }

    public EstadoPostulacion buscarEstadoPostulacion(Usuario sesion, long idPostulacion) throws SQLException {
        validarPermisoAdmin(sesion, OPERACION);
        return postulacionDAO.buscarEstado(idPostulacion);
    }

    public List<TipoDocumento> listarTipos(Usuario sesion, long idPostulacion) throws SQLException {
        validarPermisoAdmin(sesion, OPERACION);
        return documentoDAO.listarTipos(idPostulacion);
    }

    public List<DocumentoAdjunto> listarDocumentos(Usuario sesion, long idPostulacion) throws SQLException {
        validarPermisoAdmin(sesion, OPERACION);
        return documentoDAO.listarDocumentos(idPostulacion);
    }

    public void insertar(Usuario sesion, long idPostulacion, int idDocuTipo, String rutArchivo)
            throws SQLException {
        validarPermisoAdmin(sesion, OPERACION);
        documentoDAO.insertar(idPostulacion, idDocuTipo, rutArchivo);
    }

    public void eliminar(Usuario sesion, long idPostulacion, long idDocuAdjunto) throws SQLException {
        validarPermisoAdmin(sesion, OPERACION);
        documentoDAO.eliminar(idPostulacion, idDocuAdjunto);
    }
}
