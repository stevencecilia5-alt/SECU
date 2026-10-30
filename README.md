# SECU — Sistema de Evaluación de Convocatorias UTEC
**Licenciatura en Tecnologías de la Información — UTEC (Generación 2026)**  
**Grupo 10 — ANGLO Deploy**  
**Versión:** 3.0 (Etapa 3 — Proceso de evaluación, documentos adjuntos, orden final CES, vista consolidada y pruebas unitarias)

---

## 1. Descripción del Proyecto
**SECU** es una plataforma de software desarrollada en Java y PostgreSQL concebida para centralizar, automatizar y auditar el proceso de postulación, evaluación y selección de becas académicas de la Universidad Tecnológica (UTEC).

La solución sustituye circuitos administrativos dispersos mediante una arquitectura orientada a objetos modular, desacoplada en capas (`ui`, `servicio`, `seguridad`, `dao`, `factory`, `conexion`, `modelo` y `excepciones`), integrando persistencia transaccional sobre **PostgreSQL 16** normalizada en **Tercera Forma Normal (3FN)**.

---

## 2. Integrantes del Equipo (ANGLO Deploy) y Tutores

| Integrante | Rol en el Proyecto | Responsabilidad Principal |
| :--- | :--- | :--- |
| **Juan San Pedro** | Desarrollador Java (Líder) | Arquitectura del backend, servicios y persistencia DAO |
| **Jorge Pedreira** | Desarrollador Java (Apoyo) | Lógica de interfaz de consola, flujos interactivos y menús |
| **Steven Cecilia** | Especialista en Base de Datos (DBA) | Modelo relacional 3FN, scripts DDL/DML y Docker |
| **Rocío Cortazzo** | Líder de Proyecto & Coordinación QA | Gestión de entregables, planificación y diseño de pruebas |
| **Facundo Fernández** | Analista de Calidad de Software (QA) | Ejecución en TestLink, diseño de casos y reporte en Mantis |

* **Tutores por Área (UTEC):**
  * **Tutor de Grupo y Testing:** María Pilar Albacete
  * **Tutor de Base de Datos:** Sergio Baudine
  * **Tutor de Programación:** Juan Maccio

---

## 3. Módulos Desarrollados y Alcance

### Módulos Implementados en la Etapa 2
El sistema cuenta con **3 módulos asignados desarrollados, integrados y operativos por consola Java**:

1. **CU01 — Autenticación, Gestión de Sesión y Control de Acceso:**
   * Inicio de sesión interactivo por consola mediante correo institucional y contraseña. El correo no distingue mayúsculas.
   * Verificación de credenciales cifradas con el algoritmo propio de `ServicioCifrado` y validación de estado activo (`Activo` vs. `Inactivo`).
   * Detección del flag `req_cam_clave = TRUE` y redirección forzosa al flujo de cambio obligatorio de contraseña en el primer ingreso.
   * Flujo de cambio voluntario de clave desde el menú principal (cada usuario gestiona exclusivamente su propia contraseña). La contraseña actual se verifica apenas se escribe, y el servicio vuelve a validar la actual, la política y la confirmación antes de guardar.
   * Política de contraseñas: al menos 6 caracteres, no solo espacios, y solo letras (también con tilde o ñ), números y símbolos. El cifrado propio transforma los caracteres del teclado y los del alfabeto latino (del `¡` a la `ÿ`); un carácter fuera de esos, como un emoji o el `€`, se rechaza, así la base nunca guarda una parte en claro.
   * Despliegue dinámico de opciones del menú principal según el rol autenticado (`Administrador` vs. `Evaluador externo`).

2. **CU03 — Gestión de Usuarios y Roles:**
   * Alta de nuevos usuarios (`Administrador` o `Evaluador externo`) con generación automática de contraseñas temporales aleatorias de 8 caracteres.
   * Envío desacoplado de credenciales temporales a la bandeja simulada local (`correos_simulados/bandeja_salida.txt`), sin exponer jamás contraseñas en consola ni en logs.
   * Validaciones de negocio en capa de servicio: formato y longitud de nombres/apellidos (2 a 50 caracteres), algoritmo de validación de dígito verificador de cédula de identidad uruguaya, formato de correo y control estricto de unicidad de CI y correo institucional en base de datos. El teléfono y la calle son opcionales: si se ingresan, se validan.
   * **Reintento del dato inválido:** ante un dato inválido, el sistema muestra qué regla no se cumple y vuelve a pedir solo ese dato, sin perder los ya ingresados.
   * Consulta tabular de usuarios en consola con columnas alineadas.
   * Inactivación lógica (baja) y reactivación/restablecimiento de credenciales temporales.
   * Un Administrador no puede darse de baja ni cambiar su propio rol: así siempre queda uno activo para reactivar a los demás. Tampoco se cambia el rol de un evaluador con postulaciones asignadas ni de un Administrador con convocatorias a su cargo; el mensaje dice el motivo.

