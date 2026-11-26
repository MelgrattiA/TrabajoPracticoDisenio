package com.CapitalHumano.trabajointegrador.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> manejarSolicitudInvalida(
            IllegalArgumentException excepcion) {
        return crearRespuesta(HttpStatus.BAD_REQUEST, excepcion.getMessage());
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
