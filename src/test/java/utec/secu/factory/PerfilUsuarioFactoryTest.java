package utec.secu.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.dao.AdministradorDAO;
import utec.secu.dao.EvaluadorExternoDAO;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prueba del patrón Factory Method.
 */
class PerfilUsuarioFactoryTest {

    @Test
    @DisplayName("Factory Method: crea el DAO que corresponde a cada rol y rechaza un rol desconocido")
    void creaElDaoSegunElRol() {
        assertInstanceOf(AdministradorDAO.class, PerfilUsuarioFactory.crear("Administrador"));
        assertInstanceOf(EvaluadorExternoDAO.class, PerfilUsuarioFactory.crear("Evaluador externo"));
        assertThrows(IllegalArgumentException.class, () -> PerfilUsuarioFactory.crear("Postulante"));
    }
}