3. **CU07 — Consulta y Gestión de Postulaciones:**
   * Búsqueda con un filtro por consulta: convocatoria, postulante, correo, documento, tutor, grupo de investigación, estado o rango de fechas (con validación cronológica), o listado de todas las postulaciones.
   * Visualización del detalle completo con los datos registrados en CU05: datos personales y de contacto del postulante (documento, correo, celular, domicilio, nacionalidad y procedencia), formación (carrera, posgrado e institución), docencia en UTEC y actividad laboral, tipo y descripción de la convocatoria, datos del proyecto, departamento, sede, tutores asignados, documentos adjuntos cargados y evaluadores vinculados. El detalle del evaluador (CU09) muestra solo la propuesta, sin los datos personales.
   * **Modificación administrativa focalizada:** Permite modificar exclusivamente el departamento académico y la sede regional sobre postulaciones en estados editables (`Registrada` o `En revisión`), respetando la propuesta inmutable del postulante.
   * Transición controlada de estados (`Registrada`, `En revisión`, `No elegible`, `En evaluación`, `Evaluada`, `Aprobada`, `Rechazada`, `Cancelada`), aplicando control de concurrencia optimista contra PostgreSQL. Una cancelación recibida fuera del sistema se registra en cualquier estado que no sea final, también desde `Evaluada` (ver la sección 10). Si se intenta un cambio no permitido, el mensaje dice desde qué estado y a cuál.

### Módulos Implementados en la Etapa 3

4. **CU08 + CU09 — Gestión del Proceso de Evaluación:**
   * **Asignación de evaluadores (CU08, Administrador):** asigna exactamente dos Evaluadores externos activos a cada postulación `En revisión`. La postulación pasa sola a `En evaluación` cuando tiene sus dos evaluadores y al menos uno empezó su evaluación. Permite consultar los evaluadores de una postulación y las postulaciones de un evaluador, reemplazar a un evaluador que todavía no registró su evaluación y registrar el juicio global de una postulación ya evaluada (`Evaluada`, `Aprobada` o `Rechazada`).
   * **Registro de evaluaciones (CU09, Evaluador externo):** cada evaluador ve solo las postulaciones que tiene asignadas y las califica con la rúbrica fija (6 secciones que suman 100% y 16 criterios con puntaje entero de 1 a 5). Puede guardar un borrador (`En proceso`) o finalizar (`Completada`), con el puntaje total ponderado por sección. Si la postulación ya tiene sus dos evaluadores, la primera evaluación guardada la pasa de `En revisión` a `En evaluación`. Cuando las dos evaluaciones están completas, la postulación pasa sola a `Evaluada`. Una evaluación completada ya no se modifica: el servicio lo vuelve a controlar al guardar. Un borrador sin ningún puntaje ni comentario no se guarda y la evaluación sigue `Pendiente`. La opción "Cancelar sin guardar" sale sin cambios.

5. **CU06 — Gestión de Documentos Adjuntos (opción 5 del menú de postulaciones):**
   * Muestra los documentos que pide la convocatoria de la postulación (obligatorios y opcionales), los que ya están cargados con el tamaño de cada archivo, y avisa qué documentos obligatorios faltan.
   * Carga cada documento validando que el archivo exista, tenga formato PDF (extensión `.pdf` y contenido PDF), no esté vacío y pese como máximo 10 MB. Si el archivo no es válido, informa el motivo y vuelve a pedir la ruta.
   * Cada tipo de documento se carga una sola vez, salvo *Otros documentos adicionales*, que admite hasta 4 archivos.
   * Permite abrir un documento con el visor de PDF del equipo y quitar un documento cargado por error. El evaluador también abre los documentos de sus postulaciones asignadas, desde *Ver detalle de una postulación asignada* (CU09).
   * Solo se cargan o quitan documentos de postulaciones `Registrada` o `En revisión`. El Administrador carga los documentos que entregó el postulante.
   * El archivo se copia a la carpeta `adjuntos/` y la base guarda solo su ruta (ver la sección 9).

6. **CU11 — Registro del Orden Final CES (opción 6 del menú de postulaciones):**
   * Muestra la lista de prelación de una convocatoria: puntaje de cada evaluador, puntaje consolidado, orden calculado por SECU, orden final CES y observaciones.
   * Registra el orden final CES partiendo del orden calculado: el Administrador mueve una postulación al puesto que decidió el CES (por ejemplo, al puesto 1) y las demás se corren solas. Se pueden mover varias; cada movimiento pide el motivo (observación obligatoria). La opción "Agregar una observación" le suma una observación del CES a cualquier postulación, aunque quede en su orden calculado. La opción "Volver al orden calculado" empieza de nuevo.
   * El orden calculado por SECU se guarda aparte (`ord_pre_calculado`) y nunca se sobrescribe con el orden CES (`ord_final_ces`).
   * Si ya hay un orden CES registrado, se puede reemplazar (pide confirmación).
   * Antes de guardar, el servicio vuelve a controlar las reglas del CU11: cada puesto de 1 a N una sola vez, y observación cuando el puesto final difiere del calculado.

