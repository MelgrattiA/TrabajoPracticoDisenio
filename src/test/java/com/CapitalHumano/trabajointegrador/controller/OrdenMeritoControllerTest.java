package com.CapitalHumano.trabajointegrador.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import com.CapitalHumano.trabajointegrador.dto.PuestoOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.dto.ReporteOrdenMeritoDTO;
import com.CapitalHumano.trabajointegrador.exception.ApiExceptionHandler;
import com.CapitalHumano.trabajointegrador.exception.PuestoNoEncontradoException;
import com.CapitalHumano.trabajointegrador.exception.PuestoSinCompetenciasException;
import com.CapitalHumano.trabajointegrador.service.GestorOrdenMerito;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrdenMeritoController.class)
@Import(ApiExceptionHandler.class)
class OrdenMeritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GestorOrdenMerito gestorOrdenMerito;

    @Test
    void buscaPuestosYDevuelveDtos() throws Exception {
        when(gestorOrdenMerito.buscarPuestos("P-100", null, null))
                .thenReturn(List.of(
                        new PuestoOrdenMeritoDTO(
                                "P-100", "Analista", "Acme", 2L, 3L)));

        mockMvc.perform(get("/api/orden-merito/puestos")
                        .param("codigo", "P-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("P-100"))
                .andExpect(jsonPath("$[0].candidatos").value(2));
    }

    @Test
    void devuelveBadRequestCuandoLaBusquedaNoTieneCriterios() throws Exception {
        when(gestorOrdenMerito.buscarPuestos(null, null, null))
                .thenThrow(new IllegalArgumentException(
                        "Debe ingresar al menos un criterio de búsqueda"));

        mockMvc.perform(get("/api/orden-merito/puestos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("Debe ingresar al menos un criterio de búsqueda"));
    }

    @Test
    void generaReporteDelPuestoSeleccionado() throws Exception {
        when(gestorOrdenMerito.generarReporteOrdenMerito("P-100"))
                .thenReturn(new ReporteOrdenMeritoDTO(
                        List.of(), List.of(), Map.of("Activo", List.of())));

        mockMvc.perform(get(
                        "/api/orden-merito/puestos/P-100/reporte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aprobados").isEmpty())
                .andExpect(jsonPath("$.cuestionariosSinCompletarPorEstado.Activo")
                        .isArray());
    }

    @Test
    void devuelveNotFoundParaPuestoInexistente() throws Exception {
        when(gestorOrdenMerito.generarReporteOrdenMerito("P-999"))
                .thenThrow(new PuestoNoEncontradoException("P-999"));

        mockMvc.perform(get(
                        "/api/orden-merito/puestos/P-999/reporte"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("No se encontró el puesto: P-999"));
    }

    @Test
    void devuelveUnprocessableEntitySiElPuestoNoTieneCompetencias()
            throws Exception {
        when(gestorOrdenMerito.generarReporteOrdenMerito("P-100"))
                .thenThrow(new PuestoSinCompetenciasException());

        mockMvc.perform(get(
                        "/api/orden-merito/puestos/P-100/reporte"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.detail")
                        .value("El puesto no tiene competencias configuradas"));
    }
}
