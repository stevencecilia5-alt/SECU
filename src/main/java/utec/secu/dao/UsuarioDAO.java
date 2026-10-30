package utec.secu.dao;

import utec.secu.conexion.ConexionBD;
import utec.secu.factory.PerfilUsuarioFactory;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.ServicioCifrado;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Patrón DAO: acceso a datos de los usuarios (CU01 y CU03).
 * <p>
 * Trabaja sobre {@code usuarios}, {@code tele_usuarios} y los perfiles
 * ({@code administradores}, {@code eval_externos}).
 * Las contraseñas se guardan cifradas con {@link ServicioCifrado}.
 * Las operaciones que escriben en más de una tabla (crear y modificar) van en una sola
 * transacción, escrita a mano con setAutoCommit, commit y rollback.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 3.0
 */
public class UsuarioDAO {

    private static final String CARACTERES_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom AZAR = new SecureRandom();
    private static final String SELECT_USUARIO =
            "SELECT u.ci, u.correo, u.nombre, u.apellido, u.rol, u.estado, u.req_cam_clave, "
            + "u.nacionalidad, u.calle, u.num_domicilio, "
            + "(SELECT t.num_telefono FROM tele_usuarios t WHERE t.id_usuario = u.id_usuario "
            + " ORDER BY t.id_tele_usuario LIMIT 1) AS telefono "
            + "FROM usuarios u ";

    // =========================================================================
    // 1. AUTENTICACIÓN Y CONTRASEÑAS (CU01)
    // =========================================================================