7. **CU12 — Vista Consolidada y Reportes (opción 7 del menú de postulaciones):**
   * Muestra en una sola vista todas las postulaciones de la convocatoria elegida, ordenadas por puntaje consolidado (las canceladas, al final), con los datos mínimos del CU12: fecha, postulante, correo, proyecto, grupo (y otro grupo, si tiene), tutor y co-tutor, estado, los dos evaluadores con el estado, puntaje y comentarios de cada evaluación, puntaje consolidado, juicio global, orden calculado, orden final CES y observación CES.
   * Lo que todavía no existe (evaluadores, puntajes, órdenes) se muestra con `-`, sin impedir la consulta.
   * Exporta la misma vista a una planilla de Excel (.xlsx) generada con Apache POI (ver la sección 10).

---

## 4. Patrones de Diseño Implementados

Cada patrón resuelve una necesidad concreta de la aplicación:

| Patrón | Tipo / Clasificación | Clases Participantes | Ubicación / Paquete | Problema que Resuelve en SECU |
| :--- | :--- | :--- | :--- | :--- |
| **Singleton** | Creacional | `ConexionBD` | `utec.secu.conexion` | Garantiza una **única instancia activa de conexión JDBC** compartida a PostgreSQL para toda la sesión del programa, evitando la sobrecarga de abrir y cerrar conexiones físicas por cada consulta SQL. |
| **Factory Method** | Creacional | `PerfilUsuarioFactory`, `PerfilUsuarioDAO` (Interfaz), `AdministradorDAO`, `EvaluadorExternoDAO` | `utec.secu.factory` y `utec.secu.dao` | Encapsula y desacopla la instanciación de los DAOs de perfiles especializados (`administradores` o `eval_externos`) según el rol del usuario durante el alta o modificación, sin acoplar `UsuarioDAO` a clases concretas. |
| **Proxy** | Estructural | `ProxyBase` (Abstracta), `SeguridadProxy`, `PostulacionProxy`, `EvaluacionProxy`, `DocumentoProxy`, `ResultadoProxy` | `utec.secu.seguridad` | Actúa como barrera intermedia de seguridad frente a los DAOs (`UsuarioDAO`, `PostulacionDAO`, `EvaluacionDAO`, `DocumentoDAO` y `ResultadoDAO`). Verifica que exista sesión activa, que el usuario esté activo y que posea el rol que exige la operación (`Administrador`, o `Evaluador externo` para CU09, que además solo accede a las postulaciones que tiene asignadas) antes de delegarla. Lanza `AccesoDenegadoException` ante accesos no autorizados. |
| **Facade (Fachada)** | Estructural | `UsuarioService`, `PostulacionService`, `EvaluacionService`, `DocumentoService`, `ResultadoService` | `utec.secu.servicio` | Proporciona una **interfaz unificada y de alto nivel a los menús de consola (`ui`)**, coordinando validaciones de negocio, seguridad (Proxies), persistencia transaccional (DAOs) y servicios auxiliares (Correo). Además traduce los errores de la base (`SQLException`) a `ErrorDeDatosException`, así la consola no depende de JDBC. |
| **DAO (Data Access Object)** | Arquitectura de Persistencia | `UsuarioDAO`, `PostulacionDAO`, `EvaluacionDAO`, `DocumentoDAO`, `ResultadoDAO`, `AdministradorDAO`, `EvaluadorExternoDAO` | `utec.secu.dao` | Encapsula y centraliza todas las sentencias SQL parametrizadas (`PreparedStatement`), incluidas las condiciones de los filtros de búsqueda de CU07, aislando la base de datos relacional de la lógica de negocio y presentación. |

### Excepciones Personalizadas (`utec.secu.excepciones`)

Los servicios validan las reglas de negocio antes de guardar y, si algo no se cumple, lanzan una de estas excepciones. Los menús las atrapan y muestran su mensaje, así un error controlado nunca cierra la aplicación.

| Excepción | Cuándo se lanza | Ejemplo de mensaje |
| :--- | :--- | :--- |
| `DatoInvalidoException` | Un dato no cumple su regla de formato, longitud, rango o existencia. | "La Cédula no es válida…", "El Puntaje no es válido: debe ser un número entero de 1 a 5." |
| `OperacionNoPermitidaException` | El dato es correcto, pero el estado actual no permite la operación. | "La postulación ya tiene dos evaluadores asignados.", "Solo se pueden modificar postulaciones Registradas o En revisión." |
| `AccesoDenegadoException` | El Proxy detecta que no hay sesión, que el usuario está inactivo o que no tiene el rol requerido. | "Acceso denegado: Se requieren permisos de Administrador para asignar evaluadores." |
| `ErrorDeDatosException` | Falla la base de datos; los servicios la lanzan en lugar de la `SQLException`, que queda guardada como causa. | "No se pudo completar la operación con la base de datos." |

