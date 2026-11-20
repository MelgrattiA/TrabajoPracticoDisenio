package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "factor",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_factor_competencia_orden",
                columnNames = {"codigo_competencia", "nro_orden"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factor {

    @Id
    @Column(name = "codigo", nullable = false)
    private Integer codigo;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "nro_orden", nullable = false)
    private Integer nroOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_competencia", nullable = false)
    private Competencia competencia;
}
