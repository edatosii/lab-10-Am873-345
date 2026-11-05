package co.umariana.construccion;

import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.ArbolInvalidoException;
import co.umariana.modelo.Persona;
import co.umariana.modelo.Posicion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye un árbol genealógico a partir de un texto: cada línea es una persona.
 *
 *     posicion;nombre;nacimiento;fallecimiento
 *
 * El fallecimiento puede quedar vacío. Los errores se informan con ArbolInvalidoException
 * y, si pertenecen a una línea, con el prefijo "Línea N: ".
 */
public final class ConstructorArbol {

    private ConstructorArbol() {
    }

    /** Una persona leída del texto junto con el número de la línea de la que salió (para informar errores). */
    private record Linea(int numero, Persona persona) {
    }

    /**
     * Convierte el texto en un árbol.
     *
     * @throws ArbolInvalidoException si el texto no cumple las reglas
     */
    public static ArbolGenealogico construir(String texto) {
        // Regla 1: texto nulo o sin contenido
        if (texto == null || texto.isBlank()) {
            throw new ArbolInvalidoException("El texto está vacío: escriba al menos una persona (la posición 1).");
        }

        // Paso 1: leer y validar cada línea (los números de línea cuentan también las líneas en blanco)
        String[] lineas = texto.split("\\r?\\n", -1);
        List<Linea> leidas = new ArrayList<>();
        Map<String, Integer> posicionesVistas = new HashMap<>();
        for (int i = 0; i < lineas.length; i++) {
            if (lineas[i].isBlank()) {
                continue;
            }
            int numero = i + 1;
            Persona persona = leerPersona(numero, lineas[i]);
            Integer anterior = posicionesVistas.put(persona.getPosicion(), numero);
            if (anterior != null) {
                throw error(numero, "la posición " + persona.getPosicion()
                        + " está repetida (ya aparece en la línea " + anterior + ").");
            }
            leidas.add(new Linea(numero, persona));
        }

        // Paso 2: debe existir el sujeto (en cualquier línea)
        if (!posicionesVistas.containsKey(Posicion.SUJETO)) {
            throw new ArbolInvalidoException("Falta el sujeto: debe haber una línea con la posición 1.");
        }

        // Paso 3: ordenar para que cada persona se agregue después de la que necesita:
        // el sujeto, los ascendientes por número Ahnentafel y los descendientes por generación
        List<Linea> sujeto = new ArrayList<>();
        List<Linea> ascendientes = new ArrayList<>();
        List<Linea> descendientes = new ArrayList<>();
        for (Linea l : leidas) {
            String pos = l.persona().getPosicion();
            if (Posicion.esSujeto(pos)) {
                sujeto.add(l);
            } else if (Posicion.esAscendiente(pos)) {
                ascendientes.add(l);
            } else {
                descendientes.add(l);
            }
        }
        ascendientes.sort((a, b) -> Integer.compare(Integer.parseInt(a.persona().getPosicion()),
                Integer.parseInt(b.persona().getPosicion())));
        descendientes.sort((a, b) -> Integer.compare(Posicion.generacion(a.persona().getPosicion()),
                Posicion.generacion(b.persona().getPosicion())));

        // Paso 4: armar el árbol; el propio árbol valida sus reglas y el error se informa con la línea de la persona
        ArbolGenealogico arbol;
        Linea lineaSujeto = sujeto.get(0);
        try {
            arbol = new ArbolGenealogico(lineaSujeto.persona());
        } catch (IllegalArgumentException e) {
            throw error(lineaSujeto.numero(), e.getMessage());
        }
        List<Linea> restantes = new ArrayList<>(ascendientes);
        restantes.addAll(descendientes);
        for (Linea l : restantes) {
            try {
                arbol.agregar(l.persona());
            } catch (IllegalArgumentException e) {
                throw error(l.numero(), e.getMessage());
            }
        }
        return arbol;
    }

    /** Convierte una línea "posicion;nombre;nacimiento;fallecimiento" en una Persona válida. */
    private static Persona leerPersona(int numero, String linea) {
        String[] campos = linea.split(";", -1);
        if (campos.length < 3 || campos.length > 4) {
            throw error(numero, "debe tener el formato posicion;nombre;nacimiento;fallecimiento "
                    + "(se encontraron " + campos.length + " campos).");
        }
        String posicion = campos[0].strip();
        String nombre = campos[1].strip();
        int nacimiento = entero(numero, "nacimiento", campos[2]);
        Integer fallecimiento = null;
        if (campos.length == 4 && !campos[3].isBlank()) {
            fallecimiento = entero(numero, "fallecimiento", campos[3]);
        }
        Persona persona = new Persona(posicion, nombre, nacimiento, fallecimiento);
        try {
            persona.validar();
        } catch (IllegalArgumentException e) {
            throw error(numero, e.getMessage());
        }
        return persona;
    }

    private static int entero(int numero, String campo, String valor) {
        try {
            return Integer.parseInt(valor.strip());
        } catch (NumberFormatException e) {
            throw error(numero, "el " + campo + " debe ser un número entero y se encontró '" + valor.strip() + "'.");
        }
    }

    private static ArbolInvalidoException error(int numero, String mensaje) {
        return new ArbolInvalidoException("Línea " + numero + ": " + mensaje);
    }
}