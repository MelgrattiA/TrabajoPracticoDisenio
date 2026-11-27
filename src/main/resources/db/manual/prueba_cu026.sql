-- Datos de prueba para recorrer CU026 de punta a punta.
-- Ejecutar después de iniciar Spring Boot al menos una vez, para que Hibernate cree
-- las tablas. La clave del cuestionario queda fija para poder abrirla desde el front.
--
-- URL local:
-- http://localhost:3000/api/cuestionario/instrucciones?clave=T0260001
--
-- El fixture usa dos factores con dos preguntas cada uno. Con la configuración
-- inicial de dos preguntas por bloque, se generan dos bloques y se puede probar
-- el flujo Siguiente/Finalizar. Al volver a ejecutarlo, solo limpia respuestas y
-- bloques del cuestionario reservado T0260001.

BEGIN;

INSERT INTO estado_cuestionario (id_estado_cuestionario, nombre)
VALUES
    (1, 'Activo'),
    (2, 'En Proceso'),
    (3, 'Completo'),
    (4, 'Incompleto'),
    (5, 'Sin Contestar')
ON CONFLICT (id_estado_cuestionario) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (VALUES
            (1, 'Activo'),
            (2, 'En Proceso'),
            (3, 'Completo'),
            (4, 'Incompleto'),
            (5, 'Sin Contestar')
        ) AS esperado(id, nombre)
        LEFT JOIN estado_cuestionario estado
          ON estado.id_estado_cuestionario = esperado.id
        WHERE estado.nombre IS DISTINCT FROM esperado.nombre
    ) THEN
        RAISE EXCEPTION
            'Los estados de cuestionario no coinciden con los IDs requeridos por CU026';
    END IF;
END;
$$;

INSERT INTO configuracion_sistema (
    id_configuracion,
    instrucciones_cuestionario,
    cantidad_preguntas_por_bloque,
    cantidad_preguntas_por_factor,
    maximo_accesos,
    tiempo_maximo_activo_dias,
    tiempo_maximo_completar_dias
)
VALUES (
    1,
    'Responda todas las preguntas de cada bloque y seleccione una opción por pregunta.',
    2,
    2,
    3,
    7,
    1
)
ON CONFLICT (id_configuracion) DO UPDATE SET
    instrucciones_cuestionario = EXCLUDED.instrucciones_cuestionario
WHERE BTRIM(configuracion_sistema.instrucciones_cuestionario) = '';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM configuracion_sistema
        WHERE id_configuracion = 1
          AND cantidad_preguntas_por_bloque = 2
          AND cantidad_preguntas_por_factor = 2
    ) THEN
        RAISE EXCEPTION
            'Para este fixture configure 2 preguntas por bloque y 2 por factor';
    END IF;
END;
$$;

INSERT INTO candidato (
    nro_candidato,
    apellido,
    nombre,
    tipo_documento,
    nro_documento,
    fecha_nacimiento,
    genero,
    nacionalidad,
    email,
    escolaridad,
    eliminado
)
VALUES (
    99002601,
    'Prueba CU026',
    'Candidato',
    'DNI',
    99002601,
    DATE '1995-01-15',
    'No especificado',
    'Argentina',
    'cu026.prueba@example.invalid',
    'Universitario',
    false
)
ON CONFLICT (nro_candidato) DO UPDATE SET
    apellido = EXCLUDED.apellido,
    nombre = EXCLUDED.nombre,
    tipo_documento = EXCLUDED.tipo_documento,
    nro_documento = EXCLUDED.nro_documento,
    fecha_nacimiento = EXCLUDED.fecha_nacimiento,
    genero = EXCLUDED.genero,
    nacionalidad = EXCLUDED.nacionalidad,
    email = EXCLUDED.email,
    escolaridad = EXCLUDED.escolaridad,
    eliminado = false;

INSERT INTO puesto (
    codigo_puesto,
    nombre_puesto,
    descripcion,
    empresa,
    eliminado
)
VALUES (
    'TEST-CU026',
    'Puesto de prueba CU026',
    'Puesto creado exclusivamente para probar el cuestionario.',
    'Empresa de prueba',
    false
)
ON CONFLICT (codigo_puesto) DO UPDATE SET
    nombre_puesto = EXCLUDED.nombre_puesto,
    descripcion = EXCLUDED.descripcion,
    empresa = EXCLUDED.empresa,
    eliminado = false;

INSERT INTO competencia (codigo, nombre, descripcion, eliminado)
VALUES (
    99002601,
    'Competencia de prueba CU026',
    'Competencia para probar cálculo y respuestas.',
    false
)
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    eliminado = false;

INSERT INTO caracteristica_puesto (
    codigo_puesto,
    codigo_competencia,
    ponderacion_minima
)
VALUES ('TEST-CU026', 99002601, 0)
ON CONFLICT (codigo_puesto, codigo_competencia) DO UPDATE SET
    ponderacion_minima = EXCLUDED.ponderacion_minima;

