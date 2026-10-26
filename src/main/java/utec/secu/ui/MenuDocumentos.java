package utec.secu.ui;

import utec.secu.excepciones.AccesoDenegadoException;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.DocumentoAdjunto;
import utec.secu.modelo.TipoDocumento;
import utec.secu.modelo.Usuario;
import utec.secu.servicio.DocumentoService;
import utec.secu.servicio.PostulacionService;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Menú de consola de CU06: Gestión de documentos adjuntos (opción 5 del menú de CU07).
 * <p>
 * Solo lee datos y muestra resultados; las reglas de negocio están en
 * {@link DocumentoService} y el control de acceso en el Proxy.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class MenuDocumentos extends MenuBase {

    private final DocumentoService servicio;

    public MenuDocumentos(Scanner sc, Usuario sesion) {
        super(sc, sesion);
        this.servicio = new DocumentoService();
    }

    /** Muestra el submenú hasta que el usuario elige Volver. */
    public void mostrar() {
        boolean enSubmenu = true;
        while (enSubmenu) {
            System.out.println("\n--- Documentos adjuntos ---");
            System.out.println("1. Ver documentos de una postulación");
            System.out.println("2. Cargar un documento");
            System.out.println("3. Abrir un documento");
            System.out.println("4. Quitar un documento");
            System.out.println("5. Volver");

            // Si una opción falla, se muestra el mensaje y se vuelve a este menú
            try {
                switch (leer("Elegí una opción: ").trim()) {
                    case "1": ver(); break;
                    case "2": cargar(); break;
                    case "3": abrir(); break;
                    case "4": quitar(); break;
                    case "5": enSubmenu = false; break;
                    default: System.out.println("Opción inválida.");
                }
            } catch (AccesoDenegadoException | DatoInvalidoException | OperacionNoPermitidaException
                     | ErrorDeDatosException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void ver() {
        long id = pedirId();
        List<TipoDocumento> tipos = servicio.listarTipos(sesion, id);
        List<DocumentoAdjunto> cargados = servicio.listarDocumentos(sesion, id);
        System.out.println("Estado de la postulación: " + servicio.obtenerEstado(sesion, id).getEtiqueta());
        imprimirTipos(tipos, cargados);
        imprimirDocumentos(cargados);
        imprimirFaltantes(tipos, cargados);
    }

    private void cargar() {
        long id = pedirId();
        servicio.verificarModificable(sesion, id);
        List<TipoDocumento> tipos = servicio.listarTipos(sesion, id);
        if (tipos.isEmpty()) {
            System.out.println("La convocatoria de la postulación no tiene documentos definidos.");
            return;
        }
        List<DocumentoAdjunto> cargados = servicio.listarDocumentos(sesion, id);
        imprimirTipos(tipos, cargados);
        int opcion = elegir("Tipo de documento (Enter para volver): ", tipos.size());
        if (opcion == 0) {
            return;
        }
        TipoDocumento tipo = tipos.get(opcion - 1);
        DocumentoService.verificarTipo(tipo, cargados);

        // Si el archivo no es válido, se muestra el motivo y se vuelve a pedir la ruta
        while (true) {
            String ruta = leer("Ruta del archivo PDF (Enter para volver): ");
            if (ruta.trim().isEmpty()) {
                return;
            }
            try {
                servicio.cargar(sesion, id, tipo, ruta);
                System.out.println("Documento cargado correctamente.");
                imprimirFaltantes(tipos, servicio.listarDocumentos(sesion, id));
                return;
            } catch (DatoInvalidoException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }

    private void abrir() {
        long id = pedirId();
        List<DocumentoAdjunto> cargados = servicio.listarDocumentos(sesion, id);
        imprimirDocumentos(cargados);
        int opcion = elegir("Documento a abrir (Enter para volver): ", cargados.size());
        if (opcion == 0) {
            return;
        }
        abrirArchivo(cargados.get(opcion - 1));
    }

    /**
     * Abre el PDF con el visor del equipo; si no se puede, muestra la ruta para abrirlo a mano.
     * También lo usa el evaluador desde el detalle de una postulación asignada (CU09).
     */
    static void abrirArchivo(DocumentoAdjunto documento) {
        Path archivo = DocumentoService.archivoParaAbrir(documento);
        System.out.println("Archivo: " + archivo);
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            System.out.println("Este equipo no permite abrirlo desde la consola: abrilo desde esa ruta.");
            return;
        }
        try {
            Desktop.getDesktop().open(archivo.toFile());
            System.out.println("Documento abierto con el visor de PDF del equipo.");
        } catch (IOException ex) {
            System.out.println("No se pudo abrir el documento: abrilo desde esa ruta.");
        }
    }

    private void quitar() {
        long id = pedirId();
        servicio.verificarModificable(sesion, id);
        List<DocumentoAdjunto> cargados = servicio.listarDocumentos(sesion, id);
        imprimirDocumentos(cargados);
        int opcion = elegir("Documento a quitar (Enter para volver): ", cargados.size());
        if (opcion == 0) {
            return;
        }
        if (!confirmar("¿Confirmás que querés quitar el documento? (s/n): ")) {
            System.out.println("Cambios descartados.");
            return;
        }
        servicio.quitar(sesion, id, cargados.get(opcion - 1));
        System.out.println("Documento quitado correctamente.");
    }

    // =========================================================================
    // AUXILIARES DE CONSOLA
    // =========================================================================

    /** Muestra cada tipo que pide la convocatoria, si es obligatorio y si ya está cargado. */
    private void imprimirTipos(List<TipoDocumento> tipos, List<DocumentoAdjunto> cargados) {
        System.out.println("\nDocumentos que pide la convocatoria:");
        if (tipos.isEmpty()) {
            System.out.println("  (ninguno)");
        }
        for (int i = 0; i < tipos.size(); i++) {
            TipoDocumento t = tipos.get(i);
            int cantidad = DocumentoService.cantidadCargada(t, cargados);
            String situacion;
            if (t.esAdicional()) {
                situacion = cantidad + " de " + DocumentoService.MAXIMO_ADICIONALES;
            } else if (cantidad > 0) {
                situacion = "cargado";
            } else {
                situacion = t.isObligatorio() ? "falta" : "-";
            }
            System.out.println((i + 1) + ") " + t.getNombre() + " (" + (t.isObligatorio() ? "obligatorio" : "opcional")
                    + "): " + situacion);
        }
    }

    /**
     * Muestra los documentos cargados, numerados, con el tamaño de cada archivo.
     * También lo usa el detalle del evaluador (CU09).
     */
    static void imprimirDocumentos(List<DocumentoAdjunto> cargados) {
        System.out.println("\nDocumentos cargados:");
        if (cargados.isEmpty()) {
            System.out.println("  (ninguno)");
        }
        for (int i = 0; i < cargados.size(); i++) {
            DocumentoAdjunto d = cargados.get(i);
            long tamanio = DocumentoService.tamanioArchivo(d);
            System.out.println((i + 1) + ") " + d.getNomTipo() + ": " + d.getRutArchivo() + " ("
                    + (tamanio < 0 ? "archivo no encontrado" : formatoTamanio(tamanio)) + ")");
        }
    }

    private void imprimirFaltantes(List<TipoDocumento> tipos, List<DocumentoAdjunto> cargados) {
        List<String> faltantes = DocumentoService.obligatoriosFaltantes(tipos, cargados);
        if (faltantes.isEmpty()) {
            System.out.println("Documentación obligatoria completa.");
        } else {
            System.out.println("Faltan documentos obligatorios: " + String.join(", ", faltantes) + ".");
        }
    }

    private long pedirId() {
        return PostulacionService.parsearId(leer("Identificador de la postulación: "));
    }

    private static String formatoTamanio(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024));
    }
}
