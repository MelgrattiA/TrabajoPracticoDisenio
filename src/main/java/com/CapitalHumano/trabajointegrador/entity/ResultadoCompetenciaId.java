package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ResultadoCompetenciaId implements Serializable {

    @Column(name = "clave_cuestionario", nullable = false)
    private String claveCuestionario;

    @Column(name = "codigo_puesto", nullable = false)
    private String codigoPuesto;

    @Column(name = "codigo_competencia", nullable = false)
    private Integer codigoCompetencia;
}