---

## 5. Seguridad y Cifrado de Contraseñas (Ámbito Académico)

En cumplimiento de las especificaciones de la cátedra de Programación (2026):

* **Clase responsable:** `utec.secu.seguridad.ServicioCifrado` (clase utilitaria final con constructor privado, desacoplada de la UI y los DAOs).
* **Algoritmo propio y reversible, con clave:** cada carácter de la contraseña se desplaza según la letra de la clave que le toca (una frase, constante `CLAVE`), dentro de su grupo: los 95 caracteres del teclado o los 94 del alfabeto latino (del `¡` a la `ÿ`, con la ñ y las tildes). Con la misma clave, `descifrar(texto, clave)` recupera la contraseña original; con otra clave, no. Como los caracteres del teclado quedan dentro del teclado, una contraseña sin ñ ni tildes se cifra igual que en la Etapa 2.
* **Justificación académica obligatoria:** Este mecanismo se implementa **únicamente con fines académicos** para ejercitar y evaluar el desarrollo de algoritmos propios en Java sin librerías externas que resuelvan completamente el proceso. En un entorno de producción real, este esquema se reemplaza por funciones de derivación criptográficamente seguras como BCrypt o Argon2 con salt aleatorio.
* **Persistencia segura:** PostgreSQL almacena exclusivamente el valor cifrado en la columna `contrasenia` de la tabla `usuarios`. En ningún caso se persiste texto plano: la política de contraseñas rechaza los caracteres que el algoritmo no transforma (`ServicioCifrado.puedeCifrar`).
* **Contraseñas con asteriscos:** al escribir una contraseña se muestran asteriscos (librería JLine, dependencia `org.jline:jline` del `pom.xml`). Funciona en una terminal real, como CMD o PowerShell al ejecutar el JAR (sección 8); en la consola Run de IntelliJ se ve lo que se escribe.
* **Confidencialidad de credenciales:** Las contraseñas temporales se escriben exclusivamente en el archivo local `correos_simulados/bandeja_salida.txt` a través de `ServicioCorreo`. La carpeta `correos_simulados/` se encuentra excluida en `.gitignore` para garantizar que las claves no se suban al repositorio remoto de GitLab.

---

## 6. Estructura de Paquetes del Proyecto (47 Clases)

El código fuente se organiza bajo la convención estándar de Maven en `src/main/java/utec/secu/`:

