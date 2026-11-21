package com.CapitalHumano.trabajointegrador.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;

import java.util.ArrayList;
import java.util.List;

@Service 
public class GestorPregunta {
    @Autowired 
    private PreguntaDAO preguntaDAO;

    public List<PreguntaDTO> buscarPreguntas( String competencia, String factor, String pregunta){
        boolean sinFiltros = ((competencia == null || competencia.isBlank()) && 
                              (factor == null || factor.isBlank()) &&
                              (pregunta == null || pregunta.isBlank()));
        
        if(sinFiltros){
            throw new IllegalArgumentException("Debe ingresar al menos un argumento");
        }

        List<Pregunta> preguntaLista = preguntaDAO.buscarPreguntas(competencia, factor, pregunta);

        ArrayList<PreguntaDTO> PreguntaDTOLista = new ArrayList<PreguntaDTO>();

        for (Pregunta p: preguntaLista){
            PreguntaDTOLista.add(generarPreguntaDTO(p));
        }

        return PreguntaDTOLista;

    }

    public PreguntaDTO generarPreguntaDTO(Pregunta pregunta){
        String competencia = pregunta.getFactor().getCompetencia().getNombre();
        String factor = pregunta.getFactor().getNombre();
        String nomPregunta = pregunta.getNombre();
        
        return new PreguntaDTO(competencia, factor, nomPregunta);
    }
}
