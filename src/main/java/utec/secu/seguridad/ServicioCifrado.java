package utec.secu.seguridad;

/**
 * Clase de servicio responsable del cifrado y descifrado artesanal
 * utilizando funciones dinámicas basadas en la posición para evitar
 * hardcodeo de constantes o claves compartigas.
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 2.0
 */
public final class ServicioCifrado {

    private ServicioCifrado() {
        throw new UnsupportedOperationException("Clase de utilidades; no instanciable.");
    }

    /**
     * Cifra el texto plano aplicando un factor dinámico basado en el índice del carácter.
     * Fórmula: código + (índice + 1)
     */
    public static String cifrar(String textoPlano) {
        if (textoPlano == null) return null;

        StringBuilder cifrado = new StringBuilder();
        for (int i = 0; i < textoPlano.length(); i++) {
            char c = textoPlano.charAt(i);
            // El desplazamiento cambia para cada letra según su posición (i + 1)
            int desplazamiento = i + 1;
            char caracterCifrado = (char) (c + desplazamiento);
            cifrado.append(caracterCifrado);
        }
        return cifrado.toString();
    }

    /**
     * Descifra el texto aplicando la operación inversa usando la misma lógica de indice.
     */
    public static String descifrar(String textoCifrado) {
        if (textoCifrado == null) return null;

        StringBuilder descifrado = new StringBuilder();
        for (int i = 0; i < textoCifrado.length(); i++) {
            char c = textoCifrado.charAt(i);
            int desplazamiento = i + 1;
            char caracterOriginal = (char) (c - desplazamiento);
            descifrado.append(caracterOriginal);
        }
        return descifrado.toString();
    }
    public static String hashPassword(String textoPlano) {
        return cifrar(textoPlano);
    }
}
