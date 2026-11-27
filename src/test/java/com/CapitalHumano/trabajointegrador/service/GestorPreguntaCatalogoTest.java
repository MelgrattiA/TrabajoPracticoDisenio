package com.CapitalHumano.trabajointegrador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.CapitalHumano.trabajointegrador.dto.CompetenciaCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.FactorCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.ItemOpcionCatalogoDTO;
import com.CapitalHumano.trabajointegrador.dto.OpcionRespuestaCatalogoDTO;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.entity.ItemOpcion;
import com.CapitalHumano.trabajointegrador.entity.OpcionRespuesta;
import com.CapitalHumano.trabajointegrador.exception.RecursoCatalogoNoEncontradoException;
import com.CapitalHumano.trabajointegrador.repository.CompetenciaDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.ItemOpcionDAO;
import com.CapitalHumano.trabajointegrador.repository.OpcionRespuestaDAO;
import com.CapitalHumano.trabajointegrador.repository.PonderacionItemDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;

@ExtendWith(MockitoExtension.class)
class GestorPreguntaCatalogoTest {

    @Mock
    private PreguntaDAO preguntaDAO;

    @Mock
    private CompetenciaDAO competenciaDAO;

    @Mock
    private FactorDAO factorDAO;

    @Mock
    private OpcionRespuestaDAO opcionRespuestaDAO;

    @Mock
    private ItemOpcionDAO itemOpcionDAO;

    @Mock
    private PonderacionItemDAO ponderacionItemDAO;

    @InjectMocks
    private GestorPregunta gestorPregunta;

    @Test
    void transformaCompetenciasYOpcionesDisponiblesEnDtos() {
        when(competenciaDAO.findByEliminadoFalseOrderByCodigoAsc())
                .thenReturn(List.of(Competencia.builder()
                        .codigo(3)
                        .nombre("COMPETENCIA")
                        .eliminado(false)
                        .build()));
        when(opcionRespuestaDAO.findByEliminadoFalseOrderByIdOpcionRespuestaAsc())
                .thenReturn(List.of(OpcionRespuesta.builder()
                        .idOpcionRespuesta(12L)
                        .nombre("OPCIÓN")
                        .eliminado(false)
                        .build()));

        List<CompetenciaCatalogoDTO> competencias =
                gestorPregunta.listarCompetenciasDisponibles();
        List<OpcionRespuestaCatalogoDTO> opciones =
                gestorPregunta.listarOpcionesRespuestaDisponibles();

        assertThat(competencias)
                .containsExactly(new CompetenciaCatalogoDTO(3, "COMPETENCIA"));
        assertThat(opciones)
                .containsExactly(new OpcionRespuestaCatalogoDTO(12L, "OPCIÓN"));
    }

    @Test
    void transformaFactoresYItemsManteniendoElOrdenDelDao() {
        Competencia competencia = Competencia.builder()
                .codigo(3)
                .nombre("COMPETENCIA")
                .eliminado(false)
                .build();
        when(competenciaDAO.findById(3)).thenReturn(Optional.of(competencia));
        when(factorDAO
                .findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(3))
                .thenReturn(List.of(
                        Factor.builder()
                                .codigo(7)
                                .nombre("PRIMERO")
                                .nroOrden(1)
                                .eliminado(false)
                                .build(),
                        Factor.builder()
                                .codigo(8)
                                .nombre("SEGUNDO")
                                .nroOrden(2)
                                .eliminado(false)
                                .build()));

        OpcionRespuesta opcion = OpcionRespuesta.builder()
                .idOpcionRespuesta(12L)
                .nombre("OPCIÓN")
                .eliminado(false)
                .build();
        when(opcionRespuestaDAO.findById(12L)).thenReturn(Optional.of(opcion));
        when(itemOpcionDAO
                .findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
                        12L))
                .thenReturn(List.of(
                        ItemOpcion.builder()
                                .idItemOpcion(21L)
                                .descripcion("PRIMERO")
                                .ordenVisualizacion(1)
                                .opcionRespuesta(opcion)
                                .build(),
                        ItemOpcion.builder()
                                .idItemOpcion(22L)
                                .descripcion("SEGUNDO")
                                .ordenVisualizacion(2)
                                .opcionRespuesta(opcion)
                                .build()));

