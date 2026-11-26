package com.CapitalHumano.trabajointegrador.exception;

public class PuestoNoEncontradoException extends RuntimeException {

    public PuestoNoEncontradoException(String codigoPuesto) {
        super("No se encontró el puesto: " + codigoPuesto);
    }
}
