package com.CapitalHumano.trabajointegrador.dto;

import java.time.LocalDateTime;

public record OrdenMeritoIncompletoDTO(
        String claveCuestionario,
        String nombre,
        String apellido,
        String tipoDocumento,
        Integer nroDocumento,
        LocalDateTime fechaInicio,
        LocalDateTime fechaUltimoIngreso,
        Integer cantidadAccesos) {
}
