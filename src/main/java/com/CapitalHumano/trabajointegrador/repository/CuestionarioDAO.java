package com.CapitalHumano.trabajointegrador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Cuestionario;

@Repository
public interface CuestionarioDAO extends JpaRepository<Cuestionario, String> {

    @Query("""
            SELECT c
            FROM Cuestionario c
            WHERE c.candidato.nroCandidato = :nroCandidato
              AND c.estado.idEstadoCuestionario IN (1, 2)
            """)
    Optional<Cuestionario> buscarActivoOEnProceso(
            @Param("nroCandidato") Integer nroCandidato);
}
