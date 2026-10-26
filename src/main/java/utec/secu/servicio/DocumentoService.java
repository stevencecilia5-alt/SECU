package utec.secu.servicio;

import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.excepciones.ErrorDeDatosException;
import utec.secu.excepciones.OperacionNoPermitidaException;
import utec.secu.modelo.DocumentoAdjunto;
import utec.secu.modelo.EstadoPostulacion;
import utec.secu.modelo.TipoDocumento;
import utec.secu.modelo.Usuario;
import utec.secu.seguridad.DocumentoProxy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Patrón FACADE para CU06: Gestión de documentos adjuntos.
 * <p>
 * Valida cada archivo (formato PDF y tamaño máximo de 10 MB), aplica las reglas de la
 * convocatoria (cada tipo una sola vez, salvo hasta 4 documentos adicionales) y copia el
 * archivo a la carpeta {@code adjuntos/} antes de registrar su ruta con {@link DocumentoProxy}.
 * La base guarda solo la ruta; el PDF queda en el disco.
 * </p>
 * <p>
 * El Administrador carga los documentos que entregó el postulante, mientras la postulación está
 * Registrada o En revisión.
 * </p>
 * <p>
 * Si falla la base de datos, cada método atrapa la {@code SQLException} del Proxy y lanza
 * {@link ErrorDeDatosException}: la consola no depende de JDBC.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public class DocumentoService {

    /** Tamaño máximo de cada archivo: 10 MB (10 × 1024 × 1024 bytes). */
    public static final long TAMANIO_MAXIMO = 10L * 1024 * 1024;
    public static final int MAXIMO_ADICIONALES = 4;
    /** Carpeta donde se guardan los archivos cargados, relativa a donde se ejecuta la aplicación. */
    public static final String CARPETA_ADJUNTOS = "adjuntos";

    public static final String MSG_NO_MODIFICABLE =
            "Solo se pueden cargar o quitar documentos de postulaciones Registradas o En revisión.";
    public static final String MSG_YA_CARGADO = "Ese documento ya está cargado. Para cambiarlo, primero quitá el actual.";
    public static final String MSG_MAXIMO_ADICIONALES = "Ya se cargaron los 4 documentos adicionales permitidos.";
    public static final String MSG_NO_EXISTE = "No se encontró el archivo indicado.";
    public static final String MSG_NO_PDF = "Archivo inválido: solo se aceptan documentos en formato PDF.";
    public static final String MSG_VACIO = "Archivo inválido: el archivo está vacío.";
    public static final String MSG_TAMANIO = "Archivo inválido: supera el tamaño máximo de 10 MB.";
    public static final String MSG_FALLA_CARGA = "No se pudo cargar el archivo. Intentá nuevamente.";
    public static final String MSG_ARCHIVO_PERDIDO = "No se encontró el archivo del documento en SECU.";

    private final DocumentoProxy proxy;

    public DocumentoService() {
        this.proxy = new DocumentoProxy();
    }

    // =========================================================================
    // 1. CONSULTAS
    // =========================================================================

    /**
     * @return El estado de la postulación.
     * @throws DatoInvalidoException Si no existe una postulación con ese identificador.
     */
    public EstadoPostulacion obtenerEstado(Usuario sesion, long idPostulacion) {
        try {
            return PostulacionService.verificarExiste(proxy.buscarEstadoPostulacion(sesion, idPostulacion));
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @return Los tipos de documento que pide la convocatoria de la postulación. */
    public List<TipoDocumento> listarTipos(Usuario sesion, long idPostulacion) {
        try {
            obtenerEstado(sesion, idPostulacion);
            return proxy.listarTipos(sesion, idPostulacion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    /** @return Los documentos cargados en la postulación. */
    public List<DocumentoAdjunto> listarDocumentos(Usuario sesion, long idPostulacion) {
        try {
            obtenerEstado(sesion, idPostulacion);
            return proxy.listarDocumentos(sesion, idPostulacion);
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
    }

    // =========================================================================
    // 2. CARGAR Y QUITAR DOCUMENTOS
    // =========================================================================

    /**
     * @throws DatoInvalidoException         Si la postulación no existe.
     * @throws OperacionNoPermitidaException Si la postulación no está Registrada ni En revisión.
     */
    public void verificarModificable(Usuario sesion, long idPostulacion) {
        if (!obtenerEstado(sesion, idPostulacion).admiteModificarDatos()) {
            throw new OperacionNoPermitidaException(MSG_NO_MODIFICABLE);
        }
    }

    /**
     * Carga un documento: valida el archivo, lo copia a {@code adjuntos/P<id>/} y guarda la ruta.
     * Si la ruta no se puede guardar en la base, se borra la copia.
     *
     * @param ruta Ruta del archivo PDF en el equipo.
     * @throws DatoInvalidoException         Si el archivo no existe, no es PDF, está vacío o supera 10 MB.
     * @throws OperacionNoPermitidaException Si la postulación no admite cambios, el tipo ya está cargado,
     *                                       ya hay 4 adicionales o falló la copia del archivo.
     */
    public void cargar(Usuario sesion, long idPostulacion, TipoDocumento tipo, String ruta) {
        verificarModificable(sesion, idPostulacion);
        Path origen = validarArchivo(ruta);
        verificarTipo(tipo, listarDocumentos(sesion, idPostulacion));

        Path destino = destinoLibre(idPostulacion, tipo.getNombre());
        try {
            Files.createDirectories(destino.getParent());
            Files.copy(origen, destino);
        } catch (IOException e) {
            throw new OperacionNoPermitidaException(MSG_FALLA_CARGA);
        }

        try {
            proxy.insertar(sesion, idPostulacion, tipo.getIdDocuTipo(), destino.toString().replace('\\', '/'));
        } catch (SQLException e) {
            borrarArchivo(destino);   // la base no lo registró: no se deja la copia suelta
            throw new ErrorDeDatosException(e);
        }
    }

    /**
     * Quita un documento de la postulación. Si el archivo está en {@code adjuntos/}, también lo borra;
     * los documentos de ejemplo de la carpeta {@code documentos/} están en Git y no se borran.
     *
     * @throws OperacionNoPermitidaException Si la postulación no está Registrada ni En revisión.
     */
    public void quitar(Usuario sesion, long idPostulacion, DocumentoAdjunto documento) {
        verificarModificable(sesion, idPostulacion);
        try {
            proxy.eliminar(sesion, idPostulacion, documento.getIdDocuAdjunto());
        } catch (SQLException e) {
            throw new ErrorDeDatosException(e);
        }
        if (documento.getRutArchivo().startsWith(CARPETA_ADJUNTOS + "/")) {
            borrarArchivo(Paths.get(documento.getRutArchivo()));
        }
    }

    // =========================================================================
    // 3. REGLAS DE LA CONVOCATORIA (no necesitan la base)
    // =========================================================================

    /** @return Cuántos documentos de ese tipo están cargados. */
    public static int cantidadCargada(TipoDocumento tipo, List<DocumentoAdjunto> cargados) {
        int cantidad = 0;
        for (DocumentoAdjunto d : cargados) {
            if (d.getIdDocuTipo() == tipo.getIdDocuTipo()) {
                cantidad++;
            }
        }
        return cantidad;
    }

    /**
     * Cada tipo se carga una sola vez, salvo "Otros documentos adicionales", que admite hasta 4.
     *
     * @throws OperacionNoPermitidaException Si ya no se puede cargar otro documento de ese tipo.
     */
    public static void verificarTipo(TipoDocumento tipo, List<DocumentoAdjunto> cargados) {
        int cantidad = cantidadCargada(tipo, cargados);
        if (tipo.esAdicional() && cantidad >= MAXIMO_ADICIONALES) {
            throw new OperacionNoPermitidaException(MSG_MAXIMO_ADICIONALES);
        }
        if (!tipo.esAdicional() && cantidad > 0) {
            throw new OperacionNoPermitidaException(MSG_YA_CARGADO);
        }
    }

    /** @return Los nombres de los documentos obligatorios que todavía no están cargados. */
    public static List<String> obligatoriosFaltantes(List<TipoDocumento> tipos, List<DocumentoAdjunto> cargados) {
        List<String> faltantes = new ArrayList<>();
        for (TipoDocumento t : tipos) {
            if (t.isObligatorio() && cantidadCargada(t, cargados) == 0) {
                faltantes.add(t.getNombre());
            }
        }
        return faltantes;
    }

    /**
     * Valida el archivo que se quiere cargar: que exista, que sea PDF (extensión {@code .pdf} y
     * contenido que empieza con {@code %PDF-}) y que pese más de 0 bytes y como máximo 10 MB.
     * Acepta la ruta entre comillas, como la copia el explorador de Windows.
     *
     * @return La ruta del archivo.
     * @throws DatoInvalidoException         Si el archivo no cumple alguna de las condiciones.
     * @throws OperacionNoPermitidaException Si no se pudo leer el archivo.
     */
    public static Path validarArchivo(String ruta) {
        String limpia = ruta.trim();
        if (limpia.length() >= 2 && limpia.startsWith("\"") && limpia.endsWith("\"")) {
            limpia = limpia.substring(1, limpia.length() - 1).trim();
        }
        Path archivo;
        try {
            archivo = Paths.get(limpia);
        } catch (InvalidPathException e) {
            throw new DatoInvalidoException(MSG_NO_EXISTE);
        }
        if (!Files.isRegularFile(archivo)) {
            throw new DatoInvalidoException(MSG_NO_EXISTE);
        }
        if (!archivo.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new DatoInvalidoException(MSG_NO_PDF);
        }
        try {
            long tamanio = Files.size(archivo);
            if (tamanio == 0) {
                throw new DatoInvalidoException(MSG_VACIO);
            }
            if (tamanio > TAMANIO_MAXIMO) {
                throw new DatoInvalidoException(MSG_TAMANIO);
            }
            if (!empiezaComoPdf(archivo)) {
                throw new DatoInvalidoException(MSG_NO_PDF);
            }
        } catch (IOException e) {
            throw new OperacionNoPermitidaException(MSG_FALLA_CARGA);
        }
        return archivo;
    }

    // =========================================================================
    // 4. ARCHIVOS EN EL DISCO
    // =========================================================================

    /** @return El tamaño del archivo en bytes, o -1 si no está en el disco. */
    public static long tamanioArchivo(DocumentoAdjunto documento) {
        try {
            Path archivo = Paths.get(documento.getRutArchivo());
            return Files.isRegularFile(archivo) ? Files.size(archivo) : -1;
        } catch (InvalidPathException | IOException e) {
            return -1;
        }
    }

    /**
     * @return La ruta completa del archivo, para abrirlo.
     * @throws OperacionNoPermitidaException Si el archivo no está en el disco.
     */
    public static Path archivoParaAbrir(DocumentoAdjunto documento) {
        if (tamanioArchivo(documento) < 0) {
            throw new OperacionNoPermitidaException(MSG_ARCHIVO_PERDIDO);
        }
        return Paths.get(documento.getRutArchivo()).toAbsolutePath();
    }

    /**
     * Arma el destino de la copia con el nombre del tipo, por ejemplo
     * {@code adjuntos/P04/plan_de_trabajo.pdf}. Si ya existe (documentos adicionales),
     * agrega un número: {@code otros_documentos_adicionales_2.pdf}.
     */
    private static Path destinoLibre(long idPostulacion, String nombreTipo) {
        Path carpeta = Paths.get(CARPETA_ADJUNTOS, String.format("P%02d", idPostulacion));
        // Sin tildes: "Carta de motivación" queda carta_de_motivacion
        String sinTildes = Normalizer.normalize(nombreTipo, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String base = sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        Path destino = carpeta.resolve(base + ".pdf");
        int numero = 2;
        while (Files.exists(destino)) {
            destino = carpeta.resolve(base + "_" + numero + ".pdf");
            numero++;
        }
        return destino;
    }

    /** @return {@code true} si el archivo empieza con la marca de los PDF ({@code %PDF-}). */
    private static boolean empiezaComoPdf(Path archivo) throws IOException {
        try (InputStream entrada = Files.newInputStream(archivo)) {
            return "%PDF-".equals(new String(entrada.readNBytes(5), StandardCharsets.US_ASCII));
        }
    }

    private static void borrarArchivo(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            // Si no se puede borrar, el archivo queda en la carpeta, pero la base ya no lo usa.
        }
    }
}
