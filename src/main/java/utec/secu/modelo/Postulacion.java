package utec.secu.modelo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una postulación y los datos asociados que se muestran en CU07:
 * postulante, convocatoria, departamento, sede, tutores, documentos y evaluadores.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class Postulacion {

    private final long idPostulacion;
    private EstadoPostulacion estado;   // lo cambian las transiciones de CU07 y CU09
    private final LocalDateTime fecSolicitud;

    // Datos del proyecto (departamento y sede se pueden modificar mientras está Registrada o En revisión)
    private int idDeptAcademico;
    private String deptAcademico;
    private int idSede;
    private String sede;
    private String nomProyecto;
    private String linInvestigacion;
    private int canMeses;
    private String gruInvestigacion;
    private String otrGruInvestigacion;

    // Datos que CU07 no permite modificar: son final y no tienen setter
    private final String nombrePostulante;
    private final String apellidoPostulante;
    private final String correoPostulante;
    private final String documentoPostulante;
    private final String tipoDocumento;
    private final String convocatoria;

    // Tutores
    private String tutorPrincipal;
    private String correoTutorPrincipal;
    private String coTutor;

    // Listas que se muestran en el detalle, ya formateadas
    private final List<String> documentos = new ArrayList<>();
    private final List<String> evaluadores = new ArrayList<>();

    /**
     * Crea una postulación con los datos que no se modifican: identificador, fecha de solicitud,
     * datos del postulante y convocatoria.
     */
    public Postulacion(long idPostulacion, LocalDateTime fecSolicitud, String nombrePostulante,
                       String apellidoPostulante, String correoPostulante, String documentoPostulante,
                       String tipoDocumento, String convocatoria) {
        this.idPostulacion = idPostulacion;
        this.fecSolicitud = fecSolicitud;
        this.nombrePostulante = nombrePostulante;
        this.apellidoPostulante = apellidoPostulante;
        this.correoPostulante = correoPostulante;
        this.documentoPostulante = documentoPostulante;
        this.tipoDocumento = tipoDocumento;
        this.convocatoria = convocatoria;
    }

    public long getIdPostulacion() { return idPostulacion; }

    public EstadoPostulacion getEstado() { return estado; }
    public void setEstado(EstadoPostulacion estado) { this.estado = estado; }

    public LocalDateTime getFecSolicitud() { return fecSolicitud; }

    public int getIdDeptAcademico() { return idDeptAcademico; }
    public void setIdDeptAcademico(int idDeptAcademico) { this.idDeptAcademico = idDeptAcademico; }

    public String getDeptAcademico() { return deptAcademico; }
    public void setDeptAcademico(String deptAcademico) { this.deptAcademico = deptAcademico; }

    public int getIdSede() { return idSede; }
    public void setIdSede(int idSede) { this.idSede = idSede; }

    public String getSede() { return sede; }
    public void setSede(String sede) { this.sede = sede; }

    public String getNomProyecto() { return nomProyecto; }
    public void setNomProyecto(String nomProyecto) { this.nomProyecto = nomProyecto; }

    public String getLinInvestigacion() { return linInvestigacion; }
    public void setLinInvestigacion(String linInvestigacion) { this.linInvestigacion = linInvestigacion; }

    public int getCanMeses() { return canMeses; }
    public void setCanMeses(int canMeses) { this.canMeses = canMeses; }

    public String getGruInvestigacion() { return gruInvestigacion; }
    public void setGruInvestigacion(String gruInvestigacion) { this.gruInvestigacion = gruInvestigacion; }

    public String getOtrGruInvestigacion() { return otrGruInvestigacion; }
    public void setOtrGruInvestigacion(String otrGruInvestigacion) { this.otrGruInvestigacion = otrGruInvestigacion; }

    public String getCorreoPostulante() { return correoPostulante; }

    public String getDocumentoPostulante() { return documentoPostulante; }

    public String getTipoDocumento() { return tipoDocumento; }

    public String getConvocatoria() { return convocatoria; }

    public String getTutorPrincipal() { return tutorPrincipal; }
    public void setTutorPrincipal(String tutorPrincipal) { this.tutorPrincipal = tutorPrincipal; }

    public String getCorreoTutorPrincipal() { return correoTutorPrincipal; }
    public void setCorreoTutorPrincipal(String correoTutorPrincipal) { this.correoTutorPrincipal = correoTutorPrincipal; }

    public String getCoTutor() { return coTutor; }
    public void setCoTutor(String coTutor) { this.coTutor = coTutor; }

    public List<String> getDocumentos() { return documentos; }
    public List<String> getEvaluadores() { return evaluadores; }

    /** @return Nombre y apellido del postulante. */
    public String getNombreCompletoPostulante() {
        return nombrePostulante + " " + apellidoPostulante;
    }
}
