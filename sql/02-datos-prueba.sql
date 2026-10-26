-- =============================================================================
-- SECU - Datos de prueba (Grupo 10)
-- Se ejecuta DESPUES de 01-init-secu.sql. Solo para pruebas: no usar en produccion.
-- Todos los usuarios de prueba tienen la contrasena Proyecto2026! (cifrada igual que
-- la del administrador) y no deben cambiarla en el primer ingreso.
-- Para volver al estado inicial: docker compose down -v && docker compose up -d
-- =============================================================================

SET search_path TO "PROYECTO";

BEGIN;

-- ---------- 1. Usuarios evaluadores ----------
-- Evaluador con evaluaciones en los tres estados
INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, estado, rol, req_cam_clave)
VALUES ('37125485', 'eval1@utec.edu.uy', '2"w''u$YuC=BPA', 'Martín', 'Rodríguez', 'Uruguaya', 'Activo', 'Evaluador externo', FALSE);
-- Evaluador; tiene la evaluación En proceso
INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, estado, rol, req_cam_clave)
VALUES ('39451204', 'eval2@utec.edu.uy', '2"w''u$YuC=BPA', 'Lucía', 'Fernández', 'Uruguaya', 'Activo', 'Evaluador externo', FALSE);
-- Evaluador; tercer evaluador disponible
INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, estado, rol, req_cam_clave)
VALUES ('40873164', 'eval3@utec.edu.uy', '2"w''u$YuC=BPA', 'Diego', 'Silva', 'Uruguaya', 'Activo', 'Evaluador externo', FALSE);
-- Evaluador Inactivo
INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, estado, rol, req_cam_clave)
VALUES ('35988423', 'eval4@utec.edu.uy', '2"w''u$YuC=BPA', 'Sofía', 'Pereira', 'Uruguaya', 'Inactivo', 'Evaluador externo', FALSE);
-- Cédula y correo ya registrados (CU03)
INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, estado, rol, req_cam_clave)
VALUES ('38765418', 'ana.gomez@utec.edu.uy', '2"w''u$YuC=BPA', 'Ana', 'Gómez', 'Uruguaya', 'Activo', 'Evaluador externo', FALSE);
-- Usuario Inactivo para restablecer y reactivar (CU03)
INSERT INTO Usuarios (ci, correo, contrasenia, nombre, apellido, nacionalidad, estado, rol, req_cam_clave)
VALUES ('36543286', 'carla.diaz@utec.edu.uy', '2"w''u$YuC=BPA', 'Carla', 'Díaz', 'Uruguaya', 'Inactivo', 'Evaluador externo', FALSE);

INSERT INTO Eval_Externos (id_usuario)
SELECT id_usuario FROM Usuarios WHERE rol = 'Evaluador externo';

INSERT INTO Tele_Usuarios (id_usuario, num_telefono)
SELECT id_usuario, '099123456' FROM Usuarios WHERE correo = 'eval1@utec.edu.uy';

-- ---------- 2. Convocatorias ----------
-- La primera tiene postulaciones en todos los estados; la segunda no tiene ninguna (CU12).
INSERT INTO Convocatorias (id_usuario, nom_convocatoria, fec_apertura, fec_cierre, ani_convocatoria,
                           descripcion, can_becas, tip_beca, estado)
SELECT id_usuario, 'Becas de Iniciación a la Investigación 2026', '2026-08-15 00:00', '2026-09-30 23:59', 2026, 'Convocatoria de prueba con postulaciones en todos los estados.', 10, 'Iniciacion a la investigacion', 'Cerrada'
FROM Usuarios WHERE correo = 'admin@secu.edu.uy';
INSERT INTO Convocatorias (id_usuario, nom_convocatoria, fec_apertura, fec_cierre, ani_convocatoria,
                           descripcion, can_becas, tip_beca, estado)
SELECT id_usuario, 'Becas de Posgrado 2026', '2026-10-01 00:00', '2026-11-15 23:59', 2026, 'Convocatoria de prueba sin postulaciones.', 5, 'Posgrado', 'Publicada'
FROM Usuarios WHERE correo = 'admin@secu.edu.uy';

