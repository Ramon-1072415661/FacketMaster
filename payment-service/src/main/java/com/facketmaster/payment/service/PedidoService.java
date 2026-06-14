package com.facketmaster.payment.service;

import com.facketmaster.config.AuthenticatedUserProvider;
import com.facketmaster.controller.response.JwtTokenResponse;
import com.facketmaster.payment.client.EventoClient;
import com.facketmaster.payment.controller.request.CriarPedidoRequest;
import com.facketmaster.payment.controller.response.PedidoResponse;
import com.facketmaster.payment.entity.Pagamento;
import com.facketmaster.payment.entity.Pedido;
import com.facketmaster.payment.enums.StatusPagamento;
import com.facketmaster.payment.enums.StatusPedido;
import com.facketmaster.payment.exception.PedidoNotFoundException;
import com.facketmaster.payment.mapper.PedidoMapper;
import com.facketmaster.payment.messaging.PaymentPublisher;
import com.facketmaster.payment.messaging.PedidoPagamentoMessage;
import com.facketmaster.payment.repository.PagamentoRepository;
import com.facketmaster.payment.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final PagamentoRepository pagamentoRepository;
    private final PedidoMapper mapper;
    private final PaymentPublisher publisher;
    private final PedidoCacheService cacheService;
    private final ProcessamentoPagamentoService processamentoService;
    private final EventoClient eventoClient;
    private final EmailService emailService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public PedidoResponse criar(CriarPedidoRequest request, String usuarioId, String usuarioEmail, String authorizationHeader) {
        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info("[PEDIDO] Criando pedido | eventoId={} usuario={} metodo={}",
                request.getEventoId(), usuarioId, request.getMetodoPagamento());

        log.info(
                "business_event",
                kv("event_type", "CREATING_ORDER"),
                kv("order_id", request.getEventoId()),
                kv("order_payment_method", request.getMetodoPagamento()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );

        eventoClient.reservar(request.getEventoId(), request.getQuantidade(), authorizationHeader);

        try {
            BigDecimal valorTotal = request.getValorUnitario()
                    .multiply(BigDecimal.valueOf(request.getQuantidade()));

            Pedido pedido = Pedido.builder()
                    .eventoId(request.getEventoId())
                    .usuarioId(usuarioId)
                    .usuarioEmail(usuarioEmail)
                    .quantidade(request.getQuantidade())
                    .valorUnitario(request.getValorUnitario())
                    .valorTotal(valorTotal)
                    .metodoPagamento(request.getMetodoPagamento())
                    .statusPedido(StatusPedido.AGUARDANDO_PAGAMENTO)
                    .build();

            pedido = pedidoRepository.save(pedido);
            emailService.notificar(usuarioEmail, pedido.getId(), StatusPedido.AGUARDANDO_PAGAMENTO);

            Pagamento pagamento = Pagamento.builder()
                    .pedido(pedido)
                    .metodoPagamento(request.getMetodoPagamento())
                    .statusPagamento(StatusPagamento.PENDENTE)
                    .tentativas(0)
                    .build();
            pagamento = pagamentoRepository.save(pagamento);

            cacheService.atualizarCache(pedido, pagamento, List.of());

            PedidoPagamentoMessage message = mapper.toMessage(pedido, request);

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publisher.publicarPedido(message);
                }
            });

            log.info("[PEDIDO] Pedido criado e publicado na fila | pedidoId={}", pedido.getId());


            log.info(
                    "business_event",
                    kv("event_type", "ORDER_CREATED"),
                    kv("order_id", pedido.getId()),
                    kv("order_amount", pedido.getQuantidade()),
                    kv("order_total_value", pedido.getValorTotal()),
                    kv("order_payment_method", pedido.getMetodoPagamento()),
                    kv("order_status", pedido.getStatusPedido()),
                    kv("order_createdAt", pedido.getCriadoEm()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );
            return mapper.toResponse(pedido, pagamento, List.of());
        } catch (Exception ex) {
            log.error("[PEDIDO] Falha ao criar pedido após reserva de ingressos, liberando reserva | eventoId={} quantidade={}",
                    request.getEventoId(), request.getQuantidade(), ex);
            eventoClient.liberar(request.getEventoId(), request.getQuantidade(), authorizationHeader);

            log.error(
                    "business_event",
                    kv("event_type", "ORDER_CREATED_ERROR"),
                    kv("order_id", request.getEventoId()),
                    kv("order_amount", request.getQuantidade()),
                    kv("error_message", ex),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );

            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(UUID id) {
        Optional<PedidoStatusCache> cached = cacheService.buscarPorId(id.toString());
        if (cached.isPresent()) {
            return mapper.fromCache(cached.get());
        }

        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));

        Pagamento pagamento = pagamentoRepository
                .findTopByPedidoIdOrderByCriadoEmDesc(id)
                .orElseThrow(() -> new PedidoNotFoundException(id));

        return mapper.toResponse(pedido, pagamento, pedido.getIngressos());
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorUsuario(String usuarioId) {
        return pedidoRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(p -> {
                    Optional<PedidoStatusCache> cached = cacheService.buscarPorId(p.getId().toString());
                    if (cached.isPresent()) return mapper.fromCache(cached.get());

                    Pagamento pag = pagamentoRepository
                            .findTopByPedidoIdOrderByCriadoEmDesc(p.getId())
                            .orElse(Pagamento.builder()
                                    .statusPagamento(StatusPagamento.PENDENTE).build());
                    return mapper.toResponse(p, pag, p.getIngressos());
                })
                .toList();
    }

    @Transactional
    public PedidoResponse confirmarPagamento(UUID pedidoId) {
        processamentoService.confirmarPagamento(pedidoId);
        return buscarPorId(pedidoId);
    }

    @Transactional
    public PedidoResponse cancelarPedido(UUID pedidoId) {
        processamentoService.cancelarPedido(pedidoId);
        return buscarPorId(pedidoId);
    }
}
