package utec.secu.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.OperacionNoPermitidaException;
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

    @Test
    @DisplayName("CU01: contraseña con € o un emoji se rechaza (el cifrado no los cubre); con ñ y tildes se acepta")
    void contraseniaConCaracteresQueNoSeCifranSeRechaza() {
        UsuarioService.validarNuevaContrasenia("contraseñaÁrbol1");   // no lanza: la ñ y las tildes se cifran
        for (String clave : new String[]{"clave€2026", "clave\uD83D\uDE0026"}) {
            DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                    () -> UsuarioService.validarNuevaContrasenia(clave));
            assertEquals(UsuarioService.MSG_CARACTERES_CLAVE, ex.getMessage());
        }
    }

    @Test
    @DisplayName("CU01 (Mockito): con la contraseña actual incorrecta no se cambia la contraseña")
    void cambioConContraseniaActualIncorrectaSeRechaza() throws Exception {
        // El Proxy simulado responde que la contraseña actual no es la vigente
        SeguridadProxy proxy = mock(SeguridadProxy.class);
        when(proxy.verificarContrasenia(any(), eq("Incorrecta1"))).thenReturn(false);
        UsuarioService servicio = new UsuarioService(proxy);

        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> servicio.cambiarContrasenia(DatosDePrueba.administrador(), "Incorrecta1", "Nueva2026", "Nueva2026"));

        assertEquals(UsuarioService.MSG_ACTUAL_INCORRECTA, ex.getMessage());
        verify(proxy, never()).cambiarContrasenia(any(), any());   // la contraseña no se cambió
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

    @Test
    @DisplayName("CU03 (Mockito): un Administrador no se puede dar de baja a sí mismo")
    void unAdministradorNoSeDaDeBajaASiMismo() throws Exception {
        Usuario admin = DatosDePrueba.administrador();
        SeguridadProxy proxy = mock(SeguridadProxy.class);
        when(proxy.buscarPorCi(any(), eq(admin.getCi()))).thenReturn(admin);
        UsuarioService servicio = new UsuarioService(proxy);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> servicio.darDeBaja(admin, admin.getCi()));

        assertEquals(UsuarioService.MSG_BAJA_PROPIA, ex.getMessage());
        verify(proxy, never()).darDeBajaUsuario(any(), any());   // sigue habiendo un Administrador activo
    }

    @Test
    @DisplayName("CU03 (Mockito): no se cambia el rol de un evaluador con postulaciones asignadas")
    void noSeCambiaElRolDeUnEvaluadorConAsignaciones() throws Exception {
        Usuario evaluador = DatosDePrueba.evaluador();
        SeguridadProxy proxy = mock(SeguridadProxy.class);
        when(proxy.buscarPorCi(any(), eq(evaluador.getCi()))).thenReturn(evaluador);
        when(proxy.tieneAsignaciones(any(), eq(evaluador.getCi()))).thenReturn(true);
        UsuarioService servicio = new UsuarioService(proxy);
        Usuario datos = new Usuario(evaluador.getCi(), evaluador.getCorreo(), "Lucía", "Fernández",
                "Administrador", "Activo", false, "Uruguaya", null, null, null);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> servicio.modificarUsuario(DatosDePrueba.administrador(), datos));

        assertEquals(UsuarioService.MSG_ROL_CON_ASIGNACIONES, ex.getMessage());
        verify(proxy, never()).modificarUsuario(any(), any());   // no llega a la base
    }
}
