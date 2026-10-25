package utec.secu.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Define las operaciones de acceso a datos comunes para los perfiles de usuario.
 * Cada implementación se encarga de crear la fila correspondiente al perfil
 * utilizando el identificador del usuario previamente registrado.
 */
public interface PerfilUsuarioDAO {

    /**
     * Crea el registro correspondiente al perfil de un usuario existente.
     *
     * @param con Conexión activa con la base de datos.
     * @param idUsuario Identificador del usuario al que pertenece el perfil.
     * @throws SQLException Si ocurre un error al crear el registro en la base de datos.
     */
    void crearPerfil(Connection con, long idUsuario) throws SQLException;
}