-- Documentos requeridos por la primera convocatoria (TRUE = obligatorio).
INSERT INTO Docu_Tipo_Convocatorias (id_docu_tipo, id_convocatoria, obligatorio)
SELECT d.id_docu_tipo, c.id_convocatoria, d.nom_tip_documento IN ('Plan de trabajo', 'Presupuesto', 'Escolaridad de grado', 'CVuy del postulante', 'CVuy del tutor', 'Carta aval del tutor', 'Carta de motivación')
FROM Docu_Tipos d CROSS JOIN Convocatorias c
WHERE c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026';
-- La segunda convocatoria es de posgrado: también exige la constancia de inscripción.
INSERT INTO Docu_Tipo_Convocatorias (id_docu_tipo, id_convocatoria, obligatorio)
SELECT d.id_docu_tipo, c.id_convocatoria, d.nom_tip_documento IN ('Plan de trabajo', 'Presupuesto', 'Constancia de inscripción al posgrado', 'Escolaridad de grado', 'CVuy del postulante', 'CVuy del tutor', 'Carta aval del tutor', 'Carta de motivación')
FROM Docu_Tipos d CROSS JOIN Convocatorias c
WHERE c.nom_convocatoria = 'Becas de Posgrado 2026';

-- ---------- 3. Tutores UTEC ----------
INSERT INTO Tuto_Utec (cor_utec, nombre, apellido, dep_area) VALUES
    ('maria.suarez@utec.edu.uy', 'María', 'Suárez', 'Tecnologías de la Información'),
    ('jorge.benitez@utec.edu.uy', 'Jorge', 'Benítez', 'Energías Renovables'),
    ('paula.castro@utec.edu.uy', 'Paula', 'Castro', 'Ciencias Agrarias y Agroambientales'),
    ('andres.nunez@utec.edu.uy', 'Andrés', 'Núñez', 'Procesos Industriales');

-- ---------- 4. Postulantes (ninguno es usuario de SECU) ----------
INSERT INTO Postulantes (documento, tip_documento, nombre, apellido, correo, pai_procedencia,
                         dep_procedencia, loc_procedencia, nacionalidad, calle, num_domicilio, es_doc_utec) VALUES
    ('40128767', 'CI', 'Valentina', 'Acosta', 'valentina.acosta@gmail.com', 'Uruguay', 'Montevideo', 'Montevideo', 'Uruguaya', 'Av. Rivera', '2345', FALSE),
    ('41562300', 'CI', 'Tomás', 'Méndez', 'tomas.mendez@gmail.com', 'Uruguay', 'Durazno', 'Durazno', 'Uruguaya', 'Rivera', '512', FALSE),
    ('38745127', 'CI', 'Camila', 'Ríos', 'camila.rios@gmail.com', 'Uruguay', 'Rivera', 'Rivera', 'Uruguaya', 'Sarandí', '880', FALSE),
    ('39567811', 'CI', 'Bruno', 'Sosa', 'bruno.sosa@gmail.com', 'Uruguay', 'Paysandú', 'Paysandú', 'Uruguaya', '18 de Julio', '1023', FALSE),
    ('42033481', 'CI', 'Florencia', 'Vega', 'florencia.vega@gmail.com', 'Uruguay', 'Maldonado', 'San Carlos', 'Uruguaya', 'Treinta y Tres', '45', FALSE),
    ('43210074', 'CI', 'Matías', 'Cabrera', 'matias.cabrera@gmail.com', 'Uruguay', 'Fray Bentos', 'Fray Bentos', 'Uruguaya', 'Zorrilla', '301', FALSE),
    ('AAB123456', 'Pasaporte', 'Gabriela', 'Duarte', 'gabriela.duarte@gmail.com', 'Argentina', 'Entre Ríos', 'Concordia', 'Argentina', 'Urquiza', '1550', FALSE),
    ('36891251', 'CI', 'Joaquín', 'Romero', 'joaquin.romero@gmail.com', 'Uruguay', 'Tacuarembó', 'Tacuarembó', 'Uruguaya', 'Artigas', '77', FALSE),
    ('40987654', 'CI', 'Sofía', 'Olivera', 'sofia.olivera@gmail.com', 'Uruguay', 'Montevideo', 'Montevideo', 'Uruguaya', 'Bv. Artigas', '1440', FALSE),
    ('37654327', 'CI', 'Nicolás', 'Ferreira', 'nicolas.ferreira@gmail.com', 'Uruguay', 'Salto', 'Salto', 'Uruguaya', 'Uruguay', '640', FALSE),
    ('41876543', 'CI', 'Lucas', 'Bentancor', 'lucas.bentancor@gmail.com', 'Uruguay', 'Colonia', 'Nueva Helvecia', 'Uruguaya', 'Suiza', '210', FALSE),
    ('39123451', 'CI', 'Martina', 'Silveira', 'martina.silveira@gmail.com', 'Uruguay', 'Canelones', 'Las Piedras', 'Uruguaya', 'Batlle y Ordóñez', '903', FALSE),
    ('42543218', 'CI', 'Emilia', 'Cardozo', 'emilia.cardozo@gmail.com', 'Uruguay', 'Soriano', 'Mercedes', 'Uruguaya', 'Colón', '355', FALSE),
    ('38234562', 'CI', 'Federico', 'Martínez', 'federico.martinez@gmail.com', 'Uruguay', 'Rocha', 'Chuy', 'Uruguaya', 'Brasil', '128', FALSE);

