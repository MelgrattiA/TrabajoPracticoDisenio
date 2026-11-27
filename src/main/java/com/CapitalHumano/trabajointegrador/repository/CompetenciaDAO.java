package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Competencia;

@Repository
public interface CompetenciaDAO extends JpaRepository<Competencia, Integer> {

    List<Competencia> findByEliminadoFalseOrderByCodigoAsc();
}
