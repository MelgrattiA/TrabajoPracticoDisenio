package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuesto;
import com.CapitalHumano.trabajointegrador.entity.CaracteristicaPuestoId;
import com.CapitalHumano.trabajointegrador.entity.Competencia;

@Repository
public interface CaracteristicaPuestoDAO
        extends JpaRepository<CaracteristicaPuesto, CaracteristicaPuestoId> {


    @Query("""
            SELECT DISTINCT cp
            FROM CaracteristicaPuesto cp
            JOIN FETCH cp.competencia
            WHERE cp.puesto.codigoPuesto = :codigoPuesto
            ORDER BY cp.competencia.codigo
            """)
    List<CaracteristicaPuesto> buscarCaracteristicasPorCodigoPuesto(
            @Param("codigoPuesto") String codigoPuesto);

        @Query("""
            SELECT cp.competencia
            FROM CaracteristicaPuesto cp
            WHERE cp.puesto.codigoPuesto = :codigoPuesto
              AND cp.competencia.eliminado = false
            """)
        List<Competencia> buscarCompetenciasEvaluadas(
            @Param("codigoPuesto") String codigoPuesto);
        
        @Query("""
                SELECT cp
                FROM CaracteristicaPuesto cp
                WHERE cp.puesto = :codigoPuesto
                AND cp.competencia.eliminado = false
        """)
        List<CaracteristicaPuesto> buscarCaracteristicaPuesto(
                @Param("codigoPuesto") String codigoPuesto
        );

}
