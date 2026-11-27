package com.CapitalHumano.trabajointegrador.dto;

public record ConflictoPreguntaDTO(
        String codigo,
        String campoCoincidencia,
        String mensaje,
        PreguntaEliminadaRestaurableDTO restauracion) {
}