-- ---------- 5. Postulaciones ----------
-- Una por postulante, todas en la primera convocatoria. Fechas de solicitud entre el
-- 01/09/2026 y el 15/09/2026, salvo la de Federico Martinez (20/09/2026), fuera de ese rango.
-- P01: Registrada
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Monitoreo de calidad de agua con sensores IoT',
       'Internet de las cosas', 12, 'Sistemas embebidos', '2026-09-01 10:15', 'Registrada'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'valentina.acosta@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Tecnologías de la Información' AND s.nom_sede = 'Norte';
-- P02: Registrada
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Eficiencia de paneles solares en climas templados',
       'Energía solar', 6, 'Energías renovables', '2026-09-03 11:40', 'Registrada'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'tomas.mendez@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Energías Renovables' AND s.nom_sede = 'Centro-Sur';
-- P03: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Riego de precisión en cultivos de arroz',
       'Agricultura de precisión', 12, 'Agroambiente', '2026-09-04 09:05', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'camila.rios@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Ciencias Agrarias y Agroambientales' AND s.nom_sede = 'Este';
-- P04: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Almacenamiento de energía eólica',
       'Energía eólica', 9, 'Energías renovables', '2026-09-05 16:20', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'bruno.sosa@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Energías Renovables' AND s.nom_sede = 'Norte';
-- P05: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Optimización de procesos de envasado',
       'Automatización industrial', 6, 'Procesos industriales', '2026-09-08 14:00', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'florencia.vega@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Procesos Industriales' AND s.nom_sede = 'Este';
-- P06: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Trazabilidad de ganado con blockchain',
       'Cadenas de bloques', 12, 'Sistemas de información', '2026-09-09 08:30', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'matias.cabrera@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Tecnologías de la Información' AND s.nom_sede = 'Suroeste';
-- P07: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Bioinsumos para suelos degradados',
       'Suelos', 9, 'Agroambiente', '2026-09-10 17:45', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'gabriela.duarte@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Ciencias Agrarias y Agroambientales' AND s.nom_sede = 'Centro-Sur';
-- P08: No elegible
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Aplicación móvil para turismo rural',
       'Desarrollo móvil', 6, 'Sistemas de información', '2026-09-02 12:00', 'No elegible'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'joaquin.romero@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Tecnologías de la Información' AND s.nom_sede = 'Norte';
-- P09: Evaluada
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado, pun_consolidado, ord_pre_calculado, jui_global)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Hidrógeno verde a partir de biomasa',
       'Hidrógeno verde', 12, 'Energías renovables', '2026-09-11 10:00', 'Evaluada', 4.50, 1, 'Propuesta sólida, con alto impacto para el grupo receptor.'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'sofia.olivera@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Energías Renovables' AND s.nom_sede = 'Centro-Sur';