```
src/main/java/utec/secu/
├── conexion/
│   └── ConexionBD.java                         # Singleton: Conexión JDBC compartida a PostgreSQL (se vuelve a abrir si se cortó)
├── dao/
│   ├── AdministradorDAO.java                   # DAO específico para perfil Administrador
│   ├── DocumentoDAO.java                       # DAO de documentos adjuntos y tipos por convocatoria (CU06)
│   ├── EvaluacionDAO.java                      # DAO transaccional de asignaciones y evaluaciones (CU08, CU09)
│   ├── EvaluadorExternoDAO.java                # DAO específico para perfil Evaluador Externo
│   ├── PerfilUsuarioDAO.java                   # Interfaz base para DAOs de perfiles especializados
│   ├── PostulacionDAO.java                     # DAO transaccional para postulaciones, filtros y catálogos
│   ├── ResultadoDAO.java                       # DAO de puntaje consolidado, orden de prelación y orden CES (CU11, CU12)
│   └── UsuarioDAO.java                         # DAO de usuarios, autenticación y credenciales
├── excepciones/
│   ├── AccesoDenegadoException.java            # Control de acceso y sesiones (lanzada por Proxies)
│   ├── DatoInvalidoException.java              # Validación de entradas y reglas de negocio
│   ├── ErrorDeDatosException.java              # Falla de la base, traducida por los servicios (sin SQLException en ui)
│   └── OperacionNoPermitidaException.java      # Operaciones que el estado actual de los datos no permite
├── factory/
│   └── PerfilUsuarioFactory.java               # Factory Method para instanciar DAOs de perfiles
├── modelo/
│   ├── Criterio.java                           # Criterio de la rúbrica fija con su sección, peso y puntaje (CU09)
│   ├── DatosPostulante.java                    # Datos personales, de contacto y académicos del postulante registrados en CU05 (detalle de CU07)
│   ├── DocumentoAdjunto.java                   # Documento cargado en una postulación, con la ruta del PDF (CU06)
│   ├── EstadoPostulacion.java                  # Enum con estados y transiciones permitidas (CU07)
│   ├── Evaluacion.java                         # Asignación de un evaluador y su evaluación (CU08, CU09)
│   ├── FiltroPostulacion.java                  # Enum con los filtros de búsqueda de CU07 (sin SQL)
│   ├── Postulacion.java                        # Entidad del dominio de postulación
│   ├── ResultadoPostulacion.java               # Postulación con sus evaluaciones, consolidado y órdenes (CU11, CU12)
│   ├── TipoDocumento.java                      # Tipo de documento que pide una convocatoria, obligatorio u opcional (CU06)
│   └── Usuario.java                            # Entidad del dominio de usuario
├── seguridad/
│   ├── DocumentoProxy.java                     # Proxy de autorización para CU06 (Administrador)
│   ├── EvaluacionProxy.java                    # Proxy de autorización para CU08 (Administrador) y CU09 (evaluador asignado)
│   ├── PostulacionProxy.java                   # Proxy de autorización para PostulacionDAO
│   ├── ProxyBase.java                          # Clase abstracta con reglas comunes de sesión y rol
│   ├── ResultadoProxy.java                     # Proxy de autorización para CU11 y CU12 (Administrador)
│   ├── SeguridadProxy.java                     # Proxy de autenticación y autorización para UsuarioDAO
│   └── ServicioCifrado.java                    # Cifrado algorítmico propio y reversible (académico)
├── servicio/
│   ├── DocumentoService.java                   # Facade de negocio para CU06 (validación del PDF, copia y reglas)
│   ├── EvaluacionService.java                  # Facade de negocio para CU08 y CU09 (asignaciones, puntajes, cálculo)
│   ├── PostulacionService.java                 # Facade de negocio para CU07 (fechas, estados, filtros)
│   ├── ResultadoService.java                   # Facade de negocio para CU11 y CU12 (orden CES, exportación a Excel)
│   ├── ServicioCorreo.java                     # Simulador de correo electrónico a archivo local
│   └── UsuarioService.java                     # Facade de negocio para CU01 y CU03 (validaciones, altas)
└── ui/
    ├── Main.java                               # Punto de entrada de la aplicación y ciclo general
    ├── MenuAsignacion.java                     # Submenú interactivo para CU08 (asignación de evaluadores)
    ├── MenuBase.java                           # Clase abstracta base de los menús: lecturas, confirmaciones, listas y formatos comunes
    ├── MenuDocumentos.java                     # Submenú interactivo para CU06 (documentos adjuntos)
    ├── MenuInicio.java                         # Submenú de inicio de sesión y recuperación (CU01)
    ├── MenuMisEvaluaciones.java                # Submenú interactivo para CU09 (evaluaciones del evaluador)
    ├── MenuPostulaciones.java                  # Submenú interactivo para CU07 (postulaciones)
    ├── MenuPrincipal.java                      # Menú principal con opciones según rol autenticado
    ├── MenuResultados.java                     # Submenús interactivos para CU11 (orden CES) y CU12 (vista consolidada)
    └── MenuUsuarios.java                       # Submenú interactivo para CU03 (gestión de usuarios)
```

---

## 7. Requisitos y Configuración del Entorno

### Prerrequisitos
* **Java Development Kit (JDK):** Versión 17 o 21.
* **Apache Maven:** Versión 3.8 o superior. Descarga las dependencias del `pom.xml`: el driver de PostgreSQL, JLine (contraseñas con asteriscos), Apache POI (planilla de Excel del CU12), Log4j (usado por POI) y, solo para las pruebas, JUnit 5 y Mockito.
* **Motor de Base de Datos / Contenedor:** Docker Desktop **o** PostgreSQL 16+ instalado localmente.

---

### Configuración de la Base de Datos (PostgreSQL 16)

#### Opción A: Despliegue con Docker Compose (Recomendado)
Desde la terminal en la raíz del proyecto, ejecutar:
```bash
docker compose up -d
```

**Parámetros de conexión preconfigurados:**
* **Host:** `localhost`
* **Puerto:** `5432`
* **Base de datos:** `secu_db`
* **Esquema de trabajo:** `PROYECTO`
* **Usuario:** `PROYECTO`
* **Contraseña:** `Proyecto2026!`
* **URL JDBC:** `jdbc:postgresql://localhost:5432/secu_db?currentSchema="PROYECTO"`

#### Opción B: Carga Manual del Script SQL (Instancia Local)
Si se utiliza una instalación nativa de PostgreSQL sin Docker, ejecutar en DBeaver o pgAdmin sobre la base `secu_db`:
1. `sql/01-init-secu.sql` (Recrea el esquema `PROYECTO`, genera las 24 tablas en 3FN, restricciones CHECK, UNIQUE, dominios e inserta semillas de catálogos y usuario administrador inicial).
2. `sql/02-datos-prueba.sql` (Carga registros de prueba de postulaciones y convocatorias para probar todos los módulos: usuarios, convocatorias, postulantes con celular y formación, postulaciones en distintos estados (Aprobada, Rechazada y Cancelada se obtienen con el cambio de estado del CU07), evaluadores y evaluaciones; sus documentos adjuntos apuntan a los PDF de ejemplo de la carpeta `documentos/`).

