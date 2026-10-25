package utec.secu.conexion;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Patrón SINGLETON: existe una sola instancia de esta clase en todo el programa.
//"ConexionBD es Singleton porque garantiza una única instancia y una única conexión compartida a PostgreSQL,
// en vez de abrir una conexión por cada consulta."
public class ConexionBD {

    private static final String HOST = "localhost";
    private static final String PUERTO = "5432";
    private static final String BASE_DE_DATOS = "secu_db";
    private static final String ESQUEMA = "PROYECTO";
    private static final String USUARIO = "PROYECTO";
    private static final String CONTRASENA = "Proyecto2026!";

    // %22 = comillas dobles, necesarias porque el esquema está en mayúsculas
    private static final String URL = "jdbc:postgresql://" + HOST + ":" + PUERTO + "/"
            + BASE_DE_DATOS + "?currentSchema=%22" + ESQUEMA + "%22";

    // La única instancia de la clase (empieza vacía)
    private static ConexionBD instancia;

    // La conexión a la base de datos que se comparte en todo el programa
    private Connection conexion;

    /**
     * Constructor privado que impide crear instancias directamente desde otras clases.
     */
    private ConexionBD() {
    }

    /**
     * Obtiene la única instancia de {@code ConexionBD}.
     *
     * @return Instancia única de {@code ConexionBD}.
     */
    public static ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    /**
     * Obtiene la conexión activa con la base de datos.
     * Si no existe una conexión o la conexión actual está cerrada, establece una nueva.
     *
     * @return Conexión activa con la base de datos PostgreSQL.
     * @throws SQLException Si ocurre un error al establecer o verificar la conexión.
     */
    public Connection getConnection() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            conexion = DriverManager.getConnection(URL, USUARIO, CONTRASENA);
        }
        return conexion;
    }
}