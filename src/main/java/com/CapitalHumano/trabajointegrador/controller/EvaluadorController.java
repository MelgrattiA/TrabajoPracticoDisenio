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

import com.CapitalHumano.trabajointegrador.dto.CandidatoClaveDTO;
import com.CapitalHumano.trabajointegrador.dto.CandidatoDTO;
import com.CapitalHumano.trabajointegrador.dto.CompetenciaPonderadaDTO;
import com.CapitalHumano.trabajointegrador.service.EvaluadorCandidato;
import com.CapitalHumano.trabajointegrador.dto.PuestoEvaluadoDTO;

import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping("/api/evaluar")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class EvaluadorController {
    private final EvaluadorCandidato evaluadorCandidato;

    @GetMapping 
    public ResponseEntity<?> buscarCandidato(
        @RequestParam(required = false) Integer nroCandidato,
        @RequestParam(required = false) String nombre,
        @RequestParam(required = false) String apellido
    ){
        try {
            List<CandidatoDTO> candidatoLista = evaluadorCandidato.buscarCandidato(nroCandidato, nombre, apellido);
            return ResponseEntity.ok(candidatoLista);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validarCandidatos(@RequestBody List<Integer> listaIdCandidatos){
        try{
            List<Integer> candidatosNoValidos = evaluadorCandidato.validarCandidatos(listaIdCandidatos);
            
            if(!candidatosNoValidos.isEmpty()){
                return ResponseEntity.status(HttpStatus.CONFLICT).body(candidatosNoValidos);
            }

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/puestos")
    public ResponseEntity<List<PuestoEvaluadoDTO>> obtenerPuestos(){
        return ResponseEntity.ok(evaluadorCandidato.obtenerPuestos());
    }

    @GetMapping("/puestos/{codPuesto}/competencias")
    public ResponseEntity<?> obtenerCompetencias (@PathVariable String codPuesto){
        try {
            List<CompetenciaPonderadaDTO> competencias = evaluadorCandidato.buscarCompetenciasEvaluarValidas(codPuesto);
            
            return ResponseEntity.ok(competencias);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        
    }

    @PostMapping("/candidatos-a-evaluar")
    public ResponseEntity<List<CandidatoClaveDTO>> obtenerCandidatos (@RequestBody List<Integer> idCandidatos){
        List<CandidatoClaveDTO> candidatos = evaluadorCandidato.buscarCandidatosYGenerarClaves(idCandidatos);
        return ResponseEntity.ok(candidatos);
    }

    @PostMapping("/finalizar")
    public ResponseEntity<?> finalizarEvaluacion(
        @RequestParam String codPuesto,
        @RequestBody List<CandidatoClaveDTO> candidatos)
    {
        evaluadorCandidato.guardarInformacion(candidatos, codPuesto);
        return ResponseEntity.ok().build();
    }
    


}
