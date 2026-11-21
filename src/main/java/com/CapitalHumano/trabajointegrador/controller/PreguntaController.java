package com.CapitalHumano.trabajointegrador.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CapitalHumano.trabajointegrador.service.GestorPregunta;
import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;

import java.util.List;

@RestController 
@RequestMapping("/api/preguntas")
public class PreguntaController {
    
    @Autowired
    private GestorPregunta gestorPregunta;

    @GetMapping 
    public ResponseEntity<?> buscarPreguntas(
        @RequestParam(required = false) String competencia,
        @RequestParam(required = false) String factor,
        @RequestParam(required = false) String pregunta){
            try {
                List<PreguntaDTO> preguntaLista = gestorPregunta.buscarPreguntas(competencia, factor, pregunta);
                return ResponseEntity.ok(preguntaLista);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }
}
