package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreguntaBloqueDTO {
    private Long idPreguntaBloque;
    private Integer ordenEnBloque;
    private Integer codigoPregunta;
    private String nombrePregunta;
    private String pregunta;
    private List<OpcionRespuestaDTO> opciones;
}
