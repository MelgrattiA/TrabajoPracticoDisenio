package com.CapitalHumano.trabajointegrador.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PonderacionItemRequest(
        @NotNull @Positive Long idItemOpcion,
        @NotNull BigDecimal ponderacion) {
}
