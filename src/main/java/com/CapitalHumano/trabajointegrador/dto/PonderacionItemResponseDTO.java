package com.CapitalHumano.trabajointegrador.dto;

import java.math.BigDecimal;

public record PonderacionItemResponseDTO(
        Long idItemOpcion,
        String descripcion,
        Integer ordenVisualizacion,
        BigDecimal ponderacion) {
}
