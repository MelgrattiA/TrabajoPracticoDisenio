package com.CapitalHumano.trabajointegrador.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@TestConfiguration(proxyBeanMethods = false)
public class PostgreSqlTestConfiguration {

	private static final String DATABASE_NAME = "gestion_evaluaciones_test";
	private static final String USERNAME = "test_evaluaciones";
	private static final String PASSWORD = "test_evaluaciones";
	private static final String CATALOGO_CONTAINER_PATH =
			"/tmp/catalogo_estados_y_unicidad_cuestionario.sql";
	private static final String MIGRACION_CU21_CONTAINER_PATH =
			"/tmp/cu21_alineacion_modelo.sql";

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgreSqlContainer() {
		return new PostgreSQLContainer("postgres:18.6")
				.withDatabaseName(DATABASE_NAME)
				.withUsername(USERNAME)
				.withPassword(PASSWORD);
	}

	@Bean
	ApplicationRunner sqlInitializer(PostgreSQLContainer postgreSqlContainer) {
		return arguments -> {
			postgreSqlContainer.copyFileToContainer(
					MountableFile.forClasspathResource(
							"db/manual/catalogo_estados_y_unicidad_cuestionario.sql"),
					CATALOGO_CONTAINER_PATH);

			org.testcontainers.containers.Container.ExecResult result =
					postgreSqlContainer.execInContainer(
							"psql",
							"-X",
							"-v",
							"ON_ERROR_STOP=1",
							"-d",
							DATABASE_NAME,
							"-U",
							USERNAME,
							"-f",
							CATALOGO_CONTAINER_PATH);

			if (result.getExitCode() != 0) {
				throw new IllegalStateException(
						"La inicializacion SQL fallo. Salida: " + result.getStderr());
			}

			postgreSqlContainer.copyFileToContainer(
					MountableFile.forClasspathResource(
							"db/manual/cu21_alineacion_modelo.sql"),
					MIGRACION_CU21_CONTAINER_PATH);

			org.testcontainers.containers.Container.ExecResult migracion =
					postgreSqlContainer.execInContainer(
							"psql",
							"-X",
							"-v",
							"ON_ERROR_STOP=1",
							"-d",
							DATABASE_NAME,
							"-U",
							USERNAME,
							"-f",
							MIGRACION_CU21_CONTAINER_PATH);

			if (migracion.getExitCode() != 0) {
				throw new IllegalStateException(
						"La migracion CU-21 fallo. Salida: " + migracion.getStderr());
			}
		};
	}
}
