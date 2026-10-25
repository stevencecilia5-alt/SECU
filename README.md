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
   * Verificación de credenciales protegidas con algoritmo propio/hash y validación de estado activo (`Activo` vs. `Inactivo`).
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
   * Listado general y filtrado avanzado de postulaciones por convocatoria, postulante, documento, correo, tutor, grupo o rango de fechas (con validación cronológica).
   * Visualización del detalle completo multinivel: datos del postulante, carrera, departamento, sede, tutores asignados, documentos adjuntos cargados y evaluadores vinculados.
   * **Modificación administrativa focalizada:** Permite modificar exclusivamente el departamento académico y la sede regional sobre postulaciones en estados editables (`Registrada` o `En revisión`), respetando la propuesta inmutable del postulante.
   * Transición controlada de estados (`Registrada`, `En revisión`, `No elegible`, `En evaluación`, `Evaluada`, `Aprobada`, `Rechazada`, `Cancelada`), aplicando control de concurrencia optimista contra PostgreSQL.

---

### Módulos del Proyecto Global (Proyección Etapa 3 y Opcionales)
* **CU08 + CU09 — Asignación de Evaluadores y Rúbrica de Evaluación:** Asignación de duplas de evaluadores externos por postulación y calificación cuantitativa ponderada sobre las 6 secciones fijas de la rúbrica (suma del 100%).
* **CU06 — Gestión de Documentos Adjuntos (Opcional):** Validación y persistencia de archivos PDF (máx. 10 MB) para 11 tipos de documentos de respaldo.
* **CU11 — Registro del Orden Final CES (Opcional):** Registro de la prelación definitiva del Consejo de Enseñanza Superior con justificación ante discrepancias.
* **CU12 — Vista Consolidada y Reportes (Opcional):** Panel integral de consulta del ciclo evaluativo y exportación de resultados a planillas Excel.

---

## 4. Patrones de Diseño Implementados

Cada patrón resuelve una necesidad concreta de la aplicación:

| Patrón | Tipo / Clasificación | Clases Participantes | Ubicación / Paquete | Problema que Resuelve en SECU |
| :--- | :--- | :--- | :--- | :--- |
| **Singleton** | Creacional | `ConexionBD` | `utec.secu.conexion` | Garantiza una **única instancia activa de conexión JDBC** compartida a PostgreSQL para toda la sesión del programa, evitando la sobrecarga de abrir y cerrar conexiones físicas por cada consulta SQL. |
| **Factory Method** | Creacional | `PerfilUsuarioFactory`, `PerfilUsuarioDAO` (Interfaz), `AdministradorDAO`, `EvaluadorExternoDAO` | `utec.secu.factory` y `utec.secu.dao` | Encapsula y desacopla la instanciación de los DAOs de perfiles especializados (`administradores` o `eval_externos`) según el rol del usuario durante el alta o modificación, sin acoplar `UsuarioDAO` a clases concretas. |
| **Proxy** | Estructural | `ProxyBase` (Abstracta), `SeguridadProxy`, `PostulacionProxy` | `utec.secu.seguridad` | Actúa como barrera intermedia de seguridad frente a los DAOs (`UsuarioDAO` y `PostulacionDAO`). Verifica que exista sesión activa, que el usuario esté activo y que posea rol `Administrador` antes de delegar la operación. Lanza `AccesoDenegadoException` ante accesos no autorizados. |
| **Facade (Fachada)** | Estructural | `UsuarioService`, `PostulacionService` | `utec.secu.servicio` | Proporciona una **interfaz unificada y de alto nivel a los menús de consola (`ui`)**, coordinando validaciones de negocio, seguridad (Proxies), persistencia transaccional (DAOs) y servicios auxiliares (Correo). Además traduce los errores de la base (`SQLException`) a `ErrorDeDatosException`, así la consola no depende de JDBC. |
| **DAO (Data Access Object)** | Arquitectura de Persistencia | `UsuarioDAO`, `PostulacionDAO`, `AdministradorDAO`, `EvaluadorExternoDAO` | `utec.secu.dao` | Encapsula y centraliza todas las sentencias SQL parametrizadas (`PreparedStatement`), incluidas las condiciones de los filtros de búsqueda de CU07, aislando la base de datos relacional de la lógica de negocio y presentación. |

