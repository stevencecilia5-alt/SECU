package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.DatosPostulante;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.FiltroPostulacion;
import utec.secu.modelo.Postulacion;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.PostulacionService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Menú de consola de CU07: Gestión y revisión de postulaciones.
 * <p>
 * Solo lee datos y muestra resultados; las reglas de negocio están en
 * {@link PostulacionService} y el control de acceso en el Proxy.
 * Las opciones 5, 6 y 7 corresponden a CU06, CU11 y CU12.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuPostulaciones extends MenuBase {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_REGISTRAR = "(sin registrar)";

    private final PostulacionService servicio;

    public MenuPostulaciones(Scanner sc, Usuario sesion) {
        super(sc, sesion);
        this.servicio = new PostulacionService();
    }

    /** Muestra el submenú hasta que el usuario elige Volver. */
    public void mostrar() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Gestión y revisión de postulaciones ---");
            System.out.println("1. Listar / buscar postulaciones");
            System.out.println("2. Ver detalle de una postulación");
            System.out.println("3. Modificar datos de una postulación");
            System.out.println("4. Cambiar estado de una postulación");
            System.out.println("5. Documentos adjuntos");
            System.out.println("6. Registrar orden final CES");
            System.out.println("7. Vista consolidada y reportes");
            System.out.println("8. Volver");
            String opcion = leer("Elegí una opción: ").trim();

            // Si una opción falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (opcion) {
                    case "1": menuBuscar(); break;
                    case "2": verDetalle(); break;
                    case "3": modificar(); break;
                    case "4": cambiarEstado(); break;
                    case "5": new MenuDocumentos(sc, sesion).mostrar(); break;
                    case "6": new MenuResultados(sc, sesion).mostrarOrdenCes(); break;
                    case "7": new MenuResultados(sc, sesion).mostrarVistaConsolidada(); break;
                    case "8": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    // =========================================================================
    // 1. LISTAR / BUSCAR
    // =========================================================================

    private void menuBuscar() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Buscar postulaciones ---");
            System.out.println("1. Convocatoria");
            System.out.println("2. Postulante");
            System.out.println("3. Correo electrónico");
            System.out.println("4. Documento de identidad");
            System.out.println("5. Tutor");
            System.out.println("6. Grupo de investigación");
            System.out.println("7. Estado");
            System.out.println("8. Rango de fechas");
            System.out.println("9. Mostrar todas");
            System.out.println("10. Volver");
            String opcion = leer("Elegí una opción: ").trim();

            // Si una búsqueda falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (opcion) {
                    case "1": buscarPorTexto(FiltroPostulacion.CONVOCATORIA, "Convocatoria"); break;
                    case "2": buscarPorTexto(FiltroPostulacion.POSTULANTE, "Postulante"); break;
                    case "3": buscarPorTexto(FiltroPostulacion.CORREO, "Correo electrónico"); break;
                    case "4": buscarPorTexto(FiltroPostulacion.DOCUMENTO, "Documento de identidad"); break;
                    case "5": buscarPorTexto(FiltroPostulacion.TUTOR, "Tutor"); break;
                    case "6": buscarPorTexto(FiltroPostulacion.GRUPO, "Grupo de investigación"); break;
                    case "7": buscarPorEstado(); break;
                    case "8": buscarPorRango(); break;
                    case "9": imprimirListado(servicio.listarTodas(sesion)); break;
                    case "10": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void buscarPorTexto(FiltroPostulacion filtro, String etiqueta) {
        String valor = leer(etiqueta + ": ");
        imprimirListado(servicio.buscar(sesion, filtro, valor));
    }

    private void buscarPorEstado() {
        EstadoPostulacion[] estados = EstadoPostulacion.values();
        while (true) {
            System.out.println("Estado:");
            for (int i = 0; i < estados.length; i++) {
                System.out.println((i + 1) + ") " + estados[i].getEtiqueta());
            }
            int opcion = convertirOpcion(leer("Elegí una opción: ").trim(), estados.length);
            if (opcion > 0) {
                imprimirListado(servicio.buscarPorEstado(sesion, estados[opcion - 1]));
                return;
            }
            System.out.println("Opción inválida.");
        }
    }

    private void buscarPorRango() {
        while (true) {
            LocalDate inicio = pedirFecha("Fecha de inicio (dd/mm/aaaa): ");
            LocalDate fin = pedirFecha("Fecha de fin (dd/mm/aaaa): ");
            try {
                PostulacionService.validarRango(inicio, fin);
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
                continue;
            }
            imprimirListado(servicio.buscarPorRango(sesion, inicio, fin));
            return;
        }
    }

    private void imprimirListado(List<Postulacion> lista) {
        if (lista.isEmpty()) {
            System.out.println("No se encontraron postulaciones.");
            return;
        }
        System.out.printf("%n%-4s %-24s %-42s %-14s %s%n", "ID", "Postulante", "Proyecto", "Estado", "Fecha de solicitud");
        System.out.println("-".repeat(105));
        for (Postulacion p : lista) {
            System.out.printf("%-4d %-24s %-42s %-14s %s%n",
                    p.getIdPostulacion(),
                    recortar(p.getNombreCompletoPostulante(), 24),
                    recortar(p.getNomProyecto(), 42),
                    p.getEstado().getEtiqueta(),
                    p.getFecSolicitud() == null ? "" : p.getFecSolicitud().format(FORMATO_FECHA_HORA));
        }
    }

    // =========================================================================
    // 2. VER DETALLE
    // =========================================================================

    /** Muestra todos los datos registrados en CU05, los documentos de CU06 y los evaluadores (CU07). */
    private void verDetalle() {
        Postulacion p = servicio.obtenerDetalle(sesion, pedirId());
        System.out.println("\n--- Postulación " + p.getIdPostulacion() + " ---");
        imprimirPostulante(p);
        System.out.println();
        imprimirPropuesta(p);
        imprimirLista("Documentos adjuntos:", p.getDocumentos());
        imprimirLista("Evaluadores:", p.getEvaluadores());
    }

    /**
     * Muestra los datos personales, de contacto y académicos que el postulante registró en CU05.
     * Solo los ve el Administrador: el detalle del evaluador (CU09) muestra únicamente la propuesta.
     */
    private void imprimirPostulante(Postulacion p) {
        DatosPostulante d = p.getDatosPostulante();
        System.out.println("Postulante:         " + p.getNombreCompletoPostulante() + " ("
                + p.getTipoDocumento() + " " + p.getDocumentoPostulante() + ")");
        System.out.println("Correo:             " + p.getCorreoPostulante());
        System.out.println("Celular:            "
                + (d.getCelulares().isEmpty() ? SIN_REGISTRAR : String.join(" / ", d.getCelulares())));
        System.out.println("Domicilio:          " + textoOSinRegistrar(d.getDomicilio()));
        System.out.println("Nacionalidad:       " + d.getNacionalidad());
        System.out.println("Procedencia:        " + d.getLocalidadProcedencia() + ", "
                + d.getDepartamentoProcedencia() + " (" + d.getPaisProcedencia() + ")");
        System.out.println("Carrera:            " + textoOSinRegistrar(d.getCarrera()));
        System.out.println("Posgrado:           " + (d.getPosgrado() == null ? SIN_REGISTRAR
                : d.getPosgrado() + " (" + d.getInstitucion() + ")"));
        System.out.println("Docente en UTEC:    " + (d.isDocenteUtec() ? "Sí" : "No"));
        System.out.println("Actividad laboral:  " + textoOSinRegistrar(d.getActividadLaboral()));
    }

    /**
     * Muestra los datos de la propuesta, de la convocatoria al co-tutor. También los usa el detalle
     * del evaluador (CU09), que no muestra los datos personales del postulante.
     */
    static void imprimirPropuesta(Postulacion p) {
        System.out.println("Convocatoria:       " + p.getConvocatoria());
        System.out.println("Tipo de beca:       " + p.getTipoBeca());
        if (p.getDescripcionConvocatoria() != null) {
            System.out.println("Descripción:        " + p.getDescripcionConvocatoria());
        }
        System.out.println("Proyecto:           " + p.getNomProyecto());
        System.out.println("Línea:              " + p.getLinInvestigacion());
        System.out.println("Grupo:              " + p.getGruInvestigacion()
                + (p.getOtrGruInvestigacion() == null ? "" : " / " + p.getOtrGruInvestigacion()));
        System.out.println("Departamento:       " + p.getDeptAcademico());
        System.out.println("Sede:               " + p.getSede());
        System.out.println("Meses solicitados:  " + p.getCanMeses());
        System.out.println("Fecha de solicitud: " + p.getFecSolicitud().format(FORMATO_FECHA_HORA));
        System.out.println("Estado:             " + p.getEstado().getEtiqueta());
        System.out.println("Tutor principal:    " + (p.getTutorPrincipal() == null ? "(sin asignar)"
                : p.getTutorPrincipal() + " (" + p.getCorreoTutorPrincipal() + ")"));
        System.out.println("Co-tutor:           " + (p.getCoTutor() == null ? "(sin asignar)" : p.getCoTutor()));
    }

    private void imprimirLista(String titulo, List<String> elementos) {
        System.out.println(titulo);
        if (elementos.isEmpty()) {
            System.out.println("  (ninguno)");
        }
        for (String e : elementos) {
            System.out.println("  - " + e);
        }
    }

    // =========================================================================
    // 3. MODIFICAR DATOS
    // =========================================================================

    private void modificar() {
        Postulacion p = servicio.obtenerParaModificar(sesion, pedirId());
        System.out.println("\n--- Modificar postulación " + p.getIdPostulacion() + " ---");
        System.out.println("(Presioná Enter para conservar el valor actual)");

        Map<Integer, String> departamentos = servicio.listarDepartamentos(sesion);
        p.setIdDeptAcademico(elegirDeCatalogo("Departamento académico", departamentos, p.getIdDeptAcademico()));
        Map<Integer, String> sedes = servicio.listarSedes(sesion);
        p.setIdSede(elegirDeCatalogo("Sede", sedes, p.getIdSede()));

        if (!confirmar("¿Confirmás los cambios? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        servicio.guardarCambios(sesion, p);
        System.out.println("Postulación modificada correctamente.");
    }

    private int elegirDeCatalogo(String campo, Map<Integer, String> catalogo, int idActual) {
        List<Integer> ids = new ArrayList<>(catalogo.keySet());
        while (true) {
            for (int i = 0; i < ids.size(); i++) {
                System.out.println((i + 1) + ") " + catalogo.get(ids.get(i)));
            }
            String entrada = leer(campo + " [" + catalogo.get(idActual) + "]: ").trim();
            if (entrada.isEmpty()) {
                return idActual;
            }
            int opcion = convertirOpcion(entrada, ids.size());
            if (opcion > 0) {
                return ids.get(opcion - 1);
            }
            System.out.println("Opción inválida.");
        }
    }

    // =========================================================================
    // 4. CAMBIAR ESTADO
    // =========================================================================

    private void cambiarEstado() {
        Postulacion p = servicio.obtenerParaCambiarEstado(sesion, pedirId());
        List<EstadoPostulacion> opciones = p.getEstado().transicionesManuales();
        System.out.println("Estado actual: " + p.getEstado().getEtiqueta());

        EstadoPostulacion nuevo = null;
        while (nuevo == null) {
            for (int i = 0; i < opciones.size(); i++) {
                System.out.println((i + 1) + ") " + opciones.get(i).getEtiqueta());
            }
            String entrada = leer("Elegí una opción (Enter para volver): ").trim();
            if (entrada.isEmpty()) {
                return;
            }
            int opcion = convertirOpcion(entrada, opciones.size());
            if (opcion > 0) {
                nuevo = opciones.get(opcion - 1);
            } else {
                System.out.println("Opción inválida.");
            }
        }

        if (!confirmar("¿Confirmás el cambio de estado? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        servicio.cambiarEstado(sesion, p.getIdPostulacion(), nuevo);
        System.out.println("Estado actualizado a " + nuevo.getEtiqueta() + ".");
    }

    // =========================================================================
    // AUXILIARES DE CONSOLA
    // =========================================================================

    private LocalDate pedirFecha(String mensaje) {
        while (true) {
            try {
                return PostulacionService.parsearFecha(leer(mensaje));
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    /** Muestra un dato opcional del postulante sin escribir "null". */
    private static String textoOSinRegistrar(String texto) {
        return texto == null || texto.isBlank() ? SIN_REGISTRAR : texto;
    }
}
