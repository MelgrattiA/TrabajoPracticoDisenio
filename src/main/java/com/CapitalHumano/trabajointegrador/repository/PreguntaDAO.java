package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Pregunta;

@Repository 
public interface PreguntaDAO extends JpaRepository <Pregunta, Integer> {

    List<Pregunta> findByFactorCodigoAndEliminadoFalse(
            Integer codigoFactor);

   @Query("""
    SELECT p FROM Pregunta p
    JOIN p.factor f
    JOIN f.competencia c
    WHERE p.eliminado = false
      AND (:competencia IS NOT NULL OR :factor IS NOT NULL OR :nombrePregunta IS NOT NULL)
      AND (:competencia IS NULL OR lower(c.nombre) LIKE :competencia)
      AND (:factor IS NULL OR lower(f.nombre) LIKE :factor)
      AND (:nombrePregunta IS NULL OR lower(p.nombre) LIKE :nombrePregunta)
    """)
    List<Pregunta> buscarPreguntas(
        @Param("competencia") String competencia,
        @Param("factor") String factor,
        @Param("nombrePregunta") String nombrePregunta
    );
    
    
    
} 
    
