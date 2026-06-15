package com.facketmaster.event.service;

//import com.facketmaster.config.EventoPublisher;

import com.facketmaster.config.AuthenticatedUserProvider;
import com.facketmaster.controller.response.JwtTokenResponse;
import com.facketmaster.event.controller.request.AtualizarEventoRequest;
import com.facketmaster.event.controller.request.CriarEventoRequest;
import com.facketmaster.event.controller.response.EventoResponse;
import com.facketmaster.event.entity.Evento;
import com.facketmaster.event.exception.EstoqueInsuficienteException;
import com.facketmaster.event.exception.EventoNotFoundException;
import com.facketmaster.event.mapper.EventoMapper;
import com.facketmaster.event.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repository;
    private final EventoMapper mapper;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public EventoResponse criar(CriarEventoRequest request) {
        Evento evento = mapper.toModel(request);
        Evento salvo = repository.save(evento);
        EventoResponse response = mapper.toResponse(salvo);

        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info(
                "business_event",
                kv("event_type", "EVENT_CREATED"),
                kv("event_id", salvo.getId()),
                kv("event_name", salvo.getNome()),
                kv("event_desciption", salvo.getDescricao()),
                kv("ticket_price", salvo.getValor()),
                kv("event_date", salvo.getDataEvento()),
                kv("event_location", salvo.getLocal()),
                kv("total_tickets", salvo.getQuantidadeTotal()),
                kv("available_tickets", salvo.getQuantidadeDisponivel()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );

        return response;
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listarTodos() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listarDisponiveis() {
        return repository.findEventosDisponivels(LocalDateTime.now())
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EventoResponse buscarPorId(Long id) {
        Evento evento = repository.findById(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
        return mapper.toResponse(evento);
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome)
                .stream().map(mapper::toResponse).toList();
    }

    @Transactional
    public EventoResponse atualizar(Long id, AtualizarEventoRequest request) {
        Evento evento = repository.findByIdAtivo(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
        mapper.aplicarAtualizacao(request, evento);
        Evento atualizado = repository.save(evento);
        EventoResponse response = mapper.toResponse(atualizado);

        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info(
                "business_event",
                kv("event_type", "EVENT_UPDATED"),
                kv("event_id", atualizado.getId()),
                kv("new_event_name", atualizado.getNome()),
                kv("new_event_desciption", atualizado.getDescricao()),
                kv("new_ticket_price", atualizado.getValor()),
                kv("new_event_date", atualizado.getDataEvento()),
                kv("new_event_location", atualizado.getLocal()),
                kv("new_total_tickets", atualizado.getQuantidadeTotal()),
                kv("new_available_tickets", atualizado.getQuantidadeDisponivel()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );

        return response;
    }

    @Transactional
    public Optional<EventoResponse> atualizarQuantidade(Long id, Integer novaQuantidade) {
        Optional<Evento> optEvento = repository.findById(id);
        if (optEvento.isPresent()) {
            Evento evento = optEvento.get();

            if (novaQuantidade > evento.getQuantidadeTotal()) {
                throw new IllegalArgumentException(
                        "A quantidade disponível (%d) não pode exceder o total de ingressos do evento (%d)."
                                .formatted(novaQuantidade, evento.getQuantidadeTotal()));
            }

            if (novaQuantidade == 0) {
                evento.setQuantidadeDisponivel(novaQuantidade);
                evento.setStatus(Evento.StatusEvento.ESGOTADO);
            }
            if (novaQuantidade > 0) {
                evento.setQuantidadeDisponivel(novaQuantidade);
                evento.setStatus(Evento.StatusEvento.ATIVO);
            }

            repository.save(evento);

            JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

            log.info(
                    "business_event",
                    kv("event_type", "EVENT_AMOUNT_UPDATED"),
                    kv("event_id", evento.getId()),
                    kv("event_name", evento.getNome()),
                    kv("ticket_amount", evento.getQuantidadeDisponivel()),
                    kv("user_id", user != null ? user.id() : null),
                    kv("user_email", user != null ? user.email() : "unknown"),
                    kv("user_role", user != null ? user.role() : "unknown")
            );

            EventoResponse eventoResponse = mapper.toResponse(evento);
            return Optional.of(eventoResponse);
        }
        return Optional.empty();
    }

    @Transactional
    public void deletar(Long id) {
        Evento evento = repository.findById(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
        evento.setStatus(Evento.StatusEvento.CANCELADO);
        repository.save(evento);

        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info(
                "business_event",
                kv("event_type", "EVENT_CANCELLED"),
                kv("event_id", evento.getId()),
                kv("event_name", evento.getNome()),
                kv("event_status", evento.getStatus()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );
    }

    public boolean isAvailable(Long id) {
        Optional<Evento> optEvento = repository.findById(id);
        if (optEvento.isPresent() && optEvento.get().getQuantidadeDisponivel() != 0) {
            return true;
        } else
            return false;
    }

    public EventoResponse updateStatus(Long id, Evento.StatusEvento newStatus) {
        Evento evento = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado")
                );
        evento.setStatus(newStatus);

        Evento updatedEvent = repository.save(evento);

        JwtTokenResponse user = authenticatedUserProvider.getCurrentUser();

        log.info(
                "business_event",
                kv("event_type", "EVENT_STATUS_UPDATE"),
                kv("event_id", evento.getId()),
                kv("event_name", evento.getNome()),
                kv("event_status", evento.getStatus()),
                kv("user_id", user != null ? user.id() : null),
                kv("user_email", user != null ? user.email() : "unknown"),
                kv("user_role", user != null ? user.role() : "unknown")
        );

        return mapper.toResponse(updatedEvent);
    }

    /**
     * Reserva {@code quantidade} ingressos do evento de forma atômica.
     * Usado pelo payment-service antes de persistir um pedido, para
     * evitar overselling.
     *
     * @throws EventoNotFoundException        se o evento não existir
     * @throws EstoqueInsuficienteException    se o evento não estiver ATIVO
     *                                          ou não houver disponibilidade
     */
    @Transactional
    public void reservar(Long eventoId, Integer quantidade) {
        if (!repository.existsById(eventoId)) {
            throw new EventoNotFoundException(eventoId);
        }

        int linhasAfetadas = repository.reservarQuantidade(eventoId, quantidade);
        if (linhasAfetadas == 0) {
            throw new EstoqueInsuficienteException(eventoId);
        }

        log.info("[ESTOQUE] Reserva efetuada | eventoId={} quantidade={}", eventoId, quantidade);
    }

    /**
     * Libera (devolve) {@code quantidade} ingressos previamente reservados
     * para o evento. Usado quando um pedido é recusado, cancelado ou expira.
     *
     * @throws EventoNotFoundException se o evento não existir
     */
    @Transactional
    public void liberar(Long eventoId, Integer quantidade) {
        int linhasAfetadas = repository.liberarQuantidade(eventoId, quantidade);
        if (linhasAfetadas == 0) {
            throw new EventoNotFoundException(eventoId);
        }

        log.info("[ESTOQUE] Reserva liberada | eventoId={} quantidade={}", eventoId, quantidade);
    }
}
