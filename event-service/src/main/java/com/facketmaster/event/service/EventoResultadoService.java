package com.facketmaster.event.service;

import com.facketmaster.event.entity.ResultadoProcessado;
import com.facketmaster.event.messaging.ResultadoPagamentoMessage;
import com.facketmaster.event.messaging.ResultadoPagamentoMessage.StatusPedido;
import com.facketmaster.event.repository.ResultadoProcessadoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Aplica os efeitos colaterais (no estoque de ingressos) decorrentes do
 * resultado de um pagamento, consumido da fila {@code pagamentos.resultado}.
 * <p>
 * Apenas estados finais negativos (RECUSADO, CANCELADO, EXPIRADO) liberam
 * a reserva feita em {@code PedidoService.criar}. APROVADO não exige ação,
 * pois a reserva já reflete o consumo definitivo do estoque.
 * <p>
 * Idempotência: cada liberação de estoque é registrada em
 * {@code resultados_processados}, chaveada pelo {@code pedidoId}. Mensagens
 * redeliveradas pelo RabbitMQ (at-least-once) são detectadas e ignoradas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventoResultadoService {

    private static final Set<StatusPedido> STATUS_LIBERA_ESTOQUE =
            EnumSet.of(StatusPedido.RECUSADO, StatusPedido.CANCELADO, StatusPedido.EXPIRADO);

    private final EventoService eventoService;
    private final ResultadoProcessadoRepository resultadoProcessadoRepository;

    @Transactional
    public void processar(ResultadoPagamentoMessage message) {
        StatusPedido status = message.getStatusPedido();

        if (!STATUS_LIBERA_ESTOQUE.contains(status)) {
            log.debug("[RESULTADO] Status {} não requer ação de estoque | pedidoId={}",
                    status, message.getPedidoId());
            return;
        }

        if (resultadoProcessadoRepository.existsById(message.getPedidoId())) {
            log.info("[RESULTADO] Pedido já processado, ignorando (idempotência) | pedidoId={} status={}",
                    message.getPedidoId(), status);
            return;
        }

        eventoService.liberar(message.getEventoId(), message.getQuantidade());

        resultadoProcessadoRepository.save(ResultadoProcessado.builder()
                .pedidoId(message.getPedidoId())
                .eventoId(message.getEventoId())
                .statusPedido(status.name())
                .quantidade(message.getQuantidade())
                .processadoEm(LocalDateTime.now())
                .build());

        log.info("[RESULTADO] Estoque liberado | pedidoId={} eventoId={} quantidade={} status={}",
                message.getPedidoId(), message.getEventoId(), message.getQuantidade(), status);
    }
}
