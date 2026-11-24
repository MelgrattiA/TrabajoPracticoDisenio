package com.CapitalHumano.trabajointegrador.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.CapitalHumano.trabajointegrador.dto.RegistroActualizacionDeEstadoDTO;
import com.CapitalHumano.trabajointegrador.entity.Cuestionario;
import com.CapitalHumano.trabajointegrador.entity.RegistroActualizacionDeEstado;
import com.CapitalHumano.trabajointegrador.entity.BloquePregunta;
import com.CapitalHumano.trabajointegrador.entity.Pregunta;
import com.CapitalHumano.trabajointegrador.entity.PreguntaBloque;
import com.CapitalHumano.trabajointegrador.entity.Competencia;
import com.CapitalHumano.trabajointegrador.entity.Factor;
import com.CapitalHumano.trabajointegrador.repository.BloquePreguntaDAO;
import com.CapitalHumano.trabajointegrador.repository.CaracteristicaPuestoDAO;
import com.CapitalHumano.trabajointegrador.repository.CuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.EstadoCuestionarioDAO;
import com.CapitalHumano.trabajointegrador.repository.FactorDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaDAO;
import com.CapitalHumano.trabajointegrador.repository.RegistroActualizacionDeEstadoDAO;
import com.CapitalHumano.trabajointegrador.repository.PreguntaBloqueDAO;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


@Service
public class GestorCuestionario {

    private final CuestionarioDAO cuestionarioDAO;
    private final EstadoCuestionarioDAO estadoCuestionarioDAO;
    private final RegistroActualizacionDeEstadoDAO registroActualizacionDeEstadoDAO;
    private final BloquePreguntaDAO bloquePreguntaDAO;
    private final PreguntaBloqueDAO preguntaBloqueDAO;
    private final CaracteristicaPuestoDAO caracteristicaPuestoDAO;
    private final FactorDAO factorDAO;
    private final PreguntaDAO preguntaDAO;

    public GestorCuestionario(
            CuestionarioDAO cuestionarioDAO,
            EstadoCuestionarioDAO estadoCuestionarioDAO,
            RegistroActualizacionDeEstadoDAO registroActualizacionDeEstadoDAO,
            BloquePreguntaDAO bloquePreguntaDAO,
            PreguntaBloqueDAO preguntaBloqueDAO,
            CaracteristicaPuestoDAO caracteristicaPuestoDAO,
            FactorDAO factorDAO,
            PreguntaDAO preguntaDAO) {
        this.cuestionarioDAO = cuestionarioDAO;
        this.estadoCuestionarioDAO = estadoCuestionarioDAO;
        this.registroActualizacionDeEstadoDAO = registroActualizacionDeEstadoDAO;
        this.bloquePreguntaDAO = bloquePreguntaDAO;
        this.preguntaBloqueDAO = preguntaBloqueDAO;
        this.caracteristicaPuestoDAO = caracteristicaPuestoDAO;
        this.factorDAO = factorDAO;
        this.preguntaDAO = preguntaDAO;
    }