---

## 8. Instrucciones de Compilación y Ejecución

### Desde la Terminal con Maven
1. **Compilar el proyecto:**
   ```bash
   mvn clean compile
   ```
2. **Ejecutar la aplicación de consola interactiva:**
   ```bash
   mvn exec:java -Dexec.mainClass="utec.secu.ui.Main"
   ```
3. **Ejecutar las pruebas unitarias** (no necesitan la base de datos ni Docker):
   ```bash
   mvn test
   ```

### Desde el IDE (IntelliJ IDEA)
1. Abrir la carpeta raíz del proyecto en IntelliJ IDEA.
2. Aguardar a que Maven sincronice las dependencias del `pom.xml`. Si el `pom.xml` cambió (por ejemplo, al agregar Apache POI), recargarlo desde el panel de Maven con **Reload All Maven Projects**.
3. Navegar a `src/main/java/utec/secu/ui/Main.java`.
4. Hacer clic derecho sobre la clase y seleccionar **Run 'Main.main()'**.
5. Para ejecutar las pruebas unitarias: clic derecho sobre la carpeta `src/test/java` y seleccionar **Run 'All Tests'**. También se pueden ejecutar desde el panel de Maven: **Lifecycle → test**.

### Generar y ejecutar el JAR (fuera de IntelliJ)
1. **File → Project Structure → Artifacts → + → JAR → From modules with dependencies.** En *Main class* elegir `utec.secu.ui.Main` y en *JAR files from libraries* elegir **Extract to the target JAR**, para que el JAR lleve adentro todas las dependencias del `pom.xml`. En *Directory for META-INF/MANIFEST.MF* elegir `src\main\java`: en `src\main\resources` ya está el `MANIFEST.MF` del proyecto (con `Enable-Native-Access: ALL-UNNAMED`, para que JLine no muestre un aviso con Java 22 o más nuevo) e IntelliJ no deja crear otro ahí. Confirmar con **OK**.
2. **Build → Build Artifacts → Build.** El JAR queda en `out\artifacts\secu_anglo_jar\` (la carpeta `out/` está en `.gitignore`).
3. Con la base levantada (`docker compose up -d`), abrir CMD o PowerShell **en la raíz del proyecto** y ejecutar:
   ```bash
   java -jar out\artifacts\secu_anglo_jar\secu-anglo.jar
   ```
   Se ejecuta desde la raíz porque las carpetas que usa SECU, como `correos_simulados/`, son rutas relativas a la carpeta actual.

---

## 9. Documentos Adjuntos (CU06): dónde quedan los archivos

* **La base guarda solo la ruta** de cada PDF (columna `rut_archivo` de `Docu_Adjuntos`), relativa a la carpeta desde donde se ejecuta la aplicación. Por eso hay que ejecutarla desde la **raíz del proyecto** (es lo que hace IntelliJ por defecto y también `mvn exec:java`).
* **Documentos de ejemplo (`documentos/`):** son los 56 PDF ficticios a los que apunta `sql/02-datos-prueba.sql` (`documentos/P01/plan_de_trabajo.pdf`, etc.). **Se suben a Git**, así todos los integrantes ven y abren los mismos documentos desde CU06 y CU07.
* **Documentos cargados (`adjuntos/`):** cada documento que se carga con CU06 se copia a `adjuntos/P<id>/` con el nombre del tipo (por ejemplo `adjuntos/P04/plan_de_trabajo.pdf`). La carpeta está en `.gitignore`: cada integrante tiene los suyos, igual que los registros de su base local.
* Al quitar un documento se borra su registro; el archivo se borra solo si está en `adjuntos/` (los de ejemplo no se tocan).
* **Archivos para probar las validaciones a mano** (`pruebas_manuales/cu06/`, los usan los casos de prueba del CU06; no los usan las pruebas unitarias): `documento_valido.pdf`, `documento_vacio.pdf` (0 bytes), `no_es_pdf.pdf` (texto con extensión `.pdf`), `documento.txt`, y los valores límite de tamaño (10 MB = 10.485.760 bytes): `documento_10_mb.pdf` (exactamente 10 MB, se acepta) y `documento_mas_de_10_mb.pdf` (1 byte más, se rechaza). Los dos de 10 MB se suben a Git: son casi todo ceros, así que comprimidos ocupan unos pocos KB.

---

## 10. Orden Final CES y Reportes (CU11 y CU12): reglas

* **Puntaje consolidado y orden calculado (CU10):** el CU10 no es un módulo del equipo, pero el CU11 y el CU12 necesitan su resultado. Por eso, cuando una postulación pasa a `Evaluada` (las dos evaluaciones `Completada`), SECU calcula y guarda en la misma transacción su **puntaje consolidado** = (Evaluación 1 + Evaluación 2) / 2, y vuelve a numerar el **orden calculado** de su convocatoria: de mayor a menor puntaje consolidado y, si empatan, alfabético por nombre del postulante.
* **Postulaciones evaluadas después del orden CES:** quedan sin orden calculado y el CU11 avisa que hay que registrar de nuevo el orden CES; al hacerlo, SECU recalcula el orden de toda la lista.
* **Postulación Evaluada que se cancela (CU07):** sale de la prelación y pierde su orden calculado y su puesto CES; conserva su puntaje. Si la convocatoria no tiene orden CES, el orden calculado de las demás se renumera enseguida. Si ya lo tiene, las demás conservan su puesto (queda un hueco) y el CU11 avisa que hay que registrar de nuevo el orden CES. En la vista del CU12 la cancelada aparece al final, sin orden.
* **Observaciones del orden CES:** la postulación que movió el CES guarda el motivo escrito; las que quedan corridas reciben una observación automática, y la que termina en su orden calculado no lleva, salvo la que el CES le agregue con "Agregar una observación" (esa se suma a cualquiera de las otras).
* **Registrar de nuevo el orden CES:** se parte del orden calculado nuevo; el puesto CES anterior se muestra como referencia.
* **Resultado de cada postulación:** el resultado definitivo (`Aprobada` o `Rechazada`) se registra con la opción 4 del menú de postulaciones (cambio de estado de CU07), que solo lo permite desde `Evaluada`.
* **Exportación a Excel (CU12):** genera una planilla `.xlsx` en la carpeta `reportes/` (en `.gitignore`) con la librería Apache POI (`poi-ooxml` en el `pom.xml`).
* **Mensajes de POI:** `log4j-core` y `src/main/resources/log4j2.xml` apagan los mensajes internos de POI en la consola.

---

## 11. Pruebas Unitarias (JUnit 5 y Mockito)

Las pruebas están en `src/test/java`, con la estructura estándar de Maven, y usan **JUnit 5** y **Mockito** (dependencias de alcance `test` del `pom.xml`, junto con `maven-surefire-plugin`). **No necesitan la base de datos ni Docker**: se pueden ejecutar en cualquier máquina (ver la sección 8).

### Cómo se prueba sin base de datos
* Las validaciones de datos son métodos `static` de los servicios (`UsuarioService.validarCedula`, `EvaluacionService.parsearPuntaje`, `DocumentoService.validarArchivo`, etc.) y se prueban directamente.
* El Proxy rechaza los accesos no autorizados **antes** de llamar al DAO, así que el control de acceso se prueba con el Proxy real.
* Las reglas que primero consultan datos (cédula ya registrada, tercer evaluador, etc.) se prueban con **Mockito**: la prueba crea un Proxy simulado con `mock(...)`, le indica qué devolver con `when(...).thenReturn(...)` y comprueba qué hizo el servicio con `verify(...)`. Para poder pasarle ese Proxy, `UsuarioService`, `PostulacionService` y `EvaluacionService` tienen un constructor de paquete que lo recibe; la aplicación usa el constructor público, que crea el Proxy real.
* Con JDK 21, Mockito puede mostrar al ejecutar las pruebas un aviso que empieza con "Mockito is currently self-attaching…". Es solo un aviso: las pruebas se ejecutan igual.

### Estructura (37 pruebas)
```
src/test/java/utec/secu/
├── conexion/
│   └── ConexionBDTest.java                     # Singleton (1)
├── factory/
│   └── PerfilUsuarioFactoryTest.java           # Factory Method (1)
├── modelo/
│   └── EstadoPostulacionTest.java              # CU07: cambios de estado (3)
├── seguridad/
│   └── ServicioCifradoTest.java                # Cifrado de contraseñas con clave (4)
└── servicio/
    ├── DatosDePrueba.java                      # Usuarios de ejemplo para las pruebas
    ├── UsuarioServiceTest.java                 # CU01 y CU03 (11, cuatro con Mockito)
    ├── PostulacionServiceTest.java             # CU07: fechas, modificación y cambio de estado (3, dos con Mockito)
    ├── EvaluacionServiceTest.java              # CU08 y CU09 (9, cinco con Mockito)
    ├── DocumentoServiceTest.java               # CU06: tamaño máximo de 10 MB (2)
    └── ResultadoServiceTest.java               # CU11: reglas del orden final CES (3)
