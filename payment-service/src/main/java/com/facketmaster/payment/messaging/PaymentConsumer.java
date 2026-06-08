package com.facketmaster.payment.messaging;

import com.facketmaster.payment.config.RabbitMQConfig;
import com.facketmaster.payment.service.ProcessamentoPagamentoService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final ProcessamentoPagamentoService processamentoService;

    @RabbitListener(
            queues = RabbitMQConfig.QUEUE_PEDIDOS,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumirPedido(
            PedidoPagamentoMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag
    ) throws IOException {

        log.info("[CONSUMER] Mensagem recebida | pedidoId={} metodo={}",
                message.getPedidoId(), message.getMetodoPagamento());

        try {
            processamentoService.processar(message);

            channel.basicAck(deliveryTag, false);
            log.info("[CONSUMER] Mensagem processada e ACK enviado | pedidoId={}", message.getPedidoId());

        } catch (Exception ex) {
            log.error("[CONSUMER] Erro ao processar mensagem | pedidoId={} erro={}",
                    message.getPedidoId(), ex.getMessage(), ex);

            channel.basicNack(deliveryTag, false, false);
        }
    }
}