    @Transactional
    public Cuestionario completarCuestionario(Integer nroCandidato) {

        // Se busca el cuestionario activo(1) o en proceso(2) del candidato.
        Cuestionario cuestionario = cuestionarioDAO.buscarActivoOEnProceso(nroCandidato)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un cuestionario activo para el candidato"));

        // Si no puede acceder, el cuestionario queda incompleto.
        if (!cuestionario.verificarAcceso()) {

            // Se actualiza el estado del cuestionario a Incompleto.
            return actualizarEstadoCuestionario(
                    cuestionario,
                    4,
                    "Cuestionario incompleto por superar accesos máximos");
        }

        if (!cuestionario.verificarTiemposLimites()) {

            // Si el cuestionario está en proceso (estado 2), se cambia a Incompleto (estado 4),
            // si no de activo (estado 1), se cambia a Sin Contestar (estado 5).
            Integer idEstadoVencido =
                    cuestionario.getEstado().getIdEstadoCuestionario() == 2
                            ? 4
                            : 5;

            // Se actualiza el estado del cuestionario según el caso.
            return actualizarEstadoCuestionario(
                    cuestionario,
                    idEstadoVencido,
                    "Cuestionario vencido por superar el tiempo máximo");
        }

        // Si el cuestionario ya estaba en proceso, recupera sus preguntas pendientes.
        if (cuestionario.getEstado().getIdEstadoCuestionario() == 2) {
            recuperarBloquePendiente(cuestionario);
        }

        // Si es la primera vez, arma los bloques con preguntas aleatorias.
        if (cuestionario.getEstado().getIdEstadoCuestionario() == 1) {
            asociarPreguntasAZar(cuestionario, 2);
        }




        // Se usa la misma fecha y hora para inicio e último ingreso.
        LocalDateTime ahora = LocalDateTime.now();

        // La fecha de inicio se asigna únicamente la primera vez.
        if (cuestionario.getFechaInicio() == null) {
            cuestionario.setFechaInicio(ahora);
        }

        // Se actualiza la fecha de último ingreso y se incrementa la cantidad de accesos.
        cuestionario.setFechaUltimoIngreso(ahora);
        cuestionario.setCantidadAccesos(cuestionario.getCantidadAccesos() + 1);

        // Cada ingreso válido deja el cuestionario en estado En Proceso.
        cuestionario.setEstado(estadoCuestionarioDAO.findById(2)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado En Proceso")));

        // Se guarda el cuestionario actualizado.
        return cuestionarioDAO.save(cuestionario);
    }

    public Cuestionario actualizarEstadoCuestionario(
            Cuestionario cuestionario,
            Integer idEstado,
            String accion) {

        // Se actualiza el estado del cuestionario.
        cuestionario.setEstado(estadoCuestionarioDAO.findById(idEstado)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado del cuestionario")));

        // Se guarda el cuestionario actualizado.
        Cuestionario cuestionarioActualizado = cuestionarioDAO.save(cuestionario);

        // Se crea un DTO para el registro de actualización del estado.
        LocalDateTime ahora = LocalDateTime.now();
        RegistroActualizacionDeEstadoDTO registroDTO =
                new RegistroActualizacionDeEstadoDTO(
                        LocalDate.from(ahora),
                        LocalTime.from(ahora),
                        accion);

        // Se guarda el registro de actualización del estado.
        guardarRegistro(registroDTO);

        return cuestionarioActualizado;
    }

    public List<Pregunta> asociarPreguntasAZar(Cuestionario cuestionario,
                 int cantidadPreguntasPorBloque) {


        if (cantidadPreguntasPorBloque <= 0) {
            throw new IllegalArgumentException(
                    "La cantidad de preguntas por bloque debe ser mayor que cero");
        }

        
        List<Pregunta> preguntasSeleccionadas = new ArrayList<>();


        List<Competencia> competencias =
                caracteristicaPuestoDAO.buscarCompetenciasEvaluadas(
                        cuestionario.getPuesto().getCodigoPuesto());

        for (Competencia competencia : competencias) {

            List<Factor> factores =
                    factorDAO.findByCompetenciaCodigoAndEliminadoFalseOrderByNroOrdenAsc(
                            competencia.getCodigo());

            for (Factor factor : factores) {

                List<Pregunta> preguntas =
                        preguntaDAO.findByFactorCodigoAndEliminadoFalse(
                                factor.getCodigo());


                if (preguntas.size() < 2) {
                    continue;
                }


                Collections.shuffle(preguntas);
                preguntasSeleccionadas.add(preguntas.get(0));
                preguntasSeleccionadas.add(preguntas.get(1));
            }
        }

        if (preguntasSeleccionadas.isEmpty()) {
            throw new IllegalStateException(
                    "No hay factores con al menos 2 preguntas disponibles");
        }

        Collections.shuffle(preguntasSeleccionadas);

        int nroOrdenBloque = 1;
        for (int inicio = 0;
                inicio < preguntasSeleccionadas.size();
                inicio += cantidadPreguntasPorBloque) {

            int fin = Math.min(
                    inicio + cantidadPreguntasPorBloque,
                    preguntasSeleccionadas.size());

            BloquePregunta bloque = BloquePregunta.builder()
                    .cuestionario(cuestionario)
                    .nroOrden(nroOrdenBloque++)
                    .build();
            bloque = bloquePreguntaDAO.save(bloque);

            int ordenEnBloque = 1;
            for (int i = inicio; i < fin; i++) {
                preguntaBloqueDAO.save(PreguntaBloque.builder()
                        .bloquePregunta(bloque)
                        .pregunta(preguntasSeleccionadas.get(i))
                        .ordenEnBloque(ordenEnBloque++)
                        .itemOpcionSeleccionada(null)
                        .build());
            }
        }

        return preguntasSeleccionadas;
    }

    public List<Pregunta> recuperarBloquePendiente(Cuestionario cuestionario) {
        BloquePregunta bloquePendiente = bloquePreguntaDAO
                .buscarBloquePendiente(cuestionario.getClave())
                .orElseThrow(() -> new IllegalStateException(
                        "No existe un bloque pendiente para el cuestionario"));

        return preguntaBloqueDAO.buscarPreguntasPendientes(bloquePendiente.getIdBloquePregunta());
    }

    private void guardarRegistro(RegistroActualizacionDeEstadoDTO registroDTO) {

        registroActualizacionDeEstadoDAO.save(RegistroActualizacionDeEstado.builder()
                .fecha(registroDTO.getFecha())
                .hora(registroDTO.getHora())
                .accion(registroDTO.getAccion())
                .build());
    }
}
