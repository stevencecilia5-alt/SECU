package utec.secu.modelo;

/**
 * Filtros de búsqueda del submenú "Buscar postulaciones" (CU07).
 * <p>
 * Solo nombra el filtro que eligió el usuario. La condición SQL de cada uno está en
 * {@code PostulacionDAO}, para que el SQL quede únicamente en la capa de acceso a datos
 * y la consola no dependa de ella.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public enum FiltroPostulacion {
    CONVOCATORIA,
    POSTULANTE,
    CORREO,
    DOCUMENTO,
    TUTOR,
    GRUPO,
    ESTADO
}
