package utec.secu.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Pruebas del cifrado propio de contraseñas con clave (requisito de la pauta de Programación).
 */
class ServicioCifradoTest {

    @Test
    @DisplayName("Descifrar con la misma clave devuelve la contraseña original")
    void conLaMismaClaveSeRecuperaLaOriginal() {
        String cifrada = ServicioCifrado.cifrar("Clave2026!", "Una frase de prueba");

        assertNotEquals("Clave2026!", cifrada);
        assertEquals("Clave2026!", ServicioCifrado.descifrar(cifrada, "Una frase de prueba"));
    }

    @Test
    @DisplayName("Descifrar con otra clave no devuelve la contraseña original")
    void conOtraClaveNoSeRecuperaLaOriginal() {
        String cifrada = ServicioCifrado.cifrar("Clave2026!", "Una frase de prueba");

        assertNotEquals("Clave2026!", ServicioCifrado.descifrar(cifrada, "Otra frase distinta"));
    }

    @Test
    @DisplayName("Una contraseña con ñ y tildes también se cifra entera y se recupera")
    void laEnieYLasTildesTambienSeCifran() {
        String original = "ContraseñaÁrbolÜ?";
        String cifrada = ServicioCifrado.cifrar(original, "Una frase de prueba");

        for (int i = 0; i < original.length(); i++) {
            assertNotEquals(original.charAt(i), cifrada.charAt(i));   // ningún carácter queda a la vista
        }
        assertEquals(original, ServicioCifrado.descifrar(cifrada, "Una frase de prueba"));
        // Con la clave de SECU, a la novena posición le toca una letra que da vueltas enteras: igual se cifra
        assertFalse(ServicioCifrado.cifrarContrasenia("Contraseña1").contains("ñ"));
    }

    @Test
    @DisplayName("La base guarda solo el valor cifrado con la clave de SECU, nunca la contraseña")
    void laBaseGuardaSoloElValorCifrado() {
        String cifrada = ServicioCifrado.cifrarContrasenia("Proyecto2026!");

        assertNotEquals("Proyecto2026!", cifrada);
        assertEquals("2\"w'u$YuC=BPA", cifrada);   // el mismo valor que cargan los scripts SQL
    }
}
