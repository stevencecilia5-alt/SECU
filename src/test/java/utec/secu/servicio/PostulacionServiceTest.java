package utec.secu.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Postulacion;
import utec.secu.seguridad.PostulacionProxy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de CU07: búsqueda por fechas y modificación de postulaciones.
 */
class PostulacionServiceTest {

    @Test
    @DisplayName("CU07: fecha inexistente, formato incorrecto y rango invertido se rechazan")
    void fechasInvalidasSeRechazan() {
        DatoInvalidoException inexistente = assertThrows(DatoInvalidoException.class,
                () -> PostulacionService.parsearFecha("31/02/2026"));
        DatoInvalidoException formato = assertThrows(DatoInvalidoException.class,
                () -> PostulacionService.parsearFecha("2026-09-01"));
        DatoInvalidoException rango = assertThrows(DatoInvalidoException.class,
                () -> PostulacionService.validarRango(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 1)));

        assertEquals(PostulacionService.MSG_FECHA, inexistente.getMessage());
        assertEquals(PostulacionService.MSG_FECHA, formato.getMessage());
        assertEquals(PostulacionService.MSG_RANGO, rango.getMessage());
    }

    @Test
    @DisplayName("CU07 (Mockito): una postulación Evaluada no se puede modificar")
    void noSeModificaUnaPostulacionEvaluada() throws Exception {
        // El Proxy simulado devuelve la postulación 9 en estado Evaluada, sin consultar la base
        Postulacion evaluada = new Postulacion(9, LocalDateTime.of(2026, 9, 11, 10, 0), "Sofía", "Olivera",
                "sofia.olivera@gmail.com", "40987654", "CI", "Becas de Iniciación a la Investigación 2026");
        evaluada.setEstado(EstadoPostulacion.EVALUADA);
        PostulacionProxy proxy = mock(PostulacionProxy.class);
        when(proxy.buscarPorId(any(), eq(9L))).thenReturn(evaluada);
        PostulacionService servicio = new PostulacionService(proxy);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> servicio.obtenerParaModificar(DatosDePrueba.administrador(), 9));

        assertEquals(PostulacionService.MSG_NO_MODIFICABLE, ex.getMessage());
    }

    @Test
    @DisplayName("CU07 (Mockito): un cambio de estado no permitido dice desde qué estado y a cuál")
    void unCambioDeEstadoNoPermitidoDiceElMotivo() throws Exception {
        Postulacion enRevision = new Postulacion(3, LocalDateTime.of(2026, 9, 4, 9, 30), "Camila", "Ríos",
                "camila.rios@gmail.com", "45678912", "CI", "Becas de Iniciación a la Investigación 2026");
        enRevision.setEstado(EstadoPostulacion.EN_REVISION);
        PostulacionProxy proxy = mock(PostulacionProxy.class);
        when(proxy.buscarPorId(any(), eq(3L))).thenReturn(enRevision);
        PostulacionService servicio = new PostulacionService(proxy);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> servicio.cambiarEstado(DatosDePrueba.administrador(), 3, EstadoPostulacion.APROBADA));

        assertEquals("No se puede pasar de En revisión a Aprobada.", ex.getMessage());
        verify(proxy, never()).cambiarEstado(any(), eq(3L), any(), any());
    }
}
