package com.CapitalHumano.trabajointegrador;

import com.CapitalHumano.trabajointegrador.config.PostgreSqlTestConfiguration;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSqlTestConfiguration.class)
class TrabajointegradorApplicationTests {

	private static final List<String> EXPECTED_TABLES = List.of(
			"bloque_pregunta",
			"candidato",
			"caracteristica_puesto",
			"competencia",
			"cuestionario",
			"estado_cuestionario",
			"factor",
			"item_opcion",
			"opcion_respuesta",
			"ponderacion_item",
			"pregunta",
			"pregunta_bloque",
			"puesto",
			"registro_actualizacion_de_estado",
			"registro_eliminacion",
			"resultado_competencia",
			"usuario"
	);

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads() {
	}

	@Test
	void metamodelRecognizesSeventeenEntities() {
		Set<String> entityNames = entityManager.getMetamodel().getEntities().stream()
				.map(EntityType::getName)
				.collect(Collectors.toSet());

		assertThat(entityManager.getMetamodel().getEntities()).hasSize(17);
		assertThat(entityNames).contains(
				"Candidato",
				"Puesto",
				"EstadoCuestionario",
				"Competencia",
				"CaracteristicaPuesto",
				"Factor",
				"Pregunta",
				"OpcionRespuesta",
				"ItemOpcion",
				"PonderacionItem",
				"Cuestionario",
				"BloquePregunta",
				"PreguntaBloque",
				"ResultadoCompetencia",
				"Usuario",
				"RegistroActualizacionDeEstado",
				"RegistroEliminacion");
	}

	@Test
	void schemaContainsSeventeenTables() {
		List<String> tables = jdbcTemplate.queryForList(
				"""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_type = 'BASE TABLE'
				ORDER BY table_name
				""",
				String.class);

		assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
	}

	@Test
	void catalogContainsTheFiveReservedStates() {
		List<Map<String, Object>> states = jdbcTemplate.queryForList(
				"""
				SELECT id_estado_cuestionario, nombre
				FROM estado_cuestionario
				WHERE id_estado_cuestionario BETWEEN 1 AND 5
				ORDER BY id_estado_cuestionario
				""");

		assertThat(states).extracting(row -> ((Number) row.get("id_estado_cuestionario")).intValue())
				.containsExactly(1, 2, 3, 4, 5);
		assertThat(states).extracting(row -> (String) row.get("nombre"))
				.containsExactly(
						"Activo",
						"En Proceso",
						"Completo",
						"Incompleto",
						"Sin Contestar");
	}

	@Test
	void partialUniqueIndexExistsForActiveStates() {
		Map<String, Object> index = jdbcTemplate.queryForMap(
				"""
				SELECT i.indisunique,
				       pg_get_indexdef(i.indexrelid) AS index_definition,
				       pg_get_expr(i.indpred, i.indrelid) AS predicate
				FROM pg_class index_relation
				JOIN pg_index i ON i.indexrelid = index_relation.oid
				JOIN pg_class table_relation ON table_relation.oid = i.indrelid
				WHERE index_relation.relname = ?
				  AND table_relation.relname = 'cuestionario'
				""",
				"ux_cuestionario_candidato_activo_en_proceso");

		assertThat((Boolean) index.get("indisunique")).isTrue();

		String predicate = (String) index.get("predicate");
		assertThat(predicate).contains("estado_id");
		assertThat(predicate).contains("1");
		assertThat(predicate).contains("2");
	}

	@Test
	void preguntaUsesIdentityAndDocumentedLengths() {
		Map<String, Object> codigo = jdbcTemplate.queryForMap(
				"""
				SELECT is_identity, identity_generation
				FROM information_schema.columns
				WHERE table_schema = 'public'
				  AND table_name = 'pregunta'
				  AND column_name = 'codigo'
				""");
		assertThat(codigo.get("is_identity")).isEqualTo("YES");

		List<Map<String, Object>> columns = jdbcTemplate.queryForList(
				"""
				SELECT column_name, character_maximum_length
				FROM information_schema.columns
				WHERE table_schema = 'public'
				  AND table_name = 'pregunta'
				  AND column_name IN ('nombre', 'pregunta', 'descripcion')
				""");
		assertThat(columns)
				.extracting(row -> (String) row.get("column_name"))
				.containsExactlyInAnyOrder("nombre", "pregunta", "descripcion");
		assertThat(columns)
				.extracting(row -> Map.entry(
						(String) row.get("column_name"),
						((Number) row.get("character_maximum_length")).intValue()))
				.containsExactlyInAnyOrder(
						Map.entry("nombre", 100),
						Map.entry("pregunta", 150),
						Map.entry("descripcion", 500));
	}

	@Test
	void numericColumnsUsePrecision38AndScale2() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList(
				"""
				SELECT table_name, column_name, numeric_precision, numeric_scale
				FROM information_schema.columns
				WHERE table_schema = 'public'
				  AND (
				    (table_name = 'ponderacion_item' AND column_name = 'ponderacion')
				    OR (table_name = 'caracteristica_puesto'
				        AND column_name = 'ponderacion_minima')
				    OR (table_name = 'resultado_competencia'
				        AND column_name = 'puntaje_obtenido')
				    OR (table_name = 'cuestionario' AND column_name = 'puntaje_final')
				  )
				""");

