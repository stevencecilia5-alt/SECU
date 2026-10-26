package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Criterio;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.Postulacion;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.EvaluacionService;
import utec.secu.servicio.PostulacionService;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Menú de consola de CU09: Mis evaluaciones (solo Evaluador externo).
 * <p>
 * El evaluador ve las postulaciones que tiene asignadas, las evalúa con la rúbrica
 * fija (16 criterios con puntaje entero de 1 a 5) y puede guardar un borrador o
 * finalizar. Las reglas están en {@link EvaluacionService}.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuMisEvaluaciones extends MenuBase {

    private final EvaluacionService servicio;

    public MenuMisEvaluaciones(Scanner sc, Usuario sesion) {
        super(sc, sesion);
        this.servicio = new EvaluacionService();
    }

    /** Muestra el submenú hasta que el usuario elige Volver. */
    public void mostrar() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Mis evaluaciones ---");
            System.out.println("1. Ver mis postulaciones asignadas");
            System.out.println("2. Completar / continuar una evaluación");
            System.out.println("3. Ver detalle de una postulación asignada");
            System.out.println("4. Volver");

            // Si una opción falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (leer("Elegí una opción: ").trim()) {
                    case "1": listar(); break;
                    case "2": evaluar(); break;
                    case "3": verDetalle(); break;
                    case "4": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    // =========================================================================
    // 1. MIS POSTULACIONES
    // =========================================================================

    private void listar() {
        List<Evaluacion> lista = servicio.listarMisEvaluaciones(sesion);
        if (lista.isEmpty()) {
            System.out.println("No tenés postulaciones asignadas.");
            return;
        }
        System.out.printf("%n%-4s %-42s %-14s %s%n", "ID", "Proyecto", "Postulación", "Mi evaluación");
        System.out.println("-".repeat(78));
        for (Evaluacion e : lista) {
            System.out.printf("%-4d %-42s %-14s %s%n", e.getIdPostulacion(), recortar(e.getNomProyecto(), 42),
                    e.getEstadoPostulacion().getEtiqueta(), e.getEstado());
        }
    }

    // =========================================================================
    // 2. COMPLETAR / CONTINUAR UNA EVALUACIÓN
    // =========================================================================

    private void evaluar() {
        Evaluacion e = servicio.obtenerParaEvaluar(sesion, pedirId());
        System.out.println("(Puntaje entero de 1 a 5. Presioná Enter para conservar el valor actual o dejarlo sin puntaje)");

        String seccionActual = "";
        for (Criterio c : e.getCriterios()) {
            if (!c.getSeccion().equals(seccionActual)) {
                seccionActual = c.getSeccion();
                System.out.println("\n" + seccionActual + " (" + formatoPeso(c.getPesoSeccion()) + "%)");
            }
            c.setPuntaje(pedirPuntaje(c));
        }
        String comentario = leer("\nComentario [" + (e.getComentario() == null ? "" : e.getComentario()) + "]: ").trim();
        if (!comentario.isEmpty()) {
            e.setComentario(comentario);
        }

        while (true) {
            System.out.println("\n1. Guardar como borrador");
            System.out.println("2. Finalizar evaluación");
            System.out.println("3. Cancelar sin guardar");
            switch (leer("Elegí una opción: ").trim()) {
                case "1":
                    servicio.guardarBorrador(sesion, e);
                    System.out.println("Evaluación guardada como borrador.");
                    return;
                case "2":
                    boolean evaluada = servicio.finalizar(sesion, e);
                    System.out.println("Evaluación completada. Puntaje total: "
                            + MenuAsignacion.formatoPuntaje(e.getPuntajeTotal()) + ".");
                    if (evaluada) {
                        System.out.println("Las dos evaluaciones están completas: la postulación pasó a Evaluada.");
                    }
                    return;
                case "3":
                    System.out.println("Cambios descartados.");
                    return;
                default:
                    System.out.println("Opción inválida.");
            }
        }
    }

    /** Pide el puntaje de un criterio hasta que sea válido. Enter conserva el valor actual. */
    private Integer pedirPuntaje(Criterio c) {
        while (true) {
            String actual = c.getPuntaje() == null ? "sin puntaje" : String.valueOf(c.getPuntaje());
            try {
                Integer puntaje = EvaluacionService.parsearPuntaje(leer("  " + c.getNombre() + " [" + actual + "]: "));
                return puntaje == null ? c.getPuntaje() : puntaje;
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    // =========================================================================
    // 3. VER DETALLE (solo lectura)
    // =========================================================================

    private void verDetalle() {
        long id = pedirId();
        Postulacion p = servicio.obtenerPostulacionAsignada(sesion, id);
        Evaluacion e = servicio.obtenerMiEvaluacion(sesion, id);

        System.out.println("\n--- Postulación " + p.getIdPostulacion() + " ---");
        System.out.println("Postulante:         " + p.getNombreCompletoPostulante());
        System.out.println("Convocatoria:       " + p.getConvocatoria());
        System.out.println("Proyecto:           " + p.getNomProyecto());
        System.out.println("Línea:              " + p.getLinInvestigacion());
        System.out.println("Grupo:              " + p.getGruInvestigacion()
                + (p.getOtrGruInvestigacion() == null ? "" : " / " + p.getOtrGruInvestigacion()));
        System.out.println("Departamento:       " + p.getDeptAcademico());
        System.out.println("Sede:               " + p.getSede());
        System.out.println("Meses solicitados:  " + p.getCanMeses());
        System.out.println("Estado:             " + p.getEstado().getEtiqueta());
        System.out.println("Tutor principal:    " + (p.getTutorPrincipal() == null ? "(sin asignar)"
                : p.getTutorPrincipal() + " (" + p.getCorreoTutorPrincipal() + ")"));
        System.out.println("Co-tutor:           " + (p.getCoTutor() == null ? "(sin asignar)" : p.getCoTutor()));
        System.out.println("Documentos adjuntos:");
        if (p.getDocumentos().isEmpty()) {
            System.out.println("  (ninguno)");
        }
        for (String d : p.getDocumentos()) {
            System.out.println("  - " + d);
        }

        System.out.println("\nRúbrica (pesos fijos):");
        double suma = 0;
        for (Map.Entry<String, Double> s : EvaluacionService.seccionesDe(e.getCriterios()).entrySet()) {
            System.out.println("  - " + s.getKey() + ": " + formatoPeso(s.getValue()) + "%");
            suma += s.getValue();
        }
        System.out.println("  Total: " + formatoPeso(suma) + "%");

        System.out.println("\nMi evaluación: " + e.getEstado());
        if (!e.estaRegistrada()) {
            return;
        }
        String seccionActual = "";
        for (Criterio c : e.getCriterios()) {
            if (!c.getSeccion().equals(seccionActual)) {
                seccionActual = c.getSeccion();
                System.out.println("  " + seccionActual);
            }
            System.out.println("    - " + c.getNombre() + ": " + (c.getPuntaje() == null ? "sin puntaje" : c.getPuntaje()));
        }
        System.out.println("Comentario:    " + (e.getComentario() == null ? "" : e.getComentario()));
        System.out.println("Puntaje total: " + MenuAsignacion.formatoPuntaje(e.getPuntajeTotal()));
    }

    // =========================================================================
    // AUXILIARES DE CONSOLA
    // =========================================================================

    private long pedirId() {
        return PostulacionService.parsearId(leer("Identificador de la postulación: "));
    }

    /** @return El peso sin decimales si es entero (20.0 -> "20"). */
    private static String formatoPeso(double peso) {
        return peso == Math.rint(peso) ? String.valueOf((long) peso) : String.valueOf(peso);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
