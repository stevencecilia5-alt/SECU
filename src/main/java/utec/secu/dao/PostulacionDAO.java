package utec.secu.dao;

import utec.secu.conexion.ConexionBD;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.FiltroPostulacion;
import utec.secu.modelo.Postulacion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón DAO: acceso a datos de las postulaciones (CU07).
 * <p>
 * Lee de {@code postulaciones} y de sus tablas relacionadas, y solo escribe en
 * {@code postulaciones}. Usa la conexión compartida
 * de {@link ConexionBD}: cierra sentencias y resultados, nunca la conexión.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class PostulacionDAO {

    /**
     * Condición SQL de cada filtro de búsqueda del submenú "Buscar postulaciones".
     * Cada una tiene un único parámetro: el valor que escribe el usuario va siempre
     * como parámetro, nunca concatenado en el SQL.
     */
    private static String condicion(FiltroPostulacion filtro) {
        switch (filtro) {
            case CONVOCATORIA: return "c.nom_convocatoria ILIKE ?";
            case POSTULANTE:   return "(pt.nombre || ' ' || pt.apellido) ILIKE ?";
            case CORREO:       return "pt.correo ILIKE ?";
            case DOCUMENTO:    return "pt.documento = ?";
            case TUTOR:        return "EXISTS (SELECT 1 FROM tuto_utec_postulacion tp "
                    + "JOIN tuto_utec t ON t.id_tuto_utec = tp.id_tuto_utec "
                    + "WHERE tp.id_postulacion = p.id_postulacion "
                    + "AND (t.nombre || ' ' || t.apellido) ILIKE ?)";
            case GRUPO:        return "p.gru_investigacion ILIKE ?";
            case ESTADO:       return "p.estado = ?";
            default:           throw new IllegalArgumentException("Filtro desconocido: " + filtro);
        }
    }

    /** @return {@code true} si el filtro busca parte del texto; documento y estado buscan el valor exacto. */
    private static boolean esParcial(FiltroPostulacion filtro) {
        return filtro != FiltroPostulacion.DOCUMENTO && filtro != FiltroPostulacion.ESTADO;
    }

    private static final String SELECT_LISTADO =
            "SELECT p.id_postulacion, p.nom_proyecto, p.estado, p.fec_solicitud, "
            + "pt.nombre, pt.apellido, pt.correo, pt.documento, pt.tip_documento, c.nom_convocatoria "
            + "FROM postulaciones p "
            + "JOIN postulantes pt ON pt.id_postulante = p.id_postulante "
            + "JOIN convocatorias c ON c.id_convocatoria = p.id_convocatoria ";

    private static final String ORDEN_LISTADO = " ORDER BY p.fec_solicitud, p.id_postulacion";

    /**
     * Busca postulaciones por uno de los filtros de texto o por estado.
     *
     * @param filtro Filtro a aplicar.
     * @param valor  Valor buscado; para {@link FiltroPostulacion#ESTADO}, el valor de la base.
     * @return Postulaciones que cumplen el filtro, por fecha de solicitud.
     * @throws SQLException Si ocurre un error en la consulta.
     */
    public List<Postulacion> buscar(FiltroPostulacion filtro, String valor) throws SQLException {
        String sql = SELECT_LISTADO + "WHERE " + condicion(filtro) + ORDEN_LISTADO;
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, esParcial(filtro) ? "%" + valor + "%" : valor);
            return leerListado(ps);
        }
    }

    /**
     * Busca postulaciones por fecha de solicitud, con ambos extremos incluidos.
     * Como {@code fec_solicitud} es TIMESTAMP, el fin se compara con el día siguiente
     * a las 00:00 para incluir el día completo.
     */
    public List<Postulacion> buscarPorRango(LocalDate inicio, LocalDate fin) throws SQLException {
        String sql = SELECT_LISTADO + "WHERE p.fec_solicitud >= ? AND p.fec_solicitud < ?" + ORDEN_LISTADO;
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(inicio.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(fin.plusDays(1).atStartOfDay()));
            return leerListado(ps);
        }
    }

    /** @return Todas las postulaciones, por fecha de solicitud. */
    public List<Postulacion> listarTodas() throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(SELECT_LISTADO + ORDEN_LISTADO.trim())) {
            return leerListado(ps);
        }
    }

    /**
     * @return El estado de la postulación, o {@code null} si no existe. Lo usan los Proxy de CU06 y CU08-09.
     */
    public EstadoPostulacion buscarEstado(long id) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement("SELECT estado FROM postulaciones WHERE id_postulacion = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? EstadoPostulacion.desdeBD(rs.getString("estado")) : null;
            }
        }
    }

    /**
     * Obtiene una postulación con todos los datos de su detalle.
     *
     * @param id Identificador de la postulación.
     * @return La postulación, o {@code null} si no existe.
     * @throws SQLException Si ocurre un error en alguna de las consultas.
     */
    public Postulacion buscarPorId(long id) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "SELECT p.*, pt.nombre, pt.apellido, pt.correo, pt.documento, pt.tip_documento, "
                + "c.nom_convocatoria, d.nom_departamento, s.nom_sede "
                + "FROM postulaciones p "
                + "JOIN postulantes pt ON pt.id_postulante = p.id_postulante "
                + "JOIN convocatorias c ON c.id_convocatoria = p.id_convocatoria "
                + "JOIN dept_academicos d ON d.id_dept_academico = p.id_dept_academico "
                + "JOIN sedes s ON s.id_sede = p.id_sede "
                + "WHERE p.id_postulacion = ?";
        Postulacion p;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                p = mapearBasico(rs);
                p.setIdDeptAcademico(rs.getInt("id_dept_academico"));
                p.setDeptAcademico(rs.getString("nom_departamento"));
                p.setIdSede(rs.getInt("id_sede"));
                p.setSede(rs.getString("nom_sede"));
                p.setLinInvestigacion(rs.getString("lin_investigacion"));
                p.setCanMeses(rs.getInt("can_meses"));
                p.setGruInvestigacion(rs.getString("gru_investigacion"));
                p.setOtrGruInvestigacion(rs.getString("otr_gru_investigacion"));
            }
        }
        cargarTutores(con, p);
        cargarDocumentos(con, p);
        cargarEvaluadores(con, p);
        return p;
    }

    /** @return Departamentos académicos (id y nombre), por nombre. */
    public Map<Integer, String> listarDepartamentos() throws SQLException {
        return listarCatalogo("SELECT id_dept_academico, nom_departamento FROM dept_academicos ORDER BY nom_departamento");
    }

    /** @return Sedes (id y nombre), por nombre. */
    public Map<Integer, String> listarSedes() throws SQLException {
        return listarCatalogo("SELECT id_sede, nom_sede FROM sedes ORDER BY nom_sede");
    }

    /**
     * Guarda el departamento y la sede, solo si la postulación sigue Registrada o En revisión.
     *
     * @param p Postulación con el departamento y la sede nuevos.
     * @return {@code true} si se guardó; {@code false} si la postulación ya no admite cambios.
     * @throws SQLException Si ocurre un error en la actualización.
     */
    public boolean actualizarDatos(Postulacion p) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE postulaciones SET id_dept_academico = ?, id_sede = ? "
                + "WHERE id_postulacion = ? AND estado IN (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, p.getIdDeptAcademico());
            ps.setInt(2, p.getIdSede());
            ps.setLong(3, p.getIdPostulacion());
            ps.setString(4, EstadoPostulacion.REGISTRADA.getValorBD());
            ps.setString(5, EstadoPostulacion.EN_REVISION.getValorBD());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Cambia el estado de una postulación solo si sigue en el estado que se leyó,
     * para no pisar un cambio hecho por otra sesión entre la lectura y la escritura.
     *
     * @return {@code true} si se actualizó; {@code false} si el estado había cambiado.
     */
    public boolean cambiarEstado(long id, EstadoPostulacion actual, EstadoPostulacion nuevo) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE postulaciones SET estado = ? WHERE id_postulacion = ? AND estado = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevo.getValorBD());
            ps.setLong(2, id);
            ps.setString(3, actual.getValorBD());
            return ps.executeUpdate() == 1;
        }
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================

    private List<Postulacion> leerListado(PreparedStatement ps) throws SQLException {
        List<Postulacion> lista = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapearBasico(rs));
            }
        }
        return lista;
    }

    private Postulacion mapearBasico(ResultSet rs) throws SQLException {
        Timestamp fecha = rs.getTimestamp("fec_solicitud");
        Postulacion p = new Postulacion(rs.getLong("id_postulacion"),
                fecha == null ? null : fecha.toLocalDateTime(),
                rs.getString("nombre"), rs.getString("apellido"), rs.getString("correo"),
                rs.getString("documento"), rs.getString("tip_documento"), rs.getString("nom_convocatoria"));
        p.setNomProyecto(rs.getString("nom_proyecto"));
        p.setEstado(EstadoPostulacion.desdeBD(rs.getString("estado")));
        return p;
    }

    private void cargarTutores(Connection con, Postulacion p) throws SQLException {
        String sql = "SELECT t.nombre, t.apellido, t.cor_utec, tp.rol_tutor "
                + "FROM tuto_utec_postulacion tp JOIN tuto_utec t ON t.id_tuto_utec = tp.id_tuto_utec "
                + "WHERE tp.id_postulacion = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, p.getIdPostulacion());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String nombre = rs.getString("nombre") + " " + rs.getString("apellido");
                    if ("Tutor principal".equals(rs.getString("rol_tutor"))) {
                        p.setTutorPrincipal(nombre);
                        p.setCorreoTutorPrincipal(rs.getString("cor_utec"));
                    } else {
                        p.setCoTutor(nombre + " (" + rs.getString("cor_utec") + ")");
                    }
                }
            }
        }
    }

    private void cargarDocumentos(Connection con, Postulacion p) throws SQLException {
        String sql = "SELECT dt.nom_tip_documento, da.rut_archivo "
                + "FROM docu_adjuntos da JOIN docu_tipos dt ON dt.id_docu_tipo = da.id_docu_tipo "
                + "WHERE da.id_postulacion = ? ORDER BY dt.id_docu_tipo, da.id_docu_adjunto";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, p.getIdPostulacion());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    p.getDocumentos().add(rs.getString("nom_tip_documento") + ": " + rs.getString("rut_archivo"));
                }
            }
        }
    }

    private void cargarEvaluadores(Connection con, Postulacion p) throws SQLException {
        String sql = "SELECT u.nombre, u.apellido, u.correo, ev.estado "
                + "FROM eval_exte_postulaciones e "
                + "JOIN usuarios u ON u.id_usuario = e.id_usuario "
                + "LEFT JOIN evaluaciones ev ON ev.id_eval_exte_postulacion = e.id_eval_exte_postulacion "
                + "WHERE e.id_postulacion = ? ORDER BY e.id_eval_exte_postulacion";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, p.getIdPostulacion());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String estado = rs.getString("estado");
                    p.getEvaluadores().add(rs.getString("nombre") + " " + rs.getString("apellido")
                            + " (" + rs.getString("correo") + "): evaluación "
                            + (estado == null ? "Pendiente" : estado));
                }
            }
        }
    }

    private Map<Integer, String> listarCatalogo(String sql) throws SQLException {
        Map<Integer, String> mapa = new LinkedHashMap<>();
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                mapa.put(rs.getInt(1), rs.getString(2));
            }
        }
        return mapa;
    }
}
