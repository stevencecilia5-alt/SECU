-- =============================================================================
-- PROYECTO DESARROLLO - SECU (Sistema de Evaluacion de Convocatorias UTEC)
-- Grupo 10
--
-- SCRIPT CONSOLIDADO DE INICIALIZACION DE LA BASE DE DATOS
--   1. Usuario y esquema de trabajo "PROYECTO"
--   2. Dominios
--   3. Tablas (24)
--   4. Claves foraneas
--   5. Restricciones CHECK
--   6. Indices
--   7. Carga inicial de catalogos y usuario administrador
--   8. Permisos sobre los objetos creados
--
-- Motor: PostgreSQL | Esquema: "PROYECTO"
-- Nomenclatura: PK_Tabla / FK_Tabla_TablaRef / UK_Tabla_Columna / CK_Tabla_Columna
-- =============================================================================


-- =============================================================================
-- 1. USUARIO Y ESQUEMA DE TRABAJO
-- El script recrea el esquema completo y esta pensado para ejecutarse como
-- script de inicializacion del contenedor de PostgreSQL.
-- =============================================================================

-- ---------- 1.1 Usuario de trabajo ----------

-- El siguiente bloque crea el usuario "PROYECTO" solo si todavia no existe,
-- por lo que se ejecuta siempre igual, sin editar nada, en cualquiera de
-- los dos entornos donde puede correr este script:
--
--   - PostgreSQL local (instalacion manual, por ejemplo conectado como
--     "postgres" desde pgAdmin): el usuario no existe todavia, asi que el
--     bloque lo crea antes de que la seccion 1.2 pueda asignarselo como
--     dueno del esquema.
--
--   - Docker: el contenedor ya crea el usuario "PROYECTO" en su primer
--     arranque, a partir de las variables de entorno POSTGRES_USER y
--     POSTGRES_PASSWORD definidas en el docker-compose.yml. Al encontrarlo
--     ya creado, el bloque no hace nada y continua directamente en 1.2.

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'PROYECTO') THEN
        CREATE ROLE "PROYECTO"
            WITH LOGIN
            PASSWORD 'Proyecto2026!'
            CREATEDB
            CREATEROLE;
    END IF;
END
$$;


-- ---------- 1.2 Esquema de trabajo ----------

-- Esta seccion se ejecuta siempre a continuacion de 1.1, y puede correrse
-- tantas veces como haga falta: recrea el esquema desde cero en cada
-- ejecucion.
-- ATENCION: DROP SCHEMA ... CASCADE elimina de forma irreversible todos los
-- objetos y datos del esquema. No ejecutar sobre una base con informacion
-- que deba conservarse.

DROP SCHEMA IF EXISTS "PROYECTO" CASCADE;

CREATE SCHEMA "PROYECTO" AUTHORIZATION "PROYECTO";

ALTER ROLE "PROYECTO" SET search_path TO "PROYECTO";

SET search_path TO "PROYECTO";


-- =============================================================================
-- 2. DOMINIOS
-- Atributos recurrentes con validaciones comunes: la regla se declara una
-- sola vez y se aplica a todas las columnas que la utilizan.
-- =============================================================================

-- Nombres y apellidos: 2 a 50 caracteres, solo letras, espacios,
-- apostrofes o guiones (CU03, CU05).
CREATE DOMAIN DOM_NOMBRE_PERSONA AS VARCHAR(50)
    CHECK (VALUE ~ '^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ ''-]{2,50}$');