---

## 5. Seguridad y Cifrado de Contraseñas (Ámbito Académico)

En cumplimiento de las especificaciones de la cátedra de Programación (2026):

* **Clase responsable:** `utec.secu.seguridad.ServicioCifrado` (clase utilitaria final con constructor privado, desacoplada de la UI y los DAOs).
* **Algoritmo propio y reversible:** Aplica una transformación posicional dinámica sobre cada carácter del texto plano (`código + (índice + 1)`), permitiendo que el método `descifrar(textoCifrado)` recupere el valor original mediante la función inversa para verificación de conceptos.
* **Justificación académica obligatoria:** Este mecanismo se implementa **únicamente con fines académicos** para ejercitar y evaluar el desarrollo de algoritmos propios en Java sin librerías externas que resuelvan completamente el proceso. En un entorno de producción real, este esquema se reemplaza por funciones de derivación criptográficamente seguras como BCrypt o Argon2 con salt aleatorio.
* **Persistencia segura:** PostgreSQL almacena exclusivamente el valor cifrado en la columna `contrasenia` de la tabla `usuarios`. En ningún caso se persiste texto plano.
* **Confidencialidad de credenciales:** Las contraseñas temporales se escriben exclusivamente en el archivo local `correos_simulados/bandeja_salida.txt` a través de `ServicioCorreo`. La carpeta `correos_simulados/` se encuentra excluida en `.gitignore` para garantizar que las claves no se suban al repositorio remoto de GitLab.

---

## 6. Estructura de Paquetes del Proyecto (28 Clases)

El código fuente se organiza bajo la convención estándar de Maven en `src/main/java/utec/secu/`:

```
src/main/java/utec/secu/
├── conexion/
│   └── ConexionBD.java                         # Singleton: Conexión JDBC compartida a PostgreSQL
├── dao/
│   ├── AdministradorDAO.java                   # DAO específico para perfil Administrador
│   ├── EvaluadorExternoDAO.java                # DAO específico para perfil Evaluador Externo
│   ├── PerfilUsuarioDAO.java                   # Interfaz base para DAOs de perfiles especializados
│   ├── PostulacionDAO.java                     # DAO transaccional para postulaciones, filtros y catálogos
│   └── UsuarioDAO.java                         # DAO de usuarios, autenticación y credenciales
├── excepciones/
│   ├── AccesoDenegadoException.java            # Control de acceso y sesiones (lanzada por Proxies)
│   ├── DatoInvalidoException.java              # Validación de entradas y reglas de negocio
│   ├── ErrorDeDatosException.java              # Falla de la base, traducida por los servicios (sin SQLException en ui)
│   └── OperacionNoPermitidaException.java      # Operaciones que el estado actual de los datos no permite
├── factory/
│   └── PerfilUsuarioFactory.java               # Factory Method para instanciar DAOs de perfiles
├── modelo/
│   ├── EstadoPostulacion.java                  # Enum con estados y transiciones permitidas (CU07)
│   ├── FiltroPostulacion.java                  # Enum con los filtros de búsqueda de CU07 (sin SQL)
│   ├── Postulacion.java                        # Entidad del dominio de postulación
│   └── Usuario.java                            # Entidad del dominio de usuario
├── seguridad/
│   ├── PostulacionProxy.java                   # Proxy de autorización para PostulacionDAO
│   ├── ProxyBase.java                          # Clase abstracta con reglas comunes de sesión y rol
│   ├── SeguridadProxy.java                     # Proxy de autenticación y autorización para UsuarioDAO
│   └── ServicioCifrado.java                    # Cifrado algorítmico propio y reversible (académico)
├── servicio/
│   ├── PostulacionService.java                 # Facade de negocio para CU07 (fechas, estados, filtros)
│   ├── ServicioCorreo.java                     # Simulador de correo electrónico a archivo local
│   └── UsuarioService.java                     # Facade de negocio para CU01 y CU03 (validaciones, altas)
└── ui/
    ├── Main.java                               # Punto de entrada de la aplicación y ciclo general
    ├── MenuBase.java                           # Clase abstracta base para menús de consola y lecturas
    ├── MenuInicio.java                         # Submenú de inicio de sesión y recuperación (CU01)
    ├── MenuPostulaciones.java                  # Submenú interactivo para CU07 (postulaciones)
    ├── MenuPrincipal.java                      # Menú principal con opciones según rol autenticado
    └── MenuUsuarios.java                       # Submenú interactivo para CU03 (gestión de usuarios)
```

