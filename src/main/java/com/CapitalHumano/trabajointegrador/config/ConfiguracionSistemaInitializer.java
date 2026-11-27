package com.CapitalHumano.trabajointegrador.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.CapitalHumano.trabajointegrador.entity.ConfiguracionSistema;
import com.CapitalHumano.trabajointegrador.repository.ConfiguracionSistemaDAO;

@Configuration
public class ConfiguracionSistemaInitializer {

    @Bean
    CommandLineRunner inicializarConfiguracionSistema(
            ConfiguracionSistemaDAO configuracionSistemaDAO) {
        return args -> configuracionSistemaDAO.findById(1).orElseGet(() ->
                configuracionSistemaDAO.save(ConfiguracionSistema.builder()
                        .idConfiguracion(1)
                        .instruccionesCuestionario(
                                "Responda todas las preguntas de cada bloque y seleccione una opción por pregunta. "
                                + "Puede avanzar al siguiente bloque cuando haya respondido todas las preguntas.")
                        .cantidadPreguntasPorBloque(2)
                        .cantidadPreguntasPorFactor(2)
                        .maximoAccesos(3)
                        .tiempoMaximoActivoDias(7)
                        .tiempoMaximoCompletarDias(1)
                        .build()));
    }
}
