package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.OpcionRespuesta;

@Repository
public interface OpcionRespuestaDAO
        extends JpaRepository<OpcionRespuesta, Long> {

    List<OpcionRespuesta> findByEliminadoFalseOrderByIdOpcionRespuestaAsc();
}
