package utec.secu.servicio;

import utec.secu.modelo.Usuario;

/**
 * Usuarios de ejemplo para las pruebas: los mismos que carga 02-datos-prueba.sql.
 */
final class DatosDePrueba {

    private DatosDePrueba() {
    }

    static Usuario administrador() {
        return new Usuario("40123450", "admin@secu.edu.uy", "Administrador", "SECU", "Administrador",
                "Activo", false, "Uruguaya", null, null, null);
    }

    static Usuario evaluador() {
        return new Usuario("39451204", "eval2@utec.edu.uy", "Lucía", "Fernández", "Evaluador externo",
                "Activo", false, "Uruguaya", null, null, null);
    }
}