        assertThat(gestorPregunta.listarFactoresDisponibles(3))
                .containsExactly(
                        new FactorCatalogoDTO(7, "PRIMERO", 1),
                        new FactorCatalogoDTO(8, "SEGUNDO", 2));
        assertThat(gestorPregunta.listarItemsDisponibles(12L))
                .containsExactly(
                        new ItemOpcionCatalogoDTO(21L, "PRIMERO", 1),
                        new ItemOpcionCatalogoDTO(22L, "SEGUNDO", 2));
        verify(factorDAO)
                .findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(3);
        verify(itemOpcionDAO)
                .findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
                        12L);
    }

    @Test
    void rechazaCompetenciaInexistenteOEliminada() {
        when(competenciaDAO.findById(404)).thenReturn(Optional.empty());
        when(competenciaDAO.findById(405)).thenReturn(Optional.of(
                Competencia.builder()
                        .codigo(405)
                        .nombre("ELIMINADA")
                        .eliminado(true)
                        .build()));

        assertThatThrownBy(() -> gestorPregunta.listarFactoresDisponibles(404))
                .isInstanceOf(RecursoCatalogoNoEncontradoException.class);
        assertThatThrownBy(() -> gestorPregunta.listarFactoresDisponibles(405))
                .isInstanceOf(RecursoCatalogoNoEncontradoException.class);
    }

    @Test
    void rechazaOpcionInexistenteOEliminada() {
        when(opcionRespuestaDAO.findById(404L)).thenReturn(Optional.empty());
        when(opcionRespuestaDAO.findById(405L)).thenReturn(Optional.of(
                OpcionRespuesta.builder()
                        .idOpcionRespuesta(405L)
                        .nombre("ELIMINADA")
                        .eliminado(true)
                        .build()));

        assertThatThrownBy(() -> gestorPregunta.listarItemsDisponibles(404L))
                .isInstanceOf(RecursoCatalogoNoEncontradoException.class);
        assertThatThrownBy(() -> gestorPregunta.listarItemsDisponibles(405L))
                .isInstanceOf(RecursoCatalogoNoEncontradoException.class);
    }

    @Test
    void coleccionesDeCatalogoSinResultadosSeDevuelvenVacias() {
        when(competenciaDAO.findByEliminadoFalseOrderByCodigoAsc())
                .thenReturn(List.of());
        when(competenciaDAO.findById(3)).thenReturn(Optional.of(
                Competencia.builder()
                        .codigo(3)
                        .eliminado(false)
                        .build()));
        when(factorDAO
                .findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(3))
                .thenReturn(List.of());
        when(opcionRespuestaDAO.findByEliminadoFalseOrderByIdOpcionRespuestaAsc())
                .thenReturn(List.of());
        when(opcionRespuestaDAO.findById(12L)).thenReturn(Optional.of(
                OpcionRespuesta.builder()
                        .idOpcionRespuesta(12L)
                        .eliminado(false)
                        .build()));
        when(itemOpcionDAO
                .findByOpcionRespuestaIdOpcionRespuestaOrderByOrdenVisualizacionAsc(
                        12L))
                .thenReturn(List.of());

        assertThat(gestorPregunta.listarCompetenciasDisponibles()).isEmpty();
        assertThat(gestorPregunta.listarFactoresDisponibles(3)).isEmpty();
        assertThat(gestorPregunta.listarOpcionesRespuestaDisponibles()).isEmpty();
        assertThat(gestorPregunta.listarItemsDisponibles(12L)).isEmpty();
    }
}
