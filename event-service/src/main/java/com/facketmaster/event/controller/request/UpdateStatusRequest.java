package com.facketmaster.event.controller.request;

import com.facketmaster.event.entity.Evento;

public record UpdateStatusRequest(
        Evento.StatusEvento status
) {
}