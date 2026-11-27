package com.CapitalHumano.trabajointegrador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.BloquePregunta;

@Repository
public interface BloquePreguntaDAO
        extends JpaRepository<BloquePregunta, Long> {

    boolean existsByCuestionarioClave(String claveCuestionario);

    long countByCuestionarioClave(String claveCuestionario);

    @Query("""
            SELECT DISTINCT b
            FROM BloquePregunta b
            JOIN PreguntaBloque pb ON pb.bloquePregunta.idBloquePregunta =
                                      b.idBloquePregunta
            WHERE b.cuestionario.clave = :claveCuestionario
              AND pb.itemOpcionSeleccionada IS NULL
            ORDER BY b.nroOrden ASC
            """)
    Optional<BloquePregunta> buscarBloquePendiente(
            @Param("claveCuestionario") String claveCuestionario);
}
