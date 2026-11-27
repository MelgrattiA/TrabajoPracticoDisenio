package com.CapitalHumano.trabajointegrador.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OpcionRespuestaDTO {
    private Long idItemOpcion;
    private String descripcion;
    private Integer ordenVisualizacion;
}
