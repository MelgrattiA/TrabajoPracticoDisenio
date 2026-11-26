package com.CapitalHumano.trabajointegrador.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.CapitalHumano.trabajointegrador.entity.Candidato;

import java.util.List;

@Repository 
public interface CandidatoDAO extends JpaRepository <Candidato, Integer> {

    @Query("""
        SELECT c
        FROM Candidato c
        WHERE c.eliminado = false 
        AND ((:nro IS NOT NULL) OR (:nombre IS NOT NULL) OR (:apellido IS NOT NULL))
        AND (:nro IS NULL OR (c.nroCandidato) = :nro)
        AND (:nombre IS NULL OR LOWER(c.nombre) = LOWER(:nombre))
        AND (:apellido IS NULL OR LOWER(c.apellido) = LOWER(:apellido))
    """)
    List<Candidato> buscarCandidato(
        @Param ("nro") Integer nroCandidato,
        @Param ("nombre") String nombre,
        @Param ("apellido") String apellido 
    );

    List<Candidato> findByNroCandidatoInAndEliminadoFalse(List<Integer> listaNroCandidato);
    
} 