		assertThat(columns).hasSize(4);
		for (Map<String, Object> column : columns) {
			assertThat(((Number) column.get("numeric_precision")).intValue()).isEqualTo(38);
			assertThat(((Number) column.get("numeric_scale")).intValue()).isEqualTo(2);
		}
	}

	@Test
	void noNormalUniqueConstraintsAndPartialIndexesRemainForPreguntas() {
		List<String> uniqueConstraints = jdbcTemplate.queryForList(
				"""
				SELECT conname
				FROM pg_constraint
				WHERE conrelid = 'public.pregunta'::regclass
				  AND contype = 'u'
				""",
				String.class);
		assertThat(uniqueConstraints)
				.doesNotContain(
						"uk_pregunta_factor_nombre",
						"uk_pregunta_factor_texto");

		List<String> partialIndexes = jdbcTemplate.queryForList(
				"""
				SELECT pg_get_indexdef(i.indexrelid)
				FROM pg_index i
				JOIN pg_class table_relation ON table_relation.oid = i.indrelid
				WHERE table_relation.relname = 'pregunta'
				  AND i.indisunique
				  AND pg_get_expr(i.indpred, i.indrelid) LIKE '%eliminado = false%'
				""",
				String.class);
		assertThat(partialIndexes).hasSize(2);
		assertThat(String.join(" ", partialIndexes))
				.contains("upper(btrim");
	}

	@Test
	@Transactional
	void generatedPreguntaCodesDoNotCollideAndBigDecimalPersistsWithScale2() {
		jdbcTemplate.update(
				"""
				INSERT INTO competencia (codigo, nombre, descripcion, eliminado)
				VALUES (901, 'COMUNICACION', 'PRUEBA', false)
				ON CONFLICT (codigo) DO NOTHING
				""");
		jdbcTemplate.update(
				"""
				INSERT INTO factor (
				    codigo, nombre, descripcion, nro_orden, codigo_competencia, eliminado
				)
				VALUES (901, 'ORAL', 'PRUEBA', 1, 901, false)
				ON CONFLICT (codigo) DO NOTHING
				""");
		jdbcTemplate.update(
				"""
				INSERT INTO opcion_respuesta (nombre, descripcion, eliminado)
				VALUES ('ESCALA CU21', 'PRUEBA', false)
				ON CONFLICT (nombre) DO NOTHING
				""");
		Long opcionId = jdbcTemplate.queryForObject(
				"SELECT id_opcion_respuesta FROM opcion_respuesta "
						+ "WHERE nombre = 'ESCALA CU21'",
				Long.class);
		jdbcTemplate.update(
				"""
				INSERT INTO item_opcion (
				    orden_visualizacion, descripcion, id_opcion_respuesta
				)
				VALUES (1, 'NADA', ?), (2, 'POCO', ?)
				""",
				opcionId,
				opcionId);

		List<Long> itemIds = jdbcTemplate.queryForList(
				"""
				SELECT id_item_opcion
				FROM item_opcion
				WHERE id_opcion_respuesta = ?
				ORDER BY orden_visualizacion
				""",
				Long.class,
				opcionId);

		Integer codigoUno = jdbcTemplate.queryForObject(
				"""
				INSERT INTO pregunta (
				    nombre, pregunta, descripcion,
				    codigo_factor, id_opcion_respuesta, eliminado
				)
				VALUES ('PREGUNTA UNO', 'TEXTO UNO', 'DESCRIPCION', 901, ?, false)
				RETURNING codigo
				""",
				Integer.class,
				opcionId);

		Integer codigoDos = jdbcTemplate.queryForObject(
				"""
				INSERT INTO pregunta (
				    nombre, pregunta, descripcion,
				    codigo_factor, id_opcion_respuesta, eliminado
				)
				VALUES ('PREGUNTA DOS', 'TEXTO DOS', 'DESCRIPCION', 901, ?, false)
				RETURNING codigo
				""",
				Integer.class,
				opcionId);

		assertThat(codigoDos).isGreaterThan(codigoUno);

		jdbcTemplate.update(
				"""
				INSERT INTO ponderacion_item (
				    ponderacion, codigo_pregunta, id_item_opcion
				)
				VALUES (?, ?, ?)
				""",
				new BigDecimal("3.00"),
				codigoUno,
				itemIds.get(0));

		Map<String, Object> value = jdbcTemplate.queryForMap(
				"""
				SELECT ponderacion, scale(ponderacion) AS escala
				FROM ponderacion_item
				WHERE codigo_pregunta = ?
				  AND id_item_opcion = ?
				""",
				codigoUno,
				itemIds.get(0));

		assertThat(((BigDecimal) value.get("ponderacion")).compareTo(new BigDecimal("3.00")))
				.isZero();
		assertThat(((Number) value.get("escala")).intValue()).isEqualTo(2);
	}
}
