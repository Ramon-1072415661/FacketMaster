package com.facketmaster.event.messaging;

import com.facketmaster.event.config.RabbitMQConfig;
import com.facketmaster.event.service.EventoResultadoService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Consome os resultados de pagamento publicados pelo payment-service em
 * {@code pagamentos.resultado}, liberando ingressos reservados quando o
 * pedido é recusado/cancelado/expirado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultadoPagamentoConsumer {

    private final EventoResultadoService resultadoService;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_RESULTADO,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumirResultado(
            ResultadoPagamentoMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        log.info("[CONSUMER] Resultado recebido | pedidoId={} eventoId={} status={}",
                message.getPedidoId(), message.getEventoId(), message.getStatusPedido());

        try {
            resultadoService.processar(message);
            channel.basicAck(deliveryTag, false);
            log.info("[CONSUMER] Resultado processado e ACK enviado | pedidoId={}", message.getPedidoId());
        } catch (Exception ex) {
            log.error("[CONSUMER] Erro ao processar resultado | pedidoId={} erro={}",
                    message.getPedidoId(), ex.getMessage(), ex);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
