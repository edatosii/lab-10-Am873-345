package co.umariana.modelo;

/**
 * Un miembro del árbol genealógico: su posición, su nombre y sus años de nacimiento y fallecimiento
 * (el fallecimiento es opcional: null si la persona sigue viva o no se sabe).
 */
public class Persona {

    /** Longitud máxima del nombre (es lo que cabe en la base de datos). */
    public static final int MAX_NOMBRE = 100;

    private final String posicion;
    private final String nombre;
    private final int nacimiento;
    private final Integer fallecimiento;

    public Persona(String posicion, String nombre, int nacimiento, Integer fallecimiento) {
        this.posicion = posicion;
        this.nombre = nombre;
        this.nacimiento = nacimiento;
        this.fallecimiento = fallecimiento;
    }

    /**
     * Revisa que la persona sea válida. Si no lo es, lanza IllegalArgumentException con un mensaje
     * que explique el problema. Las reglas se revisan en este orden.
     */
    public void validar() {
        // 1. La posición debe ser Ahnentafel o d'Aboville
        if (!Posicion.esValida(posicion)) {
            throw new IllegalArgumentException("La posición '" + posicion + "' no es válida.");
        }
        // 2. El nombre no puede ser nulo ni estar en blanco
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        // 3. El nombre no puede pasar de 100 caracteres
        if (nombre.length() > MAX_NOMBRE) {
            throw new IllegalArgumentException("El nombre no puede tener más de " + MAX_NOMBRE + " caracteres.");
        }
        // 4. El nombre no puede tener ';' ni saltos de línea
        if (nombre.indexOf(';') >= 0 || nombre.indexOf('\n') >= 0 || nombre.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("El nombre no puede contener ';' ni saltos de línea.");
        }
        // 5. El nacimiento debe ser mayor que 0
        if (nacimiento <= 0) {
            throw new IllegalArgumentException("El año de nacimiento debe ser mayor que 0.");
        }
        // 6. El fallecimiento es opcional, pero no puede ser menor que el nacimiento
        if (fallecimiento != null && fallecimiento < nacimiento) {
            throw new IllegalArgumentException("El año de fallecimiento no puede ser anterior al de nacimiento.");
        }
    }

    public String getPosicion() { return posicion; }
    public String getNombre() { return nombre; }
    public int getNacimiento() { return nacimiento; }
    public Integer getFallecimiento() { return fallecimiento; }

    @Override
    public String toString() {
        return posicion + " - " + nombre + " (" + nacimiento + "-" + (fallecimiento == null ? "" : fallecimiento) + ")";
    }
}