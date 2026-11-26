package com.CapitalHumano.trabajointegrador.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor 
public class CandidatoClaveDTO {
    private Integer nro_candidato;
    private String nombre;
    private String apellido;
    private String tipoDocumento;
    private Integer nroDocumento;
    private String claveGenerada;
}