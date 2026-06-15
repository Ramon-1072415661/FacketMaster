package com.facketmaster.payment.service;

import com.facketmaster.config.AuthenticatedUserProvider;
import com.facketmaster.controller.response.JwtTokenResponse;
import com.facketmaster.payment.entity.Ingresso;
import com.facketmaster.payment.entity.Pagamento;
import com.facketmaster.payment.entity.Pedido;
import com.facketmaster.payment.enums.MetodoPagamento;
import com.facketmaster.payment.enums.StatusIngresso;
import com.facketmaster.payment.enums.StatusPagamento;
import com.facketmaster.payment.enums.StatusPedido;
import com.facketmaster.payment.gateway.GatewayFactory;
import com.facketmaster.payment.gateway.GatewayPagamento;
import com.facketmaster.payment.messaging.PaymentPublisher;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import com.facketmaster.payment.messaging.ResultadoPagamentoMessage;
import com.facketmaster.payment.repository.IngressoRepository;
import com.facketmaster.payment.repository.PagamentoRepository;
import com.facketmaster.payment.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessamentoPagamentoService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final IngressoRepository ingressoRepository;
    private final GatewayFactory gatewayFactory;
    private final PaymentPublisher publisher;
    private final PedidoCacheService cacheService;
    private final EmailService emailService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public void processar(PedidoPagamentoMessage message) {
        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info("[PROCESSAMENTO] Iniciando | pedidoId={}", message.getPedidoId());

        log.info(
                "business_event",
                kv("event_type", "STARTING_PAYMENT_PROCESSING"),
                kv("order_id", message.getPedidoId()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );

        Pedido pedido = pedidoRepository.findById(message.getPedidoId())
                .orElseThrow(() -> new IllegalStateException(
                        "Pedido não encontrado: " + message.getPedidoId()));

        if (pedido.getStatusPedido() == StatusPedido.APROVADO
                || pedido.getStatusPedido() == StatusPedido.CANCELADO
                || pedido.getStatusPedido() == StatusPedido.RECUSADO) {
            log.warn("[PROCESSAMENTO] Pedido já finalizado, ignorando | pedidoId={} status={}",
                    pedido.getId(), pedido.getStatusPedido());

            log.info(
                    "business_event",
                    kv("event_type", "ORDER_COMPLETE"),
                    kv("log_description", "Order has already been completed"),
                    kv("order_id", message.getPedidoId()),
                    kv("order_status", pedido.getStatusPedido()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );
            return;
        }

        pedido.setStatusPedido(StatusPedido.PROCESSANDO);
        pedidoRepository.save(pedido);
        emailService.notificar(pedido.getUsuarioEmail(), pedido.getId(), StatusPedido.PROCESSANDO);

        Pagamento pagamento = pagamentoRepository
                .findTopByPedidoIdOrderByCriadoEmDesc(message.getPedidoId())
                .orElseGet(() -> {
                    Pagamento p = criarPagamentoInicial(pedido, message.getMetodoPagamento());
                    return pagamentoRepository.save(p);
                });
        cacheService.atualizarCache(pedido, pagamento, List.of());

        GatewayPagamento gateway = gatewayFactory.resolver(message.getMetodoPagamento());
        GatewayPagamento.ResultadoGateway resultado = gateway.processar(message);

        pagamento.setTentativas(pagamento.getTentativas() + 1);
        pagamento.setCodigoTransacao(resultado.codigoTransacao());
        pagamento.setCodigoPagamento(resultado.codigoPagamento());
        pagamento.setUrlPagamento(resultado.urlPagamento());
        pagamento.setDataExpiracao(resultado.dataExpiracao());

        List<Ingresso> ingressosEmitidos = new ArrayList<>();

        if (isMétodoPendente(message.getMetodoPagamento()) && resultado.codigoTransacao() != null) {
            pagamento.setStatusPagamento(StatusPagamento.PROCESSANDO);
            pedido.setStatusPedido(StatusPedido.PROCESSANDO);
            log.info("[PROCESSAMENTO] Aguardando confirmação | pedidoId={} metodo={}",
                    pedido.getId(), message.getMetodoPagamento());

            log.info(
                    "business_event",
                    kv("event_type", "PAYMENT_AWAITING_CONFIRMATION"),
                    kv("order_id", pedido.getId()),
                    kv("order_status", pedido.getStatusPedido()),
                    kv("payment_method", pedido.getMetodoPagamento()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );

        } else if (resultado.aprovado()) {
            pagamento.setStatusPagamento(StatusPagamento.APROVADO);
            pagamento.setDataAprovacao(LocalDateTime.now());
            pedido.setStatusPedido(StatusPedido.APROVADO);
            emailService.notificar(pedido.getUsuarioEmail(), pedido.getId(), StatusPedido.APROVADO);
            ingressosEmitidos = emitirIngressos(pedido);
            log.info("[PROCESSAMENTO] Pagamento APROVADO | pedidoId={}", pedido.getId());

            log.info(
                    "business_event",
                    kv("event_type", "PAYMENT_APPROVED"),
                    kv("order_id", pedido.getId()),
                    kv("order_status", pedido.getStatusPedido()),
                    kv("payment_method", pedido.getMetodoPagamento()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );

        } else {
            pagamento.setStatusPagamento(StatusPagamento.RECUSADO);
            pagamento.setMotivoRecusa(resultado.motivoRecusa());
            pedido.setStatusPedido(StatusPedido.RECUSADO);
            emailService.notificar(pedido.getUsuarioEmail(), pedido.getId(), StatusPedido.RECUSADO);
            log.warn("[PROCESSAMENTO] Pagamento RECUSADO | pedidoId={} motivo={}",
                    pedido.getId(), resultado.motivoRecusa());

            log.info(
                    "business_event",
                    kv("event_type", "PAYMENT_REFUSED"),
                    kv("order_id", pedido.getId()),
                    kv("order_status", pedido.getStatusPedido()),
                    kv("payment_method", pedido.getMetodoPagamento()),
                    kv("reason", resultado.motivoRecusa()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );
        }

        pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);

        cacheService.atualizarCache(pedido, pagamento, ingressosEmitidos);

        publisher.publicarResultado(montarResultado(pedido, pagamento, ingressosEmitidos));

        log.info("[PROCESSAMENTO] Concluído | pedidoId={} statusFinal={}",
                pedido.getId(), pedido.getStatusPedido());

        log.info(
                "business_event",
                kv("event_type", "ORDER_PROCESSING_COMPLETE"),
                kv("order_id", pedido.getId()),
                kv("order_status", pedido.getStatusPedido()),
                kv("payment_method", pedido.getMetodoPagamento()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );
    }

    @Transactional
    public void confirmarPagamento(UUID pedidoId) {
        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + pedidoId));

        if (pedido.getStatusPedido() == StatusPedido.APROVADO || pedido.getStatusPedido() == StatusPedido.CANCELADO) {
            throw new IllegalStateException("Pedido já está em um estado final e não pode ser confirmado: " + pedidoId);
        }

        Pagamento pagamento = pagamentoRepository
                .findTopByPedidoIdOrderByCriadoEmDesc(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado para pedido: " + pedidoId));

        pagamento.setStatusPagamento(StatusPagamento.APROVADO);
        pagamento.setDataAprovacao(LocalDateTime.now());
        pedido.setStatusPedido(StatusPedido.APROVADO);

        List<Ingresso> ingressosEmitidos = emitirIngressos(pedido);

        pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        cacheService.atualizarCache(pedido, pagamento, ingressosEmitidos);
        publisher.publicarResultado(montarResultado(pedido, pagamento, ingressosEmitidos));
        emailService.notificar(pedido.getUsuarioEmail(), pedidoId, StatusPedido.APROVADO);

        log.info("[PROCESSAMENTO] Pagamento confirmado via webhook | pedidoId={}", pedidoId);

        log.info(
                "business_event",
                kv("event_type", "PAYMENT_CONFIRMED"),
                kv("order_id", pedido.getId()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );
    }

    @Transactional
    public void cancelarPedido(UUID pedidoId) {
        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + pedidoId));

        if (pedido.getStatusPedido() == StatusPedido.APROVADO
                || pedido.getStatusPedido() == StatusPedido.CANCELADO
                || pedido.getStatusPedido() == StatusPedido.EXPIRADO) {
            throw new IllegalStateException("Pedido não pode ser cancelado no status atual: " + pedido.getStatusPedido());
        }

        Pagamento pagamento = pagamentoRepository
                .findTopByPedidoIdOrderByCriadoEmDesc(pedidoId)
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado para pedido: " + pedidoId));

        pagamento.setStatusPagamento(StatusPagamento.CANCELADO);
        pedido.setStatusPedido(StatusPedido.CANCELADO);

        pagamentoRepository.save(pagamento);
        pedidoRepository.save(pedido);
        cacheService.atualizarCache(pedido, pagamento, List.of());
        emailService.notificar(pedido.getUsuarioEmail(), pedidoId, StatusPedido.CANCELADO);

        log.info("[PROCESSAMENTO] Pedido cancelado | pedidoId={}", pedidoId);

        log.info(
                "business_event",
                kv("event_type", "ORDER_CANCELLED"),
                kv("order_id", pedido.getId()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );
    }

    @Scheduled(fixedDelayString = "${app.payment.expiracao-check-ms:60000}")
    @Transactional
    public void expirarPedidosVencidos() {
        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        List<Pagamento> vencidos = pagamentoRepository
                .findByStatusPagamentoAndDataExpiracaoBefore(StatusPagamento.PROCESSANDO, LocalDateTime.now());

        for (Pagamento pagamento : vencidos) {
            Pedido pedido = pagamento.getPedido();
            if (pedido.getStatusPedido() != StatusPedido.PROCESSANDO) {
                continue;
            }

            pagamento.setStatusPagamento(StatusPagamento.EXPIRADO);
            pedido.setStatusPedido(StatusPedido.EXPIRADO);

            pagamentoRepository.save(pagamento);
            pedidoRepository.save(pedido);
            cacheService.atualizarCache(pedido, pagamento, List.of());
            emailService.notificar(pedido.getUsuarioEmail(), pedido.getId(), StatusPedido.EXPIRADO);

            log.info("[EXPIRACAO] Pedido expirado | pedidoId={}", pedido.getId());

            log.info(
                    "business_event",
                    kv("event_type", "ORDER_EXPIRED"),
                    kv("order_id", pedido.getId()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );
        }
    }

    private Pagamento criarPagamentoInicial(Pedido pedido, MetodoPagamento metodo) {
        return Pagamento.builder()
                .pedido(pedido)
                .metodoPagamento(metodo)
                .statusPagamento(StatusPagamento.PROCESSANDO)
                .tentativas(0)
                .build();
    }

    private List<Ingresso> emitirIngressos(Pedido pedido) {
        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        List<Ingresso> ingressos = new ArrayList<>();
        for (int i = 0; i < pedido.getQuantidade(); i++) {
            Ingresso ingresso = Ingresso.builder()
                    .pedido(pedido)
                    .eventoId(pedido.getEventoId())
                    .usuarioId(pedido.getUsuarioId())
                    .codigoIngresso(gerarCodigoIngresso(pedido, i))
                    .statusIngresso(StatusIngresso.ATIVO)
                    .build();
            ingressos.add(ingressoRepository.save(ingresso));
        }
        log.info("[PROCESSAMENTO] {} ingresso(s) emitido(s) | pedidoId={}", ingressos.size(), pedido.getId());

        log.info(
                "business_event",
                kv("event_type", "TICKET_CREATED"),
                kv("order_id", pedido.getId()),
                kv("ticket_amount", ingressos.size()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );
        return ingressos;
    }

    private String gerarCodigoIngresso(Pedido pedido, int sequencia) {
        String uuidCurto = pedido.getId().toString().replace("-", "").substring(0, 8).toUpperCase();
        return String.format("FM-%d-%s-%02d", pedido.getEventoId(), uuidCurto, sequencia + 1);
    }

    private boolean isMétodoPendente(MetodoPagamento metodo) {
        return metodo == MetodoPagamento.PIX || metodo == MetodoPagamento.BOLETO;
    }

    private ResultadoPagamentoMessage montarResultado(Pedido pedido, Pagamento pagamento,
                                                      List<Ingresso> ingressos) {
        List<ResultadoPagamentoMessage.IngressoEmitido> ingressosMsg = ingressos.stream()
                .map(i -> ResultadoPagamentoMessage.IngressoEmitido.builder()
                        .ingressoId(i.getId())
                        .codigoIngresso(i.getCodigoIngresso())
                        .build())
                .toList();

        return ResultadoPagamentoMessage.builder()
                .pedidoId(pedido.getId())
                .eventoId(pedido.getEventoId())
                .usuarioId(pedido.getUsuarioId())
                .quantidade(pedido.getQuantidade())
                .statusPedido(pedido.getStatusPedido())
                .statusPagamento(pagamento.getStatusPagamento())
                .codigoTransacao(pagamento.getCodigoTransacao())
                .motivoRecusa(pagamento.getMotivoRecusa())
                .processadoEm(LocalDateTime.now())
                .ingressosEmitidos(ingressosMsg)
                .build();
    }
}