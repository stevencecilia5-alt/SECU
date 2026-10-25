package utec.secu.modelo;

import java.util.List;

/**
 * Estados posibles de una postulación (CU07).
 * <p>
 * Cada estado guarda el valor exacto que acepta la restricción {@code CK_Post_estado}
 * de la base (sin tildes) y el texto que se muestra en consola (con tildes).
 * También define qué estados son finales y a cuáles puede pasar el Administrador a mano.
 * "En evaluación" y "Evaluada" nunca se eligen a mano: los asigna el sistema según el
 * avance de las evaluaciones (FAQ de CU09).
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public enum EstadoPostulacion {
    REGISTRADA("Registrada", "Registrada"),
    EN_REVISION("En revision", "En revisión"),
    NO_ELEGIBLE("No elegible", "No elegible"),
    EN_EVALUACION("En evaluacion", "En evaluación"),
    EVALUADA("Evaluada", "Evaluada"),
    APROBADA("Aprobada", "Aprobada"),
    RECHAZADA("Rechazada", "Rechazada"),
    CANCELADA("Cancelada", "Cancelada");

    private final String valorBD;
    private final String etiqueta;

    EstadoPostulacion(String valorBD, String etiqueta) {
        this.valorBD = valorBD;
        this.etiqueta = etiqueta;
    }

    /** @return Valor que se guarda en la columna {@code estado} de la base. */
    public String getValorBD() {
        return valorBD;
    }

    /** @return Texto que se muestra en consola. */
    public String getEtiqueta() {
        return etiqueta;
    }

    /**
     * Obtiene el estado correspondiente a un valor leído de la base.
     *
     * @param valor Valor de la columna {@code estado}.
     * @return Estado correspondiente.
     * @throws IllegalArgumentException Si el valor no corresponde a ningún estado.
     */
    public static EstadoPostulacion desdeBD(String valor) {
        for (EstadoPostulacion e : values()) {
            if (e.valorBD.equals(valor)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de postulación desconocido: " + valor);
    }

    /** @return {@code true} si el estado ya no admite cambios. */
    public boolean esFinal() {
        return this == NO_ELEGIBLE || this == APROBADA || this == RECHAZADA || this == CANCELADA;
    }

    /** @return {@code true} si la postulación todavía admite modificar sus datos. */
    public boolean admiteModificarDatos() {
        return this == REGISTRADA || this == EN_REVISION;
    }

    /**
     * Estados a los que el Administrador puede pasar a mano desde este estado,
     * en el orden en que se muestran en el menú.
     *
     * @return Lista de estados permitidos; vacía si el estado es final.
     */
    public List<EstadoPostulacion> transicionesManuales() {
        switch (this) {
            case REGISTRADA:    return List.of(EN_REVISION, CANCELADA);
            case EN_REVISION:   return List.of(NO_ELEGIBLE, CANCELADA);
            case EN_EVALUACION: return List.of(CANCELADA);
            case EVALUADA:      return List.of(APROBADA, RECHAZADA);
            default:            return List.of();
        }
    }
}
