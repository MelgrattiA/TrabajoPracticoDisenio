package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Pregunta;

@Repository 
public interface PreguntaDAO extends JpaRepository <Pregunta, Integer> {

    List<Pregunta> findByFactorCodigoAndEliminadoFalse(
            Integer codigoFactor);

   @Query("""
    SELECT p FROM Pregunta p
    JOIN p.factor f
    JOIN f.competencia c
    WHERE p.eliminado = false
      AND (:competencia IS NOT NULL OR :factor IS NOT NULL OR :nombrePregunta IS NOT NULL)
      AND (:competencia IS NULL OR lower(c.nombre) LIKE :competencia)
      AND (:factor IS NULL OR lower(f.nombre) LIKE :factor)
      AND (:nombrePregunta IS NULL OR lower(p.nombre) LIKE :nombrePregunta)
    """)
    List<Pregunta> buscarPreguntas(
        @Param("competencia") String competencia,
        @Param("factor") String factor,
        @Param("nombrePregunta") String nombrePregunta
    );

    @EntityGraph(attributePaths = {"factor.competencia", "opcionRespuesta"})
    @Query("""
            SELECT p
            FROM Pregunta p
            WHERE p.factor.codigo = :codigoFactor
              AND p.eliminado = false
              AND upper(function('btrim', p.nombre)) = :nombreNormalizado
            """)
    Optional<Pregunta> buscarPreguntaActivaPorNombreYFactor(
            @Param("nombreNormalizado") String nombreNormalizado,
            @Param("codigoFactor") Integer codigoFactor);

    @EntityGraph(attributePaths = {"factor.competencia", "opcionRespuesta"})
    @Query("""
            SELECT p
            FROM Pregunta p
            WHERE p.factor.codigo = :codigoFactor
              AND p.eliminado = false
              AND upper(function('btrim', p.pregunta)) = :textoNormalizado
            """)
    Optional<Pregunta> buscarPreguntaActivaPorTextoYFactor(
            @Param("textoNormalizado") String textoNormalizado,
            @Param("codigoFactor") Integer codigoFactor);

    @EntityGraph(attributePaths = {"factor.competencia", "opcionRespuesta"})
    @Query("""
            SELECT p
            FROM Pregunta p
            WHERE p.factor.codigo = :codigoFactor
              AND p.eliminado = true
              AND upper(function('btrim', p.nombre)) = :nombreNormalizado
            """)
    List<Pregunta> buscarPreguntasEliminadasPorNombreYFactor(
            @Param("nombreNormalizado") String nombreNormalizado,
            @Param("codigoFactor") Integer codigoFactor);

    @EntityGraph(attributePaths = {"factor.competencia", "opcionRespuesta"})
    @Query("""
            SELECT p
            FROM Pregunta p
            WHERE p.factor.codigo = :codigoFactor
              AND p.eliminado = true
              AND upper(function('btrim', p.pregunta)) = :textoNormalizado
            """)
    List<Pregunta> buscarPreguntasEliminadasPorTextoYFactor(
            @Param("textoNormalizado") String textoNormalizado,
            @Param("codigoFactor") Integer codigoFactor);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pregunta p WHERE p.codigo = :codigo")
    Optional<Pregunta> buscarPorCodigoConBloqueoEscritura(
            @Param("codigo") Integer codigo);
}
