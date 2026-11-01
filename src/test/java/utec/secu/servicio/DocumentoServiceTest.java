package utec.secu.servicio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import utec.secu.excepciones.DatoInvalidoException;
import utec.secu.modelo.TipoDocumento;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de CU06 (opcional): tamaño máximo de 10 MB por archivo y tipos de documento de la convocatoria.
 */
class DocumentoServiceTest {

    @Test
    @DisplayName("CU06: un PDF de exactamente 10 MB se acepta (valor límite)")
    void pdfDeExactamente10MBSeAcepta() throws IOException {
        Path archivo = crearPdf(DocumentoService.TAMANIO_MAXIMO);
        try {
            assertEquals(archivo, DocumentoService.validarArchivo(archivo.toString()));
        } finally {
            Files.delete(archivo);
        }
    }

    @Test
    @DisplayName("CU06: un PDF de 10 MB más 1 byte se rechaza")
    void pdfDeMasDe10MBSeRechaza() throws IOException {
        Path archivo = crearPdf(DocumentoService.TAMANIO_MAXIMO + 1);
        try {
            DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                    () -> DocumentoService.validarArchivo(archivo.toString()));
            assertEquals(DocumentoService.MSG_TAMANIO, ex.getMessage());
        } finally {
            Files.delete(archivo);
        }
    }

    @Test
    @DisplayName("CU06: un tipo de documento que no pide la convocatoria se rechaza")
    void tipoQueNoPideLaConvocatoriaSeRechaza() {
        TipoDocumento planDeTrabajo = new TipoDocumento(1, "Plan de trabajo", true);
        TipoDocumento constancia = new TipoDocumento(3, "Constancia de inscripción al posgrado", false);
        List<TipoDocumento> tiposDeLaConvocatoria = List.of(planDeTrabajo);

        DatoInvalidoException ex = assertThrows(DatoInvalidoException.class,
                () -> DocumentoService.verificarTipoDeLaConvocatoria(constancia, tiposDeLaConvocatoria));

        assertEquals(DocumentoService.MSG_TIPO_NO_PEDIDO, ex.getMessage());
        DocumentoService.verificarTipoDeLaConvocatoria(planDeTrabajo, tiposDeLaConvocatoria);   // este sí: no lanza
    }

    /** Crea un archivo temporal .pdf que empieza con la marca de los PDF y tiene el tamaño pedido. */
    private static Path crearPdf(long tamanio) throws IOException {
        Path archivo = Files.createTempFile("secu_prueba", ".pdf");
        try (RandomAccessFile f = new RandomAccessFile(archivo.toFile(), "rw")) {
            f.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            f.setLength(tamanio);
        }
        return archivo;
    }
}