-- Correo electronico (CU01, CU03, CU05).
CREATE DOMAIN DOM_CORREO AS VARCHAR(100)
    CHECK (VALUE ~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$');

-- Documento de identidad: cedula, ID o pasaporte (CU03, CU05).
CREATE DOMAIN DOM_DOCUMENTO AS VARCHAR(20)
    CHECK (VALUE ~ '^[A-Za-z0-9.-]{5,20}$');

-- Telefono fijo o celular: solo numeros, admite prefijo internacional.
CREATE DOMAIN DOM_TELEFONO AS VARCHAR(30)
    CHECK (VALUE ~ '^\+?[0-9]{6,20}$');

-- Nacionalidad y procedencias geograficas.
CREATE DOMAIN DOM_NACIONALIDAD AS VARCHAR(50)
    CHECK (char_length(trim(VALUE)) >= 2);

-- Nombre de entidades de catalogo.
CREATE DOMAIN DOM_NOM_ENTIDAD AS VARCHAR(100)
    CHECK (char_length(trim(VALUE)) >= 2);

-- Puntajes de la rubrica: escala de 1 a 5 (CU09, CU10).
CREATE DOMAIN DOM_PUNTAJE AS NUMERIC(5,2)
    CHECK (VALUE BETWEEN 1 AND 5);


-- =============================================================================
-- 3. TABLAS
-- El identificador interno de cada tabla usa BIGSERIAL o SERIAL segun su
-- naturaleza: BIGSERIAL en las tablas transaccionales, cuyo volumen de filas
-- crece continuamente con el uso del sistema (Usuarios, Postulaciones,
-- Evaluaciones, etc.), y SERIAL en las tablas de catalogo, cuyo numero de
-- registros es acotado y estable en el tiempo (Sedes, Carreras, Docu_Tipos,
-- etc.), optimizando el espacio de almacenamiento sin sacrificar capacidad
-- donde realmente se necesita.
-- Administradores, Eval_Externos y Formaciones no declaran un identificador
-- propio: al ser extensiones 1:1 de su tabla padre, reutilizan la PK de esta
-- como clave primaria compartida, que a su vez es clave foranea hacia ella.
-- Los identificadores naturales y las combinaciones que no pueden repetirse
-- se declaran como claves unicas (UK).
-- =============================================================================

-- ---------- Modulo 1: usuarios, accesos y perfiles ----------

CREATE TABLE Usuarios (
    id_usuario BIGSERIAL NOT NULL,
    ci DOM_DOCUMENTO NOT NULL,
    correo DOM_CORREO NOT NULL,
    contrasenia VARCHAR(255) NOT NULL,
    nombre DOM_NOMBRE_PERSONA NOT NULL,
    apellido DOM_NOMBRE_PERSONA NOT NULL,
    nacionalidad DOM_NACIONALIDAD NOT NULL,
    calle VARCHAR(40),
    num_domicilio VARCHAR(8),
    estado VARCHAR(20) NOT NULL DEFAULT 'Activo',
    rol VARCHAR(30) NOT NULL,
    req_cam_clave BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT PK_Usuario PRIMARY KEY (id_usuario),
    CONSTRAINT UK_Usua_ci UNIQUE (ci),
    CONSTRAINT UK_Usua_correo UNIQUE (correo)
);

CREATE TABLE Tele_Usuarios (
    id_tele_usuario BIGSERIAL NOT NULL,
    id_usuario BIGINT NOT NULL,
    num_telefono DOM_TELEFONO NOT NULL,
    CONSTRAINT PK_Tele_Usuario PRIMARY KEY (id_tele_usuario)
);

-- Especializacion de Usuarios (clave primaria compartida). id_usuario es a
-- la vez PK propia y FK hacia Usuarios: al ser tambien primary key, ya
-- garantiza por si sola la relacion 1:1 con el supertipo, sin necesitar una
-- clave unica adicional.
CREATE TABLE Administradores (
    id_usuario BIGINT NOT NULL,
    CONSTRAINT PK_Administrador PRIMARY KEY (id_usuario)
);

CREATE TABLE Eval_Externos (
    id_usuario BIGINT NOT NULL,
    CONSTRAINT PK_Eval_Externo PRIMARY KEY (id_usuario)
);

-- ---------- Modulo 2: catalogos institucionales y convocatorias ----------

CREATE TABLE Sedes (
    id_sede SERIAL NOT NULL,
    nom_sede VARCHAR(50) NOT NULL,
    CONSTRAINT PK_Sede PRIMARY KEY (id_sede),
    CONSTRAINT UK_Sede_nom_sede UNIQUE (nom_sede)
);

CREATE TABLE Dept_Academicos (
    id_dept_academico SERIAL NOT NULL,
    nom_departamento DOM_NOM_ENTIDAD NOT NULL,
    CONSTRAINT PK_Dept_Academico PRIMARY KEY (id_dept_academico),
    CONSTRAINT UK_Dept_nom_departamento UNIQUE (nom_departamento)
);

CREATE TABLE Convocatorias (
    id_convocatoria BIGSERIAL NOT NULL,
    id_usuario BIGINT NOT NULL,
    nom_convocatoria DOM_NOM_ENTIDAD NOT NULL,
    fec_apertura TIMESTAMP NOT NULL,
    fec_cierre TIMESTAMP NOT NULL,
    ani_convocatoria INT NOT NULL,
    descripcion TEXT,
    can_becas INT NOT NULL,
    tip_beca VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Borrador',
    CONSTRAINT PK_Convocatoria PRIMARY KEY (id_convocatoria),
    CONSTRAINT UK_Conv_Convocatoria UNIQUE (nom_convocatoria, fec_apertura)
);

CREATE TABLE Docu_Tipos (
    id_docu_tipo SERIAL NOT NULL,
    nom_tip_documento DOM_NOM_ENTIDAD NOT NULL,
    CONSTRAINT PK_Docu_Tipo PRIMARY KEY (id_docu_tipo),
    CONSTRAINT UK_Docu_Tipo_nom_tip_documento UNIQUE (nom_tip_documento)
);

CREATE TABLE Docu_Tipo_Convocatorias (
    id_docu_tipo_convocatoria BIGSERIAL NOT NULL,
    id_docu_tipo INT NOT NULL,
    id_convocatoria BIGINT NOT NULL,
    obligatorio BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT PK_Docu_Tipo_Convocatoria PRIMARY KEY (id_docu_tipo_convocatoria),
    CONSTRAINT UK_Docu_Tipo_Conv_Convocatoria UNIQUE (id_docu_tipo, id_convocatoria)
);

-- ---------- Modulo 3: postulante, formacion y organizacion ----------

CREATE TABLE Instituciones (
    id_institucion SERIAL NOT NULL,
    nom_institucion DOM_NOM_ENTIDAD NOT NULL,
    CONSTRAINT PK_Institucion PRIMARY KEY (id_institucion),
    CONSTRAINT UK_Inst_nom_institucion UNIQUE (nom_institucion)
);

-- El nombre del posgrado es unico DENTRO de cada institucion: dos
-- instituciones distintas pueden dictar un posgrado homonimo.
CREATE TABLE Posgrados (
    id_posgrado SERIAL NOT NULL,
    id_institucion INT NOT NULL,
    nom_posgrado DOM_NOM_ENTIDAD NOT NULL,
    CONSTRAINT PK_Posgrado PRIMARY KEY (id_posgrado),
    CONSTRAINT UK_Posg_Posgrado UNIQUE (id_institucion, nom_posgrado)
);

CREATE TABLE Carreras (
    id_carrera SERIAL NOT NULL,
    nom_carrera DOM_NOM_ENTIDAD NOT NULL,
    CONSTRAINT PK_Carrera PRIMARY KEY (id_carrera),
    CONSTRAINT UK_Carr_nom_carrera UNIQUE (nom_carrera)
);

CREATE TABLE Postulantes (
    id_postulante BIGSERIAL NOT NULL,
    documento DOM_DOCUMENTO NOT NULL,
    tip_documento VARCHAR(20) NOT NULL,
    nombre DOM_NOMBRE_PERSONA NOT NULL,
    apellido DOM_NOMBRE_PERSONA NOT NULL,
    correo DOM_CORREO NOT NULL,
    pai_procedencia DOM_NACIONALIDAD NOT NULL,
    dep_procedencia DOM_NACIONALIDAD NOT NULL,
    loc_procedencia DOM_NACIONALIDAD NOT NULL,
    nacionalidad DOM_NACIONALIDAD NOT NULL,
    calle VARCHAR(40),
    num_domicilio VARCHAR(8),
    apartamento VARCHAR(20),
    bis BOOLEAN,
    es_doc_utec BOOLEAN NOT NULL DEFAULT FALSE,
    act_laboral VARCHAR(100),
    CONSTRAINT PK_Postulante PRIMARY KEY (id_postulante),
    CONSTRAINT UK_Post_Postulante UNIQUE (documento, tip_documento)
);

CREATE TABLE Celu_Postulantes (
    id_celu_postulante BIGSERIAL NOT NULL,
    id_postulante BIGINT NOT NULL,
    num_celular DOM_TELEFONO NOT NULL,
    CONSTRAINT PK_Celu_Postulante PRIMARY KEY (id_celu_postulante)
);

-- Un postulante registra a lo sumo una formacion, con una carrera y/o un
-- posgrado. La clave primaria compartida con Postulantes impone esa
-- cardinalidad.
CREATE TABLE Formaciones (
    id_postulante BIGINT NOT NULL,
    id_carrera INT,
    id_posgrado INT,
    CONSTRAINT PK_Formacion PRIMARY KEY (id_postulante)
);

-- ---------- Modulo 4: postulacion, tutorias y adjuntos ----------

CREATE TABLE Postulaciones (
    id_postulacion BIGSERIAL NOT NULL,
    id_postulante BIGINT NOT NULL,
    id_convocatoria BIGINT NOT NULL,
    id_dept_academico INT NOT NULL,
    id_sede INT NOT NULL,
    nom_proyecto VARCHAR(150) NOT NULL,
    lin_investigacion VARCHAR(100) NOT NULL,
    can_meses INT NOT NULL,
    gru_investigacion VARCHAR(100) NOT NULL,
    otr_gru_investigacion VARCHAR(100),
    fec_solicitud TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'Registrada',
    pun_consolidado DOM_PUNTAJE,
    ord_pre_calculado INT,
    ord_final_ces INT,
    obs_ces TEXT,
    jui_global TEXT,
    CONSTRAINT PK_Postulacion PRIMARY KEY (id_postulacion),
    CONSTRAINT UK_Post_Postulacion UNIQUE (id_postulante, id_convocatoria),
    -- CU11: no puede repetirse una posicion final dentro de la convocatoria.
    CONSTRAINT UK_Post_ord_final_ces UNIQUE (id_convocatoria, ord_final_ces)
);

CREATE TABLE Docu_Adjuntos (
    id_docu_adjunto BIGSERIAL NOT NULL,
    id_docu_tipo INT NOT NULL,
    id_postulacion BIGINT NOT NULL,
    rut_archivo VARCHAR(255) NOT NULL,
    CONSTRAINT PK_Docu_Adjunto PRIMARY KEY (id_docu_adjunto)
);

CREATE TABLE Tuto_Utec (
    id_tuto_utec SERIAL NOT NULL,
    cor_utec DOM_CORREO NOT NULL,
    nombre DOM_NOMBRE_PERSONA NOT NULL,
    apellido DOM_NOMBRE_PERSONA NOT NULL,
    dep_area VARCHAR(100),
    CONSTRAINT PK_Tuto_Utec PRIMARY KEY (id_tuto_utec),
    CONSTRAINT UK_Tuto_cor_utec UNIQUE (cor_utec)
);

CREATE TABLE Tuto_Utec_Postulacion (
    id_tuto_utec_postulacion BIGSERIAL NOT NULL,
    id_tuto_utec INT NOT NULL,
    id_postulacion BIGINT NOT NULL,
    rol_tutor VARCHAR(20) NOT NULL,
    CONSTRAINT PK_Tuto_Utec_Postulacion PRIMARY KEY (id_tuto_utec_postulacion),
    -- Un mismo tutor no se asigna dos veces a la misma postulacion.
    CONSTRAINT UK_Tuto_Postulacion UNIQUE (id_tuto_utec, id_postulacion),
    -- CU05: a lo sumo un tutor principal y un co-tutor por postulacion.
    CONSTRAINT UK_Tuto_rol_tutor UNIQUE (id_postulacion, rol_tutor)
);

-- ---------- Modulo 5: evaluacion y rubrica ponderada ----------

-- Asignacion de un evaluador externo a una postulacion (CU08).
CREATE TABLE Eval_Exte_Postulaciones (
    id_eval_exte_postulacion BIGSERIAL NOT NULL,
    id_usuario BIGINT NOT NULL,
    id_postulacion BIGINT NOT NULL,
    CONSTRAINT PK_Eval_Exte_Postulacion PRIMARY KEY (id_eval_exte_postulacion),
    CONSTRAINT UK_Eval_Exte_Postulacion UNIQUE (id_usuario, id_postulacion)
);

CREATE TABLE Secc_Rubricas (
    id_secc_rubrica SERIAL NOT NULL,
    nom_seccion DOM_NOM_ENTIDAD NOT NULL,
    peso NUMERIC(5,2) NOT NULL,
    CONSTRAINT PK_Secc_Rubrica PRIMARY KEY (id_secc_rubrica),
    CONSTRAINT UK_Secc_nom_seccion UNIQUE (nom_seccion)
);

CREATE TABLE Criterios (
    id_criterio SERIAL NOT NULL,
    id_secc_rubrica INT NOT NULL,
    nom_criterio VARCHAR(150) NOT NULL,
    CONSTRAINT PK_Criterio PRIMARY KEY (id_criterio),
    CONSTRAINT UK_Crit_nom_criterio UNIQUE (nom_criterio)
);

-- La evaluacion cuelga de la ASIGNACION, no de la postulacion y el evaluador
-- por separado. Asi la base garantiza que solo pueda evaluar quien fue
-- efectivamente asignado (CU09), con una unica evaluacion por asignacion.
CREATE TABLE Evaluaciones (
    id_evaluacion BIGSERIAL NOT NULL,
    id_eval_exte_postulacion BIGINT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'Pendiente',
    pun_total DOM_PUNTAJE,
    com_evaluador TEXT,
    CONSTRAINT PK_Evaluacion PRIMARY KEY (id_evaluacion),
    CONSTRAINT UK_Eval_id_eval_exte_postulacion UNIQUE (id_eval_exte_postulacion)
);

CREATE TABLE Eval_Criterios (
    id_eval_criterio BIGSERIAL NOT NULL,
    id_evaluacion BIGINT NOT NULL,
    id_criterio INT NOT NULL,
    puntaje INT NOT NULL,
    CONSTRAINT PK_Eval_Criterio PRIMARY KEY (id_eval_criterio),
    CONSTRAINT UK_Eval_Crit_Criterio UNIQUE (id_evaluacion, id_criterio)
);


-- =============================================================================
-- 4. CLAVES FORANEAS
-- No se declara ON DELETE CASCADE: se mantiene el comportamiento por defecto
-- NO ACTION, que impide eliminar una fila mientras existan registros que la
-- referencien. Las politicas de baja se resuelven desde la aplicacion.
-- =============================================================================

ALTER TABLE Tele_Usuarios
    ADD CONSTRAINT FK_Tele_Usuario
    FOREIGN KEY (id_usuario) REFERENCES Usuarios (id_usuario);

ALTER TABLE Administradores
    ADD CONSTRAINT FK_Admi_Usuario
    FOREIGN KEY (id_usuario) REFERENCES Usuarios (id_usuario);

ALTER TABLE Eval_Externos
    ADD CONSTRAINT FK_Eval_Exte_Usuario
    FOREIGN KEY (id_usuario) REFERENCES Usuarios (id_usuario);

ALTER TABLE Convocatorias
    ADD CONSTRAINT FK_Conv_Administrador
    FOREIGN KEY (id_usuario) REFERENCES Administradores (id_usuario);

ALTER TABLE Docu_Tipo_Convocatorias
    ADD CONSTRAINT FK_Docu_Tipo_Conv_Docu_Tipo
    FOREIGN KEY (id_docu_tipo) REFERENCES Docu_Tipos (id_docu_tipo);

ALTER TABLE Docu_Tipo_Convocatorias
    ADD CONSTRAINT FK_Docu_Tipo_Conv_Convocatoria
    FOREIGN KEY (id_convocatoria) REFERENCES Convocatorias (id_convocatoria);

ALTER TABLE Posgrados
    ADD CONSTRAINT FK_Posg_Institucion
    FOREIGN KEY (id_institucion) REFERENCES Instituciones (id_institucion);

ALTER TABLE Celu_Postulantes
    ADD CONSTRAINT FK_Celu_Postulante
    FOREIGN KEY (id_postulante) REFERENCES Postulantes (id_postulante);

ALTER TABLE Formaciones
    ADD CONSTRAINT FK_Form_Postulante
    FOREIGN KEY (id_postulante) REFERENCES Postulantes (id_postulante);

ALTER TABLE Formaciones
    ADD CONSTRAINT FK_Form_Carrera
    FOREIGN KEY (id_carrera) REFERENCES Carreras (id_carrera);

ALTER TABLE Formaciones
    ADD CONSTRAINT FK_Form_Posgrado
    FOREIGN KEY (id_posgrado) REFERENCES Posgrados (id_posgrado);

ALTER TABLE Postulaciones
    ADD CONSTRAINT FK_Post_Postulante
    FOREIGN KEY (id_postulante) REFERENCES Postulantes (id_postulante);

ALTER TABLE Postulaciones
    ADD CONSTRAINT FK_Post_Convocatoria
    FOREIGN KEY (id_convocatoria) REFERENCES Convocatorias (id_convocatoria);

ALTER TABLE Postulaciones
    ADD CONSTRAINT FK_Post_Dept_Academico
    FOREIGN KEY (id_dept_academico) REFERENCES Dept_Academicos (id_dept_academico);

ALTER TABLE Postulaciones
    ADD CONSTRAINT FK_Post_Sede
    FOREIGN KEY (id_sede) REFERENCES Sedes (id_sede);

ALTER TABLE Docu_Adjuntos
    ADD CONSTRAINT FK_Docu_Adju_Docu_Tipo
    FOREIGN KEY (id_docu_tipo) REFERENCES Docu_Tipos (id_docu_tipo);

ALTER TABLE Docu_Adjuntos
    ADD CONSTRAINT FK_Docu_Adju_Postulacion
    FOREIGN KEY (id_postulacion) REFERENCES Postulaciones (id_postulacion);

ALTER TABLE Tuto_Utec_Postulacion
    ADD CONSTRAINT FK_Tuto_Tuto_Utec
    FOREIGN KEY (id_tuto_utec) REFERENCES Tuto_Utec (id_tuto_utec);

ALTER TABLE Tuto_Utec_Postulacion
    ADD CONSTRAINT FK_Tuto_Postulacion
    FOREIGN KEY (id_postulacion) REFERENCES Postulaciones (id_postulacion);

ALTER TABLE Eval_Exte_Postulaciones
    ADD CONSTRAINT FK_Eval_Exte_Post_Eval_Externo
    FOREIGN KEY (id_usuario) REFERENCES Eval_Externos (id_usuario);

ALTER TABLE Eval_Exte_Postulaciones
    ADD CONSTRAINT FK_Eval_Exte_Post_Postulacion
    FOREIGN KEY (id_postulacion) REFERENCES Postulaciones (id_postulacion);

ALTER TABLE Criterios
    ADD CONSTRAINT FK_Crit_Secc_Rubrica
    FOREIGN KEY (id_secc_rubrica) REFERENCES Secc_Rubricas (id_secc_rubrica);

ALTER TABLE Evaluaciones
    ADD CONSTRAINT FK_Eval_Eval_Exte_Postulacion
    FOREIGN KEY (id_eval_exte_postulacion)
    REFERENCES Eval_Exte_Postulaciones (id_eval_exte_postulacion);

ALTER TABLE Eval_Criterios
    ADD CONSTRAINT FK_Eval_Crit_Evaluacion
    FOREIGN KEY (id_evaluacion) REFERENCES Evaluaciones (id_evaluacion);

ALTER TABLE Eval_Criterios
    ADD CONSTRAINT FK_Eval_Crit_Criterio
    FOREIGN KEY (id_criterio) REFERENCES Criterios (id_criterio);


-- =============================================================================
-- 5. RESTRICCIONES CHECK - REGLAS DE NEGOCIO
-- Los valores admitidos reproducen la redaccion del documento de casos de uso.
-- =============================================================================

-- ---------- 5.1 Dominios de valores ----------

-- CU03: roles y estado del usuario.
ALTER TABLE Usuarios
    ADD CONSTRAINT CK_Usua_rol
    CHECK (rol IN ('Administrador', 'Evaluador externo'));

ALTER TABLE Usuarios
    ADD CONSTRAINT CK_Usua_estado
    CHECK (estado IN ('Activo', 'Inactivo'));

-- CU04: tipos de beca y estados de la convocatoria.
ALTER TABLE Convocatorias
    ADD CONSTRAINT CK_Conv_tip_beca
    CHECK (tip_beca IN ('Iniciacion a la investigacion', 'Posgrado', 'Posdoctorado'));

ALTER TABLE Convocatorias
    ADD CONSTRAINT CK_Conv_estado
    CHECK (estado IN ('Borrador', 'Publicada', 'Cerrada'));

-- CU05: tipos de documento del postulante.
ALTER TABLE Postulantes
    ADD CONSTRAINT CK_Post_tip_documento
    CHECK (tip_documento IN ('CI', 'Pasaporte', 'Otro'));

-- CU07: estados de la postulacion.
ALTER TABLE Postulaciones
    ADD CONSTRAINT CK_Post_estado
    CHECK (estado IN ('Registrada', 'En revision', 'No elegible', 'En evaluacion',
                      'Evaluada', 'Aprobada', 'Rechazada', 'Cancelada'));

-- CU05: un tutor principal y, opcionalmente, un co-tutor.
ALTER TABLE Tuto_Utec_Postulacion
    ADD CONSTRAINT CK_Tuto_rol_tutor
    CHECK (rol_tutor IN ('Tutor principal', 'Co-tutor'));

-- CU09: estados de la evaluacion.
ALTER TABLE Evaluaciones
    ADD CONSTRAINT CK_Eval_estado
    CHECK (estado IN ('Pendiente', 'En proceso', 'Completada'));

-- ---------- 5.2 Rangos numericos ----------

-- CU09: escala de 1 a 5 por criterio.
ALTER TABLE Eval_Criterios
    ADD CONSTRAINT CK_Eval_Crit_puntaje
    CHECK (puntaje BETWEEN 1 AND 5);

-- CU09: el peso de cada seccion de la rubrica es un porcentaje.
ALTER TABLE Secc_Rubricas
    ADD CONSTRAINT CK_Secc_peso
    CHECK (peso > 0 AND peso <= 100);

-- CU05: cantidad de meses solicitados.
ALTER TABLE Postulaciones
    ADD CONSTRAINT CK_Post_can_meses
    CHECK (can_meses > 0);

-- CU04: cantidad estimada de becas y anio de la convocatoria.
ALTER TABLE Convocatorias
    ADD CONSTRAINT CK_Conv_can_becas
    CHECK (can_becas > 0);

ALTER TABLE Convocatorias
    ADD CONSTRAINT CK_Conv_ani_convocatoria
    CHECK (ani_convocatoria BETWEEN 2020 AND 2100);

-- CU10 y CU11: los ordenes de prelacion son posiciones positivas.
ALTER TABLE Postulaciones
    ADD CONSTRAINT CK_Post_ordenes
    CHECK ((ord_pre_calculado IS NULL OR ord_pre_calculado > 0)
       AND (ord_final_ces IS NULL OR ord_final_ces > 0));

-- ---------- 5.3 Coherencia entre columnas ----------

-- CU04: el cierre debe ser posterior a la apertura.
ALTER TABLE Convocatorias
    ADD CONSTRAINT CK_Conv_fechas
    CHECK (fec_cierre > fec_apertura);

-- CU11: si el orden final CES difiere del calculado, la observacion
-- es obligatoria.
ALTER TABLE Postulaciones
    ADD CONSTRAINT CK_Post_obs_ces
    CHECK (ord_final_ces IS NULL
           OR ord_final_ces = ord_pre_calculado
           OR obs_ces IS NOT NULL);

-- CU06: la documentacion adjunta se almacena en formato PDF.
ALTER TABLE Docu_Adjuntos
    ADD CONSTRAINT CK_Docu_Adju_rut_archivo
    CHECK (lower(rut_archivo) LIKE '%.pdf');

-- Una formacion debe referenciar al menos una carrera o un posgrado.
ALTER TABLE Formaciones
    ADD CONSTRAINT CK_Form_formacion
    CHECK (id_carrera IS NOT NULL OR id_posgrado IS NOT NULL);


-- =============================================================================
-- 6. INDICES
-- PostgreSQL crea indices automaticamente para PRIMARY KEY y UNIQUE, pero no
-- para las columnas de clave foranea. Se indexan las FK que no quedan
-- cubiertas por la primera columna de un indice existente.
-- =============================================================================

-- ---------- 6.1 Claves foraneas ----------

CREATE INDEX IX_Tele_Usuarios_id_usuario
    ON Tele_Usuarios (id_usuario);

CREATE INDEX IX_Convocatorias_id_usuario
    ON Convocatorias (id_usuario);

CREATE INDEX IX_Docu_Tipo_Convocatorias_id_convocatoria
    ON Docu_Tipo_Convocatorias (id_convocatoria);

CREATE INDEX IX_Posgrados_id_institucion
    ON Posgrados (id_institucion);

CREATE INDEX IX_Celu_Postulantes_id_postulante
    ON Celu_Postulantes (id_postulante);

CREATE INDEX IX_Formaciones_id_carrera
    ON Formaciones (id_carrera);

CREATE INDEX IX_Formaciones_id_posgrado
    ON Formaciones (id_posgrado);

CREATE INDEX IX_Postulaciones_id_convocatoria
    ON Postulaciones (id_convocatoria);

CREATE INDEX IX_Postulaciones_id_dept_academico
    ON Postulaciones (id_dept_academico);

CREATE INDEX IX_Postulaciones_id_sede
    ON Postulaciones (id_sede);

CREATE INDEX IX_Docu_Adjuntos_id_docu_tipo
    ON Docu_Adjuntos (id_docu_tipo);

CREATE INDEX IX_Docu_Adjuntos_id_postulacion
    ON Docu_Adjuntos (id_postulacion);

CREATE INDEX IX_Tuto_Utec_Postulacion_id_postulacion
    ON Tuto_Utec_Postulacion (id_postulacion);

CREATE INDEX IX_Eval_Exte_Postulaciones_id_postulacion
    ON Eval_Exte_Postulaciones (id_postulacion);

CREATE INDEX IX_Criterios_id_secc_rubrica
    ON Criterios (id_secc_rubrica);

CREATE INDEX IX_Eval_Criterios_id_criterio
    ON Eval_Criterios (id_criterio);

-- ---------- 6.2 Filtros de consulta (CU07 y CU12) ----------

CREATE INDEX IX_Usuarios_estado
    ON Usuarios (estado);

CREATE INDEX IX_Convocatorias_estado
    ON Convocatorias (estado);

CREATE INDEX IX_Postulaciones_estado
    ON Postulaciones (estado);

CREATE INDEX IX_Postulaciones_fec_solicitud
    ON Postulaciones (fec_solicitud);

CREATE INDEX IX_Evaluaciones_estado
    ON Evaluaciones (estado);

-- El correo del postulante se usa como filtro y no es unico.
CREATE INDEX IX_Postulantes_correo
    ON Postulantes (correo);

-- CU01: el correo es el identificador de acceso. El indice funcional permite
-- que la busqueda de credenciales no distinga mayusculas de minusculas.
CREATE INDEX IX_Usuarios_correo_lower
    ON Usuarios (lower(correo));

-- NOTA: Usuarios.ci, Usuarios.correo, Postulantes.documento y Tuto_Utec.cor_utec
-- YA cuentan con indice, creado por sus respectivas restricciones UNIQUE.
-- Declarar un CREATE INDEX adicional sobre ellas generaria un indice duplicado.

-- ---------- 6.3 Indices unicos funcionales (catalogos abiertos) ----------

-- Carreras, Instituciones y Posgrados se alimentan con el texto que escribe
-- el propio postulante. Las restricciones UNIQUE declaradas en cada tabla
-- comparan el valor exacto, por lo que no impiden variantes como
-- 'Ingenieria en Sistemas', 'ingenieria en sistemas' o el mismo nombre con
-- un espacio sobrante. Los siguientes indices unicos normalizan el valor
-- (minusculas y sin espacios en los extremos) ANTES de compararlo, de modo
-- que esas variantes se reconozcan como duplicados.
--
-- Conviven con las restricciones UNIQUE: estas documentan la regla en el
-- catalogo de restricciones y los indices la hacen efectiva en la practica.

CREATE UNIQUE INDEX UK_Carr_nom_carrera_norm
    ON Carreras (lower(trim(nom_carrera)));

CREATE UNIQUE INDEX UK_Inst_nom_institucion_norm
    ON Instituciones (lower(trim(nom_institucion)));

CREATE UNIQUE INDEX UK_Posg_Posgrado_norm
    ON Posgrados (id_institucion, lower(trim(nom_posgrado)));

-- ---------- 6.4 Indice unico parcial ----------

-- CU04: en el MVP puede existir una unica convocatoria publicada a la vez.
-- La regla involucra varias filas, por lo que no puede expresarse con CHECK.
CREATE UNIQUE INDEX UK_Conv_unica_publicada
    ON Convocatorias (estado)
    WHERE estado = 'Publicada';


-- =============================================================================
-- 7. CARGA INICIAL
-- Los catalogos con dependencias se cargan referenciando por nombre, para no
-- depender de los identificadores que genere BIGSERIAL.
-- =============================================================================

-- ---------- 7.1 Usuario administrador inicial (CU01, CU03) ----------
-- Contrasena: Proyecto2026!
-- Almacenada cifrada con el algoritmo propio y reversible de
-- ServicioCifrado, que desplaza cada caracter segun el caracter de la
-- clave (una frase) que le corresponde por su posicion.
-- El valor debe coincidir con ServicioCifrado.hashPassword("Proyecto2026!");
-- si se modifica el algoritmo o la clave, hay que regenerarlo. La comilla
-- simple del valor cifrado va duplicada ('') porque asi se escribe en SQL.

INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido,
                      nacionalidad, estado, rol, req_cam_clave)
