package com.CapitalHumano.trabajointegrador.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.PonderacionItem;

@Repository
public interface PonderacionItemDAO extends JpaRepository<PonderacionItem, Long> {

    Optional<PonderacionItem> findByPreguntaCodigoAndItemOpcionIdItemOpcion(
            Integer codigoPregunta,
            Long idItemOpcion);

    @EntityGraph(attributePaths = "itemOpcion")
    List<PonderacionItem> findByPreguntaCodigoIn(
            Collection<Integer> codigosPregunta);
}