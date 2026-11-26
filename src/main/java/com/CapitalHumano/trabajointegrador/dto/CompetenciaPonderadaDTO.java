package com.CapitalHumano.trabajointegrador.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CompetenciaPonderadaDTO {
    private String competenciaNombre;
    private BigDecimal ponderacion;
}
