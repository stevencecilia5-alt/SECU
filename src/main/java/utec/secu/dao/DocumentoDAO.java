package utec.secu.dao;

import utec.secu.conexion.ConexionBD;
import utec.secu.modelo.DocumentoAdjunto;
import utec.secu.modelo.TipoDocumento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón DAO: documentos adjuntos de las postulaciones (CU06).
 * <p>
 * Lee los tipos de documento de cada convocatoria ({@code docu_tipo_convocatorias}) y
 * escribe en {@code docu_adjuntos}, que guarda solo la ruta de cada archivo PDF.
 * Usa la conexión compartida de {@link ConexionBD}: cierra sentencias y resultados,
 * nunca la conexión.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class DocumentoDAO {

    /** @return Los tipos de documento que pide la convocatoria de la postulación, en el orden previsto. */
    public List<TipoDocumento> listarTipos(long idPostulacion) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "SELECT dt.id_docu_tipo, dt.nom_tip_documento, dtc.obligatorio "
                + "FROM postulaciones p "
                + "JOIN docu_tipo_convocatorias dtc ON dtc.id_convocatoria = p.id_convocatoria "
                + "JOIN docu_tipos dt ON dt.id_docu_tipo = dtc.id_docu_tipo "
                + "WHERE p.id_postulacion = ? ORDER BY dt.id_docu_tipo";
        List<TipoDocumento> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new TipoDocumento(rs.getInt("id_docu_tipo"), rs.getString("nom_tip_documento"),
                            rs.getBoolean("obligatorio")));
                }
            }
        }
        return lista;
    }

    /** @return Los documentos cargados en la postulación, por tipo y en el orden en que se cargaron. */
    public List<DocumentoAdjunto> listarDocumentos(long idPostulacion) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "SELECT da.id_docu_adjunto, da.id_docu_tipo, dt.nom_tip_documento, da.rut_archivo "
                + "FROM docu_adjuntos da JOIN docu_tipos dt ON dt.id_docu_tipo = da.id_docu_tipo "
                + "WHERE da.id_postulacion = ? ORDER BY da.id_docu_tipo, da.id_docu_adjunto";
        List<DocumentoAdjunto> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new DocumentoAdjunto(rs.getLong("id_docu_adjunto"), rs.getInt("id_docu_tipo"),
                            rs.getString("nom_tip_documento"), rs.getString("rut_archivo")));
                }
            }
        }
        return lista;
    }

    /**
     * Guarda la ruta de un documento de la postulación.
     *
     * @throws SQLException Si ocurre un error en la inserción.
     */
    public void insertar(long idPostulacion, int idDocuTipo, String rutArchivo) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "INSERT INTO docu_adjuntos (id_docu_tipo, id_postulacion, rut_archivo) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idDocuTipo);
            ps.setLong(2, idPostulacion);
            ps.setString(3, rutArchivo);
            ps.executeUpdate();
        }
    }

    /**
     * Borra un documento de la postulación.
     *
     * @throws SQLException Si ocurre un error en el borrado.
     */
    public void eliminar(long idPostulacion, long idDocuAdjunto) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "DELETE FROM docu_adjuntos WHERE id_docu_adjunto = ? AND id_postulacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idDocuAdjunto);
            ps.setLong(2, idPostulacion);
            ps.executeUpdate();
        }
    }
}
