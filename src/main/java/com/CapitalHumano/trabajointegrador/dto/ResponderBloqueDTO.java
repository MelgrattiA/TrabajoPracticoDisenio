package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponderBloqueDTO {
    private List<RespuestaPreguntaDTO> respuestas;
}