VALUES ('40123450',
        'admin@secu.edu.uy',
        '2"w''u$YuC=BPA',
        'Administrador',
        'SECU',
        'Uruguaya',
        'Activo',
        'Administrador',
        FALSE);

INSERT INTO Administradores (id_usuario)
SELECT id_usuario FROM Usuarios WHERE correo = 'admin@secu.edu.uy';

-- ---------- 7.2 Sedes (CU05) ----------

INSERT INTO Sedes (nom_sede) VALUES
    ('Centro-Sur'),
    ('Este'),
    ('Norte'),
    ('Suroeste');

-- ---------- 7.3 Departamentos academicos ----------
-- *** LISTA PROVISORIA - VERIFICAR CON EL LISTADO INSTITUCIONAL ***
-- CU05 exige registrar el departamento academico pero no enumera los valores.

INSERT INTO Dept_Academicos (nom_departamento) VALUES
    ('Departamento de Innovación y Emprendimiento'),
    ('Departamento de Tecnologías de la Información'),
    ('Departamento de Energías Renovables'),
    ('Departamento de Procesos Industriales'),
    ('Departamento de Ciencias Agrarias y Agroambientales'),
    ('Departamento de Ciencia y Tecnología de Alimentos'),
    ('Departamento de Logística y Transporte'),
    ('Departamento de Ciencias Básicas');