-- P10: Evaluada
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado, pun_consolidado, ord_pre_calculado, jui_global)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Mantenimiento predictivo de motores',
       'Industria 4.0', 12, 'Procesos industriales', '2026-09-12 13:30', 'Evaluada', 3.50, 3, 'Buen plan de trabajo, con antecedentes del postulante limitados.'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'nicolas.ferreira@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Procesos Industriales' AND s.nom_sede = 'Este';
-- P11: Evaluada
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado, pun_consolidado, ord_pre_calculado, jui_global)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Riego deficitario en vid',
       'Agricultura de precisión', 6, 'Agroambiente', '2026-09-14 15:10', 'Evaluada', 3.50, 2, 'Propuesta viable; conviene ajustar el cronograma.'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'lucas.bentancor@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Ciencias Agrarias y Agroambientales' AND s.nom_sede = 'Suroeste';
-- P12: En evaluacion
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Detección de fraudes con aprendizaje automático',
       'Inteligencia artificial', 12, 'Ciencia de datos', '2026-09-15 18:00', 'En evaluacion'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'martina.silveira@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Tecnologías de la Información' AND s.nom_sede = 'Norte';
-- P13: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Gemelo digital de una planta lechera',
       'Industria 4.0', 9, 'Procesos industriales', '2026-09-07 10:50', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'emilia.cardozo@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Procesos Industriales' AND s.nom_sede = 'Centro-Sur';
-- P14: En revision
INSERT INTO Postulaciones (id_postulante, id_convocatoria, id_dept_academico, id_sede, nom_proyecto,
                           lin_investigacion, can_meses, gru_investigacion, fec_solicitud, estado)
SELECT pt.id_postulante, c.id_convocatoria, d.id_dept_academico, s.id_sede, 'Microrredes eléctricas rurales',
       'Energía solar', 12, 'Energías renovables', '2026-09-20 09:40', 'En revision'
FROM Postulantes pt, Convocatorias c, Dept_Academicos d, Sedes s
WHERE pt.correo = 'federico.martinez@gmail.com' AND c.nom_convocatoria = 'Becas de Iniciación a la Investigación 2026'
  AND d.nom_departamento = 'Departamento de Energías Renovables' AND s.nom_sede = 'Este';

-- ---------- 6. Tutores de cada postulacion ----------
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'maria.suarez@utec.edu.uy' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'jorge.benitez@utec.edu.uy' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'maria.suarez@utec.edu.uy' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Co-tutor'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'jorge.benitez@utec.edu.uy' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'jorge.benitez@utec.edu.uy' AND pt.correo = 'bruno.sosa@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'andres.nunez@utec.edu.uy' AND pt.correo = 'florencia.vega@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'maria.suarez@utec.edu.uy' AND pt.correo = 'matias.cabrera@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'paula.castro@utec.edu.uy' AND pt.correo = 'gabriela.duarte@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'maria.suarez@utec.edu.uy' AND pt.correo = 'joaquin.romero@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'jorge.benitez@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Co-tutor'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'maria.suarez@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'andres.nunez@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'paula.castro@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'maria.suarez@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'andres.nunez@utec.edu.uy' AND pt.correo = 'emilia.cardozo@gmail.com';
INSERT INTO Tuto_Utec_Postulacion (id_tuto_utec, id_postulacion, rol_tutor)
SELECT t.id_tuto_utec, p.id_postulacion, 'Tutor principal'
FROM Tuto_Utec t, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE t.cor_utec = 'jorge.benitez@utec.edu.uy' AND pt.correo = 'federico.martinez@gmail.com';