```

### Qué verifican
| Módulo / componente | Pruebas | Reglas comprobadas |
| :--- | :---: | :--- |
| **CU01** | 5 | Contraseña de al menos 6 caracteres (límite 5 y 6); no formada solo por espacios (Mantis #0044831); con `€` o un emoji se rechaza y con ñ y tildes se acepta; confirmación igual a la nueva; con la contraseña actual incorrecta no se cambia nada (**Mockito**). |
| **CU03** | 6 | Cédula con dígito verificador correcto e incorrecto; nombre de 2 a 50 caracteres (límite 1 y 2); cédula ya registrada en un alta; un Administrador no se da de baja a sí mismo; no se cambia el rol de un evaluador con postulaciones asignadas (estas tres con **Mockito**: verifican además que no se guarde nada). |
| **CU07** | 6 | Transición permitida y no permitida; una Evaluada se puede cancelar; un estado final ya no cambia; fecha inexistente, formato incorrecto y rango invertido; una postulación Evaluada no se modifica y un cambio de estado no permitido dice el motivo (**Mockito**). |
| **CU08 y CU09** | 9 | Puntaje entero de 1 a 5 (inválidos y límites); puntaje total ponderado por sección; un Evaluador externo no puede asignar evaluadores (Proxy); no se asigna un tercer evaluador y no se reemplaza a un evaluador que ya empezó a evaluar; finalizar con criterios sin puntaje la deja En proceso (FAQ 4); una evaluación Completada no se vuelve a guardar y un borrador vacío no se guarda (**Mockito**). |
| **Seguridad y patrones** | 6 | Cifrado: con la misma clave se recupera la contraseña y con otra clave no; la ñ y las tildes también se cifran; el valor cifrado coincide con el que cargan los scripts de la base; Singleton; Factory Method. |
| **CU06 y CU11** (opcionales) | 5 | PDF de 10 MB exactos aceptado y de 10 MB más 1 byte rechazado; mover una postulación en el orden CES sin escribir el motivo; al moverla, las que quedan corridas reciben la observación automática; un orden con un puesto repetido no se guarda. |

Cada prueba con datos inválidos verifica también el mensaje de la excepción personalizada que ve el usuario en la consola.

---

## 12. Credenciales Iniciales de Prueba (CU01 / CU03)

| Rol | Correo Electrónico | Contraseña Inicial | Comportamiento en Consola |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@secu.edu.uy` | `Proyecto2026!` | Acceso completo a CU03 (Usuarios), CU07 (Postulaciones, con CU06 Documentos adjuntos, CU11 Orden final CES y CU12 Vista consolidada) y CU08 (Asignación de evaluadores). Ingreso directo sin cambio forzado (`req_cam_clave = FALSE`). |
| **Segundo Administrador** | `admin2@secu.edu.uy` | `Proyecto2026!` | El mismo acceso, sin convocatorias a su cargo. Sirve para probar en CU03 que no se cambia el rol de un Administrador con convocatorias. |
| **Usuario Nuevo (Alta CU03)** | *(Correo asignado)* | *(Clave temporal de 8 caracteres)* | Acceso inicial forzado a cambio de contraseña (`req_cam_clave = TRUE`). La clave temporal se consulta en `correos_simulados/bandeja_salida.txt`. |

