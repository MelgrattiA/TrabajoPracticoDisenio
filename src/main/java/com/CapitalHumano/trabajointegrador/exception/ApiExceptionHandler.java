package com.CapitalHumano.trabajointegrador.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.CapitalHumano.trabajointegrador.dto.ConflictoPreguntaDTO;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PreguntaConflictoException.class)
    public ResponseEntity<ConflictoPreguntaDTO> manejarConflictoPregunta(
            PreguntaConflictoException excepcion) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(excepcion.getConflicto());
    }

    @ExceptionHandler(PreguntaNoEncontradaException.class)
    public ResponseEntity<ProblemDetail> manejarPreguntaNoEncontrada(
            PreguntaNoEncontradaException excepcion) {
        return crearRespuesta(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(RecursoCatalogoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarRecursoCatalogoNoEncontrado(
            RecursoCatalogoNoEncontradoException excepcion) {
        return crearRespuesta(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> manejarSolicitudInvalida(
            IllegalArgumentException excepcion) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, excepcion.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarErroresDeValidacion(
            MethodArgumentNotValidException excepcion) {
        Map<String, List<String>> erroresPorCampo =
                excepcion.getBindingResult().getFieldErrors().stream()
                        .collect(Collectors.groupingBy(
                                error -> error.getField(),
                                LinkedHashMap::new,
                                Collectors.mapping(
                                        error -> error.getDefaultMessage(),
                                        Collectors.toList())));
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La solicitud contiene errores de validación");
        problema.setProperty("errores", erroresPorCampo);
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(PuestoNoEncontradoException.class)
    public ResponseEntity<ProblemDetail> manejarPuestoNoEncontrado(
            PuestoNoEncontradoException excepcion) {
        return crearRespuesta(HttpStatus.NOT_FOUND, excepcion.getMessage());
    }

    @ExceptionHandler(PuestoSinCompetenciasException.class)
    public ResponseEntity<ProblemDetail> manejarPuestoSinCompetencias(
            PuestoSinCompetenciasException excepcion) {
        return crearRespuesta(
                    HttpStatus.UNPROCESSABLE_CONTENT, excepcion.getMessage());
    }

    private ResponseEntity<ProblemDetail> crearRespuesta(
            HttpStatus estado,
            String detalle) {
        ProblemDetail problema =
                ProblemDetail.forStatusAndDetail(estado, detalle);
        return ResponseEntity.status(estado).body(problema);
    }
}
