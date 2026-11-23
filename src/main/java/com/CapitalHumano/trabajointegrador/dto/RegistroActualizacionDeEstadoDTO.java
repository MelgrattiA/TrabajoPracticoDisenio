package com.CapitalHumano.trabajointegrador.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistroActualizacionDeEstadoDTO {

    private LocalDate fecha;
    private LocalTime hora;
    private String accion;
}
