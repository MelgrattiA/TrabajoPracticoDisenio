package com.CapitalHumano.trabajointegrador.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.CapitalHumano.trabajointegrador.dto.CandidatoRestauracionPreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.CompetenciaCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.CrearPreguntaRequest;
import com.CapitalHumano.trabajointegrador.dto.FactorCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.ItemOpcionCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.OpcionRespuestaCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaCreadaDTO;
import com.CapitalHumano.trabajointegrador.dto.PreguntaDTO;
import com.CapitalHumano.trabajointegrador.service.GestorPregunta;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

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

    @PostMapping
    public ResponseEntity<PreguntaCreadaDTO> crearPregunta(
            @Valid @RequestBody CrearPreguntaRequest solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gestorPregunta.crearPregunta(solicitud));
    }

    @PostMapping("/{codigo}/restaurar")
    public ResponseEntity<CandidatoRestauracionPreguntaDTO> restaurarPregunta(
            @PathVariable Integer codigo) {
        return ResponseEntity.ok(gestorPregunta.restaurarPregunta(codigo));
    }

    @GetMapping("/competencias")
    public ResponseEntity<List<CompetenciaCatalogoDTO>> listarCompetencias() {
        return ResponseEntity.ok(gestorPregunta.listarCompetenciasDisponibles());
    }

    @GetMapping("/competencias/{codigoCompetencia}/factores")
    public ResponseEntity<List<FactorCatalogoDTO>> listarFactores(
            @PathVariable Integer codigoCompetencia) {
        return ResponseEntity.ok(
                gestorPregunta.listarFactoresDisponibles(codigoCompetencia));
    }

    @GetMapping("/opciones-respuesta")
    public ResponseEntity<List<OpcionRespuestaCatalogoDTO>> listarOpcionesRespuesta() {
        return ResponseEntity.ok(
                gestorPregunta.listarOpcionesRespuestaDisponibles());
    }

    @GetMapping("/opciones-respuesta/{idOpcionRespuesta}/items")
    public ResponseEntity<List<ItemOpcionCatalogoDTO>> listarItemsOpcionRespuesta(
            @PathVariable Long idOpcionRespuesta) {
        return ResponseEntity.ok(
                gestorPregunta.listarItemsDisponibles(idOpcionRespuesta));
    }
}
