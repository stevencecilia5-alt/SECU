package utec.secu.servicio;

import utec.secu.excepciones.OperacionNoPermitidaException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Envío de correos simulado (MVP).
 * <p>
 * En lugar de mandar un correo real, agrega cada mensaje al archivo
 * {@code correos_simulados/bandeja_salida.txt}, en la carpeta desde donde se ejecuta
 * la aplicación. Así las contraseñas temporales nunca se muestran en la consola,
 * como pide la pauta de Programación.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 1.0
 */
public final class ServicioCorreo {

    private static final Path BANDEJA = Paths.get("correos_simulados", "bandeja_salida.txt");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private ServicioCorreo() {
        throw new UnsupportedOperationException("Clase de utilidades; no instanciable.");
    }

    /**
     * Registra un correo en la bandeja de salida simulada.
     *
     * @throws OperacionNoPermitidaException Si no se pudo escribir el archivo.
     */
    public static void enviar(String destinatario, String asunto, String cuerpo) {
        String mensaje = "========================================" + System.lineSeparator()
                + "Fecha:  " + LocalDateTime.now().format(FORMATO) + System.lineSeparator()
                + "Para:   " + destinatario + System.lineSeparator()
                + "Asunto: " + asunto + System.lineSeparator() + System.lineSeparator()
                + cuerpo + System.lineSeparator() + System.lineSeparator();
        try {
            Files.createDirectories(BANDEJA.getParent());
            Files.writeString(BANDEJA, mensaje, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new OperacionNoPermitidaException("No se pudo enviar el correo simulado: " + ex.getMessage());
        }
    }
}
