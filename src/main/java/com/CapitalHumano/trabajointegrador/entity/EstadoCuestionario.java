package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "estado_cuestionario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estado_cuestionario_nombre",
                columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoCuestionario {

    @Id
    @Column(name = "id_estado_cuestionario", nullable = false)
    private Integer idEstadoCuestionario;

    @Column(name = "nombre", nullable = false)
    private String nombre;
}
