package com.CapitalHumano.trabajointegrador.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

class Cu21MigrationTest {

    @Test
    void migraUnEsquemaVacioSinErrores() throws Exception {
        try (PostgreSQLContainer container = new PostgreSQLContainer("postgres:18.6")
                .withDatabaseName("cu21_empty")
                .withUsername("test")
                .withPassword("test")) {
            container.start();

            try (Connection connection = DriverManager.getConnection(
                    container.getJdbcUrl(), container.getUsername(), container.getPassword());
                 Statement statement = connection.createStatement()) {
                connection.setAutoCommit(true);
                statement.execute(crearEsquemaVacio());
                statement.execute(leerMigracion());

                try (ResultSet codigo = informacionColumna(statement, "pregunta", "codigo")) {
                    assertThat(codigo.next()).isTrue();
                    assertThat(codigo.getString("is_identity")).isEqualTo("YES");
                }
                try (ResultSet nombre = informacionColumna(statement, "pregunta", "nombre")) {
                    assertThat(nombre.next()).isTrue();
                    assertThat(nombre.getInt("character_maximum_length")).isEqualTo(100);
                }
                try (ResultSet texto = informacionColumna(statement, "pregunta", "pregunta")) {
                    assertThat(texto.next()).isTrue();
                    assertThat(texto.getInt("character_maximum_length")).isEqualTo(150);
                }
                try (ResultSet descripcion = informacionColumna(statement, "pregunta", "descripcion")) {
                    assertThat(descripcion.next()).isTrue();
                    assertThat(descripcion.getInt("character_maximum_length")).isEqualTo(500);
                }
                try (ResultSet ponderacion =
                        informacionColumna(statement, "ponderacion_item", "ponderacion")) {
                    assertThat(ponderacion.next()).isTrue();
                    assertThat(ponderacion.getString("data_type")).isEqualTo("numeric");
                    assertThat(ponderacion.getInt("numeric_precision")).isEqualTo(38);
                    assertThat(ponderacion.getInt("numeric_scale")).isEqualTo(2);
                }
            }
        }
    }

