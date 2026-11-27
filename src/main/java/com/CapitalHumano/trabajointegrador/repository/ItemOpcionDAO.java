package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.ItemOpcion;

@Repository
public interface ItemOpcionDAO extends JpaRepository<ItemOpcion, Long> {

    List<ItemOpcion> findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
            Long idOpcionRespuesta);
}
