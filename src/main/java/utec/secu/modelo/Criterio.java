package utec.secu.modelo;

/**
 * Un criterio de la rúbrica fija (CU09), con la sección a la que pertenece y el
 * puntaje que le dio el evaluador.
 * <p>
 * La rúbrica la carga el script de la base: tiene 6 secciones con un peso cada una
 * (suman 100%) y 16 criterios. Ningún menú permite modificarla.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class Criterio {

    private final int idCriterio;
    private final String seccion;
    private final double pesoSeccion;
    private final String nombre;
    /** Puntaje de 1 a 5; {@code null} mientras no se haya puntuado. */
    private Integer puntaje;

    public Criterio(int idCriterio, String seccion, double pesoSeccion, String nombre, Integer puntaje) {
        this.idCriterio = idCriterio;
        this.seccion = seccion;
        this.pesoSeccion = pesoSeccion;
        this.nombre = nombre;
        this.puntaje = puntaje;
    }

    public int getIdCriterio() { return idCriterio; }

    public String getSeccion() { return seccion; }

    /** @return Peso de la sección en la rúbrica, como porcentaje (por ejemplo 20.0). */
    public double getPesoSeccion() { return pesoSeccion; }

    public String getNombre() { return nombre; }

    public Integer getPuntaje() { return puntaje; }

    public void setPuntaje(Integer puntaje) { this.puntaje = puntaje; }
}
