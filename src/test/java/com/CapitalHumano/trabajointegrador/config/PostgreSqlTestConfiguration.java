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
	private static final String SQL_CONTAINER_PATH = "/tmp/catalogo_estados_y_unicidad_cuestionario.sql";

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
					SQL_CONTAINER_PATH);

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
							SQL_CONTAINER_PATH);

			if (result.getExitCode() != 0) {
				throw new IllegalStateException(
						"La inicializacion SQL fallo. Salida: " + result.getStderr());
			}
		};
	}
}
