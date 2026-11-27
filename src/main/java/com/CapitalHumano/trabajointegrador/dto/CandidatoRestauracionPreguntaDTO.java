package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

public record CandidatoRestauracionPreguntaDTO(
        Integer codigo,
        Integer codigoCompetencia,
        Integer codigoFactor,
        String nombre,
        String pregunta,
        String descripcion,
        Long idOpcionRespuesta,
        String nombreOpcionRespuesta,
        List<PonderacionItemResponseDTO> ponderaciones) {
}
