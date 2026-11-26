package com.CapitalHumano.trabajointegrador.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ClasificacionCuestionariosDTO(
        List<String> cuestionariosAprobados,
        List<String> cuestionariosDesaprobados,
        Map<String, List<String>> cuestionariosSinCompletarPorEstado,
        Map<String, InconsistenciaCuestionarioDTO> cuestionariosInconsistentes) {

    public ClasificacionCuestionariosDTO {
        cuestionariosAprobados = List.copyOf(cuestionariosAprobados);
        cuestionariosDesaprobados = List.copyOf(cuestionariosDesaprobados);

        Map<String, List<String>> estadosInmutables = new LinkedHashMap<>();
        cuestionariosSinCompletarPorEstado.forEach(
                (estado, claves) -> estadosInmutables.put(estado, List.copyOf(claves)));
        cuestionariosSinCompletarPorEstado =
                Collections.unmodifiableMap(estadosInmutables);

        cuestionariosInconsistentes = Map.copyOf(cuestionariosInconsistentes);
    }

    public record InconsistenciaCuestionarioDTO(
            List<Integer> codigosCompetenciasFaltantes,
            boolean puntajeFinalAusente) {

        public InconsistenciaCuestionarioDTO {
            codigosCompetenciasFaltantes =
                    List.copyOf(codigosCompetenciasFaltantes);
        }
    }
}
