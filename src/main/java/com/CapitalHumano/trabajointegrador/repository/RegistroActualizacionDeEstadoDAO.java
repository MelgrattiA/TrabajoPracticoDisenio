package com.CapitalHumano.trabajointegrador.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.RegistroActualizacionDeEstado;

@Repository
public interface RegistroActualizacionDeEstadoDAO
        extends JpaRepository<RegistroActualizacionDeEstado, Long> {
}
