package com.CapitalHumano.trabajointegrador.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.EstadoCuestionario;

@Repository
public interface EstadoCuestionarioDAO
        extends JpaRepository<EstadoCuestionario, Integer> {
}
