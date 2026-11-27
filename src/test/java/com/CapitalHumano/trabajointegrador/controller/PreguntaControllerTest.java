package com.CapitalHumano.trabajointegrador.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.CapitalHumano.trabajointegrador.dto.CandidatoRestauracionPreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.ConflictoPreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.PonderacionItemResponseDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaCreadaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaEliminadaRestaurableDTO;
import com.CapitalHumano.trabajointegrador.exception.ApiExceptionHandler;
import com.CapitalHumano.trabajointegrador.exception.PreguntaConflictoException;
import com.CapitalHumano.trabajointegrador.exception.PreguntaNoEncontradaException;
import com.CapitalHumano.trabajointegrador.service.GestorPregunta;

@WebMvcTest(PreguntaController.class)
@Import(ApiExceptionHandler.class)
class PreguntaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestorPregunta gestorPregunta;

    @Test
    void creaPreguntaYDevuelve201() throws Exception {
        when(gestorPregunta.crearPregunta(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PreguntaCreadaDTO(
                        41,
                        3,
                        7,
                        "PREGUNTA",
                        "¿TEXTO?",
                        null,
                        12L,
                        List.of(new PonderacionItemResponseDTO(
                                18L,
                                "SI",
                                1,
                                new BigDecimal("10.00")))));

        mockMvc.perform(post("/api/preguntas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value(41))
                .andExpect(jsonPath("$.nombre").value("PREGUNTA"))
                .andExpect(jsonPath("$.ponderaciones[0].ponderacion")
                        .value(10.00));
    }

    @Test
    void requestInvalidoDevuelveErroresBeanValidationPorCampo()
            throws Exception {
        mockMvc.perform(post("/api/preguntas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoCompetencia": 3,
                                  "codigoFactor": 7,
                                  "nombre": " ",
                                  "pregunta": "¿TEXTO?",
                                  "idOpcionRespuesta": 12,
                                  "ponderaciones": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errores.nombre").isArray())
                .andExpect(jsonPath("$.errores.ponderaciones").isArray());
    }

    @Test
    void conflictoDeAltaDevuelve409ConCandidatosDeRestauracion()
            throws Exception {
        CandidatoRestauracionPreguntaDTO candidato =
                new CandidatoRestauracionPreguntaDTO(
                        22,
                        3,
                        7,
                        "PREGUNTA",
                        "¿TEXTO?",
                        null,
                        12L,
                        "OPCIÓN",
                        List.of(new PonderacionItemResponseDTO(
                                18L,
                                "SI",
                                1,
                                new BigDecimal("10.00"))));
        PreguntaEliminadaRestaurableDTO restauracion =
                new PreguntaEliminadaRestaurableDTO(
                        List.of(candidato),
                        "NOMBRE",
                        "Requiere restauración");
        when(gestorPregunta.crearPregunta(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new PreguntaConflictoException(
                        new ConflictoPreguntaDTO(
                                "PREGUNTA_REQUIERE_RESTAURACION",
                                "NOMBRE",
                                "Requiere restauración",
                                restauracion)));

        mockMvc.perform(post("/api/preguntas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo")
                        .value("PREGUNTA_REQUIERE_RESTAURACION"))
                .andExpect(jsonPath("$.campoCoincidencia").value("NOMBRE"))
                .andExpect(jsonPath("$.restauracion.candidatos[0].codigo")
                        .value(22));
    }

    @Test
    void restauraPreguntaYDevuelve200() throws Exception {
        when(gestorPregunta.restaurarPregunta(22))
                .thenReturn(new CandidatoRestauracionPreguntaDTO(
                        22,
                        3,
                        7,
                        "PREGUNTA",
                        "¿TEXTO?",
                        null,
                        12L,
                        "OPCIÓN",
                        List.of()));

        mockMvc.perform(post("/api/preguntas/22/restaurar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(22))
                .andExpect(jsonPath("$.idOpcionRespuesta").value(12));
    }

    @Test
    void preguntaInexistenteDevuelve404() throws Exception {
        when(gestorPregunta.restaurarPregunta(999))
                .thenThrow(new PreguntaNoEncontradaException(999));

        mockMvc.perform(post("/api/preguntas/999/restaurar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("La pregunta no existe; código: 999"));
    }

    @Test
    void preguntaYaRestauradaDevuelve409() throws Exception {
        when(gestorPregunta.restaurarPregunta(22))
                .thenThrow(new PreguntaConflictoException(
                        new ConflictoPreguntaDTO(
                                "PREGUNTA_YA_RESTAURADA",
                                "ESTADO",
                                "La pregunta ya fue restaurada",
                                null)));

        mockMvc.perform(post("/api/preguntas/22/restaurar"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo")
                        .value("PREGUNTA_YA_RESTAURADA"));
    }

    @Test
    void conservaLaBusquedaExistenteDelCu20() throws Exception {
        when(gestorPregunta.buscarPreguntas("Competencia", null, "texto"))
                .thenReturn(List.of(
                        new PreguntaDTO("Competencia", "Factor", "Pregunta")));

        mockMvc.perform(get("/api/preguntas")
                        .param("competencia", "Competencia")
                        .param("pregunta", "texto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].competencia").value("Competencia"))
                .andExpect(jsonPath("$[0].factor").value("Factor"))
                .andExpect(jsonPath("$[0].pregunta").value("Pregunta"));
    }

    private String requestValido() {
        return """
                {
                  "codigoCompetencia": 3,
                  "codigoFactor": 7,
                  "nombre": "PREGUNTA",
                  "pregunta": "¿TEXTO?",
                  "descripcion": null,
                  "idOpcionRespuesta": 12,
                  "ponderaciones": [
                    {
                      "idItemOpcion": 18,
                      "ponderacion": 10
                    }
                  ]
                }
                """;
    }
}
