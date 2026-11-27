package com.CapitalHumano.trabajointegrador.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import jakarta.validation.ConstraintViolation;

class CrearPreguntaRequestTest {

    private static LocalValidatorFactoryBean validator;
    private static ObjectMapper objectMapper;

    @BeforeAll
    static void inicializar() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        objectMapper = new ObjectMapper();
    }

    @AfterAll
    static void cerrar() {
        validator.close();
    }

    @Test
    void aceptaDatosValidosDeserializados() throws JacksonException {
        CrearPreguntaRequest request = deserializar(
                "NOMBRE",
                "PREGUNTA",
                "DESCRIPCION",
                ponderacionesValidas());

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void informaCamposObligatoriosDeserializados() throws JacksonException {
        ObjectNode json = solicitudJson(
                " ",
                "",
                null,
                objectMapper.createArrayNode());
        json.putNull("codigoCompetencia");
        json.putNull("codigoFactor");
        json.putNull("idOpcionRespuesta");

        CrearPreguntaRequest request = deserializarJson(json);

        Set<ConstraintViolation<CrearPreguntaRequest>> errores =
                validator.validate(request);

        assertThat(errores).hasSizeGreaterThanOrEqualTo(5);
    }

    @Test
    void normalizaAntesDeValidarLasLongitudesMaximas()
            throws JacksonException {
        CrearPreguntaRequest request = deserializar(
                "  " + "n".repeat(100) + "  ",
                "  " + "p".repeat(150) + "  ",
                "  " + "d".repeat(500) + "  ",
                ponderacionesValidas());

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.nombre()).isEqualTo("N".repeat(100));
        assertThat(request.pregunta()).isEqualTo("P".repeat(150));
        assertThat(request.descripcion()).isEqualTo("D".repeat(500));
    }

    @Test
    void rechazaLiteralesQueExcedenLasLongitudesNormalizadas()
            throws JacksonException {
        assertThat(propiedadesInvalidas(deserializar(
                "N".repeat(101),
                "P".repeat(150),
                "D".repeat(500),
                ponderacionesValidas())))
                .contains("nombre");

        assertThat(propiedadesInvalidas(deserializar(
                "N".repeat(100),
                "P".repeat(151),
                "D".repeat(500),
                ponderacionesValidas())))
                .contains("pregunta");

        assertThat(propiedadesInvalidas(deserializar(
                "N".repeat(100),
                "P".repeat(150),
                "D".repeat(501),
                ponderacionesValidas())))
                .contains("descripcion");
    }

    @Test
    void normalizaDescripcionNulaVaciaYCompuestaPorEspacios()
            throws JacksonException {
        for (String descripcion : new String[] {null, "", "   "}) {
            CrearPreguntaRequest request = deserializar(
                    "NOMBRE",
                    "PREGUNTA",
                    descripcion,
                    ponderacionesValidas());

            assertThat(request.descripcion()).isNull();
            assertThat(validator.validate(request)).isEmpty();
        }
    }

    @Test
    void rechazaColeccionDePonderacionesNulaVaciaOConElementosNulos()
            throws JacksonException {
        CrearPreguntaRequest coleccionNula = deserializar(
                "NOMBRE", "PREGUNTA", null, JsonNodeFactory.instance.nullNode());
        CrearPreguntaRequest coleccionVacia = deserializar(
                "NOMBRE", "PREGUNTA", null, objectMapper.createArrayNode());
        ArrayNode conElementoNulo = objectMapper.createArrayNode().addNull();
        CrearPreguntaRequest elementoNulo = deserializar(
                "NOMBRE", "PREGUNTA", null, conElementoNulo);

        assertThat(propiedadesInvalidas(coleccionNula))
                .contains("ponderaciones");
        assertThat(propiedadesInvalidas(coleccionVacia))
                .contains("ponderaciones");
        assertThat(propiedadesInvalidas(elementoNulo))
                .anyMatch(propiedad -> propiedad.startsWith("ponderaciones"));
    }

    @Test
    void validaLosElementosAnidadosYSusIdentificadores()
            throws JacksonException {
        ArrayNode ponderaciones = objectMapper.createArrayNode();
        ObjectNode elemento = ponderaciones.addObject();
        elemento.put("idItemOpcion", 0);
        elemento.put("ponderacion", new BigDecimal("10.00"));

        CrearPreguntaRequest request = deserializar(
                "NOMBRE",
                "PREGUNTA",
                null,
                ponderaciones);

        assertThat(propiedadesInvalidas(request))
                .anyMatch(propiedad -> propiedad.startsWith("ponderaciones"));
    }

    @Test
    void normalizaLosLiteralesAlDeserializar()
            throws JacksonException {
        CrearPreguntaRequest request = deserializar(
                "  pregunta mixta ÁÉÑ  ",
                "  texto mixto ÚÜ  ",
                "  descripción ñ  ",
                ponderacionesValidas());

        assertThat(request.nombre()).isEqualTo("PREGUNTA MIXTA ÁÉÑ");
        assertThat(request.pregunta()).isEqualTo("TEXTO MIXTO ÚÜ");
        assertThat(request.descripcion()).isEqualTo("DESCRIPCIÓN Ñ");
    }

    @Test
    void utilizaBigDecimalParaLasPonderaciones()
            throws JacksonException {
        CrearPreguntaRequest request = deserializar(
                "NOMBRE",
                "PREGUNTA",
                null,
                ponderacionesValidas());

        assertThat(request.ponderaciones().getFirst().idItemOpcion())
                .isEqualTo(12L);
        assertThat(request.ponderaciones().getFirst().ponderacion())
                .isInstanceOf(BigDecimal.class)
                .isEqualByComparingTo("10.00");
    }

    private CrearPreguntaRequest deserializar(
            String nombre,
            String pregunta,
            String descripcion,
            JsonNode ponderaciones) throws JacksonException {
        return deserializarJson(
                solicitudJson(nombre, pregunta, descripcion, ponderaciones));
    }

    private CrearPreguntaRequest deserializarJson(ObjectNode json)
            throws JacksonException {
        return objectMapper.readValue(
                objectMapper.writeValueAsString(json),
                CrearPreguntaRequest.class);
    }

    private ObjectNode solicitudJson(
            String nombre,
            String pregunta,
            String descripcion,
            JsonNode ponderaciones) {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("codigoCompetencia", 1);
        json.put("codigoFactor", 1);
        json.put("nombre", nombre);
        json.put("pregunta", pregunta);
        if (descripcion == null) {
            json.putNull("descripcion");
        } else {
            json.put("descripcion", descripcion);
        }
        json.put("idOpcionRespuesta", 1L);
        json.set("ponderaciones", ponderaciones);
        return json;
    }

    private ArrayNode ponderacionesValidas() {
        ArrayNode ponderaciones = objectMapper.createArrayNode();
        ObjectNode elemento = ponderaciones.addObject();
        elemento.put("idItemOpcion", 12L);
        elemento.put("ponderacion", new BigDecimal("10.00"));
        return ponderaciones;
    }

    private Set<String> propiedadesInvalidas(CrearPreguntaRequest request) {
        return validator.validate(request).stream()
                .map(violacion -> violacion.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
