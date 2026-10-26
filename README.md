# SECU — Sistema de Evaluación de Convocatorias UTEC
**Licenciatura en Tecnologías de la Información — UTEC (Generación 2026)**  
**Grupo 10 — ANGLO Deploy**  
**Versión:** 2.1 (Etapa 2 — Primer Entregable Funcional de Programación y Base de Datos)

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
   * Inicio de sesión interactivo por consola mediante correo institucional y contraseña.
   * Verificación de credenciales cifradas con el algoritmo propio de `ServicioCifrado` y validación de estado activo (`Activo` vs. `Inactivo`).
   * Detección del flag `req_cam_clave = TRUE` y redirección forzosa al flujo de cambio obligatorio de contraseña en el primer ingreso.
   * Flujo de cambio voluntario de clave desde el menú principal (cada usuario gestiona exclusivamente su propia contraseña).
   * Despliegue dinámico de opciones del menú principal según el rol autenticado (`Administrador` vs. `Evaluador externo`).

2. **CU03 — Gestión de Usuarios y Roles:**
   * Alta de nuevos usuarios (`Administrador` o `Evaluador externo`) con generación automática de contraseñas temporales aleatorias de 8 caracteres.
   * Envío desacoplado de credenciales temporales a la bandeja simulada local (`correos_simulados/bandeja_salida.txt`), sin exponer jamás contraseñas en consola ni en logs.
   * Validaciones de negocio en capa de servicio: formato y longitud de nombres/apellidos (2 a 50 caracteres), algoritmo de validación de dígito verificador de cédula de identidad uruguaya, formato de correo y control estricto de unicidad de CI y correo institucional en base de datos.
   * **Reintento del dato inválido:** ante un dato inválido, el sistema muestra qué regla no se cumple y vuelve a pedir solo ese dato, sin perder los ya ingresados.
   * Consulta tabular de usuarios en consola con columnas alineadas.
   * Inactivación lógica (baja) y reactivación/restablecimiento de credenciales temporales.

3. **CU07 — Consulta y Gestión de Postulaciones:**
   * Búsqueda con un filtro por consulta: convocatoria, postulante, correo, documento, tutor, grupo de investigación, estado o rango de fechas (con validación cronológica), o listado de todas las postulaciones.
   * Visualización del detalle completo multinivel: datos del postulante, carrera, departamento, sede, tutores asignados, documentos adjuntos cargados y evaluadores vinculados.
   * **Modificación administrativa focalizada:** Permite modificar exclusivamente el departamento académico y la sede regional sobre postulaciones en estados editables (`Registrada` o `En revisión`), respetando la propuesta inmutable del postulante.
   * Transición controlada de estados (`Registrada`, `En revisión`, `No elegible`, `En evaluación`, `Evaluada`, `Aprobada`, `Rechazada`, `Cancelada`), aplicando control de concurrencia optimista contra PostgreSQL.

### Módulos Implementados en la Etapa 3

4. **CU08 + CU09 — Gestión del Proceso de Evaluación:**
   * **Asignación de evaluadores (CU08, Administrador):** asigna exactamente dos Evaluadores externos activos a cada postulación `En revisión`. La postulación pasa sola a `En evaluación` cuando tiene sus dos evaluadores y al menos uno empezó su evaluación. Permite consultar los evaluadores de una postulación y las postulaciones de un evaluador, reemplazar a un evaluador que todavía no registró su evaluación y registrar el juicio global de una postulación ya evaluada (`Evaluada`, `Aprobada` o `Rechazada`).
   * **Registro de evaluaciones (CU09, Evaluador externo):** cada evaluador ve solo las postulaciones que tiene asignadas y las califica con la rúbrica fija (6 secciones que suman 100% y 16 criterios con puntaje entero de 1 a 5). Puede guardar un borrador (`En proceso`) o finalizar (`Completada`), con el puntaje total ponderado por sección. Si la postulación ya tiene sus dos evaluadores, la primera evaluación guardada la pasa de `En revisión` a `En evaluación`. Cuando las dos evaluaciones están completas, la postulación pasa sola a `Evaluada`. Una evaluación completada ya no se modifica.

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

