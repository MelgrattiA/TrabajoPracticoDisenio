package com.CapitalHumano.trabajointegrador.service;

import org.springframework.stereotype.Service;

import com.CapitalHumano.trabajointegrador.repository.CompetenciaDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;

import lombok.RequiredArgsConstructor;

import com.CapitalHumano.trabajointegrador.dto.CompetenciaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor 
public class GestorPregunta { 
    private final PreguntaDAO preguntaDAO;
    private final CompetenciaDAO competenciaDAO;

    public List<CompetenciaDTO> obtenerCompetencia(){
        List<Competencia> competenciaLista = competenciaDAO.buscarCompetenciasActivas(); 
        List<CompetenciaDTO> competenciaDTOs = new ArrayList<CompetenciaDTO>();

        for(Competencia c: competenciaLista){
            competenciaDTOs.add(generarCompetenciaDTO(c));
        }

        return competenciaDTOs;
    }

    public CompetenciaDTO generarCompetenciaDTO(Competencia c){
        Integer codigo = c.getCodigo();
        String nombre = c.getNombre();

        return new CompetenciaDTO(codigo, nombre);
    }

    public List<PreguntaDTO> buscarPreguntas( String competencia, String factor, String pregunta){
        String compFiltro = (competencia == null || competencia.isBlank()) ? null : "%" + competencia.trim().toLowerCase() + "%";
        String factorFiltro = (factor == null || factor.isBlank()) ? null : "%" + factor.trim().toLowerCase() + "%";
        String nombreFiltro = (pregunta == null || pregunta.isBlank()) ? null : "%" + pregunta.trim().toLowerCase() + "%";

        if (compFiltro == null && factorFiltro == null && nombreFiltro == null) {
            throw new IllegalArgumentException("Debe ingresar al menos un criterio de búsqueda.");
        }

        List<Pregunta> preguntaLista = preguntaDAO.buscarPreguntas(compFiltro, factorFiltro, nombreFiltro);
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
