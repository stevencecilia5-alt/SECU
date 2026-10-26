package utec.secu.modelo;

/**
 * Un documento cargado en una postulación (CU06).
 * <p>
 * La base guarda solo la ruta del archivo PDF, relativa a la carpeta desde donde se
 * ejecuta la aplicación (por ejemplo {@code adjuntos/P04/plan_de_trabajo.pdf}); el
 * archivo está en el disco.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class DocumentoAdjunto {

    private final long idDocuAdjunto;
    private final int idDocuTipo;
    private final String nomTipo;
    private final String rutArchivo;

    public DocumentoAdjunto(long idDocuAdjunto, int idDocuTipo, String nomTipo, String rutArchivo) {
        this.idDocuAdjunto = idDocuAdjunto;
        this.idDocuTipo = idDocuTipo;
        this.nomTipo = nomTipo;
        this.rutArchivo = rutArchivo;
    }

    public long getIdDocuAdjunto() { return idDocuAdjunto; }

    public int getIdDocuTipo() { return idDocuTipo; }

    public String getNomTipo() { return nomTipo; }

    public String getRutArchivo() { return rutArchivo; }
}
