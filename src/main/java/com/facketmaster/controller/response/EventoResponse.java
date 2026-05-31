package com.facketmaster.controller.response;

import com.facketmaster.entity.Evento.StatusEvento;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoResponse {

    private Long id;
    private String nome;
    private LocalDateTime dataEvento;
    private BigDecimal valor;
    private Integer quantidadeDisponivel;
    private Integer quantidadeTotal;
    private String descricao;
    private String local;
    private StatusEvento status;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
