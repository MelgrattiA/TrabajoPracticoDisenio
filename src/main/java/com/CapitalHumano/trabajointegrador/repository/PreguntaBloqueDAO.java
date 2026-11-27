package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Pregunta;
import com.CapitalHumano.trabajointegrador.entity.PreguntaBloque;

@Repository
public interface PreguntaBloqueDAO
        extends JpaRepository<PreguntaBloque, Long> {

    @Query("""
            SELECT pb.pregunta
            FROM PreguntaBloque pb
            WHERE pb.bloquePregunta.idBloquePregunta = :idBloque
              AND pb.itemOpcionSeleccionada IS NULL
            ORDER BY pb.ordenEnBloque ASC
            """)
    List<Pregunta> buscarPreguntasPendientes(
            @Param("idBloque") Long idBloque);

    List<PreguntaBloque> findByBloquePreguntaIdBloquePreguntaOrderByOrdenEnBloqueAsc(
            Long idBloque);

    @Query("""
            SELECT pb
            FROM PreguntaBloque pb
            JOIN FETCH pb.bloquePregunta b
            JOIN FETCH pb.pregunta p
            JOIN FETCH p.factor f
            WHERE b.cuestionario.clave = :claveCuestionario
              AND pb.itemOpcionSeleccionada IS NOT NULL
            ORDER BY f.competencia.codigo, f.codigo, pb.idPreguntaBloque
            """)
    List<PreguntaBloque> buscarRespondidasPorCuestionario(
            @Param("claveCuestionario") String claveCuestionario);
}
