package com.CapitalHumano.trabajointegrador.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrdenMeritoDTO(
        String claveCuestionario,
        Integer posicion,
        String nombre,
        String apellido,
        String tipoDocumento,
        Integer nroDocumento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFinalizacion,
        Integer cantidadAccesos,
        BigDecimal puntajeObtenido,
        Boolean superaMinimo) {
}
