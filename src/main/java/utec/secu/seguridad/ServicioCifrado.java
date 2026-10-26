package utec.secu.seguridad;

/**
 * Cifrado propio y reversible de las contraseñas, con una clave (pauta de Programación).
 * <p>
 * Cada carácter se desplaza según la letra de la clave que le toca por su posición. Para
 * descifrar se desplaza en sentido contrario: con la misma clave se recupera la contraseña
 * original y con otra, no.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 3.0
 */
public final class ServicioCifrado {

    /** Clave del cifrado: una frase. Si se cambia, hay que regenerar las contraseñas de los scripts SQL. */
    private static final String CLAVE = "Anglo Deploy 2026";

    /** Primer carácter imprimible (el espacio). */
    private static final int PRIMERO = 32;
    /** Cantidad de caracteres imprimibles: del 32 al 126. */
    private static final int CANTIDAD = 95;

    private ServicioCifrado() {
        throw new UnsupportedOperationException("Clase de utilidades; no instanciable.");
    }

    /** Cifra una contraseña con la clave de SECU: es el valor que se guarda en la base y el que compara el login. */
    public static String hashPassword(String textoPlano) {
        return cifrar(textoPlano, CLAVE);
    }

    /**
     * Cifra un texto con la clave indicada.
     *
     * @return El texto cifrado, o {@code null} si el texto es {@code null}.
     */
    public static String cifrar(String textoPlano, String clave) {
        return desplazar(textoPlano, clave, 1);
    }

    /**
     * Descifra un texto con la clave indicada. Con la misma clave que se usó para cifrar,
     * devuelve el texto original.
     *
     * @return El texto descifrado, o {@code null} si el texto es {@code null}.
     */
    public static String descifrar(String textoCifrado, String clave) {
        return desplazar(textoCifrado, clave, -1);
    }

    /**
     * Desplaza cada carácter imprimible según el carácter de la clave que le toca.
     *
     * @param sentido 1 para cifrar (suma) o -1 para descifrar (resta).
     */
    private static String desplazar(String texto, String clave, int sentido) {
        if (texto == null) {
            return null;
        }
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c >= PRIMERO && c < PRIMERO + CANTIDAD) {
                int desplazamiento = clave.charAt(i % clave.length());   // la clave se repite
                // floorMod da siempre un resultado entre 0 y 94, aunque la resta sea negativa
                c = (char) (PRIMERO + Math.floorMod(c - PRIMERO + sentido * desplazamiento, CANTIDAD));
            }
            resultado.append(c);
        }
        return resultado.toString();
    }
}
