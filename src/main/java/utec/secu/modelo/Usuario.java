package utec.secu.modelo;

/**
 * Representa los datos de un usuario del sistema SECU.
 * Se utiliza para transportar la información del usuario entre las distintas
 * capas de la aplicación.
 */
public class Usuario {
    /** Cédula de identidad: identifica al usuario y no cambia nunca, por eso es final y no tiene setter. */
    private final String ci;
    private String correo;
    private String nombre;
    private String apellido;
    private String rol;
    private String estado;
    private boolean requiereCambioClave;
    private String nacionalidad;
    private String calle;
    private String numDomicilio;
    private String telefono;

    /**
     * Crea un usuario con todos sus datos. Lo usan {@code UsuarioDAO}, al leer un usuario
     * de la base, y {@code MenuUsuarios}, para el alta y la modificación.
     *
     * @param ci                  Cédula de identidad; no cambia nunca.
     * @param correo              Correo electrónico.
     * @param nombre              Nombre.
     * @param apellido            Apellido.
     * @param rol                 Rol asignado.
     * @param estado              Estado actual (Activo o Inactivo).
     * @param requiereCambioClave Indica si debe cambiar su contraseña al ingresar.
     * @param nacionalidad        Nacionalidad.
     * @param calle               Calle del domicilio (opcional).
     * @param numDomicilio        Número de puerta (opcional).
     * @param telefono            Teléfono (opcional).
     */
    public Usuario(String ci, String correo, String nombre, String apellido,
                   String rol, String estado, boolean requiereCambioClave,
                   String nacionalidad, String calle, String numDomicilio, String telefono) {
        this.ci = ci;
        this.correo = correo;
        this.nombre = nombre;
        this.apellido = apellido;
        this.rol = rol;
        this.estado = estado;
        this.requiereCambioClave = requiereCambioClave;
        this.nacionalidad = nacionalidad;
        this.calle = calle;
        this.numDomicilio = numDomicilio;
        this.telefono = telefono;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public void setNumDomicilio(String numDomicilio) {
        this.numDomicilio = numDomicilio;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public String getCalle() {
        return calle;
    }

    public String getNumDomicilio() {
        return numDomicilio;
    }

    /**
     * Obtiene la cédula de identidad del usuario.
     *
     * @return Cédula de identidad del usuario.
     */
    public String getCi() {
        return ci;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return Correo electrónico del usuario.
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * Establece el correo electrónico del usuario.
     *
     * @param correo Registra correo electrónico del usuario.
     */
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    /**
     * Obtiene el nombre del usuario.
     *
     * @return Nombre del usuario.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del usuario.
     *
     * @param nombre Registra nombre del usuario.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene el apellido del usuario.
     *
     * @return Apellido del usuario.
     */
    public String getApellido() {
        return apellido;
    }

    /**
     * Establece el apellido del usuario.
     *
     * @param apellido Registra apellido del usuario.
     */
    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    /**
     * Obtiene el rol asignado al usuario.
     *
     * @return Rol del usuario.
     */
    public String getRol() {
        return rol;
    }

    /**
     * Obtiene el estado actual del usuario.
     *
     * @return Estado del usuario.
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Indica si el usuario debe cambiar su contraseña.
     *
     * @return {@code true} si el usuario debe cambiar su contraseña;
     *         {@code false} en caso contrario.
     */
    public boolean isRequiereCambioClave() {
        return requiereCambioClave;
    }

    /**
     * Establece si el usuario debe cambiar su contraseña.
     *
     * @param requiereCambioClave {@code true} si el usuario debe cambiar
     *                            su contraseña; {@code false} en caso contrario.
     */
    public void setRequiereCambioClave(boolean requiereCambioClave) {
        this.requiereCambioClave = requiereCambioClave;
    }
}
