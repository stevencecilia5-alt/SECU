package utec.secu.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Asignación de un evaluador externo a una postulación (CU08) y su evaluación (CU09).
 * <p>
 * Cada postulación tiene a lo sumo dos asignaciones. Mientras el evaluador no guarda
 * nada, la evaluación está Pendiente; al guardar un borrador pasa a En proceso, y al
 * finalizar con los 16 criterios puntuados, a Completada.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class Evaluacion {

    public static final String PENDIENTE = "Pendiente";
    public static final String EN_PROCESO = "En proceso";
    public static final String COMPLETADA = "Completada";

    private final long idAsignacion;
    private final long idPostulacion;
    private final String nomProyecto;
    private final EstadoPostulacion estadoPostulacion;
    private final String nombreEvaluador;
    private final String correoEvaluador;
    private String estado;
    /** Puntaje ponderado de 1 a 5; {@code null} hasta que la evaluación se completa. */
    private Double puntajeTotal;
    private String comentario;
    private final List<Criterio> criterios = new ArrayList<>();

    public Evaluacion(long idAsignacion, long idPostulacion, String nomProyecto, EstadoPostulacion estadoPostulacion,
                      String nombreEvaluador, String correoEvaluador, String estado, Double puntajeTotal,
                      String comentario) {
        this.idAsignacion = idAsignacion;
        this.idPostulacion = idPostulacion;
        this.nomProyecto = nomProyecto;
        this.estadoPostulacion = estadoPostulacion;
        this.nombreEvaluador = nombreEvaluador;
        this.correoEvaluador = correoEvaluador;
        this.estado = estado;
        this.puntajeTotal = puntajeTotal;
        this.comentario = comentario;
    }

    /** @return {@code true} si el evaluador ya guardó algo (En proceso o Completada). */
    public boolean estaRegistrada() {
        return !PENDIENTE.equals(estado);
    }

    public boolean estaCompletada() {
        return COMPLETADA.equals(estado);
    }

    /** @return {@code true} si no tiene ningún puntaje ni comentario: el evaluador todavía no empezó. */
    public boolean estaVacia() {
        for (Criterio c : criterios) {
            if (c.getPuntaje() != null) {
                return false;
            }
        }
        return comentario == null || comentario.isBlank();
    }

    /** @return {@code true} si todos los criterios de la rúbrica tienen puntaje. */
    public boolean todosPuntuados() {
        for (Criterio c : criterios) {
            if (c.getPuntaje() == null) {
                return false;
            }
        }
        return !criterios.isEmpty();
    }

    public long getIdAsignacion() { return idAsignacion; }

    public long getIdPostulacion() { return idPostulacion; }

    public String getNomProyecto() { return nomProyecto; }

    public EstadoPostulacion getEstadoPostulacion() { return estadoPostulacion; }

    public String getNombreEvaluador() { return nombreEvaluador; }

    public String getCorreoEvaluador() { return correoEvaluador; }

    public String getEstado() { return estado; }

    public void setEstado(String estado) { this.estado = estado; }

    public Double getPuntajeTotal() { return puntajeTotal; }

    public void setPuntajeTotal(Double puntajeTotal) { this.puntajeTotal = puntajeTotal; }

    public String getComentario() { return comentario; }

    public void setComentario(String comentario) { this.comentario = comentario; }

    public List<Criterio> getCriterios() { return criterios; }
}
