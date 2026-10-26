package utec.secu.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Criterio;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Evaluacion;
import utec.secu.seguridad.EvaluacionProxy;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de CU08 (asignación de evaluadores) y CU09 (puntajes de la evaluación).
 */
class EvaluacionServiceTest {

    // ---------- CU09: puntajes ----------

    @Test
    @DisplayName("CU09: puntaje fuera de 1 a 5, decimal o con letras se rechaza")
    void puntajeInvalidoSeRechaza() {
        String[] invalidos = {"0", "6", "3.5", "abc"};
        for (String valor : invalidos) {
            DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                    () -> EvaluacionService.parsearPuntaje(valor));
            assertEquals(EvaluacionService.MSG_PUNTAJE, ex.getMessage());
        }
    }

    @Test
    @DisplayName("CU09: los puntajes límite 1 y 5 se aceptan")
    void puntajesLimiteSeAceptan() {
        assertEquals(Integer.valueOf(1), EvaluacionService.parsearPuntaje("1"));
        assertEquals(Integer.valueOf(5), EvaluacionService.parsearPuntaje("5"));
    }

    @Test
    @DisplayName("CU09: el puntaje total promedia cada sección y aplica su peso")
    void puntajeTotalPonderado() {
        // Sección A (60%): 4 y 5, promedio 4,5. Sección B (40%): 3. Total = 4,5 x 0,6 + 3 x 0,4 = 3,90
        List<Criterio> criterios = List.of(
                new Criterio(1, "A", 60, "Criterio 1", 4),
                new Criterio(2, "A", 60, "Criterio 2", 5),
                new Criterio(3, "B", 40, "Criterio 3", 3));

        assertEquals(3.90, EvaluacionService.calcularPuntajeTotal(criterios), 0.001);
    }

    // ---------- CU08: asignación de evaluadores ----------

    @Test
    @DisplayName("CU08 (Proxy): un Evaluador externo no puede asignar evaluadores")
    void unEvaluadorNoPuedeAsignarEvaluadores() {
        // Usa el Proxy real: rechaza el acceso antes de consultar la base
        EvaluacionService servicio = new EvaluacionService();

        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> servicio.asignarEvaluador(DatosDePrueba.evaluador(), 4, "eval3@utec.edu.uy"));

        assertEquals("Acceso denegado: Se requieren permisos de Administrador para asignar evaluadores.",
                ex.getMessage());
    }

    @Test
    @DisplayName("CU08 (Mockito): no se puede asignar un tercer evaluador")
    void noSePuedeAsignarUnTercerEvaluador() throws Exception {
        // El Proxy simulado responde que la postulación 3 está En revisión y ya tiene dos evaluadores
        EvaluacionProxy proxy = mock(EvaluacionProxy.class);
        when(proxy.buscarEstadoPostulacion(any(), eq(3L))).thenReturn(EstadoPostulacion.EN_REVISION);
        when(proxy.listarPorPostulacion(any(), eq(3L))).thenReturn(List.of(
                asignacion("eval2@utec.edu.uy", Evaluacion.PENDIENTE),
                asignacion("eval3@utec.edu.uy", Evaluacion.PENDIENTE)));
        EvaluacionService servicio = new EvaluacionService(proxy);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> servicio.asignarEvaluador(DatosDePrueba.administrador(), 3, "eval1@utec.edu.uy"));

        assertEquals(EvaluacionService.MSG_DOS_EVALUADORES, ex.getMessage());
        verify(proxy, never()).asignar(any(), anyLong(), anyString());   // no se guardó la asignación
    }

    @Test
    @DisplayName("CU08 (Mockito): no se reemplaza a un evaluador que ya empezó su evaluación")
    void noSeReemplazaUnEvaluadorQueYaEvaluo() throws Exception {
        EvaluacionProxy proxy = mock(EvaluacionProxy.class);
        when(proxy.buscarEstadoPostulacion(any(), eq(12L))).thenReturn(EstadoPostulacion.EN_EVALUACION);
        when(proxy.listarPorPostulacion(any(), eq(12L))).thenReturn(List.of(
                asignacion("eval1@utec.edu.uy", Evaluacion.COMPLETADA),
                asignacion("eval2@utec.edu.uy", Evaluacion.EN_PROCESO)));
        EvaluacionService servicio = new EvaluacionService(proxy);

        OperacionNoPermitidaException ex = assertThrows(OperacionNoPermitidaException.class,
                () -> servicio.obtenerParaReemplazar(DatosDePrueba.administrador(), 12, "eval2@utec.edu.uy"));

        assertEquals(EvaluacionService.MSG_YA_REGISTRO, ex.getMessage());
    }

    /** Asignación de ejemplo de un evaluador, con el estado de su evaluación. */
    private static Evaluacion asignacion(String correo, String estado) {
        return new Evaluacion(1, 3, "Proyecto de prueba", EstadoPostulacion.EN_REVISION,
                "Evaluador de prueba", correo, estado, null, null);
    }
}
