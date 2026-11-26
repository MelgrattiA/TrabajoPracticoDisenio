package com.CapitalHumano.trabajointegrador.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PuestoOrdenMeritoDTO {

    private String codigo;
    private String nombrePuesto;
    private String empresa;
    private Long candidatos;
    private Long evaluacionesCompletas;
}
