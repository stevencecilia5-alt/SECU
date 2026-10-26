package utec.secu.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Situación de una postulación dentro de su convocatoria: sus evaluaciones, el puntaje
 * consolidado y los órdenes de prelación (CU11).
 * <p>
 * El orden calculado por SECU y el orden final CES se guardan por separado: registrar
 * el orden CES nunca sobrescribe el calculado.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class ResultadoPostulacion {

    private final long idPostulacion;
    private final String postulante;
    /** Evaluador 1 y Evaluador 2, en el orden en que se asignaron. */
    private final List<Evaluacion> evaluaciones = new ArrayList<>();
    /** Promedio de las dos evaluaciones; {@code null} mientras no esté Evaluada. */
    private Double puntajeConsolidado;
    private Integer ordenCalculado;
    private Integer ordenCes;
    private String observacionCes;
    /** Observación que agrega el CES mientras se arma el orden; se suma a la observación al confirmar. */
    private String notaCes;

    public ResultadoPostulacion(long idPostulacion, String postulante) {
        this.idPostulacion = idPostulacion;
        this.postulante = postulante;
    }

    public void agregarEvaluacion(Evaluacion evaluacion) {
        evaluaciones.add(evaluacion);
    }

    /**
     * @param numero 1 para el Evaluador 1, 2 para el Evaluador 2.
     * @return La evaluación, o {@code null} si todavía no tiene ese evaluador.
     */
    public Evaluacion getEvaluacion(int numero) {
        return numero <= evaluaciones.size() ? evaluaciones.get(numero - 1) : null;
    }

    public long getIdPostulacion() { return idPostulacion; }

    public String getPostulante() { return postulante; }

    public Double getPuntajeConsolidado() { return puntajeConsolidado; }

    public void setPuntajeConsolidado(Double puntajeConsolidado) { this.puntajeConsolidado = puntajeConsolidado; }

    public Integer getOrdenCalculado() { return ordenCalculado; }

    public void setOrdenCalculado(Integer ordenCalculado) { this.ordenCalculado = ordenCalculado; }

    public Integer getOrdenCes() { return ordenCes; }

    public void setOrdenCes(Integer ordenCes) { this.ordenCes = ordenCes; }

    public String getObservacionCes() { return observacionCes; }

    public void setObservacionCes(String observacionCes) { this.observacionCes = observacionCes; }

    public String getNotaCes() { return notaCes; }

    public void setNotaCes(String notaCes) { this.notaCes = notaCes; }
}