-- ---------- 7. Documentos adjuntos ----------
-- P01 tiene 4 adicionales (el maximo) y P02 tiene 3. P06 y P07 no tienen documentos.
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P03/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P09/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P10/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P11/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P12/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/otros_documentos_adicionales_1.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/otros_documentos_adicionales_2.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/otros_documentos_adicionales_3.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P01/otros_documentos_adicionales_4.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'valentina.acosta@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/plan_de_trabajo.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Plan de trabajo' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/presupuesto.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Presupuesto' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/escolaridad_de_grado.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Escolaridad de grado' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/cvuy_del_postulante.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del postulante' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/cvuy_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'CVuy del tutor' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/carta_aval_del_tutor.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta aval del tutor' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/carta_de_motivacion.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Carta de motivación' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/otros_documentos_adicionales_1.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/otros_documentos_adicionales_2.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'tomas.mendez@gmail.com';
INSERT INTO Docu_Adjuntos (id_docu_tipo, id_postulacion, rut_archivo)
SELECT d.id_docu_tipo, p.id_postulacion, 'documentos/P02/otros_documentos_adicionales_3.pdf'
FROM Docu_Tipos d, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE d.nom_tip_documento = 'Otros documentos adicionales' AND pt.correo = 'tomas.mendez@gmail.com';

-- ---------- 8. Evaluadores asignados y evaluaciones ----------
-- Puntaje uniforme por evaluacion: el puntaje total coincide con ese valor.
-- P09, eval1@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 5.00, 'Excelente propuesta y plan de trabajo.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 5
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
-- P09, eval2@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 4.00, 'Muy buena, con objetivos claros.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 4
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'sofia.olivera@gmail.com';
-- P10, eval1@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 4.00, 'Plan sólido y bien fundamentado.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 4
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
-- P10, eval3@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 3.00, 'Antecedentes del postulante escasos.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 3
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'nicolas.ferreira@gmail.com';
-- P11, eval2@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 3.00, 'Cronograma ajustado para los meses solicitados.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 3
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
-- P11, eval3@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 4.00, 'Buen aporte al grupo receptor.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 4
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'lucas.bentancor@gmail.com';
-- P12, eval1@utec.edu.uy: Completada
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Completada', 4.00, 'Propuesta pertinente y viable.'
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 4
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 16) cr ON TRUE
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
-- P12, eval2@utec.edu.uy: En proceso
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'En proceso', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
INSERT INTO Eval_Criterios (id_evaluacion, id_criterio, puntaje)
SELECT ev.id_evaluacion, cr.id_criterio, 4
FROM Evaluaciones ev JOIN Eval_Exte_Postulaciones e ON e.id_eval_exte_postulacion = ev.id_eval_exte_postulacion
JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
JOIN (SELECT id_criterio FROM Criterios ORDER BY id_criterio LIMIT 5) cr ON TRUE
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'martina.silveira@gmail.com';
-- P03, eval2@utec.edu.uy: Pendiente
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Pendiente', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'camila.rios@gmail.com';
-- P03, eval3@utec.edu.uy: Pendiente
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'camila.rios@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Pendiente', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'camila.rios@gmail.com';
-- P13, eval1@utec.edu.uy: Pendiente
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'emilia.cardozo@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Pendiente', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'emilia.cardozo@gmail.com';
-- P13, eval3@utec.edu.uy: Pendiente
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'emilia.cardozo@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Pendiente', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval3@utec.edu.uy' AND pt.correo = 'emilia.cardozo@gmail.com';
-- P14, eval1@utec.edu.uy: Pendiente
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'federico.martinez@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Pendiente', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval1@utec.edu.uy' AND pt.correo = 'federico.martinez@gmail.com';
-- P14, eval2@utec.edu.uy: Pendiente
INSERT INTO Eval_Exte_Postulaciones (id_usuario, id_postulacion)
SELECT u.id_usuario, p.id_postulacion
FROM Usuarios u, Postulaciones p JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'federico.martinez@gmail.com';
INSERT INTO Evaluaciones (id_eval_exte_postulacion, estado, pun_total, com_evaluador)
SELECT e.id_eval_exte_postulacion, 'Pendiente', NULL, NULL
FROM Eval_Exte_Postulaciones e JOIN Usuarios u ON u.id_usuario = e.id_usuario
JOIN Postulaciones p ON p.id_postulacion = e.id_postulacion
JOIN Postulantes pt ON pt.id_postulante = p.id_postulante
WHERE u.correo = 'eval2@utec.edu.uy' AND pt.correo = 'federico.martinez@gmail.com';

COMMIT;
