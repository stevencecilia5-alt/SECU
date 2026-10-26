package utec.secu.dao;

import utec.secu.conexion.ConexionBD;
import utec.secu.modelo.Criterio;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Evaluacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón DAO: asignación de evaluadores (CU08) y registro de evaluaciones (CU09).
 * <p>
 * Escribe en {@code eval_exte_postulaciones}, {@code evaluaciones}, {@code eval_criterios}
 * y, para los cambios de estado que hace el sistema, en {@code postulaciones}.
 * Los evaluadores se identifican por cédula. Usa la conexión compartida de
 * {@link ConexionBD}: cierra sentencias y resultados, nunca la conexión.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class EvaluacionDAO {

    private static final String SELECT_ASIGNACION =
            "SELECT ep.id_eval_exte_postulacion, ep.id_postulacion, p.nom_proyecto, p.estado AS estado_postulacion, "
            + "u.nombre, u.apellido, u.correo, COALESCE(ev.estado, 'Pendiente') AS estado_evaluacion, "
            + "ev.pun_total, ev.com_evaluador "
            + "FROM eval_exte_postulaciones ep "
            + "JOIN usuarios u ON u.id_usuario = ep.id_usuario "
            + "JOIN postulaciones p ON p.id_postulacion = ep.id_postulacion "
            + "LEFT JOIN evaluaciones ev ON ev.id_eval_exte_postulacion = ep.id_eval_exte_postulacion ";

    // =========================================================================
    // 1. CONSULTAS
    // =========================================================================

    /** @return Los evaluadores asignados a una postulación, con el estado de cada evaluación. */
    public List<Evaluacion> listarPorPostulacion(long idPostulacion) throws SQLException {
        return listar(SELECT_ASIGNACION + "WHERE ep.id_postulacion = ? ORDER BY ep.id_eval_exte_postulacion",
                idPostulacion, null);
    }

    /** @return Las postulaciones asignadas al evaluador con esa cédula. */
    public List<Evaluacion> listarPorEvaluador(String ciEvaluador) throws SQLException {
        return listar(SELECT_ASIGNACION + "WHERE u.ci = ? ORDER BY ep.id_postulacion", null, ciEvaluador);
    }

    /**
     * Busca la asignación de un evaluador a una postulación, con los 16 criterios de la
     * rúbrica y los puntajes que ya cargó.
     *
     * @return La asignación, o {@code null} si ese evaluador no tiene asignada la postulación.
     */
    public Evaluacion buscarAsignacion(long idPostulacion, String ciEvaluador) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        Evaluacion e;
        try (PreparedStatement ps = con.prepareStatement(SELECT_ASIGNACION + "WHERE ep.id_postulacion = ? AND u.ci = ?")) {
            ps.setLong(1, idPostulacion);
            ps.setString(2, ciEvaluador);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                e = mapear(rs);
            }
        }
        String sql = "SELECT c.id_criterio, s.nom_seccion, s.peso, c.nom_criterio, ec.puntaje "
                + "FROM criterios c JOIN secc_rubricas s ON s.id_secc_rubrica = c.id_secc_rubrica "
                + "LEFT JOIN eval_criterios ec ON ec.id_criterio = c.id_criterio AND ec.id_evaluacion = "
                + "(SELECT id_evaluacion FROM evaluaciones WHERE id_eval_exte_postulacion = ?) "
                + "ORDER BY s.id_secc_rubrica, c.id_criterio";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, e.getIdAsignacion());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int puntaje = rs.getInt("puntaje");
                    e.getCriterios().add(new Criterio(rs.getInt("id_criterio"), rs.getString("nom_seccion"),
                            rs.getDouble("peso"), rs.getString("nom_criterio"), rs.wasNull() ? null : puntaje));
                }
            }
        }
        return e;
    }

    // =========================================================================
    // 2. ASIGNACIÓN DE EVALUADORES (CU08)
    // =========================================================================

    /**
     * Asigna un evaluador a una postulación. Si con esta asignación la postulación queda con
     * dos evaluadores y alguno ya empezó su evaluación, pasa de En revisión a En evaluación.
     * Si ninguno empezó, pasa cuando uno guarde la evaluación (ver {@link #guardar}). Todo en una
     * sola transacción.
     *
     * @return Cantidad de evaluadores asignados después de la operación (1 o 2).
     */
    public int asignar(long idPostulacion, String ciEvaluador) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        con.setAutoCommit(false);      // empieza la transacción: nada se guarda todavía
        try {
            String sql = "INSERT INTO eval_exte_postulaciones (id_usuario, id_postulacion) "
                    + "SELECT id_usuario, ? FROM usuarios WHERE ci = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, idPostulacion);
                ps.setString(2, ciEvaluador);
                ps.executeUpdate();
            }
            int asignados;
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT count(*) FROM eval_exte_postulaciones WHERE id_postulacion = ?")) {
                ps.setLong(1, idPostulacion);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    asignados = rs.getInt(1);
                }
            }
            if (asignados == 2) {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE postulaciones SET estado = ? WHERE id_postulacion = ? AND estado = ? AND EXISTS ("
                        + "SELECT 1 FROM eval_exte_postulaciones ep JOIN evaluaciones ev "
                        + "ON ev.id_eval_exte_postulacion = ep.id_eval_exte_postulacion "
                        + "WHERE ep.id_postulacion = ? AND ev.estado IN (?, ?))")) {
                    ps.setString(1, EstadoPostulacion.EN_EVALUACION.getValorBD());
                    ps.setLong(2, idPostulacion);
                    ps.setString(3, EstadoPostulacion.EN_REVISION.getValorBD());
                    ps.setLong(4, idPostulacion);
                    ps.setString(5, Evaluacion.EN_PROCESO);
                    ps.setString(6, Evaluacion.COMPLETADA);
                    ps.executeUpdate();
                }
            }
            con.commit();              // todo salió bien: se guarda todo junto
            return asignados;
        } catch (SQLException ex) {
            con.rollback();            // algo falló: se deshace todo
            throw ex;
        } finally {
            con.setAutoCommit(true);   // la conexión vuelve a guardar cada consulta sola
        }
    }

    /** Cambia el evaluador de una asignación. */
    public void reemplazar(long idAsignacion, String ciNuevo) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE eval_exte_postulaciones SET id_usuario = (SELECT id_usuario FROM usuarios WHERE ci = ?) "
                + "WHERE id_eval_exte_postulacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ciNuevo);
            ps.setLong(2, idAsignacion);
            ps.executeUpdate();
        }
    }

    /** @return El juicio global de la postulación, o {@code null} si todavía no tiene. */
    public String buscarJuicioGlobal(long idPostulacion) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT jui_global FROM postulaciones WHERE id_postulacion = ?")) {
            ps.setLong(1, idPostulacion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("jui_global") : null;
            }
        }
    }

    /** Guarda el juicio global de una postulación. */
    public void registrarJuicioGlobal(long idPostulacion, String juicio) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE postulaciones SET jui_global = ? WHERE id_postulacion = ?")) {
            ps.setString(1, juicio);
            ps.setLong(2, idPostulacion);
            ps.executeUpdate();
        }
    }

    // =========================================================================
    // 3. REGISTRO DE EVALUACIONES (CU09)
    // =========================================================================

    /**
     * Guarda la evaluación con sus puntajes. Si la postulación está En revisión y ya tiene sus
     * dos evaluadores, pasa a En evaluación. Si la evaluación queda Completada y la
     * otra también lo está, la postulación pasa a Evaluada. Todo en una sola transacción.
     *
     * @return {@code true} si la postulación pasó a Evaluada.
     */
    public boolean guardar(Evaluacion e) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        con.setAutoCommit(false);      // empieza la transacción: nada se guarda todavía
        try {
            long idEvaluacion;
            String sql = "INSERT INTO evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador) "
                    + "VALUES (?, ?, ?, ?) ON CONFLICT (id_eval_exte_postulacion) DO UPDATE SET "
                    + "estado = EXCLUDED.estado, pun_total = EXCLUDED.pun_total, com_evaluador = EXCLUDED.com_evaluador "
                    + "RETURNING id_evaluacion";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setLong(1, e.getIdAsignacion());
                ps.setString(2, e.getEstado());
                if (e.getPuntajeTotal() == null) {
                    ps.setNull(3, Types.NUMERIC);
                } else {
                    ps.setDouble(3, e.getPuntajeTotal());
                }
                String comentario = e.getComentario();
                ps.setString(4, comentario == null || comentario.isBlank() ? null : comentario.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    idEvaluacion = rs.getLong("id_evaluacion");
                }
            }
            // Los puntajes se reescriben enteros: se borran y se insertan los que tienen valor
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM eval_criterios WHERE id_evaluacion = ?")) {
                ps.setLong(1, idEvaluacion);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO eval_criterios (id_evaluacion, id_criterio, puntaje) VALUES (?, ?, ?)")) {
                for (Criterio c : e.getCriterios()) {
                    if (c.getPuntaje() != null) {
                        ps.setLong(1, idEvaluacion);
                        ps.setInt(2, c.getIdCriterio());
                        ps.setInt(3, c.getPuntaje());
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE postulaciones SET estado = ? WHERE id_postulacion = ? AND estado = ? "
                    + "AND (SELECT count(*) FROM eval_exte_postulaciones WHERE id_postulacion = ?) = 2")) {
                ps.setString(1, EstadoPostulacion.EN_EVALUACION.getValorBD());
                ps.setLong(2, e.getIdPostulacion());
                ps.setString(3, EstadoPostulacion.EN_REVISION.getValorBD());
                ps.setLong(4, e.getIdPostulacion());
                ps.executeUpdate();
            }
            boolean pasoAEvaluada = false;
            if (e.estaCompletada()) {
                String sqlEstado = "UPDATE postulaciones SET estado = ? WHERE id_postulacion = ? AND estado IN (?, ?) "
                        + "AND (SELECT count(*) FROM eval_exte_postulaciones ep JOIN evaluaciones ev "
                        + "ON ev.id_eval_exte_postulacion = ep.id_eval_exte_postulacion "
                        + "WHERE ep.id_postulacion = ? AND ev.estado = ?) = 2";
                try (PreparedStatement ps = con.prepareStatement(sqlEstado)) {
                    ps.setString(1, EstadoPostulacion.EVALUADA.getValorBD());
                    ps.setLong(2, e.getIdPostulacion());
                    ps.setString(3, EstadoPostulacion.EN_REVISION.getValorBD());
                    ps.setString(4, EstadoPostulacion.EN_EVALUACION.getValorBD());
                    ps.setLong(5, e.getIdPostulacion());
                    ps.setString(6, Evaluacion.COMPLETADA);
                    pasoAEvaluada = ps.executeUpdate() == 1;
                }
            }
            con.commit();              // todo salió bien: se guarda todo junto
            return pasoAEvaluada;
        } catch (SQLException ex) {
            con.rollback();            // algo falló: se deshace todo
            throw ex;
        } finally {
            con.setAutoCommit(true);   // la conexión vuelve a guardar cada consulta sola
        }
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================

    /** Ejecuta un listado con un único parámetro: un número (id) o un texto (cédula). */
    private List<Evaluacion> listar(String sql, Long numero, String texto) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        List<Evaluacion> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (numero != null) {
                ps.setLong(1, numero);
            } else {
                ps.setString(1, texto);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private Evaluacion mapear(ResultSet rs) throws SQLException {
        double total = rs.getDouble("pun_total");
        Double puntajeTotal = rs.wasNull() ? null : total;
        return new Evaluacion(rs.getLong("id_eval_exte_postulacion"), rs.getLong("id_postulacion"),
                rs.getString("nom_proyecto"), EstadoPostulacion.desdeBD(rs.getString("estado_postulacion")),
                rs.getString("nombre") + " " + rs.getString("apellido"), rs.getString("correo"),
                rs.getString("estado_evaluacion"), puntajeTotal, rs.getString("com_evaluador"));
    }
}
