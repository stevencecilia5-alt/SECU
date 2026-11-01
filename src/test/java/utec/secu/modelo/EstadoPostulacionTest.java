package utec.secu.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de CU07: cambios de estado que el Administrador puede hacer a mano.
 */
class EstadoPostulacionTest {

    @Test
    @DisplayName("CU07: una postulación Registrada puede pasar a En revisión")
    void registradaPuedePasarAEnRevision() {
        assertTrue(EstadoPostulacion.REGISTRADA.transicionesManuales().contains(EstadoPostulacion.EN_REVISION));
    }

    @Test
    @DisplayName("CU07: Registrada no puede pasar directo a Aprobada, y un estado final ya no cambia")
    void registradaNoPuedePasarAAprobada() {
        assertFalse(EstadoPostulacion.REGISTRADA.transicionesManuales().contains(EstadoPostulacion.APROBADA));
        assertTrue(EstadoPostulacion.APROBADA.esFinal());
        assertTrue(EstadoPostulacion.APROBADA.transicionesManuales().isEmpty());
    }
}
