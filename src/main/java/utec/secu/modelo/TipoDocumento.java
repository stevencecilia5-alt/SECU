package utec.secu.modelo;

/**
 * Un tipo de documento que pide una convocatoria (CU06), y si es obligatorio u opcional.
 * <p>
 * Los 11 tipos previstos están en {@code docu_tipos}; cada convocatoria indica en
 * {@code docu_tipo_convocatorias} cuáles pide y cuáles son obligatorios.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class TipoDocumento {

    /** Único tipo que se puede cargar varias veces (hasta 4 archivos). */
    public static final String OTROS_ADICIONALES = "Otros documentos adicionales";

    private final int idDocuTipo;
    private final String nombre;
    private final boolean obligatorio;

    public TipoDocumento(int idDocuTipo, String nombre, boolean obligatorio) {
        this.idDocuTipo = idDocuTipo;
        this.nombre = nombre;
        this.obligatorio = obligatorio;
    }

    public int getIdDocuTipo() { return idDocuTipo; }

    public String getNombre() { return nombre; }

    public boolean isObligatorio() { return obligatorio; }

    /** @return {@code true} si es "Otros documentos adicionales". */
    public boolean esAdicional() {
        return OTROS_ADICIONALES.equals(nombre);
    }
}
