package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BloquePreguntaDTO {
    private Long idBloquePregunta;
    private Integer nroOrden;
    private Long cantidadBloques;
    private List<PreguntaBloqueDTO> preguntas;
}