    @Test
    void migraDatosRepresentativosYProtegeDuplicadosActivos() throws Exception {
        try (PostgreSQLContainer container = new PostgreSQLContainer("postgres:18.6")
                .withDatabaseName("cu21_data")
                .withUsername("test")
                .withPassword("test")) {
            container.start();

            try (Connection connection = DriverManager.getConnection(
                    container.getJdbcUrl(), container.getUsername(), container.getPassword());
                 Statement statement = connection.createStatement()) {
                connection.setAutoCommit(true);
                statement.execute(crearEsquemaVacio());
                statement.execute("""
                        INSERT INTO competencia (codigo, nombre, descripcion, eliminado)
                        VALUES (1, 'COMUNICACION', NULL, false);
                        INSERT INTO factor (codigo, nombre, descripcion, nro_orden, codigo_competencia, eliminado)
                        VALUES (10, 'ORAL', NULL, 1, 1, false);
                        INSERT INTO opcion_respuesta (nombre, descripcion, eliminado)
                        VALUES ('ESCALA', NULL, false);
                        INSERT INTO item_opcion (orden_visualizacion, descripcion, id_opcion_respuesta)
                        VALUES (1, 'NADA', 1), (2, 'POCO', 1);
                        INSERT INTO pregunta (
                            codigo, nombre, pregunta, descripcion, codigo_factor,
                            id_opcion_respuesta, eliminado
                        )
                        VALUES (
                            7, '  pregunta uno  ', '  texto uno  ', '  descripcion uno  ',
                            10, 1, false
                        );
                        INSERT INTO ponderacion_item (
                            ponderacion, codigo_pregunta, id_item_opcion
                        )
                        VALUES (3, 7, 1), (7, 7, 2);
                        """);

                statement.execute(leerMigracion());

                try (ResultSet row = statement.executeQuery(
                        "SELECT nombre, pregunta, descripcion FROM pregunta WHERE codigo = 7")) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getString("nombre")).isEqualTo("PREGUNTA UNO");
                    assertThat(row.getString("pregunta")).isEqualTo("TEXTO UNO");
                    assertThat(row.getString("descripcion")).isEqualTo("DESCRIPCION UNO");
                }

                try (ResultSet row = statement.executeQuery(
                        "SELECT ponderacion, scale(ponderacion) AS escala FROM ponderacion_item "
                                + "WHERE codigo_pregunta = 7 ORDER BY id_ponderacion_item")) {
                    assertThat(row.next()).isTrue();
                    assertThat(row.getBigDecimal("ponderacion").toPlainString()).isEqualTo("3.00");
                    assertThat(row.getInt("escala")).isEqualTo(2);
                    assertThat(row.next()).isTrue();
                    assertThat(row.getBigDecimal("ponderacion").toPlainString()).isEqualTo("7.00");
                    assertThat(row.getInt("escala")).isEqualTo(2);
                }

                assertThatThrownBy(() -> statement.execute(
                        "INSERT INTO pregunta "
                                + "(nombre, pregunta, descripcion, codigo_factor, id_opcion_respuesta, eliminado) "
                                + "VALUES ('pregunta uno', 'OTRO TEXTO', NULL, 10, 1, false)"))
                        .isInstanceOf(Exception.class);

                assertThatThrownBy(() -> statement.execute(
                        "INSERT INTO pregunta "
                                + "(nombre, pregunta, descripcion, codigo_factor, id_opcion_respuesta, eliminado) "
                                + "VALUES ('OTRO NOMBRE', 'texto uno', NULL, 10, 1, false)"))
                        .isInstanceOf(Exception.class);
            }
        }
    }

    private ResultSet informacionColumna(
            Statement statement,
            String tabla,
            String columna) throws Exception {
        return statement.executeQuery(
                "SELECT data_type, character_maximum_length, numeric_precision, "
                        + "numeric_scale, is_identity "
                        + "FROM information_schema.columns "
                        + "WHERE table_schema = 'public' "
                        + "AND table_name = '" + tabla + "' "
                        + "AND column_name = '" + columna + "'");
    }

    private String leerMigracion() throws Exception {
        return Files.readString(
                Path.of("src/main/resources/db/manual/cu21_alineacion_modelo.sql"),
                StandardCharsets.UTF_8);
    }

    private String crearEsquemaVacio() {
        return """
                CREATE TABLE competencia (
                    codigo integer PRIMARY KEY,
                    nombre varchar(255),
                    descripcion varchar(255),
                    eliminado boolean NOT NULL
                );
                CREATE TABLE factor (
                    codigo integer PRIMARY KEY,
                    nombre varchar(255),
                    descripcion varchar(255),
                    nro_orden integer NOT NULL,
                    codigo_competencia integer NOT NULL,
                    eliminado boolean NOT NULL
                );
                CREATE TABLE opcion_respuesta (
                    id_opcion_respuesta bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    nombre varchar(255) NOT NULL,
                    descripcion varchar(255),
                    eliminado boolean NOT NULL
                );
                CREATE TABLE item_opcion (
                    id_item_opcion bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    orden_visualizacion integer NOT NULL,
                    descripcion varchar(255) NOT NULL,
                    id_opcion_respuesta bigint NOT NULL
                );
                CREATE TABLE pregunta (
                    codigo integer PRIMARY KEY,
                    nombre varchar(255) NOT NULL,
                    pregunta varchar(255) NOT NULL,
                    descripcion varchar(255),
                    codigo_factor integer NOT NULL,
                    id_opcion_respuesta bigint NOT NULL,
                    eliminado boolean NOT NULL
                );
                CREATE TABLE ponderacion_item (
                    id_ponderacion_item bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    ponderacion integer NOT NULL,
                    codigo_pregunta integer NOT NULL,
                    id_item_opcion bigint NOT NULL,
                    CONSTRAINT ck_ponderacion_item_no_negativa CHECK (ponderacion >= 0)
                );
                ALTER TABLE factor
                    ADD CONSTRAINT factor_competencia_fk
                    FOREIGN KEY (codigo_competencia) REFERENCES competencia(codigo);
                ALTER TABLE item_opcion
                    ADD CONSTRAINT item_opcion_respuesta_fk
                    FOREIGN KEY (id_opcion_respuesta) REFERENCES opcion_respuesta(id_opcion_respuesta);
                ALTER TABLE pregunta
                    ADD CONSTRAINT pregunta_factor_fk FOREIGN KEY (codigo_factor) REFERENCES factor(codigo),
                    ADD CONSTRAINT pregunta_opcion_fk FOREIGN KEY (id_opcion_respuesta)
                        REFERENCES opcion_respuesta(id_opcion_respuesta),
                    ADD CONSTRAINT uk_pregunta_factor_nombre UNIQUE (codigo_factor, nombre),
                    ADD CONSTRAINT uk_pregunta_factor_texto UNIQUE (codigo_factor, pregunta);
                ALTER TABLE ponderacion_item
                    ADD CONSTRAINT uk_ponderacion_pregunta_item
                        UNIQUE (codigo_pregunta, id_item_opcion),
                    ADD CONSTRAINT ponderacion_pregunta_fk
                        FOREIGN KEY (codigo_pregunta) REFERENCES pregunta(codigo),
                    ADD CONSTRAINT ponderacion_item_fk
                        FOREIGN KEY (id_item_opcion) REFERENCES item_opcion(id_item_opcion);
                CREATE TABLE caracteristica_puesto (
                    codigo_puesto varchar(50),
                    codigo_competencia integer,
                    ponderacion_minima numeric(38,2) NOT NULL,
                    PRIMARY KEY (codigo_puesto, codigo_competencia)
                );
                CREATE TABLE resultado_competencia (
                    clave_cuestionario varchar(255),
                    codigo_puesto varchar(50),
                    codigo_competencia integer,
                    puntaje_obtenido numeric(38,2) NOT NULL,
                    PRIMARY KEY (clave_cuestionario, codigo_puesto, codigo_competencia)
                );
                CREATE TABLE cuestionario (
                    clave varchar(255) PRIMARY KEY,
                    puntaje_final numeric(38,2)
                );
                """;
    }
}
