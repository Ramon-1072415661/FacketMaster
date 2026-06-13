package com.facketmaster.payment.service;

import com.facketmaster.payment.enums.StatusPedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class EmailService {

    public void notificar(String destinatario, UUID pedidoId, StatusPedido status) {
        String assunto = resolverAssunto(status);
        String corpo = resolverCorpo(pedidoId, status);

        log.info("[EMAIL MOCK] Para={} | Assunto='{}' | Corpo='{}'",
                destinatario, assunto, corpo);
    }

    private String resolverAssunto(StatusPedido status) {
        return switch (status) {
            case AGUARDANDO_PAGAMENTO -> "Pedido recebido — aguardando pagamento";
            case PROCESSANDO          -> "Seu pagamento está sendo processado";
            case APROVADO             -> "Pagamento aprovado! Seus ingressos estão confirmados";
            case RECUSADO             -> "Pagamento recusado";
            case CANCELADO            -> "Pedido cancelado";
            case EXPIRADO             -> "Pedido expirado";
        };
    }

    private String resolverCorpo(UUID pedidoId, StatusPedido status) {
        return switch (status) {
            case AGUARDANDO_PAGAMENTO -> String.format(
                    "Seu pedido %s foi criado e está aguardando o processamento do pagamento.", pedidoId);
            case PROCESSANDO          -> String.format(
                    "O pagamento do pedido %s está sendo processado. Aguarde a confirmação.", pedidoId);
            case APROVADO             -> String.format(
                    "O pagamento do pedido %s foi aprovado. Seus ingressos foram emitidos.", pedidoId);
            case RECUSADO             -> String.format(
                    "O pagamento do pedido %s foi recusado. Tente novamente com outro método de pagamento.", pedidoId);
            case CANCELADO            -> String.format(
                    "O pedido %s foi cancelado.", pedidoId);
            case EXPIRADO             -> String.format(
                    "O pedido %s expirou. O tempo para pagamento foi esgotado.", pedidoId);
        };
    }
}
