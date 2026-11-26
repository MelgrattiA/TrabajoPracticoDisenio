package com.CapitalHumano.trabajointegrador.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CapitalHumano.trabajointegrador.service.GestorPregunta;

import lombok.RequiredArgsConstructor;

import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;

import java.util.List;

@RestController 
@RequestMapping("/api/preguntas")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor 
public class PreguntaController {
    private final GestorPregunta gestorPregunta;
   

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
