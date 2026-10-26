package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.ResultadoPostulacion;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.ResultadoService;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Menús de consola de CU11 (registro del orden final CES) y CU12 (vista consolidada y
 * reporte): opciones 6 y 7 del menú de postulaciones. Los dos trabajan sobre una
 * convocatoria, por eso comparten la elección de la convocatoria.
 * <p>
 * Solo leen datos y muestran resultados; las reglas de negocio están en
 * {@link ResultadoService} y el control de acceso en el Proxy.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuResultados extends MenuBase {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ResultadoService servicio;

    public MenuResultados(Scanner sc, Usuario sesion) {
        super(sc, sesion);
        this.servicio = new ResultadoService();
    }

    // =========================================================================
    // 1. ORDEN FINAL CES (CU11)
    // =========================================================================

    /** Muestra el submenú de CU11 hasta que el usuario elige Volver. */
    public void mostrarOrdenCes() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Orden final CES ---");
            System.out.println("1. Ver lista de prelación de una convocatoria");
            System.out.println("2. Registrar orden final CES");
            System.out.println("3. Volver");

            // Si una opción falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (leer("Elegí una opción: ").trim()) {
                    case "1": verPrelacion(); break;
                    case "2": registrarOrdenCes(); break;
                    case "3": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void verPrelacion() {
        Map<Long, String> convocatorias = servicio.listarConvocatorias(sesion);
        long id = elegirConvocatoria(convocatorias);
        if (id == 0) {
            return;
        }
        List<ResultadoPostulacion> prelacion = servicio.listarPrelacion(sesion, id);
        System.out.println("\nLista de prelación - " + convocatorias.get(id));
        imprimirPrelacion(prelacion);
        for (ResultadoPostulacion r : prelacion) {
            if (r.getOrdenCalculado() == null) {
                System.out.println("Hay postulaciones evaluadas después de registrar el orden final CES. "
                        + "Para incluirlas, registrá de nuevo el orden final CES.");
                break;
            }
        }
        if (!ResultadoService.tieneOrdenCes(prelacion)) {
            System.out.println("Todavía no se registró el orden final CES.");
        }
    }

    private void registrarOrdenCes() {
        Map<Long, String> convocatorias = servicio.listarConvocatorias(sesion);
        long id = elegirConvocatoria(convocatorias);
        if (id == 0) {
            return;
        }
        List<ResultadoPostulacion> prelacion = servicio.listarPrelacion(sesion, id);
        if (ResultadoService.tieneOrdenCes(prelacion)
                && !confirmar("La convocatoria ya tiene un orden final CES registrado. ¿Querés reemplazarlo? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        // El orden CES anterior solo se muestra como referencia: el nuevo empieza del orden calculado
        Map<Long, Integer> anteriores = new HashMap<>();
        for (ResultadoPostulacion r : prelacion) {
            anteriores.put(r.getIdPostulacion(), r.getOrdenCes());
        }
        ResultadoService.prepararRegistro(prelacion);
        final List<ResultadoPostulacion> orden = new ArrayList<>(prelacion);

        boolean armando = true;
        while (armando) {
            System.out.println("\nOrden final CES - " + convocatorias.get(id));
            imprimirArmado(orden, anteriores);
            System.out.println("\n1. Mover una postulación");
            System.out.println("2. Agregar una observación");
            System.out.println("3. Volver al orden calculado");
            System.out.println("4. Confirmar y guardar");
            System.out.println("5. Cancelar");
            switch (leer("Elegí una opción: ").trim()) {
                case "1":
                    moverPostulacion(orden);
                    break;
                case "2":
                    agregarObservacion(orden);
                    break;
                case "3":
                    ResultadoService.prepararRegistro(prelacion);
                    orden.clear();
                    orden.addAll(prelacion);
                    break;
                case "4":
                    guardarOrdenCes(id, orden);
                    armando = false;
                    break;
                case "5":
                    System.out.println("Cambios descartados.");
                    armando = false;
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        }
    }

    /** Pide qué postulación se mueve, a qué puesto y el motivo del CES. Con Enter se vuelve sin mover. */
    private void moverPostulacion(List<ResultadoPostulacion> orden) {
        int actual = elegir("Puesto de la postulación que se mueve (Enter para volver): ", orden.size());
        if (actual == 0) {
            return;
        }
        int nuevo = elegir("Puesto nuevo de la postulación " + orden.get(actual - 1).getIdPostulacion()
                + " (1 a " + orden.size() + ", Enter para volver): ", orden.size());
        if (nuevo == 0) {
            return;
        }
        if (nuevo == actual) {
            System.out.println("La postulación ya está en ese puesto.");
            return;
        }
        ResultadoService.mover(orden, actual, nuevo, pedirObservacion("  Motivo de la decisión del CES: "));
    }

    /**
     * Pide a qué postulación se le agrega una observación del CES, aunque quede en su orden calculado.
     * Con Enter se vuelve sin agregar.
     */
    private void agregarObservacion(List<ResultadoPostulacion> orden) {
        int puesto = elegir("Puesto de la postulación (Enter para volver): ", orden.size());
        if (puesto == 0) {
            return;
        }
        ResultadoService.anotar(orden, puesto, pedirObservacion("  Observación del CES: "));
        System.out.println("Observación agregada a la postulación " + orden.get(puesto - 1).getIdPostulacion() + ".");
    }

    /** Completa las observaciones, muestra el orden final y lo guarda si el usuario confirma. */
    private void guardarOrdenCes(long idConvocatoria, List<ResultadoPostulacion> orden) {
        ResultadoService.cerrarOrdenCes(orden);
        System.out.println("\nOrden final CES a registrar:");
        imprimirPrelacion(orden);
        if (!confirmar("¿Confirmás el orden final CES? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        servicio.registrarOrdenCes(sesion, idConvocatoria, orden);
        System.out.println("Orden final CES registrado correctamente.");
        System.out.println("El resultado de cada postulación (Aprobada o Rechazada) se registra en "
                + "\"Cambiar estado de una postulación\" (opción 4 del menú de postulaciones).");
    }

    /** Pide la observación del CES hasta que no esté vacía. */
    private String pedirObservacion(String mensaje) {
        while (true) {
            try {
                return ResultadoService.validarObservacion(leer(mensaje));
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    /** Muestra el orden que se está armando, con el orden calculado y el orden CES anterior como referencia. */
    private void imprimirArmado(List<ResultadoPostulacion> orden, Map<Long, Integer> anteriores) {
        System.out.printf("%-7s %-4s %-26s %-12s %-10s %s%n",
                "Puesto", "ID", "Postulante", "Consolidado", "Calculado", "CES anterior");
        System.out.println("-".repeat(76));
        for (int i = 0; i < orden.size(); i++) {
            ResultadoPostulacion r = orden.get(i);
            System.out.printf("%-7d %-4d %-26s %-12s %-10s %s%n",
                    i + 1, r.getIdPostulacion(), recortar(r.getPostulante(), 26),
                    MenuAsignacion.formatoPuntaje(r.getPuntajeConsolidado()), numero(r.getOrdenCalculado()),
                    numero(anteriores.get(r.getIdPostulacion())));
        }
        for (ResultadoPostulacion r : orden) {
            if (r.getObservacionCes() != null) {
                System.out.println("Movida por el CES: postulación " + r.getIdPostulacion() + " - "
                        + r.getObservacionCes());
            }
            if (r.getNotaCes() != null) {
                System.out.println("Observación agregada: postulación " + r.getIdPostulacion() + " - " + r.getNotaCes());
            }
        }
    }

    private void imprimirPrelacion(List<ResultadoPostulacion> prelacion) {
        System.out.printf("%-10s %-4s %-26s %-8s %-8s %-12s %s%n",
                "Calculado", "ID", "Postulante", "Eval. 1", "Eval. 2", "Consolidado", "Orden CES");
        System.out.println("-".repeat(84));
        for (ResultadoPostulacion r : prelacion) {
            System.out.printf("%-10s %-4d %-26s %-8s %-8s %-12s %s%n",
                    numero(r.getOrdenCalculado()), r.getIdPostulacion(), recortar(r.getPostulante(), 26),
                    puntajeEvaluador(r.getEvaluacion(1)), puntajeEvaluador(r.getEvaluacion(2)),
                    MenuAsignacion.formatoPuntaje(r.getPuntajeConsolidado()), numero(r.getOrdenCes()));
        }
        for (ResultadoPostulacion r : prelacion) {
            if (r.getObservacionCes() != null) {
                System.out.println("Observación CES de la postulación " + r.getIdPostulacion() + ": "
                        + r.getObservacionCes());
            }
        }
    }

    // =========================================================================
    // 2. VISTA CONSOLIDADA Y REPORTE (CU12)
    // =========================================================================

    /**
     * Muestra la vista consolidada de una convocatoria y ofrece exportarla a Excel.
     * Si algo falla, el mensaje lo muestra el menú de postulaciones.
     */
    public void mostrarVistaConsolidada() {
        System.out.println("\n--- Vista consolidada y reportes ---");
        Map<Long, String> convocatorias = servicio.listarConvocatorias(sesion);
        long id = elegirConvocatoria(convocatorias);
        if (id == 0) {
            return;
        }
        List<ResultadoPostulacion> resultados = servicio.listarResultados(sesion, id);
        System.out.println("\n=== Vista consolidada - " + convocatorias.get(id) + " ===");
        System.out.println(resultados.size() + " postulaciones, ordenadas por puntaje consolidado.");
        for (ResultadoPostulacion r : resultados) {
            imprimirResultado(r);
        }
        if (confirmar("\n¿Querés exportar esta vista a Excel? (s/n): ")) {
            Path archivo = ResultadoService.exportarExcel(convocatorias.get(id), id, resultados);
            System.out.println("Archivo de Excel generado: " + archivo);
        }
    }

    private void imprimirResultado(ResultadoPostulacion r) {
        System.out.println("\nPostulación " + r.getIdPostulacion() + " | " + r.getEstado().getEtiqueta()
                + " | Fecha de postulación: " + r.getFecSolicitud().format(FORMATO_FECHA_HORA));
        System.out.println("  Postulante:      " + r.getPostulante() + " (" + r.getCorreo() + ")");
        System.out.println("  Proyecto:        " + r.getProyecto());
        System.out.println("  Grupo:           " + r.getGrupo());
        System.out.println("  Tutor:           " + texto(r.getTutor()));
        System.out.println("  Co-tutor:        " + texto(r.getCoTutor()));
        imprimirEvaluador(1, r.getEvaluacion(1));
        imprimirEvaluador(2, r.getEvaluacion(2));
        System.out.println("  Consolidado:     " + MenuAsignacion.formatoPuntaje(r.getPuntajeConsolidado())
                + "   Orden calculado: " + numero(r.getOrdenCalculado())
                + "   Orden final CES: " + numero(r.getOrdenCes()));
        System.out.println("  Observación CES: " + texto(r.getObservacionCes()));
        System.out.println("  Juicio global:   " + texto(r.getJuicioGlobal()));
    }

    private void imprimirEvaluador(int numero, Evaluacion e) {
        if (e == null) {
            System.out.println("  Evaluador " + numero + ":     (sin asignar)");
            return;
        }
        System.out.println("  Evaluador " + numero + ":     " + e.getNombreEvaluador() + " - " + e.getEstado()
                + " - Puntaje " + MenuAsignacion.formatoPuntaje(e.getPuntajeTotal()));
        if (e.getComentario() != null) {
            System.out.println("                   Comentarios: " + e.getComentario());
        }
    }

    // =========================================================================
    // AUXILIARES DE CONSOLA
    // =========================================================================

    /** @return El identificador de la convocatoria elegida, o 0 si el usuario volvió. */
    private long elegirConvocatoria(Map<Long, String> convocatorias) {
        List<Long> ids = new ArrayList<>(convocatorias.keySet());
        System.out.println("Convocatorias:");
        if (ids.isEmpty()) {
            System.out.println("  (ninguna)");
        }
        for (int i = 0; i < ids.size(); i++) {
            System.out.println((i + 1) + ") " + convocatorias.get(ids.get(i)));
        }
        int opcion = elegir("Convocatoria (Enter para volver): ", ids.size());
        return opcion == 0 ? 0 : ids.get(opcion - 1);
    }

    private static String puntajeEvaluador(Evaluacion e) {
        return e == null ? "-" : MenuAsignacion.formatoPuntaje(e.getPuntajeTotal());
    }

    private static String numero(Integer valor) {
        return valor == null ? "-" : String.valueOf(valor);
    }

    private static String texto(String valor) {
        return valor == null ? "-" : valor;
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 3) + "...";
    }
}
