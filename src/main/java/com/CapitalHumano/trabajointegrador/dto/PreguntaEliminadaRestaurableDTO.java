package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

public record PreguntaEliminadaRestaurableDTO(
        List<CandidatoRestauracionPreguntaDTO> candidatos,
        String campoCoincidencia,
        String mensaje) {
}