-- ---------- 7.4 Tipos de documento (CU06) ----------

INSERT INTO Docu_Tipos (nom_tip_documento) VALUES
    ('Plan de trabajo'),
    ('Presupuesto'),
    ('Constancia de inscripción al posgrado'),
    ('Escolaridad de grado'),
    ('CVuy del postulante'),
    ('CVuy del tutor'),
    ('Carta aval del tutor'),
    ('Carta de motivación'),
    ('Escolaridad de posgrado'),
    ('CVuy del co-tutor'),
    ('Otros documentos adicionales');

-- ---------- 7.5 Secciones de la rubrica (CU09) ----------
-- Las seis secciones estan definidas en CU09. Los pesos NO figuran en el
-- documento: los valores propuestos cumplen la regla de que sumen 100%.

INSERT INTO Secc_Rubricas (nom_seccion, peso) VALUES
    ('Antecedentes del postulante',              20.00),
    ('Antecedentes del tutor y grupo receptor',  15.00),
    ('Descripción del plan de trabajo',          30.00),
    ('Cronograma de actividades',                10.00),
    ('Aporte al desarrollo del postulante',      15.00),
    ('Aporte al grupo receptor',                 10.00);

-- ---------- 7.6 Criterios de evaluacion (CU09) ----------
-- *** PROPUESTA DEL EQUIPO - CU09 no enumera los criterios ***

