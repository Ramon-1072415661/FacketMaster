package com.facketmaster.event.exception;

public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(Long eventoId) {
        super("Ingressos insuficientes para o evento " + eventoId);
    }
}
