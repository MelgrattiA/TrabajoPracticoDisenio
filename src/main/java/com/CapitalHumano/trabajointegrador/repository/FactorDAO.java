package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Factor;

@Repository
public interface FactorDAO extends JpaRepository<Factor, Integer> {

    List<Factor> findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(
            Integer codigoCompetencia);
}
