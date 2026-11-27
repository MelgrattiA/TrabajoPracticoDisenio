package com.CapitalHumano.trabajointegrador.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RespuestaBloqueDTO {
    private boolean guardado;
    private boolean cuestionarioCompleto;
    private BloquePreguntaDTO siguienteBloque;
    private List<Long> preguntasFaltantes;
}
