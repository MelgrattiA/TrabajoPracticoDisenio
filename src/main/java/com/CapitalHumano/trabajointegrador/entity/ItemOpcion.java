package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

@Entity
@Table(
        name = "item_opcion",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_item_opcion_orden",
                    columnNames = {"id_opcion_respuesta", "orden_visualizacion"}),
            @UniqueConstraint(
                    name = "uk_item_opcion_descripcion",
                    columnNames = {"id_opcion_respuesta", "descripcion"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemOpcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_opcion", nullable = false)
    private Long idItemOpcion;

    @Column(name = "orden_visualizacion", nullable = false)
    private Integer ordenVisualizacion;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_opcion_respuesta", nullable = false)
    private OpcionRespuesta opcionRespuesta;
}
