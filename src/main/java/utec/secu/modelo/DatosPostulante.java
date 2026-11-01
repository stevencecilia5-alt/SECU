package utec.secu.modelo;

import java.util.List;

/**
 * Datos personales, de contacto y académicos que el postulante registró en su postulación (CU05)
 * y que el Administrador consulta en el detalle de CU07.
 * <p>
 * CU07 no permite modificarlos ("el postulante no puede modificar la postulación luego de enviada"):
 * todos los campos son {@code final} y no tienen setter.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class DatosPostulante {

    private final String nacionalidad;
    private final String paisProcedencia;
    private final String departamentoProcedencia;
    private final String localidadProcedencia;
    private final String domicilio;
    private final List<String> celulares;
    private final boolean docenteUtec;
    private final String actividadLaboral;
    private final String carrera;
    private final String posgrado;
    private final String institucion;

    /**
     * @param domicilio Domicilio ya armado con {@link #armarDomicilio}, o {@code null} si no se registró.
     * @param carrera   Carrera que cursa o cursó, o {@code null}.
     * @param posgrado  Posgrado que cursa, o {@code null}.
     * @param institucion Institución que dicta el posgrado, o {@code null}.
     */
    public DatosPostulante(String nacionalidad, String paisProcedencia, String departamentoProcedencia,
                           String localidadProcedencia, String domicilio, List<String> celulares,
                           boolean docenteUtec, String actividadLaboral,
                           String carrera, String posgrado, String institucion) {
        this.nacionalidad = nacionalidad;
        this.paisProcedencia = paisProcedencia;
        this.departamentoProcedencia = departamentoProcedencia;
        this.localidadProcedencia = localidadProcedencia;
        this.domicilio = domicilio;
        this.celulares = List.copyOf(celulares);
        this.docenteUtec = docenteUtec;
        this.actividadLaboral = actividadLaboral;
        this.carrera = carrera;
        this.posgrado = posgrado;
        this.institucion = institucion;
    }

    /**
     * Arma el domicilio en una línea a partir de sus partes, por ejemplo "Sarandí 880 bis, apto. 3".
     *
     * @return El domicilio, o {@code null} si no tiene calle.
     */
    public static String armarDomicilio(String calle, String numero, String apartamento, Boolean bis) {
        if (calle == null || calle.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder(calle.trim());
        if (numero != null && !numero.isBlank()) {
            sb.append(' ').append(numero.trim());
        }
        if (Boolean.TRUE.equals(bis)) {
            sb.append(" bis");
        }
        if (apartamento != null && !apartamento.isBlank()) {
            sb.append(", apto. ").append(apartamento.trim());
        }
        return sb.toString();
    }

    public String getNacionalidad() { return nacionalidad; }

    public String getPaisProcedencia() { return paisProcedencia; }

    public String getDepartamentoProcedencia() { return departamentoProcedencia; }

    public String getLocalidadProcedencia() { return localidadProcedencia; }

    public String getDomicilio() { return domicilio; }

    /** @return Los teléfonos celulares registrados (lista que no se puede modificar). */
    public List<String> getCelulares() { return celulares; }

    public boolean isDocenteUtec() { return docenteUtec; }

    public String getActividadLaboral() { return actividadLaboral; }

    public String getCarrera() { return carrera; }

    public String getPosgrado() { return posgrado; }

    public String getInstitucion() { return institucion; }
}
