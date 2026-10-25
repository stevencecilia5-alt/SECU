package utec.secu.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Clase de acceso a datos (DAO) para el perfil de Administrador.
 * Centraliza la creación del perfil de administrador asociado a un usuario existente.
 */
public class AdministradorDAO implements PerfilUsuarioDAO {

    /**
     * Crea el perfil de administrador para un usuario previamente registrado.
     *
     * @param con → Conexión activa con la base de datos.
     * @param idUsuario → Identificador del usuario al que se asociará el perfil de administrador.
     * @throws SQLException → Si ocurre un error durante la preparación o ejecución de la consulta SQL.
     */
    public void crearPerfil(Connection con, long idUsuario) throws SQLException {
        String sql = "INSERT INTO administradores (id_usuario) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            ps.executeUpdate();
        }
    }
}