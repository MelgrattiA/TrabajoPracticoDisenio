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
        name = "bloque_pregunta",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_bloque_pregunta_orden",
                columnNames = {"clave_cuestionario", "nro_orden"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloquePregunta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bloque_pregunta", nullable = false)
    private Long idBloquePregunta;

    @Column(name = "nro_orden", nullable = false)
    private Integer nroOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clave_cuestionario", nullable = false)
    private Cuestionario cuestionario;
}
