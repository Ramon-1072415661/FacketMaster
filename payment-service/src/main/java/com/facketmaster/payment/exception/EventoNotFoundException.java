package com.facketmaster.payment.exception;

public class EventoNotFoundException extends RuntimeException {
    public EventoNotFoundException(Long eventoId) {
        super("Evento não encontrado: " + eventoId);
    }
}
