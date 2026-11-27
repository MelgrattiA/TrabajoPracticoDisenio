package com.CapitalHumano.trabajointegrador.exception;

public class PreguntaNoEncontradaException extends IllegalArgumentException {

    public PreguntaNoEncontradaException(Integer codigo) {
        super("La pregunta no existe; código: " + codigo);
    }
}
