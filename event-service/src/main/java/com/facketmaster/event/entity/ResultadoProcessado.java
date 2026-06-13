package com.facketmaster.event.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro de idempotência: cada pedido processado pelo consumidor de
 * resultados de pagamento gera uma linha aqui. Antes de aplicar qualquer
 * efeito colateral (liberar ingressos reservados), o consumidor verifica
 * se o {@code pedidoId} já foi processado — se sim, a mensagem é
 * reconhecida (ACK) sem reprocessar, protegendo contra redelivery do
 * RabbitMQ (at-least-once delivery).
 */
@Entity
@Table(name = "resultados_processados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoProcessado {

    @Id
    private UUID pedidoId;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false, length = 30)
    private String statusPedido;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private LocalDateTime processadoEm;
}
