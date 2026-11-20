package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "candidato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Candidato {

    @Id
    @Column(name = "nro_candidato", nullable = false)
    private Integer nroCandidato;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    @Column(name = "nro_documento")
    private Integer nroDocumento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "genero")
    private String genero;

    @Column(name = "nacionalidad")
    private String nacionalidad;

    @Column(name = "email")
    private String email;

    @Column(name = "escolaridad")
    private String escolaridad;

    @Column(name = "eliminado")
    private Boolean eliminado;
}