INSERT INTO Criterios (id_secc_rubrica, nom_criterio)
SELECT s.id_secc_rubrica, c.nom_criterio
FROM (VALUES
    ('Antecedentes del postulante',             'Formación académica previa y escolaridad'),
    ('Antecedentes del postulante',             'Producción académica y publicaciones en CVuy'),
    ('Antecedentes del postulante',             'Experiencia previa en proyectos de investigación'),

    ('Antecedentes del tutor y grupo receptor', 'Trayectoria académica y científica reciente del tutor'),
    ('Antecedentes del tutor y grupo receptor', 'Capacidad y fortaleza del grupo receptor'),
    ('Antecedentes del tutor y grupo receptor', 'Pertinencia del área de acogida e investigación'),

    ('Descripción del plan de trabajo',         'Claridad y precisión de los objetivos del proyecto'),
    ('Descripción del plan de trabajo',         'Solidez, rigor metodológico y viabilidad técnica'),
    ('Descripción del plan de trabajo',         'Originalidad e innovación de la propuesta'),
    ('Descripción del plan de trabajo',         'Pertinencia e impacto de los resultados esperados'),

    ('Cronograma de actividades',               'Coherencia y distribución temporal de metas e hitos'),
    ('Cronograma de actividades',               'Adecuación de los plazos solicitados con las actividades'),

    ('Aporte al desarrollo del postulante',     'Impacto directo en la formación académica del becario'),
    ('Aporte al desarrollo del postulante',     'Proyección profesional y científica posterior'),

    ('Aporte al grupo receptor',                'Contribución a la línea estratégica del grupo receptor'),
    ('Aporte al grupo receptor',                'Transferencia efectiva de conocimiento e innovación')
) AS c(nom_seccion, nom_criterio)
JOIN Secc_Rubricas s ON s.nom_seccion = c.nom_seccion;

