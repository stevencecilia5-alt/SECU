package utec.secu.factory;

import utec.secu.dao.AdministradorDAO;
import utec.secu.dao.EvaluadorExternoDAO;
import utec.secu.dao.PerfilUsuarioDAO;

/**
 * Patrón FACTORY METHOD: encargada de crear la implementación de {@link PerfilUsuarioDAO}
 * correspondiente al rol de usuario indicado.
 * Permite que el código cliente solicite el DAO del perfil sin depender
 * directamente de sus implementaciones concretas.
 */
public final class PerfilUsuarioFactory {

    private PerfilUsuarioFactory() {
        throw new UnsupportedOperationException("Clase de fábrica; no instanciable.");
    }

    /**
     * Crea el DAO de perfil correspondiente al rol especificado.
     *
     * @param rol Rol del usuario para el cual se necesita crear el DAO.
     * @return Implementación de {@link PerfilUsuarioDAO} correspondiente al rol.
     * @throws IllegalArgumentException Si el rol no está contemplado por la fábrica.
     */
    public static PerfilUsuarioDAO crear(String rol) {
        if ("Administrador".equals(rol)) {
            return new AdministradorDAO();
        } else if ("Evaluador externo".equals(rol)) {
            return new EvaluadorExternoDAO();
        } else {
            throw new IllegalArgumentException("Rol desconocido: " + rol);
        }
    }
}
