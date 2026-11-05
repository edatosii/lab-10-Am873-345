package co.umariana.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * El árbol genealógico: un SUJETO (posición "1") con sus ASCENDIENTES (Ahnentafel, árbol binario)
 * y sus DESCENDIENTES (d'Aboville, árbol general). Solo guarda a los miembros que existen.
 */
public class ArbolGenealogico {

    /**
     * Nodo del árbol: guarda una persona, su padre y su madre (ascendientes, árbol binario) y la lista de sus
     * hijos (descendientes, árbol general) ordenada por número de orden.
     */
    private static final class Nodo {
        final Persona persona;
        Nodo padre;
        Nodo madre;
        final List<Nodo> hijos = new ArrayList<>();

        Nodo(Persona persona) {
            this.persona = persona;
        }

        /** Número de orden de este nodo entre sus hermanos (solo para descendientes). */
        int orden() {
            return Posicion.orden(persona.getPosicion());
        }
    }

    private final Nodo raiz;
    private int cantidad;

    // =====================================================================
    // Etapa 2.1: el sujeto
    // =====================================================================

    /**
     * Crea un árbol que solo tiene al sujeto.
     *
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si su posición no es "1"
     */
    public ArbolGenealogico(Persona sujeto) {
        if (sujeto == null) {
            throw new ArbolInvalidoException("El sujeto no puede ser nulo.");
        }
        sujeto.validar();
        if (!Posicion.esSujeto(sujeto.getPosicion())) {
            throw new ArbolInvalidoException("El sujeto debe estar en la posición 1 y no en la " + sujeto.getPosicion() + ".");
        }
        this.raiz = new Nodo(sujeto);
        this.cantidad = 1;
    }

    /** @return el sujeto (la persona de la posición "1") */
    public Persona sujeto() {
        return raiz.persona;
    }

    /** @return cuántos miembros tiene el árbol (el sujeto cuenta; un árbol nuevo tiene 1) */
    public int tamano() {
        return cantidad;
    }

    /** @return la persona de esa posición, o null si no existe o la posición no es válida */
    public Persona buscar(String posicion) {
        Nodo nodo = buscarNodo(posicion);
        return nodo == null ? null : nodo.persona;
    }

    /** @return true si hay alguien en esa posición (false también si la posición no es válida) */
    public boolean contiene(String posicion) {
        return buscarNodo(posicion) != null;
    }

    /** Busca el nodo de una posición recorriendo el camino que ella misma indica; null si no existe. */
    private Nodo buscarNodo(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            return raiz;
        }
        if (Posicion.esAscendiente(posicion)) {
            // Los bits después del primer 1 son el camino: 0 = padre, 1 = madre.
            String bits = Integer.toBinaryString(Integer.parseInt(posicion));
            Nodo actual = raiz;
            for (int i = 1; i < bits.length() && actual != null; i++) {
                actual = bits.charAt(i) == '0' ? actual.padre : actual.madre;
            }
            return actual;
        }
        if (Posicion.esDescendiente(posicion)) {
            String[] partes = posicion.split("\\.");
            Nodo actual = raiz;
            for (int i = 1; i < partes.length && actual != null; i++) {
                actual = hijoConOrden(actual, Integer.parseInt(partes[i]));
            }
            return actual;
        }
        return null;
    }

    private static Nodo hijoConOrden(Nodo nodo, int orden) {
        for (Nodo hijo : nodo.hijos) {
            if (hijo.orden() == orden) {
                return hijo;
            }
        }
        return null;
    }

    // =====================================================================
    // Etapas 2.2 y 2.3: agregar
    // =====================================================================

    /**
     * Agrega una persona en la posición que ella misma trae. Primero debe existir la persona de la que depende.
     *
     * @return la misma persona
     * @throws IllegalArgumentException si la persona no es válida (Persona.validar)
     * @throws ArbolInvalidoException si la posición ya existe (o es "1"), si falta la persona de la que depende,
     *         o si los años de nacimiento no cumplen las reglas
     */
    public Persona agregar(Persona persona) {
        if (persona == null) {
            throw new ArbolInvalidoException("La persona no puede ser nula.");
        }
        persona.validar();
        String posicion = persona.getPosicion();
        if (contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " está repetida.");
        }

        if (Posicion.esAscendiente(posicion)) {
            String posHijo = Posicion.posicionDelHijoEnLinea(posicion);
            Nodo hijo = buscarNodo(posHijo);
            if (hijo == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + " porque falta la posición " + posHijo + ", de la que depende.");
            }
            if (persona.getNacimiento() >= hijo.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion + " (" + persona.getNombre()
                        + ") debe haber nacido antes que la posición " + posHijo + ".");
            }
            Nodo nuevo = new Nodo(persona);
            if (Integer.parseInt(posicion) % 2 == 0) {
                hijo.padre = nuevo;
            } else {
                hijo.madre = nuevo;
            }
        } else {
            String posProgenitor = Posicion.posicionDelProgenitor(posicion);
            Nodo progenitor = buscarNodo(posProgenitor);
            if (progenitor == null) {
                throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                        + " porque falta la posición " + posProgenitor + ", de la que depende.");
            }
            if (persona.getNacimiento() <= progenitor.persona.getNacimiento()) {
                throw new ArbolInvalidoException("La posición " + posicion + " (" + persona.getNombre()
                        + ") debe haber nacido después que la posición " + posProgenitor + ".");
            }
            insertarHijo(progenitor, new Nodo(persona));
        }
        cantidad++;
        return persona;
    }

    /** Inserta el hijo en la lista del progenitor, manteniéndola ordenada por número de orden. */
    private static void insertarHijo(Nodo progenitor, Nodo nuevo) {
        int orden = nuevo.orden();
        int i = 0;
        while (i < progenitor.hijos.size() && progenitor.hijos.get(i).orden() < orden) {
            i++;
        }
        progenitor.hijos.add(i, nuevo);
    }

    /** Agrega el PADRE de la persona de esa posición, en la posición 2n. */
    public Persona agregarPadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        exigirAscendienteOSujeto(posicion);
        String nueva = Posicion.posicionDelPadre(posicion);
        exigirCabeEnAhnentafel(nueva);
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    /** Agrega la MADRE de la persona de esa posición, en la posición 2n + 1. */
    public Persona agregarMadre(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        exigirAscendienteOSujeto(posicion);
        String nueva = Posicion.posicionDeLaMadre(posicion);
        exigirCabeEnAhnentafel(nueva);
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    private void exigirAscendienteOSujeto(String posicion) {
        if (!contiene(posicion)) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esAscendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar padres al sujeto y a sus ascendientes, "
                    + "y la posición " + posicion + " es un descendiente.");
        }
    }

    private static void exigirCabeEnAhnentafel(String posicion) {
        if (!Posicion.esAhnentafel(posicion)) {
            throw new ArbolInvalidoException("No se puede agregar la posición " + posicion
                    + ": se superó el máximo de " + Posicion.MAX_AHNENTAFEL + " (10 generaciones de ascendientes).");
        }
    }

    /**
     * Agrega un HIJO a la persona de esa posición. Su posición es la del progenitor + "." + el número
     * siguiente al MAYOR número de hijo que ya exista (o 1 si todavía no tiene hijos).
     */
    public Persona agregarHijo(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        Nodo progenitor = buscarNodo(posicion);
        if (progenitor == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (!Posicion.esSujeto(posicion) && !Posicion.esDescendiente(posicion)) {
            throw new ArbolInvalidoException("Solo se pueden agregar hijos al sujeto y a sus descendientes, "
                    + "y la posición " + posicion + " es un ascendiente.");
        }
        int siguiente = 1;
        for (Nodo hijo : progenitor.hijos) {
            siguiente = Math.max(siguiente, hijo.orden() + 1);
        }
        String nueva = posicion + "." + siguiente;
        if (siguiente > Posicion.MAX_ORDEN || !Posicion.esDAboville(nueva)) {
            throw new ArbolInvalidoException("No se puede agregar otro hijo a la posición " + posicion
                    + ": se superó el límite de posiciones.");
        }
        return agregar(new Persona(nueva, nombre, nacimiento, fallecimiento));
    }

    // =====================================================================
    // Etapa 2.4: eliminar
    // =====================================================================

    /**
     * Elimina a la persona de esa posición y TODO lo que depende de ella (sus ascendientes si es ascendiente,
     * sus descendientes si es descendiente). Las demás posiciones NO cambian de número.
     *
     * @return cuántas personas se eliminaron (ella incluida)
     * @throws ArbolInvalidoException si la posición es "1" o si no existe
     */
    public int eliminar(String posicion) {
        if (Posicion.esSujeto(posicion)) {
            throw new ArbolInvalidoException("El sujeto (posición 1) no se puede eliminar.");
        }
        Nodo nodo = buscarNodo(posicion);
        if (nodo == null) {
            throw new ArbolInvalidoException("La posición " + posicion + " no existe en el árbol.");
        }
        if (Posicion.esAscendiente(posicion)) {
            Nodo hijo = buscarNodo(Posicion.posicionDelHijoEnLinea(posicion));
            if (hijo.padre == nodo) {
                hijo.padre = null;
            } else {
                hijo.madre = null;
            }
        } else {
            Nodo progenitor = buscarNodo(Posicion.posicionDelProgenitor(posicion));
            progenitor.hijos.remove(nodo);
        }
        int eliminados = contarNodos(nodo);
        cantidad -= eliminados;
        return eliminados;
    }

    /** Cuenta el nodo, todos sus ascendientes y todos sus descendientes. */
    private static int contarNodos(Nodo nodo) {
        if (nodo == null) {
            return 0;
        }
        int total = 1 + contarNodos(nodo.padre) + contarNodos(nodo.madre);
        for (Nodo hijo : nodo.hijos) {
            total += contarNodos(hijo);
        }
        return total;
    }

    // =====================================================================
    // Etapa 2.5: recorridos
    // =====================================================================

    /**
     * @return todos los miembros: el sujeto; luego los ascendientes por número Ahnentafel ascendente;
     *         luego los descendientes en PREORDEN. La lista es una copia nueva.
     */
    public List<Persona> miembros() {
        List<Persona> lista = new ArrayList<>();
        lista.add(raiz.persona);
        lista.addAll(ascendientes());
        lista.addAll(descendientes());
        return lista;
    }

    /** @return solo los ascendientes, por número Ahnentafel ascendente (sin el sujeto) */
    public List<Persona> ascendientes() {
        // Recorrido por niveles (padre antes que madre): equivale al orden Ahnentafel 2, 3, 4, 5...
        List<Persona> lista = new ArrayList<>();
        List<Nodo> cola = new ArrayList<>();
        agregarSiExiste(cola, raiz.padre);
        agregarSiExiste(cola, raiz.madre);
        for (int i = 0; i < cola.size(); i++) {
            Nodo nodo = cola.get(i);
            lista.add(nodo.persona);
            agregarSiExiste(cola, nodo.padre);
            agregarSiExiste(cola, nodo.madre);
        }
        return lista;
    }

    private static void agregarSiExiste(List<Nodo> cola, Nodo nodo) {
        if (nodo != null) {
            cola.add(nodo);
        }
    }

    /** @return solo los descendientes, en preorden (sin el sujeto) */
    public List<Persona> descendientes() {
        List<Persona> lista = new ArrayList<>();
        for (Nodo hijo : raiz.hijos) {
            preorden(hijo, lista);
        }
        return lista;
    }

    private static void preorden(Nodo nodo, List<Persona> lista) {
        lista.add(nodo.persona);
        for (Nodo hijo : nodo.hijos) {
            preorden(hijo, lista);
        }
    }
}