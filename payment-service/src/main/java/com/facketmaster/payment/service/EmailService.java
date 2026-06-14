package com.facketmaster.payment.service;

import com.facketmaster.config.AuthenticatedUserProvider;
import com.facketmaster.controller.response.JwtTokenResponse;
import com.facketmaster.payment.enums.StatusPedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
public class EmailService {

    private final AuthenticatedUserProvider authenticatedUserProvider;

    public EmailService(AuthenticatedUserProvider authenticatedUserProvider) {
        this.authenticatedUserProvider = authenticatedUserProvider;
    }

    public void notificar(String destinatario, UUID pedidoId, StatusPedido status) {
        String assunto = resolverAssunto(status);
        String corpo = resolverCorpo(pedidoId, status);

        log.info("[EMAIL MOCK] Para={} | Assunto='{}' | Corpo='{}'",
                destinatario, assunto, corpo);

        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info(
                "business_event",
                kv("event_type", "EMAIL_NOTIFICATION_SENT"),
                kv("email_reciever", destinatario),
                kv("email_subject", assunto),
                kv("email_body", corpo),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );
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
