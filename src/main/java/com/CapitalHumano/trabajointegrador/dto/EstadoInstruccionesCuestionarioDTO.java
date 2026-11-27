package com.CapitalHumano.trabajointegrador.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EstadoInstruccionesCuestionarioDTO {
    private String estadoPantalla;
    private String mensaje;
    private boolean puedeContinuar;
    private String instrucciones;
    private Integer cantidadPreguntasPorBloque;
    private Integer cantidadPreguntasPorFactor;
    private Integer maximoAccesos;
    private Integer tiempoMaximoCompletarDias;
    private Integer tiempoMaximoActivoDias;
}
