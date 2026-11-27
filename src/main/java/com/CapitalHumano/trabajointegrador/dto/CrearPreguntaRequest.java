package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

import com.CapitalHumano.trabajointegrador.util.NormalizadorLiteralesPregunta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CrearPreguntaRequest(
        @NotNull @Positive Integer codigoCompetencia,
        @NotNull @Positive Integer codigoFactor,
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 150) String pregunta,
        @Size(max = 500) String descripcion,
        @NotNull @Positive Long idOpcionRespuesta,
        @NotEmpty List<@NotNull @Valid PonderacionItemRequest> ponderaciones) {

    public CrearPreguntaRequest {
        nombre = NormalizadorLiteralesPregunta.normalizarObligatorio(nombre);
        pregunta = NormalizadorLiteralesPregunta.normalizarObligatorio(pregunta);
        descripcion = NormalizadorLiteralesPregunta.normalizarDescripcion(descripcion);
    }

}
