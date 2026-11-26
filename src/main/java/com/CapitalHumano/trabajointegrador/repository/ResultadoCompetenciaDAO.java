package com.CapitalHumano.trabajointegrador.repository;

import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetencia;
import com.CapitalHumano.trabajointegrador.entity.ResultadoCompetenciaId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoCompetenciaDAO
        extends JpaRepository<ResultadoCompetencia, ResultadoCompetenciaId> {

    @Query("""
            SELECT rc
            FROM ResultadoCompetencia rc
            WHERE rc.id.codigoPuesto = :codigoPuesto
              AND rc.id.claveCuestionario IN :clavesCuestionarios
            ORDER BY rc.id.claveCuestionario, rc.id.codigoCompetencia
            """)
    List<ResultadoCompetencia> buscarPorPuestoYCuestionarios(
            @Param("codigoPuesto") String codigoPuesto,
            @Param("clavesCuestionarios") List<String> clavesCuestionarios);
}