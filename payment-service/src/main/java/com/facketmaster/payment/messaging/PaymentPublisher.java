package com.facketmaster.payment.messaging;

import com.facketmaster.payment.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicarPedido(PedidoPagamentoMessage message) {
        log.info("Publicando pedido | pedidoId={} metodo={}",
                message.getPedidoId(), message.getMetodoPagamento());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_PEDIDO,
                message
        );
        log.debug("Pedido publicado com sucesso | pedidoId={}", message.getPedidoId());
    }

    public void publicarResultado(ResultadoPagamentoMessage message) {
        log.info("Publicando resultado de pagamento | pedidoId={} status={}",
                message.getPedidoId(), message.getStatusPedido());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_RESULTADO,
                message
        );
    }
}