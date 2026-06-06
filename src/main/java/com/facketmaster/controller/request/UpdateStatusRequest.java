package com.facketmaster.controller.request;

import com.facketmaster.entity.Evento;

public record UpdateStatusRequest(
        Evento.StatusEvento status
) {
}