-- ---------- 7.7 Catalogos sin datos iniciales ----------
-- Carreras, Instituciones y Posgrados corresponden a entidades externas que
-- se incorporan a medida que los postulantes las declaran.


-- =============================================================================
-- 8. PERMISOS SOBRE LOS OBJETOS CREADOS
--
-- Con Docker estas sentencias no cambian nada: el contenedor arranca con
-- POSTGRES_USER = PROYECTO, por lo que ese usuario ejecuta el script y es
-- el dueno del esquema y de todos los objetos. Un dueno ya tiene todos los
-- permisos sobre lo que le pertenece.
--
-- Se mantienen como resguardo para la ejecucion local, cuando el script lo
-- corre otro superusuario (por ejemplo "postgres" desde pgAdmin). En ese
-- caso las tablas quedan a nombre de ese usuario, y sin estos permisos
-- "PROYECTO" no podria leerlas ni modificarlas.
--
-- El permiso sobre las secuencias es imprescindible en ese escenario: sin
-- el, los INSERT fallan, porque cada columna BIGSERIAL o SERIAL necesita
-- leer su secuencia para generar el siguiente identificador.
-- =============================================================================

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA "PROYECTO" TO "PROYECTO";

GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA "PROYECTO" TO "PROYECTO";

-- =============================================================================
-- FIN DEL SCRIPT
-- =============================================================================