INSERT INTO factor (
    codigo,
    nombre,
    descripcion,
    nro_orden,
    codigo_competencia,
    eliminado
)
VALUES
    (99002601, 'Factor 1 de prueba CU026', 'Primer factor de prueba.', 1, 99002601, false),
    (99002602, 'Factor 2 de prueba CU026', 'Segundo factor de prueba.', 2, 99002601, false)
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    descripcion = EXCLUDED.descripcion,
    nro_orden = EXCLUDED.nro_orden,
    codigo_competencia = EXCLUDED.codigo_competencia,
    eliminado = false;

WITH opcion AS (
    INSERT INTO opcion_respuesta (nombre, descripcion, eliminado)
    VALUES (
        'TEST-CU026-LIKERT',
        'Escala de opciones para probar CU026.',
        false
    )
    ON CONFLICT (nombre) DO UPDATE SET
        descripcion = EXCLUDED.descripcion,
        eliminado = false
    RETURNING id_opcion_respuesta
)
INSERT INTO item_opcion (
    orden_visualizacion,
    descripcion,
    id_opcion_respuesta
)
SELECT opciones.orden, opciones.descripcion, opcion.id_opcion_respuesta
FROM opcion
CROSS JOIN (
    VALUES
        (1, 'Nunca'),
        (2, 'A veces'),
        (3, 'Frecuentemente'),
        (4, 'Siempre')
) AS opciones(orden, descripcion)
ON CONFLICT (id_opcion_respuesta, orden_visualizacion) DO UPDATE SET
    descripcion = EXCLUDED.descripcion;

INSERT INTO pregunta (
    codigo,
    nombre,
    pregunta,
    descripcion,
    codigo_factor,
    id_opcion_respuesta,
    eliminado
)
SELECT
    preguntas.codigo,
    preguntas.nombre,
    preguntas.texto,
    preguntas.texto,
    preguntas.codigo_factor,
    opcion.id_opcion_respuesta,
    false
FROM (
    VALUES
        (99002601, 'Pregunta 1', '¿Organizás tus tareas antes de comenzar?', 99002601),
        (99002602, 'Pregunta 2', '¿Revisás tu trabajo antes de entregarlo?', 99002601),
        (99002603, 'Pregunta 3', '¿Colaborás con otras personas para resolver problemas?', 99002602),
        (99002604, 'Pregunta 4', '¿Compartís información útil con tu equipo?', 99002602)
) AS preguntas(codigo, nombre, texto, codigo_factor)
CROSS JOIN opcion_respuesta opcion
WHERE opcion.nombre = 'TEST-CU026-LIKERT'
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    pregunta = EXCLUDED.pregunta,
    descripcion = EXCLUDED.descripcion,
    codigo_factor = EXCLUDED.codigo_factor,
    id_opcion_respuesta = EXCLUDED.id_opcion_respuesta,
    eliminado = false;

INSERT INTO ponderacion_item (ponderacion, codigo_pregunta, id_item_opcion)
SELECT
    opciones.orden_visualizacion - 1,
    preguntas.codigo,
    opciones.id_item_opcion
FROM pregunta preguntas
CROSS JOIN opcion_respuesta tipo
JOIN item_opcion opciones
  ON opciones.id_opcion_respuesta = tipo.id_opcion_respuesta
WHERE tipo.nombre = 'TEST-CU026-LIKERT'
  AND preguntas.codigo BETWEEN 99002601 AND 99002604
ON CONFLICT (codigo_pregunta, id_item_opcion) DO UPDATE SET
    ponderacion = EXCLUDED.ponderacion;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM cuestionario
        WHERE nro_candidato = 99002601
          AND estado_id IN (1, 2)
          AND clave <> 'T0260001'
    ) THEN
        RAISE EXCEPTION
            'El candidato de prueba ya tiene otro cuestionario Activo o En Proceso';
    END IF;
END;
$$;

DELETE FROM resultado_competencia
WHERE clave_cuestionario = 'T0260001';

DELETE FROM pregunta_bloque pb
USING bloque_pregunta bp
WHERE pb.id_bloque_pregunta = bp.id_bloque_pregunta
  AND bp.clave_cuestionario = 'T0260001';

DELETE FROM bloque_pregunta
WHERE clave_cuestionario = 'T0260001';

INSERT INTO cuestionario (
    clave,
    fecha_creacion,
    cantidad_accesos,
    nro_candidato,
    codigo_puesto,
    estado_id
)
VALUES (
    'T0260001',
    CURRENT_TIMESTAMP,
    0,
    99002601,
    'TEST-CU026',
    1
)
ON CONFLICT (clave) DO UPDATE SET
    fecha_creacion = EXCLUDED.fecha_creacion,
    fecha_inicio = NULL,
    fecha_ultimo_ingreso = NULL,
    fecha_finalizacion = NULL,
    cantidad_accesos = 0,
    puntaje_final = NULL,
    nro_candidato = EXCLUDED.nro_candidato,
    codigo_puesto = EXCLUDED.codigo_puesto,
    estado_id = EXCLUDED.estado_id;

COMMIT;

SELECT
    c.clave,
    c.nro_candidato,
    ca.nombre,
    ca.apellido,
    c.codigo_puesto,
    ec.nombre AS estado
FROM cuestionario c
JOIN candidato ca ON ca.nro_candidato = c.nro_candidato
JOIN estado_cuestionario ec
  ON ec.id_estado_cuestionario = c.estado_id
WHERE c.clave = 'T0260001';
