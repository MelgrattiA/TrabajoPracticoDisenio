package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
        name = "opcion_respuesta",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_opcion_respuesta_nombre",
                columnNames = "nombre"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpcionRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_opcion_respuesta", nullable = false)
    private Long idOpcionRespuesta;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "eliminado", nullable = false)
    private Boolean eliminado;
}
