package com.CapitalHumano.trabajointegrador.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CapitalHumano.trabajointegrador.dto.BloquePreguntaDTO;
import com.CapitalHumano.trabajointegrador.dto.EstadoInstruccionesCuestionarioDTO;
import com.CapitalHumano.trabajointegrador.dto.ResponderBloqueDTO;
import com.CapitalHumano.trabajointegrador.dto.RespuestaBloqueDTO;
import com.CapitalHumano.trabajointegrador.service.GestorCuestionario;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cuestionarios")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class CuestionarioController {
    private final GestorCuestionario gestorCuestionario;

    @GetMapping("/{clave}/bloque-pendiente")
    public ResponseEntity<BloquePreguntaDTO> obtenerBloquePendiente(
            @PathVariable String clave) {
        return ResponseEntity.ok(gestorCuestionario.obtenerBloquePendiente(clave));
    }

    @PostMapping("/{clave}/iniciar")
    public ResponseEntity<BloquePreguntaDTO> iniciarCuestionario(
            @PathVariable String clave) {
        return ResponseEntity.ok(gestorCuestionario.iniciarCuestionario(clave));
    }

    @GetMapping("/{clave}/instrucciones")
    public ResponseEntity<EstadoInstruccionesCuestionarioDTO> obtenerEstadoInstrucciones(
            @PathVariable String clave) {
        return ResponseEntity.ok(gestorCuestionario.obtenerEstadoInstrucciones(clave));
    }

    @PostMapping("/{clave}/bloques/{idBloque}/respuestas")
    public ResponseEntity<RespuestaBloqueDTO> responderBloque(
            @PathVariable String clave,
            @PathVariable Long idBloque,
            @RequestBody ResponderBloqueDTO solicitud) {
        return ResponseEntity.ok(gestorCuestionario.responderBloque(
                clave,
                idBloque,
                solicitud == null ? null : solicitud.getRespuestas()));
    }
}
