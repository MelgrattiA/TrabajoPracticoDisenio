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
@Table(name = "configuracion_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionSistema {

    @Id
    @Column(name = "id_configuracion", nullable = false)
    private Integer idConfiguracion;

    @Column(name = "instrucciones_cuestionario", nullable = false, columnDefinition = "TEXT")
    private String instruccionesCuestionario;

    @Column(name = "cantidad_preguntas_por_bloque", nullable = false)
    private Integer cantidadPreguntasPorBloque;

    @Column(name = "cantidad_preguntas_por_factor", nullable = false)
    private Integer cantidadPreguntasPorFactor;

    @Column(name = "maximo_accesos", nullable = false)
    private Integer maximoAccesos;

    @Column(name = "tiempo_maximo_activo_dias", nullable = false)
    private Integer tiempoMaximoActivoDias;

    @Column(name = "tiempo_maximo_completar_dias", nullable = false)
    private Integer tiempoMaximoCompletarDias;
}
