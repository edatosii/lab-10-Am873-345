package co.umariana.modelo;

/**
 * Las posiciones de un árbol genealógico: Ahnentafel (ascendientes) y d'Aboville (descendientes).
 * La posición "1" (el sujeto) cuenta como Ahnentafel.
 */
public final class Posicion {

    /** La posición del sujeto. */
    public static final String SUJETO = "1";
    /** Mayor número Ahnentafel permitido (10 generaciones de ascendientes). */
    public static final int MAX_AHNENTAFEL = 2047;
    /** Mayor número de orden permitido en cada nivel d'Aboville. */
    public static final int MAX_ORDEN = 9999;
    /** Longitud máxima de una posición d'Aboville (es lo que cabe en la base de datos). */
    public static final int MAX_LONGITUD = 50;

    private Posicion() {
    }

    // ===================== Etapa 1.1: clasificar =====================

    /** true solo si la posición es exactamente "1". */
    public static boolean esSujeto(String posicion) {
        return SUJETO.equals(posicion);
    }

    /** Entero positivo sin ceros a la izquierda, de 1 a 2047. Incluye el "1". */
    public static boolean esAhnentafel(String posicion) {
        if (posicion == null || !posicion.matches("[1-9][0-9]{0,3}")) {
            return false;
        }
        return Integer.parseInt(posicion) <= MAX_AHNENTAFEL;
    }

    /** Empieza por "1" y sigue con uno o más grupos ".k" (k de 1 a 9999). Máximo 50 caracteres. */
    public static boolean esDAboville(String posicion) {
        if (posicion == null || posicion.length() > MAX_LONGITUD) {
            return false;
        }
        if (!posicion.matches("1(\\.[1-9][0-9]{0,3})+")) {
            return false;
        }
        for (String parte : posicion.substring(2).split("\\.")) {
            if (Integer.parseInt(parte) > MAX_ORDEN) {
                return false;
            }
        }
        return true;
    }

    /** true si es Ahnentafel o d'Aboville. */
    public static boolean esValida(String posicion) {
        return esAhnentafel(posicion) || esDAboville(posicion);
    }

    /** Ascendiente: Ahnentafel válida distinta de "1". */
    public static boolean esAscendiente(String posicion) {
        return esAhnentafel(posicion) && !esSujeto(posicion);
    }

    /** Descendiente: d'Aboville válida (el "1" no cuenta). */
    public static boolean esDescendiente(String posicion) {
        return esDAboville(posicion);
    }

    // ===================== Etapa 1.2: navegar =====================

    /** Posición del padre de la persona n: 2n. */
    public static String posicionDelPadre(String ahnentafel) {
        exigirAhnentafel(ahnentafel);
        return String.valueOf(2 * Integer.parseInt(ahnentafel));
    }

    /** Posición de la madre de la persona n: 2n + 1. */
    public static String posicionDeLaMadre(String ahnentafel) {
        exigirAhnentafel(ahnentafel);
        return String.valueOf(2 * Integer.parseInt(ahnentafel) + 1);
    }

    /** Posición del hijo en la línea hacia el sujeto: n / 2. */
    public static String posicionDelHijoEnLinea(String ahnentafel) {
        if (!esAscendiente(ahnentafel)) {
            throw new IllegalArgumentException("La posición '" + ahnentafel + "' no es un ascendiente.");
        }
        return String.valueOf(Integer.parseInt(ahnentafel) / 2);
    }

    /** Progenitor de un descendiente: la posición sin el último número. */
    public static String posicionDelProgenitor(String daboville) {
        exigirDescendiente(daboville);
        return daboville.substring(0, daboville.lastIndexOf('.'));
    }

    /** Número de orden entre hermanos: el último número. */
    public static int orden(String daboville) {
        exigirDescendiente(daboville);
        return Integer.parseInt(daboville.substring(daboville.lastIndexOf('.') + 1));
    }

    /** Generaciones de distancia al sujeto. */
    public static int generacion(String posicion) {
        if (esAhnentafel(posicion)) {
            // parte entera del logaritmo en base 2 = (bits del número) - 1
            return 31 - Integer.numberOfLeadingZeros(Integer.parseInt(posicion));
        }
        if (esDAboville(posicion)) {
            int puntos = 0;
            for (int i = 0; i < posicion.length(); i++) {
                if (posicion.charAt(i) == '.') {
                    puntos++;
                }
            }
            return puntos;
        }
        throw new IllegalArgumentException("La posición '" + posicion + "' no es válida.");
    }

    // ===================== Auxiliares =====================

    private static void exigirAhnentafel(String posicion) {
        if (!esAhnentafel(posicion)) {
            throw new IllegalArgumentException("La posición '" + posicion + "' no es Ahnentafel válida.");
        }
    }

    private static void exigirDescendiente(String posicion) {
        if (!esDescendiente(posicion)) {
            throw new IllegalArgumentException("La posición '" + posicion + "' no es d'Aboville válida.");
        }
    }
}