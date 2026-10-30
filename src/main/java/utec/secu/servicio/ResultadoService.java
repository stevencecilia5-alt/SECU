package utec.secu.servicio;

import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.Evaluacion;
import utec.secu.modelo.ResultadoPostulacion;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.ResultadoProxy;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Patrón FACADE para CU11 (registro del orden final CES) y CU12 (vista consolidada y reporte).
 * <p>
 * CU11: arma el orden final CES a partir de la lista de prelación. El Administrador mueve una
 * postulación a otro puesto y las demás se corren; la movida lleva el motivo del CES y las que
 * quedan corridas reciben una observación automática. El CES también puede agregar una
 * observación a cualquier postulación, aunque quede en su orden calculado. CU12: arma la vista de una convocatoria
 * y la exporta a una planilla de Excel (.xlsx) con la librería Apache POI.
 * </p>
 * <p>
 * Si falla la base de datos, cada método atrapa la {@code SQLException} del Proxy y lanza
 * {@link ErrorDeDatosException}: la consola no depende de JDBC.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class ResultadoService {

    /** Carpeta donde se guardan los reportes, relativa a donde se ejecuta la aplicación. */
    public static final String CARPETA_REPORTES = "reportes";

    public static final String MSG_SIN_POSTULACIONES = "La convocatoria no tiene postulaciones.";
    public static final String MSG_SIN_PRELACION =
            "La convocatoria no tiene postulaciones Evaluadas: todavía no hay lista de prelación.";
    public static final String MSG_POSICION = "La posición no es válida: debe ser un número entero de 1 a ";
    public static final String MSG_OBSERVACION =
            "La observación es obligatoria: escribí el motivo de la decisión del CES.";
    public static final String MSG_ORDEN_INVALIDO =
            "El orden final CES no es válido: cada puesto de 1 a %d tiene que estar asignado una sola vez.";
    public static final String MSG_FALTA_OBSERVACION =
            "Falta la observación de la postulación %d: su puesto final difiere del orden calculado.";
    public static final String MSG_FALLA_EXPORTAR = "No se pudo generar el archivo para Excel. Intentá nuevamente.";

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    /** Ancho máximo de una columna de Excel: 60 caracteres (POI mide el ancho en 1/256 de carácter). */
    private static final int ANCHO_MAXIMO = 60 * 256;
    private static final String[] COLUMNAS = {
        "ID postulación", "Fecha de postulación", "Postulante", "Correo electrónico", "Proyecto",
        "Grupo de investigación", "Tutor", "Co-tutor", "Estado de la postulación",
        "Evaluador 1", "Estado evaluación 1", "Puntaje evaluador 1", "Comentarios evaluador 1",
        "Evaluador 2", "Estado evaluación 2", "Puntaje evaluador 2", "Comentarios evaluador 2",
        "Puntaje final consolidado", "Juicio global", "Orden de prelación calculado", "Orden final CES",
        "Observación CES"
    };

    private final ResultadoProxy proxy;

    public ResultadoService() {
        this.proxy = new ResultadoProxy();
    }

    // =========================================================================
    // 1. CONSULTAS
    // =========================================================================

    /** @return Las convocatorias (id y nombre con su estado). */
    public Map<Long, String> listarConvocatorias(Usuario sesion) {
        try {
            return proxy.listarConvocatorias(sesion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * @return Todas las postulaciones de la convocatoria, ordenadas por puntaje consolidado (CU12).
     * @throws OperacionNoPermitidaException Si la convocatoria no tiene postulaciones.
     */
    public List<ResultadoPostulacion> listarResultados(Usuario sesion, long idConvocatoria) {
        try {
            List<ResultadoPostulacion> resultados = proxy.listarResultados(sesion, idConvocatoria);
            if (resultados.isEmpty()) {
                throw new OperacionNoPermitidaException(MSG_SIN_POSTULACIONES);
            }
            return resultados;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * @return La lista de prelación: las postulaciones con puntaje consolidado que no están canceladas,
     *         en el orden calculado (CU11).
     * @throws OperacionNoPermitidaException Si ninguna postulación de la convocatoria está Evaluada.
     */
    public List<ResultadoPostulacion> listarPrelacion(Usuario sesion, long idConvocatoria) {
        try {
            List<ResultadoPostulacion> prelacion = new ArrayList<>();
            for (ResultadoPostulacion r : proxy.listarResultados(sesion, idConvocatoria)) {
                if (r.getPuntajeConsolidado() != null && r.getEstado() != EstadoPostulacion.CANCELADA) {
                    prelacion.add(r);
                }
            }
            if (prelacion.isEmpty()) {
                throw new OperacionNoPermitidaException(MSG_SIN_PRELACION);
            }
            return prelacion;
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 2. ORDEN FINAL CES (CU11)
    // =========================================================================

    /**
     * @return {@code true} si hay un orden final CES registrado y le falta algún puesto calculado
     *         del 1 al N, porque se canceló una postulación después de registrarlo.
     */
    public static boolean quedoConHuecos(List<ResultadoPostulacion> prelacion) {
        boolean[] ocupado = new boolean[prelacion.size() + 1];
        for (ResultadoPostulacion r : prelacion) {
            Integer orden = r.getOrdenCalculado();
            if (orden == null || r.getOrdenCes() == null) {
                return false;   // sin orden CES, o con una evaluada después: lo informa otro aviso
            }
            if (orden >= 1 && orden <= prelacion.size()) {
                ocupado[orden] = true;
            }
        }
        for (int i = 1; i <= prelacion.size(); i++) {
            if (!ocupado[i]) {
                return true;
            }
        }
        return false;
    }

    /** @return {@code true} si ya hay un orden final CES registrado para esa lista. */
    public static boolean tieneOrdenCes(List<ResultadoPostulacion> prelacion) {
        for (ResultadoPostulacion r : prelacion) {
            if (r.getOrdenCes() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Prepara la lista para registrar un orden CES nuevo: numera el orden calculado de 1 a N
     * (el mismo que SECU guarda al registrar) y borra las posiciones y observaciones CES anteriores.
     */
    public static void prepararRegistro(List<ResultadoPostulacion> prelacion) {
        for (int i = 0; i < prelacion.size(); i++) {
            ResultadoPostulacion r = prelacion.get(i);
            r.setOrdenCalculado(i + 1);
            r.setOrdenCes(null);
            r.setObservacionCes(null);
            r.setNotaCes(null);
        }
    }

    /**
     * @return La observación sin espacios sobrantes.
     * @throws DatoInvalidoException Si está vacía.
     */
    public static String validarObservacion(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            throw new DatoInvalidoException(MSG_OBSERVACION);
        }
        return observacion.trim();
    }

    /**
     * Agrega una observación del CES a una postulación del orden que se está armando, aunque quede
     * en su orden calculado. Si se vuelve a agregar, vale la última.
     *
     * @param orden       Orden que se está armando, del puesto 1 al N.
     * @param puesto      Puesto de la postulación (1 a N).
     * @param observacion Observación del CES.
     * @throws DatoInvalidoException Si el puesto no está entre 1 y N o la observación está vacía.
     */
    public static void anotar(List<ResultadoPostulacion> orden, int puesto, String observacion) {
        String nota = validarObservacion(observacion);
        if (puesto < 1 || puesto > orden.size()) {
            throw new DatoInvalidoException(MSG_POSICION + orden.size() + ".");
        }
        orden.get(puesto - 1).setNotaCes(nota);
    }

    /**
     * Mueve una postulación a otro puesto del orden que se está armando: las que están en el medio
     * se corren un lugar. La postulación movida guarda el motivo escrito por el CES; si se vuelve
     * a mover, vale el último motivo.
     *
     * @param orden        Orden que se está armando, del puesto 1 al N.
     * @param puestoActual Puesto donde está la postulación (1 a N).
     * @param puestoNuevo  Puesto al que se mueve (1 a N).
     * @param observacion  Motivo de la decisión del CES.
     * @throws DatoInvalidoException Si un puesto no está entre 1 y N o la observación está vacía.
     */
    public static void mover(List<ResultadoPostulacion> orden, int puestoActual, int puestoNuevo,
                             String observacion) {
        String motivo = validarObservacion(observacion);
        int cantidad = orden.size();
        if (puestoActual < 1 || puestoActual > cantidad || puestoNuevo < 1 || puestoNuevo > cantidad) {
            throw new DatoInvalidoException(MSG_POSICION + cantidad + ".");
        }
        ResultadoPostulacion movida = orden.remove(puestoActual - 1);
        orden.add(puestoNuevo - 1, movida);
        movida.setObservacionCes(motivo);
    }

    /**
     * Cierra el orden armado antes de confirmarlo: le da a cada postulación su posición final CES
     * (de 1 a N) y completa las observaciones. La que quedó en su orden calculado no lleva
     * observación; la que movió el CES conserva el motivo escrito; la que quedó corrida por esos
     * movimientos recibe una observación automática que nombra las decisiones del CES. La observación
     * que agregó el CES ({@link #anotar}) se suma a la que corresponda, o queda sola si no hay otra.
     */
    public static void cerrarOrdenCes(List<ResultadoPostulacion> orden) {
        // 1) Posición final de cada una y decisiones del CES que cambiaron el orden
        StringBuilder decisiones = new StringBuilder();
        for (int i = 0; i < orden.size(); i++) {
            ResultadoPostulacion r = orden.get(i);
            r.setOrdenCes(i + 1);
            if (r.getOrdenCes().equals(r.getOrdenCalculado())) {
                r.setObservacionCes(null);
            } else if (r.getObservacionCes() != null) {
                if (decisiones.length() > 0) {
                    decisiones.append(", ");
                }
                decisiones.append("postulación ").append(r.getIdPostulacion()).append(" al puesto ").append(i + 1);
            }
        }
        // 2) Observación automática para las que quedaron corridas
        for (ResultadoPostulacion r : orden) {
            if (r.getObservacionCes() == null && !r.getOrdenCes().equals(r.getOrdenCalculado())) {
                r.setObservacionCes("Quedó en el puesto " + r.getOrdenCes() + " (orden calculado: "
                        + r.getOrdenCalculado() + ") por las decisiones del CES: " + decisiones + ".");
            }
        }
        // 3) Observación que agregó el CES: se suma a la que ya tenga
        for (ResultadoPostulacion r : orden) {
            if (r.getNotaCes() != null) {
                r.setObservacionCes(r.getObservacionCes() == null ? r.getNotaCes()
                        : r.getObservacionCes() + " / " + r.getNotaCes());
            }
        }
    }

    /**
     * Registra el orden final CES de la convocatoria, ya cerrado con {@link #cerrarOrdenCes}.
     * El orden calculado por SECU se guarda aparte y no se sobrescribe. Antes de guardar vuelve a
     * controlar las reglas del CU11, para no depender del menú.
     *
     * @throws DatoInvalidoException Si hay puestos repetidos o sin asignar, o falta una observación.
     */
    public void registrarOrdenCes(Usuario sesion, long idConvocatoria, List<ResultadoPostulacion> orden) {
        validarOrdenCes(orden);
        try {
            proxy.registrarOrdenCes(sesion, idConvocatoria, orden);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Reglas del CU11: cada puesto de 1 a N está una sola vez, y si el puesto final difiere del
     * orden calculado, la postulación tiene observación.
     *
     * @throws DatoInvalidoException Si alguna regla no se cumple.
     */
    static void validarOrdenCes(List<ResultadoPostulacion> orden) {
        boolean[] ocupado = new boolean[orden.size() + 1];
        for (ResultadoPostulacion r : orden) {
            Integer puesto = r.getOrdenCes();
            if (puesto == null || puesto < 1 || puesto > orden.size() || ocupado[puesto]) {
                throw new DatoInvalidoException(String.format(MSG_ORDEN_INVALIDO, orden.size()));
            }
            ocupado[puesto] = true;
            String observacion = r.getObservacionCes();
            if (!puesto.equals(r.getOrdenCalculado()) && (observacion == null || observacion.isBlank())) {
                throw new DatoInvalidoException(String.format(MSG_FALTA_OBSERVACION, r.getIdPostulacion()));
            }
        }
    }

    // =========================================================================
    // 3. REPORTE EN EXCEL (CU12)
    // =========================================================================

    /**
     * Guarda la vista consolidada en {@code reportes/} como planilla de Excel (.xlsx), con Apache POI:
     * la convocatoria, los títulos y una fila por postulación.
     *
     * @return La ruta completa del archivo generado.
     * @throws OperacionNoPermitidaException Si no se pudo escribir el archivo.
     */
    public static Path exportarExcel(String convocatoria, long idConvocatoria, List<ResultadoPostulacion> resultados) {
        Path archivo = Paths.get(CARPETA_REPORTES, "vista_consolidada_convocatoria_" + idConvocatoria + "_"
                + LocalDateTime.now().format(FORMATO_ARCHIVO) + ".xlsx");
        try (Workbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Vista consolidada");

            // Estilos: títulos en negrita y puntajes con dos decimales
            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(negrita);
            CellStyle estiloPuntaje = libro.createCellStyle();
            estiloPuntaje.setDataFormat(libro.createDataFormat().getFormat("0.00"));

            // Fila 3: títulos de las columnas
            Row filaTitulos = hoja.createRow(2);
            for (int i = 0; i < COLUMNAS.length; i++) {
                Cell celda = filaTitulos.createCell(i);
                celda.setCellValue(COLUMNAS[i]);
                celda.setCellStyle(estiloTitulo);
            }

            // Una fila por postulación
            int numeroFila = 3;
            for (ResultadoPostulacion r : resultados) {
                Row fila = hoja.createRow(numeroFila);
                numeroFila++;
                escribirNumero(fila, 0, r.getIdPostulacion(), null);
                escribirTexto(fila, 1, r.getFecSolicitud() == null ? null : r.getFecSolicitud().format(FORMATO_FECHA));
                escribirTexto(fila, 2, r.getPostulante());
                escribirTexto(fila, 3, r.getCorreo());
                escribirTexto(fila, 4, r.getProyecto());
                escribirTexto(fila, 5, r.getGrupo());
                escribirTexto(fila, 6, r.getTutor());
                escribirTexto(fila, 7, r.getCoTutor());
                escribirTexto(fila, 8, r.getEstado().getEtiqueta());
                escribirEvaluacion(fila, 9, r.getEvaluacion(1), estiloPuntaje);
                escribirEvaluacion(fila, 13, r.getEvaluacion(2), estiloPuntaje);
                escribirNumero(fila, 17, r.getPuntajeConsolidado(), estiloPuntaje);
                escribirTexto(fila, 18, r.getJuicioGlobal());
                escribirNumero(fila, 19, r.getOrdenCalculado(), null);
                escribirNumero(fila, 20, r.getOrdenCes(), null);
                escribirTexto(fila, 21, r.getObservacionCes());
            }

            // Cada columna se ajusta a su texto, sin pasar de 60 caracteres
            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.autoSizeColumn(i);
                if (hoja.getColumnWidth(i) > ANCHO_MAXIMO) {
                    hoja.setColumnWidth(i, ANCHO_MAXIMO);
                }
            }
            hoja.createFreezePane(0, 3);

            // Fila 1: la convocatoria, después de ajustar los anchos para no ensanchar la columna B
            Row filaConvocatoria = hoja.createRow(0);
            Cell celdaConvocatoria = filaConvocatoria.createCell(0);
            celdaConvocatoria.setCellValue("Convocatoria");
            celdaConvocatoria.setCellStyle(estiloTitulo);
            escribirTexto(filaConvocatoria, 1, convocatoria);

            Files.createDirectories(archivo.getParent());
            try (OutputStream salida = Files.newOutputStream(archivo)) {
                libro.write(salida);
            }
        } catch (IOException e) {
            throw new OperacionNoPermitidaException(MSG_FALLA_EXPORTAR);
        }
        return archivo.toAbsolutePath();
    }

    /** Escribe las 4 columnas de un evaluador; quedan vacías si la postulación todavía no lo tiene. */
    private static void escribirEvaluacion(Row fila, int primeraColumna, Evaluacion e, CellStyle estiloPuntaje) {
        if (e == null) {
            return;
        }
        escribirTexto(fila, primeraColumna, e.getNombreEvaluador());
        escribirTexto(fila, primeraColumna + 1, e.getEstado());
        escribirNumero(fila, primeraColumna + 2, e.getPuntajeTotal(), estiloPuntaje);
        escribirTexto(fila, primeraColumna + 3, e.getComentario());
    }

    /** Escribe un texto en la celda; si es {@code null}, la celda queda vacía. */
    private static void escribirTexto(Row fila, int columna, String texto) {
        if (texto != null) {
            fila.createCell(columna).setCellValue(texto);
        }
    }

    /**
     * Escribe un número en la celda, para que Excel pueda ordenar y calcular con él;
     * si es {@code null}, la celda queda vacía.
     *
     * @param estilo Formato de la celda (por ejemplo, dos decimales), o {@code null} para el formato general.
     */
    private static void escribirNumero(Row fila, int columna, Number valor, CellStyle estilo) {
        if (valor != null) {
            Cell celda = fila.createCell(columna);
            celda.setCellValue(valor.doubleValue());
            if (estilo != null) {
                celda.setCellStyle(estilo);
            }
        }
    }
}
