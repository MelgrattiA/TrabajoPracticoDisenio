package com.CapitalHumano.trabajointegrador.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.ConfiguracionSistema;

@Repository
public interface ConfiguracionSistemaDAO
        extends JpaRepository<ConfiguracionSistema, Integer> {
}
