package com.CapitalHumano.trabajointegrador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Id;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "cuestionario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cuestionario_clave_puesto",
                columnNames = {"clave", "codigo_puesto"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cuestionario {

    private static final int MAXIMO_ACCESOS = 3;
    private static final Duration TIEMPO_MAXIMO = Duration.ofDays(2);

    @Id
    @Column(name = "clave", nullable = false)
    private String clave;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_ultimo_ingreso")
    private LocalDateTime fechaUltimoIngreso;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "cantidad_accesos")
    private Integer cantidadAccesos;

    @Column(name = "puntaje_final", precision = 38, scale = 2)
    private BigDecimal puntajeFinal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nro_candidato", nullable = false)
    private Candidato candidato;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "codigo_puesto", nullable = false)
    private Puesto puesto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoCuestionario estado;

    public boolean verificarAcceso() {

        //si  la cantidad de accesos es null, se pone en 0
        if (cantidadAccesos == null) {
            cantidadAccesos = 0;
        }

        return cantidadAccesos < MAXIMO_ACCESOS;
    }

    public boolean verificarTiemposLimites() {
        if (fechaInicio == null) {
            return true;
        }

        Duration tiempoTranscurrido =
                Duration.between(fechaInicio, LocalDateTime.now());
        return tiempoTranscurrido.compareTo(TIEMPO_MAXIMO) <= 0;
    }
}
