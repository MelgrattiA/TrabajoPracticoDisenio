package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

public record PreguntaCreadaDTO(
        Integer codigo,
        Integer codigoCompetencia,
        Integer codigoFactor,
        String nombre,
        String pregunta,
        String descripcion,
        Long idOpcionRespuesta,
        List<PonderacionItemResponseDTO> ponderaciones) {
}
