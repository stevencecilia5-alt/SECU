package utec.secu.conexion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Prueba del patrón Singleton.
 */
class ConexionBDTest {

    @Test
    @DisplayName("Singleton: getInstancia() devuelve siempre la misma instancia")
    void siempreLaMismaInstancia() {
        assertSame(ConexionBD.getInstancia(), ConexionBD.getInstancia());
    }
}
