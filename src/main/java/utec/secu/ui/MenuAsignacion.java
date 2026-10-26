package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.EvaluacionService;
import utec.secu.servicio.PostulacionService;

import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Menú de consola de CU08: Asignación de evaluadores (solo Administrador).
 * <p>
 * Solo lee datos y muestra resultados; las reglas de negocio están en
 * {@link EvaluacionService} y el control de acceso en el Proxy.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuAsignacion extends MenuBase {

    private final EvaluacionService servicio;

    public MenuAsignacion(Scanner sc, Usuario sesion) {
        super(sc, sesion);
        this.servicio = new EvaluacionService();
    }

    /** Muestra el submenú hasta que el usuario elige Volver. */
    public void mostrar() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Asignación de evaluadores ---");
            System.out.println("1. Asignar evaluador a una postulación");
            System.out.println("2. Ver evaluadores asignados a una postulación");
            System.out.println("3. Ver postulaciones asignadas a un evaluador");
            System.out.println("4. Modificar un evaluador asignado");
            System.out.println("5. Registrar juicio global de una postulación");
            System.out.println("6. Volver");

            // Si una opción falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (leer("Elegí una opción: ").trim()) {
                    case "1": asignar(); break;
                    case "2": verEvaluadores(); break;
                    case "3": verPostulacionesDeEvaluador(); break;
                    case "4": modificarEvaluador(); break;
                    case "5": registrarJuicioGlobal(); break;
                    case "6": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void asignar() {
        long id = pedirId();
        String correo = leer("Correo del evaluador: ");
        int asignados = servicio.asignarEvaluador(sesion, id, correo);
        System.out.println("Evaluador asignado correctamente (" + asignados + " de "
                + EvaluacionService.EVALUADORES_POR_POSTULACION + ").");
        System.out.println("Estado de la postulación: " + servicio.obtenerEstado(sesion, id).getEtiqueta());
    }

    private void verEvaluadores() {
        long id = pedirId();
        List<Evaluacion> lista = servicio.listarEvaluadores(sesion, id);
        System.out.println("Estado de la postulación: " + servicio.obtenerEstado(sesion, id).getEtiqueta());
        imprimirEvaluadores(lista);
    }

    private void verPostulacionesDeEvaluador() {
        List<Evaluacion> lista = servicio.listarPostulacionesDe(sesion, leer("Correo del evaluador: "));
        if (lista.isEmpty()) {
            System.out.println("El evaluador no tiene postulaciones asignadas.");
            return;
        }
        System.out.printf("%n%-4s %-42s %-14s %-12s %s%n", "ID", "Proyecto", "Postulación", "Evaluación", "Puntaje");
        System.out.println("-".repeat(85));
        for (Evaluacion e : lista) {
            System.out.printf("%-4d %-42s %-14s %-12s %s%n", e.getIdPostulacion(), recortar(e.getNomProyecto(), 42),
                    e.getEstadoPostulacion().getEtiqueta(), e.getEstado(), formatoPuntaje(e.getPuntajeTotal()));
        }
    }

    private void modificarEvaluador() {
        long id = pedirId();
        List<Evaluacion> lista = servicio.listarEvaluadores(sesion, id);
        if (lista.isEmpty()) {
            System.out.println("La postulación no tiene evaluadores asignados.");
            return;
        }
        imprimirEvaluadores(lista);
        String actual = leer("Correo del evaluador a reemplazar: ");
        servicio.obtenerParaReemplazar(sesion, id, actual);
        String nuevo = leer("Correo del nuevo evaluador: ");
        servicio.verificarNuevoEvaluador(sesion, id, nuevo);
        if (!confirmar("¿Confirmás el cambio de evaluador? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        servicio.reemplazarEvaluador(sesion, id, actual, nuevo);
        System.out.println("Evaluador modificado correctamente.");
    }

    private void registrarJuicioGlobal() {
        long id = pedirId();
        String actual = servicio.obtenerJuicioGlobal(sesion, id);
        if (actual != null) {
            System.out.println("Juicio global actual: " + actual);
            if (!confirmar("¿Querés reemplazarlo? (s/n): ")) {
                System.out.println("Cambios descartados.");
                return;
            }
        }
        servicio.registrarJuicioGlobal(sesion, id, leer("Juicio global: "));
        System.out.println("Juicio global registrado correctamente.");
    }

    // =========================================================================
    // AUXILIARES DE CONSOLA
    // =========================================================================

    private void imprimirEvaluadores(List<Evaluacion> lista) {
        if (lista.isEmpty()) {
            System.out.println("La postulación no tiene evaluadores asignados.");
            return;
        }
        System.out.printf("%n%-24s %-30s %-12s %s%n", "Evaluador", "Correo", "Evaluación", "Puntaje");
        System.out.println("-".repeat(78));
        for (Evaluacion e : lista) {
            System.out.printf("%-24s %-30s %-12s %s%n", recortar(e.getNombreEvaluador(), 24),
                    recortar(e.getCorreoEvaluador(), 30), e.getEstado(), formatoPuntaje(e.getPuntajeTotal()));
        }
    }

    private long pedirId() {
        return PostulacionService.parsearId(leer("Identificador de la postulación: "));
    }

    static String formatoPuntaje(Double puntaje) {
        return puntaje == null ? "-" : String.format(Locale.ROOT, "%.2f", puntaje);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
