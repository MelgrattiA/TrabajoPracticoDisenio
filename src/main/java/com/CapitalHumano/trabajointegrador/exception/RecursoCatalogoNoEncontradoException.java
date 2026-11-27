package com.CapitalHumano.trabajointegrador.exception;

public class RecursoCatalogoNoEncontradoException extends RuntimeException {

    public RecursoCatalogoNoEncontradoException(
            String recurso,
            Object identificador) {
        super("No se encontró " + recurso + " con identificador: " + identificador);
    }
}