---

## 7. Requisitos y Configuración del Entorno

### Prerrequisitos
* **Java Development Kit (JDK):** Versión 17 o 21.
* **Apache Maven:** Versión 3.8 o superior.
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
1. `sql/01-init-secu-consolidado.sql` (Recrea el esquema `PROYECTO`, genera las 24 tablas en 3FN, restricciones CHECK, UNIQUE, dominios e inserta semillas de catálogos y usuario administrador inicial).
2. `sql/02-datos-prueba.sql` (Carga registros de prueba de postulaciones y convocatorias para verificar CU07).

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
3. **Ejecutar pruebas unitarias automatizadas:**
   ```bash
   mvn test
   ```

### Desde el IDE (IntelliJ IDEA)
1. Abrir la carpeta raíz del proyecto en IntelliJ IDEA.
2. Aguardar a que Maven sincronice las dependencias del `pom.xml`.
3. Navegar a `src/main/java/utec/secu/ui/Main.java`.
4. Hacer clic derecho sobre la clase y seleccionar **Run 'Main.main()'**.

---

## 9. Credenciales Iniciales de Prueba (CU01 / CU03)

| Rol | Correo Electrónico | Contraseña Inicial | Comportamiento en Consola |
| :--- | :--- | :--- | :--- |
| **Administrador** | `admin@secu.edu.uy` | `Proyecto2026!` | Acceso completo a CU03 (Usuarios) y CU07 (Postulaciones). Ingreso directo sin cambio forzado (`req_cam_clave = FALSE`). |
| **Usuario Nuevo (Alta CU03)** | *(Correo asignado)* | *(Clave temporal de 8 caracteres)* | Acceso inicial forzado a cambio de contraseña (`req_cam_clave = TRUE`). La clave temporal se consulta en `correos_simulados/bandeja_salida.txt`. |

---

## 10. Flujo de Trabajo en Git (Git Flow) y Rama `feature/estructura-etapa2`

* **`main`:** Rama principal con versiones estables y aprobadas para entregas oficiales.
* **`develop`:** Rama principal de integración continua del equipo.
* **`feature/*`:** Ramas auxiliares para el desarrollo aislado de funcionalidades y refactorizaciones antes de ser integradas mediante Merge Request.

### Registro de Refactorización Reciente (`feature/estructura-etapa2`):
* **Optimización neta:** −243 líneas de código redundante depuradas sin alterar la base de datos.
* **Ajuste funcional de CU07:** La modificación administrativa se acotó estrictamente a departamento y sede, preservando la inmutabilidad de la propuesta original del postulante.
* **Seguridad en CU01:** Cada usuario autenticado gestiona exclusivamente el cambio de su propia contraseña (eliminación de parámetros innecesarios en `SeguridadProxy`).
* **Calidad de software:** Se resolvieron los bucles de captura de datos en consola (Mantis `#0044248`) y se verificó la superación de las 38 pruebas de interacción por consola.
* **Higiene del repositorio:** Configuración de `.gitignore` para excluir la bandeja local de `correos_simulados/`.