---

### Módulos Opcionales del Proyecto Global
* **CU12 — Vista Consolidada y Reportes (Opcional):** Panel integral de consulta del ciclo evaluativo y exportación de resultados a planillas Excel.

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

---

## 5. Seguridad y Cifrado de Contraseñas (Ámbito Académico)

En cumplimiento de las especificaciones de la cátedra de Programación (2026):

* **Clase responsable:** `utec.secu.seguridad.ServicioCifrado` (clase utilitaria final con constructor privado, desacoplada de la UI y los DAOs).
* **Algoritmo propio y reversible, con clave:** cada carácter de la contraseña se desplaza según la letra de la clave que le toca (una frase, constante `CLAVE`). Con la misma clave, `descifrar(texto, clave)` recupera la contraseña original; con otra clave, no.
* **Justificación académica obligatoria:** Este mecanismo se implementa **únicamente con fines académicos** para ejercitar y evaluar el desarrollo de algoritmos propios en Java sin librerías externas que resuelvan completamente el proceso. En un entorno de producción real, este esquema se reemplaza por funciones de derivación criptográficamente seguras como BCrypt o Argon2 con salt aleatorio.
* **Persistencia segura:** PostgreSQL almacena exclusivamente el valor cifrado en la columna `contrasenia` de la tabla `usuarios`. En ningún caso se persiste texto plano.
* **Contraseñas con asteriscos:** al escribir una contraseña se muestran asteriscos (librería JLine, dependencia `org.jline:jline` del `pom.xml`). Funciona en una terminal real, como CMD o PowerShell al ejecutar el JAR (sección 8); en la consola Run de IntelliJ se ve lo que se escribe.
* **Confidencialidad de credenciales:** Las contraseñas temporales se escriben exclusivamente en el archivo local `correos_simulados/bandeja_salida.txt` a través de `ServicioCorreo`. La carpeta `correos_simulados/` se encuentra excluida en `.gitignore` para garantizar que las claves no se suban al repositorio remoto de GitLab.

---

## 6. Estructura de Paquetes del Proyecto (46 Clases)

El código fuente se organiza bajo la convención estándar de Maven en `src/main/java/utec/secu/`:

