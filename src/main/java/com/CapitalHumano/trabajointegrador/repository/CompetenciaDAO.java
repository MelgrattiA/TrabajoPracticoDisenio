package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Competencia;

@Repository 
public interface CompetenciaDAO extends JpaRepository<Competencia, Integer> {

    @Query("""
        SELECT c 
        FROM Competencia c
        WHERE c.eliminado = false
        ORDER BY c.nombre ASC
    """)
    List<Competencia> buscarCompetenciasActivas();
}
