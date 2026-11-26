package com.CapitalHumano.trabajointegrador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.CapitalHumano.trabajointegrador.entity.Puesto;

@Repository
public interface PuestoDAO extends JpaRepository<Puesto, String> {

    List<Puesto> findAll();

    List<Puesto> findByEliminadoFalse();

    @Query(value = """
            SELECT p.codigo_puesto AS "codigoPuesto",
                   p.nombre_puesto AS "nombrePuesto",
                   p.empresa AS empresa,
                   COUNT(DISTINCT c.nro_candidato) AS candidatos,
                   COUNT(c.clave) FILTER (WHERE c.estado_id = 3)
                       AS "evaluacionesCompletas"
            FROM puesto p
            LEFT JOIN cuestionario c
                ON c.codigo_puesto = p.codigo_puesto
            WHERE p.eliminado = false
              AND (:codigo IS NULL OR p.codigo_puesto = :codigo)
              AND (:nombre IS NULL OR LOWER(p.nombre_puesto)
                   LIKE LOWER(CONCAT('%', :nombre, '%')))
              AND (:empresa IS NULL OR LOWER(p.empresa)
                   LIKE LOWER(CONCAT('%', :empresa, '%')))
            GROUP BY p.codigo_puesto, p.nombre_puesto, p.empresa
            ORDER BY p.codigo_puesto
            """, nativeQuery = true)
    List<PuestoOrdenMeritoProjection> buscarPuestosOrdenMerito(
            @Param("codigo") String codigo,
            @Param("nombre") String nombre,
            @Param("empresa") String empresa);
}