```
src/main/java/utec/secu/
├── conexion/
│   └── ConexionBD.java                         # Singleton: Conexión JDBC compartida a PostgreSQL
├── dao/
│   ├── AdministradorDAO.java                   # DAO específico para perfil Administrador
│   ├── DocumentoDAO.java                       # DAO de documentos adjuntos y tipos por convocatoria (CU06)
│   ├── EvaluacionDAO.java                      # DAO transaccional de asignaciones y evaluaciones (CU08, CU09)
│   ├── EvaluadorExternoDAO.java                # DAO específico para perfil Evaluador Externo
│   ├── PerfilUsuarioDAO.java                   # Interfaz base para DAOs de perfiles especializados
│   ├── PostulacionDAO.java                     # DAO transaccional para postulaciones, filtros y catálogos
│   ├── ResultadoDAO.java                       # DAO de puntaje consolidado, orden de prelación y orden CES (CU11)
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
│   ├── DocumentoAdjunto.java                   # Documento cargado en una postulación, con la ruta del PDF (CU06)
│   ├── EstadoPostulacion.java                  # Enum con estados y transiciones permitidas (CU07)
│   ├── Evaluacion.java                         # Asignación de un evaluador y su evaluación (CU08, CU09)
│   ├── FiltroPostulacion.java                  # Enum con los filtros de búsqueda de CU07 (sin SQL)
│   ├── Postulacion.java                        # Entidad del dominio de postulación
│   ├── ResultadoPostulacion.java               # Postulación con sus evaluaciones, consolidado y órdenes (CU11)
│   ├── TipoDocumento.java                      # Tipo de documento que pide una convocatoria, obligatorio u opcional (CU06)
│   └── Usuario.java                            # Entidad del dominio de usuario
├── seguridad/
│   ├── DocumentoProxy.java                     # Proxy de autorización para CU06 (Administrador)
│   ├── EvaluacionProxy.java                    # Proxy de autorización para CU08 (Administrador) y CU09 (evaluador asignado)
│   ├── PostulacionProxy.java                   # Proxy de autorización para PostulacionDAO
│   ├── ProxyBase.java                          # Clase abstracta con reglas comunes de sesión y rol
│   ├── ResultadoProxy.java                     # Proxy de autorización para CU11 (Administrador)
│   ├── SeguridadProxy.java                     # Proxy de autenticación y autorización para UsuarioDAO
│   └── ServicioCifrado.java                    # Cifrado algorítmico propio y reversible (académico)
├── servicio/
│   ├── DocumentoService.java                   # Facade de negocio para CU06 (validación del PDF, copia y reglas)
│   ├── EvaluacionService.java                  # Facade de negocio para CU08 y CU09 (asignaciones, puntajes, cálculo)
│   ├── PostulacionService.java                 # Facade de negocio para CU07 (fechas, estados, filtros)
│   ├── ResultadoService.java                   # Facade de negocio para CU11 (armado y registro del orden CES)
│   ├── ServicioCorreo.java                     # Simulador de correo electrónico a archivo local
│   └── UsuarioService.java                     # Facade de negocio para CU01 y CU03 (validaciones, altas)
└── ui/
    ├── Main.java                               # Punto de entrada de la aplicación y ciclo general
    ├── MenuAsignacion.java                     # Submenú interactivo para CU08 (asignación de evaluadores)
    ├── MenuBase.java                           # Clase abstracta base para menús de consola y lecturas
    ├── MenuDocumentos.java                     # Submenú interactivo para CU06 (documentos adjuntos)
    ├── MenuInicio.java                         # Submenú de inicio de sesión y recuperación (CU01)
    ├── MenuMisEvaluaciones.java                # Submenú interactivo para CU09 (evaluaciones del evaluador)
    ├── MenuPostulaciones.java                  # Submenú interactivo para CU07 (postulaciones)
    ├── MenuPrincipal.java                      # Menú principal con opciones según rol autenticado
    ├── MenuResultados.java                     # Submenú interactivo para CU11 (orden final CES)
    └── MenuUsuarios.java                       # Submenú interactivo para CU03 (gestión de usuarios)
```

---

## 7. Requisitos y Configuración del Entorno

### Prerrequisitos
* **Java Development Kit (JDK):** Versión 17 o 21.
* **Apache Maven:** Versión 3.8 o superior. Descarga las dependencias del `pom.xml`: el driver de PostgreSQL y JLine (contraseñas con asteriscos).
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
2. `sql/02-datos-prueba.sql` (Carga registros de prueba de postulaciones y convocatorias para probar todos los módulos: usuarios, convocatorias, postulaciones en distintos estados (Aprobada, Rechazada y Cancelada se obtienen con el cambio de estado del CU07), evaluadores y evaluaciones; sus documentos adjuntos apuntan a los PDF de ejemplo de la carpeta `documentos/`).

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

### Desde el IDE (IntelliJ IDEA)
1. Abrir la carpeta raíz del proyecto en IntelliJ IDEA.
2. Aguardar a que Maven sincronice las dependencias del `pom.xml`.
3. Navegar a `src/main/java/utec/secu/ui/Main.java`.
4. Hacer clic derecho sobre la clase y seleccionar **Run 'Main.main()'**.

