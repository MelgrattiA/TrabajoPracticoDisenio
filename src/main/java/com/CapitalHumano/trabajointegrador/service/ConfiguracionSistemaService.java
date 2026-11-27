package com.CapitalHumano.trabajointegrador.service;

import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.entity.ConfiguracionSistema;
import com.CapitalHumano.trabajointegrador.repository.ConfiguracionSistemaDAO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfiguracionSistemaService {

    private final ConfiguracionSistemaDAO configuracionSistemaDAO;

    public ConfiguracionSistema obtenerConfiguracion() {
        ConfiguracionSistema configuracion = configuracionSistemaDAO.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "No está configurada la configuración del cuestionario"));
        if (configuracion.getInstruccionesCuestionario() == null
                || configuracion.getInstruccionesCuestionario().isBlank()
                || configuracion.getCantidadPreguntasPorBloque() == null
                || configuracion.getCantidadPreguntasPorBloque() <= 0
                || configuracion.getCantidadPreguntasPorFactor() == null
                || configuracion.getCantidadPreguntasPorFactor() <= 0
                || configuracion.getMaximoAccesos() == null
                || configuracion.getMaximoAccesos() <= 0
                || configuracion.getTiempoMaximoActivoDias() == null
                || configuracion.getTiempoMaximoActivoDias() <= 0
                || configuracion.getTiempoMaximoCompletarDias() == null
                || configuracion.getTiempoMaximoCompletarDias() <= 0) {
            throw new IllegalStateException(
                    "La configuración del cuestionario tiene valores inválidos");
        }
        return configuracion;
    }
}
