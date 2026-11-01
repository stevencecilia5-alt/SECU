package utec.secu.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.ResultadoPostulacion;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de CU11 (opcional): reglas del orden final CES.
 */
class ResultadoServiceTest {

    @Test
    @DisplayName("CU11: mover una postulación sin motivo se rechaza y el orden no cambia")
    void moverSinMotivoSeRechaza() {
        List<ResultadoPostulacion> orden = prelacion(9, 11, 12);

        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> ResultadoService.mover(orden, 3, 1, "   "));

        assertEquals(ResultadoService.MSG_OBSERVACION, ex.getMessage());
        assertEquals(9L, orden.get(0).getIdPostulacion());
    }

    @Test
    @DisplayName("CU11: al mover una postulación, las demás se corren y reciben una observación automática")
    void lasCorridasRecibenObservacionAutomatica() {
        List<ResultadoPostulacion> orden = prelacion(9, 11, 12);

        ResultadoService.mover(orden, 3, 1, "Acta CES 12");
        ResultadoService.cerrarOrdenCes(orden);

        ResultadoPostulacion movida = orden.get(0);
        ResultadoPostulacion corrida = orden.get(1);
        assertEquals(12L, movida.getIdPostulacion());
        assertEquals("Acta CES 12", movida.getObservacionCes());
        assertEquals(9L, corrida.getIdPostulacion());
        assertEquals(Integer.valueOf(2), corrida.getOrdenCes());
        assertEquals("Quedó en el puesto 2 (orden calculado: 1) por las decisiones del CES: "
                + "postulación 12 al puesto 1.", corrida.getObservacionCes());
    }

    /** Lista de prelación con las postulaciones indicadas, en ese orden calculado. */
    private static List<ResultadoPostulacion> prelacion(long... ids) {
        List<ResultadoPostulacion> lista = new ArrayList<>();
        for (long id : ids) {
            lista.add(new ResultadoPostulacion(id, null, "Postulante " + id, "correo" + id + "@gmail.com",
                    "Proyecto " + id, "Grupo", null, EstadoPostulacion.EVALUADA));
        }
        ResultadoService.prepararRegistro(lista);
        return lista;
    }
}
