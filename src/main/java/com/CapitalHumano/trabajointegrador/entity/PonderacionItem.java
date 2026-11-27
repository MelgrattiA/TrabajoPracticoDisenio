package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import java.math.BigDecimal;

@Entity
@Table(
        name = "ponderacion_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ponderacion_pregunta_item",
                columnNames = {"codigo_pregunta", "id_item_opcion"}),
        check = @CheckConstraint(
                name = "ck_ponderacion_item_no_negativa",
                constraint = "ponderacion >= 0"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PonderacionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ponderacion_item", nullable = false)
    private Long idPonderacionItem;

    @Column(name = "ponderacion", nullable = false, precision = 38, scale = 2)
    private BigDecimal ponderacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "codigo_pregunta",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ponderacion_item_pregunta"))
    private Pregunta pregunta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_item_opcion",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ponderacion_item_opcion"))
    private ItemOpcion itemOpcion;
}
