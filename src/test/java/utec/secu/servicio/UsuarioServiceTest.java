package utec.secu.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.SeguridadProxy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de CU01 (contraseñas) y CU03 (datos de los usuarios).
 */
class UsuarioServiceTest {

    // ---------- CU01: contraseñas ----------

    @Test
    @DisplayName("CU01: contraseña de 5 caracteres se rechaza y de 6 se acepta (valor límite)")
    void contraseniaDeMenosDe6CaracteresSeRechaza() {
        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> UsuarioService.validarNuevaContrasenia("12345"));

        assertEquals(UsuarioService.MSG_MINIMO_CLAVE, ex.getMessage());
        assertDoesNotThrow(() -> UsuarioService.validarNuevaContrasenia("123456"));
    }

    @Test
    @DisplayName("CU01: contraseña formada solo por espacios se rechaza (Mantis #0044831)")
    void contraseniaConSoloEspaciosSeRechaza() {
        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> UsuarioService.validarNuevaContrasenia("      "));

        assertEquals(UsuarioService.MSG_MINIMO_CLAVE, ex.getMessage());
    }

    @Test
    @DisplayName("CU01: la confirmación distinta de la nueva contraseña se rechaza")
    void confirmacionDistintaSeRechaza() {
        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> UsuarioService.validarConfirmacion("Clave2026", "Clave2025"));

        assertEquals(UsuarioService.MSG_NO_COINCIDEN, ex.getMessage());
    }

    // ---------- CU03: datos de los usuarios ----------

    @Test
    @DisplayName("CU03: cédula con dígito verificador correcto se acepta")
    void cedulaValidaSeAcepta() {
        assertEquals("37125485", UsuarioService.validarCedula("37125485"));
    }

    @Test
    @DisplayName("CU03: cédula con dígito verificador incorrecto se rechaza")
    void cedulaConDigitoVerificadorIncorrectoSeRechaza() {
        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> UsuarioService.validarCedula("37125484"));

        assertEquals(UsuarioService.MSG_CEDULA, ex.getMessage());
    }

    @Test
    @DisplayName("CU03: nombre de 1 letra se rechaza y de 2 se acepta (valor límite)")
    void nombreDeUnaLetraSeRechaza() {
        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> UsuarioService.validarNombre("A"));

        assertEquals(UsuarioService.MSG_NOMBRE, ex.getMessage());
        assertEquals("Al", UsuarioService.validarNombre("Al"));
    }

    @Test
    @DisplayName("CU03 (Mockito): alta con una cédula ya registrada se rechaza y no se guarda nada")
    void altaConCedulaYaRegistradaSeRechaza() throws Exception {
        // El Proxy simulado responde que la cédula ya existe, sin consultar la base
        SeguridadProxy proxy = mock(SeguridadProxy.class);
        when(proxy.existeCi(any(), eq("38765418"))).thenReturn(true);
        UsuarioService servicio = new UsuarioService(proxy);
        Usuario nuevo = new Usuario("38765418", "nuevo.evaluador@utec.edu.uy", "Ana", "Gómez",
                "Evaluador externo", "Activo", true, "Uruguaya", "Rivera", "123", "099123456");

        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> servicio.crearUsuario(DatosDePrueba.administrador(), nuevo));

        assertEquals(UsuarioService.MSG_CEDULA_DUPLICADA, ex.getMessage());
        verify(proxy, never()).crearUsuario(any(), any());   // el alta no llegó a guardarse
    }
}
