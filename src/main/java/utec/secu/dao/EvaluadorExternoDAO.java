package utec.secu.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Clase de acceso a datos (DAO) para el perfil de usuario Evaluador Externo.
 * Gestiona la creación del perfil asociado a un usuario existente.
 */
public class EvaluadorExternoDAO implements PerfilUsuarioDAO {

    /**
     * Crea el perfil de Evaluador Externo para un usuario existente.
     *
     * @param con Conexión activa con la base de datos.
     * @param idUsuario Identificador del usuario al que se asociará el perfil.
     * @throws SQLException Si ocurre un error al preparar o ejecutar la consulta SQL.
     */
    public void crearPerfil(Connection con, long idUsuario) throws SQLException {
        String sql = "INSERT INTO eval_externos (id_usuario) VALUES (?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, idUsuario);
            ps.executeUpdate();
        }
    }
}