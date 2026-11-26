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
    SELECT c FROM Candidato c
    WHERE c.eliminado = false
      AND (:nroCandidato IS NOT NULL OR :nombre IS NOT NULL OR :apellido IS NOT NULL)
      AND (:nroCandidato IS NULL OR c.nroCandidato = :nroCandidato)
      AND (:nombre IS NULL OR lower(c.nombre) = :nombre)
      AND (:apellido IS NULL OR lower(c.apellido) = :apellido)
    """)
    List<Candidato> buscarCandidato(
        @Param("nroCandidato") Integer nroCandidato,
        @Param("nombre") String nombre,
        @Param("apellido") String apellido
);

    List<Candidato> findByNroCandidatoInAndEliminadoFalse(List<Integer> listaNroCandidato);
    
} 
