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
}
