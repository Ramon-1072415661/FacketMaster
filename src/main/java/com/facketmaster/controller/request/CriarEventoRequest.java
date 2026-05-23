package com.facketmaster.controller.request;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CriarEventoRequest(String name,
                                 String description,
                                 BigDecimal ticketPrice,

                                 @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
                                 LocalDateTime eventDate,

                                 String eventLocation,
                                 int totalTicketsAmount

) {

}