    /**
     * Autentica a un usuario Activo por correo y contraseña. El correo se compara sin distinguir
     * mayúsculas, igual que en la recuperación de contraseña y en el control de correos repetidos.
     *
     * @return El usuario autenticado, o {@code null} si las credenciales no coinciden o está Inactivo.
     */
    public Usuario autenticar(String correo, String contrasenia) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = SELECT_USUARIO + "WHERE lower(u.correo) = lower(?) AND u.contrasenia = ? AND u.estado = 'Activo'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setString(2, ServicioCifrado.hashPassword(contrasenia));
            return leerUno(ps);
        }
    }

    /** @return {@code true} si la contraseña es la vigente del usuario Activo con esa cédula (CU01). */
    public boolean verificarContrasenia(String ci, String contrasenia) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE ci = ? AND contrasenia = ? AND estado = 'Activo'",
                ci, ServicioCifrado.hashPassword(contrasenia));
    }

    /** Guarda una nueva contraseña elegida por el usuario y quita el cambio obligatorio. */
    public boolean cambiarContrasenia(String ci, String nuevaContrasenia) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE usuarios SET contrasenia = ?, req_cam_clave = FALSE WHERE ci = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ServicioCifrado.hashPassword(nuevaContrasenia));
            ps.setString(2, ci);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Genera una contraseña temporal para un usuario Activo y le exige cambiarla al ingresar.
     *
     * @return La contraseña temporal en texto plano, para enviarla por correo;
     *         {@code null} si el usuario no existe o no está Activo.
     */
    public String restablecerContrasenia(String ci) throws SQLException {
        String temporal = generarContraseniaTemporal();
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE usuarios SET contrasenia = ?, req_cam_clave = TRUE WHERE ci = ? AND estado = 'Activo'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ServicioCifrado.hashPassword(temporal));
            ps.setString(2, ci);
            return ps.executeUpdate() > 0 ? temporal : null;
        }
    }

    /**
     * Recuperación de contraseña (CU01): genera una contraseña temporal y la escribe sobre la
     * anterior, que deja de funcionar. Al ingresar con ella, el usuario debe definir una nueva,
     * así que la temporal sirve una sola vez. Solo aplica a usuarios Activos, de cualquier rol.
     *
     * @return La contraseña temporal en texto plano, para enviarla por correo;
     *         {@code null} si no hay un usuario Activo con ese correo.
     */
    public String recuperarContrasenia(String correo) throws SQLException {
        String temporal = generarContraseniaTemporal();
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE usuarios SET contrasenia = ?, req_cam_clave = TRUE "
                + "WHERE lower(correo) = lower(?) AND estado = 'Activo'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ServicioCifrado.hashPassword(temporal));
            ps.setString(2, correo);
            return ps.executeUpdate() > 0 ? temporal : null;
        }
    }

    // =========================================================================
    // 2. GESTIÓN DE USUARIOS (CU03)
    // =========================================================================

    /** @return Todos los usuarios, ordenados por apellido y nombre. */
    public List<Usuario> listarTodos() throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        List<Usuario> usuarios = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(SELECT_USUARIO + "ORDER BY u.apellido, u.nombre");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                usuarios.add(mapear(rs));
            }
        }
        return usuarios;
    }

    /** @return El usuario con esa cédula, con sus datos de contacto, o {@code null} si no existe. */
    public Usuario buscarPorCi(String ci) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(SELECT_USUARIO + "WHERE u.ci = ?")) {
            ps.setString(1, ci);
            return leerUno(ps);
        }
    }

    /** @return El usuario con ese correo (sin distinguir mayúsculas), o {@code null} si no existe (CU08). */
    public Usuario buscarPorCorreo(String correo) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(SELECT_USUARIO + "WHERE lower(u.correo) = lower(?)")) {
            ps.setString(1, correo);
            return leerUno(ps);
        }
    }

    /** @return {@code true} si ya existe un usuario con esa cédula. */
    public boolean existeCi(String ci) throws SQLException {
        return existe("SELECT 1 FROM usuarios WHERE ci = ?", ci, null);
    }

    /**
     * @param ciExcluida Cédula del usuario que se está modificando, para no compararlo consigo mismo;
     *                   {@code null} en un alta.
     * @return {@code true} si otro usuario ya tiene ese correo.
     */
    public boolean existeCorreo(String correo, String ciExcluida) throws SQLException {
        if (ciExcluida == null) {
            return existe("SELECT 1 FROM usuarios WHERE lower(correo) = lower(?)", correo, null);
        }
        return existe("SELECT 1 FROM usuarios WHERE lower(correo) = lower(?) AND ci <> ?", correo, ciExcluida);
    }

    /**
     * Registra un usuario con su perfil y su teléfono, en una sola transacción.
     * Si es Activo, genera una contraseña temporal; si es Inactivo, guarda una clave
     * aleatoria que nadie conoce, porque solo los usuarios activos reciben credenciales.
     *
     * @return La contraseña temporal en texto plano si el usuario es Activo; {@code null} si es Inactivo.
     */
    public String crear(Usuario u) throws SQLException {
        boolean activo = "Activo".equals(u.getEstado());
        String temporal = generarContraseniaTemporal();
        String clave = activo ? temporal : generarContraseniaTemporal() + generarContraseniaTemporal();

        Connection con = ConexionBD.getInstancia().getConnection();
        con.setAutoCommit(false);      // empieza la transacción: nada se guarda todavía
        try {
            long idUsuario;
            String sql = "INSERT INTO usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, calle, "
                    + "num_domicilio, rol, estado, req_cam_clave) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) "
                    + "RETURNING id_usuario";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, u.getCi());
                ps.setString(2, u.getCorreo());
                ps.setString(3, ServicioCifrado.hashPassword(clave));
                ps.setString(4, u.getNombre());
                ps.setString(5, u.getApellido());
                ps.setString(6, u.getNacionalidad());
                ps.setString(7, vacioANull(u.getCalle()));
                ps.setString(8, vacioANull(u.getNumDomicilio()));
                ps.setString(9, u.getRol());
                ps.setString(10, u.getEstado());
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    idUsuario = rs.getLong("id_usuario");
                }
            }
            // Patrón Factory Method: el perfil correspondiente al rol
            PerfilUsuarioFactory.crear(u.getRol()).crearPerfil(con, idUsuario);
            guardarTelefono(con, idUsuario, u.getTelefono());
            con.commit();              // todo salió bien: se guarda todo junto
        } catch (SQLException ex) {
            con.rollback();            // algo falló: se deshace todo
            throw ex;
        } finally {
            con.setAutoCommit(true);   // la conexión vuelve a guardar cada consulta sola
        }
        return activo ? temporal : null;
    }

    /**
     * Modifica los datos de un usuario existente; si cambia el rol, rehace su perfil.
     * Todo en una sola transacción.
     *
     * @return {@code true} si el usuario existía y se modificó.
     */
    public boolean modificar(Usuario u) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        con.setAutoCommit(false);      // empieza la transacción: nada se guarda todavía
        try {
            long idUsuario;
            String rolAnterior;
            try (PreparedStatement ps = con.prepareStatement("SELECT id_usuario, rol FROM usuarios WHERE ci = ?")) {
                ps.setString(1, u.getCi());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return false;  // no existe un usuario con esa cédula
                    }
                    idUsuario = rs.getLong("id_usuario");
                    rolAnterior = rs.getString("rol");
                }
            }
            String sql = "UPDATE usuarios SET correo = ?, nombre = ?, apellido = ?, nacionalidad = ?, "
                    + "calle = ?, num_domicilio = ?, rol = ? WHERE id_usuario = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, u.getCorreo());
                ps.setString(2, u.getNombre());
                ps.setString(3, u.getApellido());
                ps.setString(4, u.getNacionalidad());
                ps.setString(5, vacioANull(u.getCalle()));
                ps.setString(6, vacioANull(u.getNumDomicilio()));
                ps.setString(7, u.getRol());
                ps.setLong(8, idUsuario);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM tele_usuarios WHERE id_usuario = ?")) {
                ps.setLong(1, idUsuario);
                ps.executeUpdate();
            }
            guardarTelefono(con, idUsuario, u.getTelefono());
            if (!rolAnterior.equals(u.getRol())) {
                // Cambió el rol: se borra el perfil anterior y se crea el nuevo
                try (PreparedStatement psA = con.prepareStatement("DELETE FROM administradores WHERE id_usuario = ?");
                     PreparedStatement psE = con.prepareStatement("DELETE FROM eval_externos WHERE id_usuario = ?")) {
                    psA.setLong(1, idUsuario);
                    psA.executeUpdate();
                    psE.setLong(1, idUsuario);
                    psE.executeUpdate();
                }
                PerfilUsuarioFactory.crear(u.getRol()).crearPerfil(con, idUsuario);
            }
            con.commit();              // todo salió bien: se guarda todo junto
            return true;
        } catch (SQLException ex) {
            con.rollback();            // algo falló: se deshace todo
            throw ex;
        } finally {
            con.setAutoCommit(true);   // la conexión vuelve a guardar cada consulta sola
        }
    }

    /** Baja lógica: pasa a Inactivo un usuario Activo. @return {@code true} si se actualizó. */
    public boolean inactivar(String ci) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE usuarios SET estado = 'Inactivo' WHERE ci = ? AND estado = 'Activo'")) {
            ps.setString(1, ci);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Pasa a Activo un usuario Inactivo, con una contraseña temporal nueva que debe cambiar al ingresar.
     *
     * @return La contraseña temporal en texto plano, o {@code null} si el usuario no estaba Inactivo.
     */
    public String reactivar(String ci) throws SQLException {
        String temporal = generarContraseniaTemporal();
        Connection con = ConexionBD.getInstancia().getConnection();
        String sql = "UPDATE usuarios SET estado = 'Activo', contrasenia = ?, req_cam_clave = TRUE "
                + "WHERE ci = ? AND estado = 'Inactivo'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, ServicioCifrado.hashPassword(temporal));
            ps.setString(2, ci);
            return ps.executeUpdate() > 0 ? temporal : null;
        }
    }

    /** @return {@code true} si el evaluador con esa cédula tiene postulaciones asignadas (CU08). */
    public boolean tieneAsignaciones(String ci) throws SQLException {
        return existe("SELECT 1 FROM eval_exte_postulaciones ep JOIN usuarios u ON u.id_usuario = ep.id_usuario "
                + "WHERE u.ci = ?", ci, null);
    }

    /** @return {@code true} si el Administrador con esa cédula tiene convocatorias a su cargo. */
    public boolean tieneConvocatorias(String ci) throws SQLException {
        return existe("SELECT 1 FROM convocatorias c JOIN usuarios u ON u.id_usuario = c.id_usuario "
                + "WHERE u.ci = ?", ci, null);
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================

    private void guardarTelefono(Connection con, long idUsuario, String telefono) throws SQLException {
        if (telefono == null || telefono.isBlank()) {
            return;
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO tele_usuarios (id_usuario, num_telefono) VALUES (?, ?)")) {
            ps.setLong(1, idUsuario);
            ps.setString(2, telefono);
            ps.executeUpdate();
        }
    }

    private boolean existe(String sql, String valor1, String valor2) throws SQLException {
        Connection con = ConexionBD.getInstancia().getConnection();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor1);
            if (valor2 != null) {
                ps.setString(2, valor2);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Usuario leerUno(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapear(rs) : null;
        }
    }

    private Usuario mapear(ResultSet rs) throws SQLException {
        return new Usuario(rs.getString("ci"), rs.getString("correo"),
                rs.getString("nombre"), rs.getString("apellido"), rs.getString("rol"), rs.getString("estado"),
                rs.getBoolean("req_cam_clave"), rs.getString("nacionalidad"), rs.getString("calle"),
                rs.getString("num_domicilio"), rs.getString("telefono"));
    }

    private static String vacioANull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    /** @return Una contraseña aleatoria de 8 caracteres, sin letras ni números que se confundan. */
    private static String generarContraseniaTemporal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append(CARACTERES_CLAVE.charAt(AZAR.nextInt(CARACTERES_CLAVE.length())));
        }
        return sb.toString();
    }
}
