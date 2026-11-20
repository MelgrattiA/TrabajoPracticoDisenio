package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "puesto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Puesto {

    @Id
    @Column(name = "codigo_puesto", nullable = false)
    private String codigoPuesto;

    @Column(name = "nombre_puesto")
    private String nombrePuesto;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "empresa")
    private String empresa;
}
