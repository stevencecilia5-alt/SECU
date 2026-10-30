package utec.secu.seguridad;

/**
 * Cifrado propio y reversible de las contraseñas, con una clave (pauta de Programación).
 * <p>
 * Cada carácter se desplaza dentro de su grupo ({@link #TECLADO} o {@link #LATINO}) según la
 * letra de la clave que le toca por su posición. Para descifrar se desplaza en sentido
 * contrario: con la misma clave se recupera la contraseña original y con otra, no.
 * Los caracteres del teclado quedan dentro del teclado, así que una contraseña sin ñ ni tildes
 * se cifra igual que en las versiones anteriores y las contraseñas guardadas siguen sirviendo.
 * </p>
 *
 * @author Grupo 10 - ANGLO Deploy
 * @version 4.0
 */
public final class ServicioCifrado {

    /** Clave del cifrado: una frase. Si se cambia, hay que regenerar las contraseñas de los scripts SQL. */
    private static final String CLAVE = "Anglo Deploy 2026";

    /** Caracteres del teclado: del espacio al {@code ~} (95). */
    private static final String TECLADO = rango(' ', '~');

    /**
     * Caracteres del alfabeto latino del {@code ¡} a la {@code ÿ}, sin el guion invisible (94):
     * la ñ, las letras con tilde, la ü, la ç, el ¿, el ¡ y otros.
     */
    private static final String LATINO = rango('¡', 'ÿ').replace("­", "");

    private ServicioCifrado() {
        throw new UnsupportedOperationException("Clase de utilidades; no instanciable.");
    }

    /**
     * Indica si el algoritmo cifra todos los caracteres del texto. Cualquier carácter fuera de los
     * dos grupos (por ejemplo, un emoji o el €) quedaría sin cifrar en la base, por eso la
     * política de contraseñas los rechaza con este método.
     *
     * @return {@code true} si todos los caracteres se pueden cifrar.
     */
    public static boolean puedeCifrar(String texto) {
        if (texto == null) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            if (grupoDe(texto.charAt(i)) == null) {
                return false;
            }
        }
        return true;
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
     * Desplaza cada carácter dentro de su grupo según el carácter de la clave que le toca.
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
            String grupo = grupoDe(c);
            if (grupo != null) {
                int desplazamiento = clave.charAt(i % clave.length());   // la clave se repite
                // floorMod da siempre una posición del grupo, aunque la resta sea negativa
                c = grupo.charAt(Math.floorMod(grupo.indexOf(c) + sentido * desplazamiento, grupo.length()));
            }
            resultado.append(c);
        }
        return resultado.toString();
    }

    /** @return El grupo del carácter, o {@code null} si el cifrado no lo transforma. */
    private static String grupoDe(char c) {
        if (TECLADO.indexOf(c) >= 0) {
            return TECLADO;
        }
        if (LATINO.indexOf(c) >= 0) {
            return LATINO;
        }
        return null;
    }

    /** @return Los caracteres de {@code desde} a {@code hasta}, inclusive. */
    private static String rango(char desde, char hasta) {
        StringBuilder caracteres = new StringBuilder();
        for (char c = desde; c <= hasta; c++) {
            caracteres.append(c);
        }
        return caracteres.toString();
    }
}
