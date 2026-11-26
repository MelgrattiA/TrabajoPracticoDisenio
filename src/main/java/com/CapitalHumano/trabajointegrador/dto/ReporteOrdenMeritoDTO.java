package com.CapitalHumano.trabajointegrador.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ReporteOrdenMeritoDTO(
        List<OrdenMeritoDTO> aprobados,
        List<OrdenMeritoDTO> desaprobados,
        Map<String, List<OrdenMeritoIncompletoDTO>> cuestionariosSinCompletarPorEstado) {

    public ReporteOrdenMeritoDTO {
        aprobados = List.copyOf(aprobados);
        desaprobados = List.copyOf(desaprobados);

        Map<String, List<OrdenMeritoIncompletoDTO>> estadosInmutables =
                new LinkedHashMap<>();
        cuestionariosSinCompletarPorEstado.forEach(
                (estado, cuestionarios) ->
                        estadosInmutables.put(estado, List.copyOf(cuestionarios)));
        cuestionariosSinCompletarPorEstado =
                Collections.unmodifiableMap(estadosInmutables);
    }
}
