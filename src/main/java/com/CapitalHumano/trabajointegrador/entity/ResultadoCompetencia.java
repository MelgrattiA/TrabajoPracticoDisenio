package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resultado_competencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoCompetencia {

    @EmbeddedId
    private ResultadoCompetenciaId id;

    @Column(name = "puntaje_obtenido", nullable = false, precision = 38, scale = 2)
    private BigDecimal puntajeObtenido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                @JoinColumn(
                        name = "clave_cuestionario",
                        referencedColumnName = "clave",
                        insertable = false,
                        updatable = false),
                @JoinColumn(
                        name = "codigo_puesto",
                        referencedColumnName = "codigo_puesto",
                        insertable = false,
                        updatable = false)
            },
            foreignKey = @ForeignKey(name = "fk_resultado_cuestionario_puesto"))
    private Cuestionario cuestionario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(
            value = {
                @JoinColumn(
                        name = "codigo_puesto",
                        referencedColumnName = "codigo_puesto",
                        insertable = false,
                        updatable = false),
                @JoinColumn(
                        name = "codigo_competencia",
                        referencedColumnName = "codigo_competencia",
                        insertable = false,
                        updatable = false)
            },
            foreignKey = @ForeignKey(name = "fk_resultado_caracteristica_puesto"))
    private CaracteristicaPuesto caracteristicaPuesto;
}
