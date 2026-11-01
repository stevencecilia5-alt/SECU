package utec.secu.dao;

import utec.secu.conexion.ConexionBD;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.ResultadoPostulacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón DAO: resultados de las convocatorias (CU11 y CU12, y el cálculo de CU10 que necesitan).
 * <p>
 * Lee la situación de cada postulación con sus evaluaciones y escribe en {@code postulaciones}
 * el puntaje consolidado, el orden calculado por SECU ({@code ord_pre_calculado}) y el orden
 * final CES ({@code ord_final_ces} y {@code obs_ces}). Usa la conexión compartida de
 * {@link ConexionBD}: cierra sentencias y resultados, nunca la conexión.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class ResultadoDAO {

    /**
     * Orden de prelación (CU10): de mayor a menor puntaje consolidado; si empatan, alfabético por
     * nombre del postulante. Las postulaciones sin puntaje van al final.
     */
    private static final String ORDEN_PRELACION =
            " ORDER BY p.pun_consolidado DESC NULLS LAST, pt.nombre, pt.apellido, p.id_postulacion";
    /** Orden de la vista consolidada (CU12): el de prelación, con las canceladas al final. */
    private static final String ORDEN_VISTA =
            " ORDER BY p.estado = ?, p.pun_consolidado DESC NULLS LAST, pt.nombre, pt.apellido, p.id_postulacion";

    /** @return Las convocatorias (id y nombre con su estado), de la más reciente a la más antigua. */
    public Map<Long, String> listarConvocatorias() throws SQLException {
        Map<Long, String> mapa = new LinkedHashMap<>();
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "SELECT id_convocatoria, nom_convocatoria, estado FROM convocatorias ORDER BY fec_apertura DESC";
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getLong("id_convocatoria"), rs.getString("nom_convocatoria")
                        + " (" + rs.getString("estado") + ")");
            }
        }
        return mapa;
    }

    /**
     * Lista todas las postulaciones de una convocatoria con sus evaluaciones, ordenadas por
     * puntaje consolidado (orden de prelación); las canceladas van al final.
     */
    public List<ResultadoPostulacion> listarResultados(long idConvocatoria) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "SELECT p.id_postulacion, p.fec_solicitud, p.nom_proyecto, p.gru_investigacion, "
                + "p.otr_gru_investigacion, p.estado, "
                + "p.pun_consolidado, p.ord_pre_calculado, p.ord_final_ces, p.obs_ces, p.jui_global, "
                + "pt.nombre, pt.apellido, pt.correo, t.nombre AS tutor_nombre, t.apellido AS tutor_apellido, "
                + "ct.nombre AS cotutor_nombre, ct.apellido AS cotutor_apellido "
                + "FROM postulaciones p "
                + "JOIN postulantes pt ON pt.id_postulante = p.id_postulante "
                + "LEFT JOIN tuto_utec_postulacion tp ON tp.id_postulacion = p.id_postulacion "
                + "AND tp.rol_tutor = 'Tutor principal' "
                + "LEFT JOIN tuto_utec t ON t.id_tuto_utec = tp.id_tuto_utec "
                + "LEFT JOIN tuto_utec_postulacion cp ON cp.id_postulacion = p.id_postulacion "
                + "AND cp.rol_tutor = 'Co-tutor' "
                + "LEFT JOIN tuto_utec ct ON ct.id_tuto_utec = cp.id_tuto_utec "
                + "WHERE p.id_convocatoria = ?" + ORDEN_VISTA;
        Map<Long, ResultadoPostulacion> resultados = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idConvocatoria);
            ps.setString(2, EstadoPostulacion.CANCELADA.getValorBD());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResultadoPostulacion r = mapear(rs);
                    resultados.put(r.getIdPostulacion(), r);
                }
            }
        }
        for (Evaluacion e : new EvaluacionDAO().listarPorConvocatoria(idConvocatoria)) {
            resultados.get(e.getIdPostulacion()).agregarEvaluacion(e);
        }
        return new ArrayList<>(resultados.values());
    }

    /**
     * Registra el orden final CES de toda la lista de prelación, en una sola transacción:
     * borra el orden CES anterior, vuelve a calcular el orden de SECU y guarda la posición
     * final y la observación de cada postulación.
     *
     * @param prelacion Postulaciones de la lista de prelación, con su posición final CES y su observación.
     */
    public void registrarOrdenCes(long idConvocatoria, List<ResultadoPostulacion> prelacion) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        con.setAutoCommit(false);      // empieza la transacción: nada se guarda todavía
        try {
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE postulaciones SET ord_final_ces = NULL, obs_ces = NULL WHERE id_convocatoria = ?")) {
                ps.setLong(1, idConvocatoria);
                ps.executeUpdate();
            }
            recalcularOrden(con, idConvocatoria);
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE postulaciones SET ord_final_ces = ?, obs_ces = ? WHERE id_postulacion = ?")) {
                for (ResultadoPostulacion r : prelacion) {
                    ps.setInt(1, r.getOrdenCes());
                    ps.setString(2, r.getObservacionCes());
                    ps.setLong(3, r.getIdPostulacion());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            con.commit();              // todo salió bien: se guarda todo junto
        } catch (SQLException ex) {
            con.rollback();            // algo falló: se deshace todo
            throw ex;
        } finally {
            con.setAutoCommit(true);   // la conexión vuelve a guardar cada consulta sola
        }
    }

    /**
     * Cálculo automático de CU10 cuando una postulación pasa a Evaluada: guarda su puntaje
     * consolidado (promedio de las dos evaluaciones) y vuelve a calcular el orden de prelación
     * de su convocatoria. Si la convocatoria ya tiene registrado el orden final CES, el orden
     * calculado no se toca: la postulación nueva queda sin orden hasta que se registre de nuevo
     * el orden CES (CU11).
     * <p>
     * La llama {@link EvaluacionDAO#guardar} dentro de su transacción, con la misma conexión.
     * </p>
     */
    static void consolidar(Connection con, long idPostulacion) throws SQLException {
        String sql = "UPDATE postulaciones SET pun_consolidado = (SELECT round(avg(ev.pun_total), 2) "
                + "FROM eval_exte_postulaciones ep JOIN evaluaciones ev "
                + "ON ev.id_eval_exte_postulacion = ep.id_eval_exte_postulacion WHERE ep.id_postulacion = ?) "
                + "WHERE id_postulacion = ? RETURNING id_convocatoria";
        long idConvocatoria;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idPostulacion);
            ps.setLong(2, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                idConvocatoria = rs.getLong("id_convocatoria");
            }
        }
        if (!tieneOrdenCes(con, idConvocatoria)) {
            recalcularOrden(con, idConvocatoria);
        }
    }

    /**
     * Saca de la prelación una postulación Evaluada que se canceló (CU07): borra su orden calculado,
     * su puesto CES y su observación CES. Si la convocatoria no tiene orden CES, renumera el orden
     * calculado de las demás; si lo tiene, las demás conservan su puesto hasta que se registre de
     * nuevo el orden final CES (CU11), igual que cuando se evalúa una postulación después del CES.
     * <p>
     * La llama {@link PostulacionDAO#cambiarEstado} dentro de su transacción, con la misma conexión.
     * </p>
     */
    static void quitarDePrelacion(Connection con, long idPostulacion) throws SQLException {
        long idConvocatoria;
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE postulaciones SET ord_pre_calculado = NULL, ord_final_ces = NULL, obs_ces = NULL "
                + "WHERE id_postulacion = ? RETURNING id_convocatoria")) {
            ps.setLong(1, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                idConvocatoria = rs.getLong("id_convocatoria");
            }
        }
        if (!tieneOrdenCes(con, idConvocatoria)) {
            recalcularOrden(con, idConvocatoria);
        }
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================

    /** @return {@code true} si la convocatoria ya tiene registrado el orden final CES. */
    private static boolean tieneOrdenCes(Connection con, long idConvocatoria) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT count(*) FROM postulaciones WHERE id_convocatoria = ? AND ord_final_ces IS NOT NULL")) {
            ps.setLong(1, idConvocatoria);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Numera de 1 en adelante las postulaciones de la convocatoria que tienen puntaje
     * consolidado y no están canceladas, según {@link #ORDEN_PRELACION}, y guarda ese número
     * como orden calculado.
     */
    private static void recalcularOrden(Connection con, long idConvocatoria) throws SQLException {
        List<Long> orden = new ArrayList<>();
        String sql = "SELECT p.id_postulacion FROM postulaciones p "
                + "JOIN postulantes pt ON pt.id_postulante = p.id_postulante "
                + "WHERE p.id_convocatoria = ? AND p.pun_consolidado IS NOT NULL AND p.estado <> ?" + ORDEN_PRELACION;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idConvocatoria);
            ps.setString(2, EstadoPostulacion.CANCELADA.getValorBD());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    orden.add(rs.getLong("id_postulacion"));
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE postulaciones SET ord_pre_calculado = ? WHERE id_postulacion = ?")) {
            for (int i = 0; i < orden.size(); i++) {
                ps.setInt(1, i + 1);
                ps.setLong(2, orden.get(i));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private ResultadoPostulacion mapear(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("fec_solicitud");
        String tutor = rs.getString("tutor_nombre") == null ? null
                : rs.getString("tutor_nombre") + " " + rs.getString("tutor_apellido");
        String otroGrupo = rs.getString("otr_gru_investigacion");
        String grupo = rs.getString("gru_investigacion") + (otroGrupo == null ? "" : " / " + otroGrupo);
        ResultadoPostulacion r = new ResultadoPostulacion(rs.getLong("id_postulacion"),
                fecha == null ? null : fecha.toLocalDateTime(),
                rs.getString("nombre") + " " + rs.getString("apellido"), rs.getString("correo"),
                rs.getString("nom_proyecto"), grupo, tutor,
                EstadoPostulacion.desdeBD(rs.getString("estado")));
        if (rs.getString("cotutor_nombre") != null) {
            r.setCoTutor(rs.getString("cotutor_nombre") + " " + rs.getString("cotutor_apellido"));
        }
        double consolidado = rs.getDouble("pun_consolidado");
        r.setPuntajeConsolidado(rs.wasNull() ? null : consolidado);
        int calculado = rs.getInt("ord_pre_calculado");
        r.setOrdenCalculado(rs.wasNull() ? null : calculado);
        int ces = rs.getInt("ord_final_ces");
        r.setOrdenCes(rs.wasNull() ? null : ces);
        r.setObservacionCes(rs.getString("obs_ces"));
        r.setJuicioGlobal(rs.getString("jui_global"));
        return r;
    }
}
