package com.facketmaster.event.exception;

public class EventoNotFoundException extends RuntimeException {
    public EventoNotFoundException(Long id) {
        super("Evento não encontrado com id: " + id);
    }
}
