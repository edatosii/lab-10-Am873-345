package co.umariana.dao;

import co.umariana.conexion.ConexionBD;
import co.umariana.modelo.ArbolGenealogico;
import co.umariana.modelo.Persona;
import co.umariana.modelo.ResumenArbol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object): la única clase que habla con la base de datos. Guarda los árboles genealógicos
 * en las tablas "arboles" y "miembros" (ver db/schema.sql).
 *
 * Los métodos NO lanzan SQLException: la capturan, imprimen el mensaje y devuelven un valor de error
 * (-1, false, lista vacía o null). Todo el SQL usa PreparedStatement con parámetros (?).
 */
public class ArbolDAO {

    private static final int MAX_NOMBRE = 100;

    private static final String INSERT_ARBOL = "INSERT INTO arboles (nombre) VALUES (?)";
    private static final String INSERT_MIEMBRO =
            "INSERT INTO miembros (arbol_id, posicion, nombre, nacimiento, fallecimiento) VALUES (?, ?, ?, ?, ?)";

    // =====================================================================
    // ETAPA 4.1. Guardar un árbol nuevo (INSERT en dos tablas, con transacción)
    // =====================================================================

    /**
     * Guarda un árbol nuevo: una fila en "arboles" y una fila en "miembros" por cada persona.
     *
     * @return el id del árbol; -1 si el nombre es inválido, el árbol es null o hubo un error
     *         (en ese caso NO queda nada guardado)
     */
    public long guardar(String nombre, ArbolGenealogico arbol) {
        if (nombre == null || nombre.isBlank() || nombre.length() > MAX_NOMBRE || arbol == null) {
            return -1;
        }
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                long id;
                try (PreparedStatement ps = con.prepareStatement(INSERT_ARBOL, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre);
                    ps.executeUpdate();
                    try (ResultSet claves = ps.getGeneratedKeys()) {
                        if (!claves.next()) {
                            throw new SQLException("La base de datos no devolvió el id del árbol.");
                        }
                        id = claves.getLong(1);
                    }
                }
                insertarMiembros(con, id, arbol);
                con.commit();
                return id;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Error al guardar el árbol: " + e.getMessage());
                return -1;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error al guardar el árbol: " + e.getMessage());
            return -1;
        }
    }

    /** Inserta todos los miembros del árbol (en el orden de arbol.miembros()) dentro de la transacción abierta. */
    private void insertarMiembros(Connection con, long arbolId, ArbolGenealogico arbol) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(INSERT_MIEMBRO)) {
            for (Persona p : arbol.miembros()) {
                ps.setLong(1, arbolId);
                ps.setString(2, p.getPosicion());
                ps.setString(3, p.getNombre());
                ps.setInt(4, p.getNacimiento());
                if (p.getFallecimiento() == null) {
                    ps.setNull(5, java.sql.Types.INTEGER);
                } else {
                    ps.setInt(5, p.getFallecimiento());
                }
                ps.executeUpdate();
            }
        }
    }

    // =====================================================================
    // ETAPA 4.2. Listar (SELECT con JOIN y COUNT)
    // =====================================================================

    /**
     * @return un resumen por cada árbol guardado, ordenados por id; lista vacía si no hay árboles o hay un error
     */
    public List<ResumenArbol> listar() {
        String sql = "SELECT a.id, a.nombre, a.creado, COUNT(m.id) AS total "
                + "FROM arboles a LEFT JOIN miembros m ON m.arbol_id = a.id "
                + "GROUP BY a.id, a.nombre, a.creado ORDER BY a.id";
        List<ResumenArbol> lista = new ArrayList<>();
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new ResumenArbol(rs.getLong("id"), rs.getString("nombre"), rs.getInt("total"),
                        rs.getTimestamp("creado").toLocalDateTime()));
            }
            return lista;
        } catch (SQLException e) {
            System.out.println("Error al listar los árboles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // =====================================================================
    // ETAPA 4.3. Cargar un árbol (SELECT y reconstrucción del árbol)
    // =====================================================================

    /**
     * @return el árbol con ese id, reconstruido con todos sus miembros; null si no existe o hay un error
     */
    public ArbolGenealogico cargar(long id) {
        String sql = "SELECT posicion, nombre, nacimiento, fallecimiento FROM miembros WHERE arbol_id = ? ORDER BY id";
        try (Connection con = ConexionBD.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                ArbolGenealogico arbol = null;
                while (rs.next()) {
                    int f = rs.getInt("fallecimiento");
                    Integer fallecimiento = rs.wasNull() ? null : f;
                    Persona persona = new Persona(rs.getString("posicion"), rs.getString("nombre"),
                            rs.getInt("nacimiento"), fallecimiento);
                    if (arbol == null) {
                        arbol = new ArbolGenealogico(persona);   // la primera fila es el sujeto
                    } else {
                        arbol.agregar(persona);
                    }
                }
                return arbol;
            }
        } catch (SQLException | IllegalArgumentException e) {
            System.out.println("Error al cargar el árbol: " + e.getMessage());
            return null;
        }
    }

    // =====================================================================
    // ETAPA 4.4. Actualizar (reemplazar los miembros de un árbol, con transacción)
    // =====================================================================

    /**
     * Deja en la base de datos exactamente los miembros de {@code arbol}. El nombre del árbol NO cambia.
     *
     * @return true si se actualizó; false si no existe el árbol, si es null o si hubo un error
     *         (en ese caso NO se modifica nada)
     */
    public boolean actualizar(long id, ArbolGenealogico arbol) {
        if (arbol == null) {
            return false;
        }
        try (Connection con = ConexionBD.conectar()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement("SELECT id FROM arboles WHERE id = ?")) {
                    ps.setLong(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            con.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM miembros WHERE arbol_id = ?")) {
                    ps.setLong(1, id);
                    ps.executeUpdate();
                }
                insertarMiembros(con, id, arbol);
                con.commit();
                return true;
            } catch (SQLException e) {
                con.rollback();
                System.out.println("Error al actualizar el árbol: " + e.getMessage());
                return false;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Error al actualizar el árbol: " + e.getMessage());
            return false;
        }
    }

    // =====================================================================
    // ETAPA 4.5. Eliminar (DELETE)
    // =====================================================================

    /**
     * Elimina el árbol y todos sus miembros (la base de datos borra los miembros sola: ON DELETE CASCADE).
     *
     * @return true si se eliminó; false si no existía ese id o si hubo un error
     */
    public boolean eliminar(long id) {
        try (Connection con = ConexionBD.conectar();
             PreparedStatement ps = con.prepareStatement("DELETE FROM arboles WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error al eliminar el árbol: " + e.getMessage());
            return false;
        }
    }
}