### Generar y ejecutar el JAR (fuera de IntelliJ)
1. **File → Project Structure → Artifacts → + → JAR → From modules with dependencies.** En *Main class* elegir `utec.secu.ui.Main` y en *JAR files from libraries* elegir **Extract to the target JAR**, para que el JAR lleve adentro todas las dependencias del `pom.xml`. Confirmar con **OK**.
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
* **Valores límite de tamaño** (10 MB = 10.485.760 bytes): no se suben a Git por su peso. Se generan desde PowerShell, en la raíz del proyecto:
  ```powershell
  # Exactamente 10 MB: se acepta
  $f=[IO.File]::Create("$PWD\limite.pdf"); $b=[Text.Encoding]::ASCII.GetBytes("%PDF-1.4`n"); $f.Write($b,0,$b.Length); $f.SetLength(10485760); $f.Close()
  # 1 byte más de 10 MB: se rechaza
  $f=[IO.File]::Create("$PWD\grande.pdf"); $b=[Text.Encoding]::ASCII.GetBytes("%PDF-1.4`n"); $f.Write($b,0,$b.Length); $f.SetLength(10485761); $f.Close()
  ```

---

## 10. Orden Final CES (CU11): reglas

* **Puntaje consolidado y orden calculado (CU10):** el CU10 no es un módulo del equipo, pero el CU11 necesita su resultado. Por eso, cuando una postulación pasa a `Evaluada` (las dos evaluaciones `Completada`), SECU calcula y guarda en la misma transacción su **puntaje consolidado** = (Evaluación 1 + Evaluación 2) / 2, y vuelve a numerar el **orden calculado** de su convocatoria: de mayor a menor puntaje consolidado y, si empatan, alfabético por nombre del postulante.
* **Postulaciones evaluadas después del orden CES:** quedan sin orden calculado y el CU11 avisa que hay que registrar de nuevo el orden CES; al hacerlo, SECU recalcula el orden de toda la lista.
* **Observaciones del orden CES:** la postulación que movió el CES guarda el motivo escrito; las que quedan corridas reciben una observación automática, y la que termina en su orden calculado no lleva, salvo la que el CES le agregue con "Agregar una observación" (esa se suma a cualquiera de las otras).
* **Registrar de nuevo el orden CES:** se parte del orden calculado nuevo; el puesto CES anterior se muestra como referencia.
* **Resultado de cada postulación:** el resultado definitivo (`Aprobada` o `Rechazada`) se registra con la opción 4 del menú de postulaciones (cambio de estado de CU07), que solo lo permite desde `Evaluada`.

---

## 11. Credenciales Iniciales de Prueba (CU01 / CU03)

| Rol | Correo Electrónico | Contraseña Inicial | Comportamiento en Consola |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@secu.edu.uy` | `Proyecto2026!` | Acceso completo a CU03 (Usuarios), CU07 (Postulaciones, con CU06 Documentos adjuntos y CU11 Orden final CES) y CU08 (Asignación de evaluadores). Ingreso directo sin cambio forzado (`req_cam_clave = FALSE`). |
| **Usuario Nuevo (Alta CU03)** | *(Correo asignado)* | *(Clave temporal de 8 caracteres)* | Acceso inicial forzado a cambio de contraseña (`req_cam_clave = TRUE`). La clave temporal se consulta en `correos_simulados/bandeja_salida.txt`. |

---

## 12. Flujo de Trabajo en Git (Git Flow) y Rama `feature/estructura-etapa2`

* **`main`:** Rama principal con versiones estables y aprobadas para entregas oficiales.
* **`develop`:** Rama principal de integración continua del equipo.
* **`feature/*`:** Ramas auxiliares para el desarrollo aislado de funcionalidades y refactorizaciones antes de ser integradas mediante Merge Request.

### Registro de Refactorización Reciente (`feature/estructura-etapa2`):
* **Optimización neta:** −243 líneas de código redundante depuradas sin alterar la base de datos.
* **Ajuste funcional de CU07:** La modificación administrativa se acotó estrictamente a departamento y sede, preservando la inmutabilidad de la propuesta original del postulante.
* **Seguridad en CU01:** Cada usuario autenticado gestiona exclusivamente el cambio de su propia contraseña (eliminación de parámetros innecesarios en `SeguridadProxy`).
* **Calidad de software:** Se resolvieron los bucles de captura de datos en consola (Mantis `#0044248`) y se verificó la superación de las 38 pruebas de interacción por consola.
* **Higiene del repositorio:** Configuración de `.gitignore` para excluir la bandeja local de `correos_simulados/`.