---

## 13. Flujo de Trabajo en Git (Git Flow)

* **`main`:** Rama principal con versiones estables y aprobadas para entregas oficiales.
* **`develop`:** Rama principal de integración continua del equipo.
* **`feature/*`:** Ramas auxiliares para el desarrollo aislado de funcionalidades y refactorizaciones antes de ser integradas mediante Merge Request.

### Etapa 3: una rama por día, cada una sobre la anterior
* Día 1: CU08 + CU09. Día 2: CU06. Día 3: CU11. Día 4: CU12. Día 5 (`feature/dia5-pruebas`): pruebas unitarias y los cambios de la revisión del código (detalle del CU07 con los datos del CU05, cifrado ampliado, reglas controladas en los servicios, cancelación de una postulación Evaluada, métodos comunes de los menús en `MenuBase`).
* **Higiene del repositorio:** el archivo de módulo de IntelliJ (`*.iml`) no se sube: IntelliJ lo arma a partir del `pom.xml`.

### Etapa 2 (`feature/estructura-etapa2`)
* **Optimización neta:** −243 líneas de código redundante depuradas sin alterar la base de datos.
* **Ajuste funcional de CU07:** La modificación administrativa se acotó estrictamente a departamento y sede, preservando la inmutabilidad de la propuesta original del postulante.
* **Seguridad en CU01:** Cada usuario autenticado gestiona exclusivamente el cambio de su propia contraseña (eliminación de parámetros innecesarios en `SeguridadProxy`).
* **Calidad de software:** Se resolvieron los bucles de captura de datos en consola (Mantis `#0044248`) y se verificó la superación de las 38 pruebas de interacción por consola.
* **Higiene del repositorio:** Configuración de `.gitignore` para excluir la bandeja local de `correos_simulados/`.
