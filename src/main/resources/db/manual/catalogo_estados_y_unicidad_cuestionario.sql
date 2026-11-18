-- Aplicar manualmente luego de revisar que las tablas y columnas existan.
-- Este script no se ejecuta automáticamente desde Spring Boot.

BEGIN;

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
        ) AS esperado(id_estado_cuestionario, nombre)
        JOIN estado_cuestionario AS existente
          ON existente.id_estado_cuestionario = esperado.id_estado_cuestionario
        WHERE existente.nombre IS DISTINCT FROM esperado.nombre
    ) THEN
        RAISE EXCEPTION
            'El catálogo EstadoCuestionario contiene IDs reservados con nombres distintos';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES
            (1, 'Activo'),
            (2, 'En Proceso'),
            (3, 'Completo'),
            (4, 'Incompleto'),
            (5, 'Sin Contestar')
        ) AS esperado(id_estado_cuestionario, nombre)
        JOIN estado_cuestionario AS existente
          ON existente.nombre = esperado.nombre
        WHERE existente.id_estado_cuestionario <> esperado.id_estado_cuestionario
    ) THEN
        RAISE EXCEPTION
            'Un nombre reservado de EstadoCuestionario ya está asociado a otro ID';
    END IF;
END
$$;

INSERT INTO estado_cuestionario (id_estado_cuestionario, nombre)
VALUES
    (1, 'Activo'),
    (2, 'En Proceso'),
    (3, 'Completo'),
    (4, 'Incompleto'),
    (5, 'Sin Contestar')
ON CONFLICT (id_estado_cuestionario) DO NOTHING;

-- Si el índice ya existe, CREATE falla en vez de omitir una definición distinta.
CREATE UNIQUE INDEX ux_cuestionario_candidato_activo_en_proceso
    ON cuestionario (nro_candidato)
    WHERE estado_id IN (1, 2);

COMMIT;

-- Verificación manual posterior:
-- SELECT id_estado_cuestionario, nombre
-- FROM estado_cuestionario
-- WHERE id_estado_cuestionario BETWEEN 1 AND 5
-- ORDER BY id_estado_cuestionario;
--
-- SELECT indexdef
-- FROM pg_indexes
-- WHERE schemaname = current_schema()
--   AND indexname = 'ux_cuestionario_candidato_activo_en_proceso';
