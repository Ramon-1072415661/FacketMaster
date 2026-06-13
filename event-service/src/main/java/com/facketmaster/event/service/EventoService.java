package com.facketmaster.event.service;

//import com.facketmaster.config.EventoPublisher;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repository;
    private final EventoMapper mapper;

    @Transactional
    public EventoResponse criar(CriarEventoRequest request) {
        log.info("Criando evento | nome={}", request.name());
        Evento evento = mapper.toModel(request);
        Evento salvo = repository.save(evento);
        EventoResponse response = mapper.toResponse(salvo);
        log.info("Evento criado com sucesso | id={} nome={}", salvo.getId(), salvo.getNome());
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
        log.info("Atualizando evento | id={}", id);
        Evento evento = repository.findByIdAtivo(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
        mapper.aplicarAtualizacao(request, evento);
        Evento atualizado = repository.save(evento);
        EventoResponse response = mapper.toResponse(atualizado);
        log.info("Evento atualizado | id={}", id);
        return response;
    }

    @Transactional
    public Optional<EventoResponse> atualizarQuantidade(Long id, Integer novaQuantidade) {
        log.info("Atualizando quantidade | id={} novaQtd={}", id, novaQuantidade);
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

            EventoResponse eventoResponse = mapper.toResponse(evento);
            return Optional.of(eventoResponse);
        }
        return Optional.empty();
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Cancelando evento | id={}", id);
        Evento evento = repository.findById(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
        evento.setStatus(Evento.StatusEvento.CANCELADO);
        repository.save(evento);
        log.info("Evento cancelado | id={}", id);
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
