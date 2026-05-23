package com.facketmaster.service;

//import com.facketmaster.config.EventoPublisher;
import com.facketmaster.controller.request.CriarEventoRequest;
import com.facketmaster.controller.request.AtualizarEventoRequest;
import com.facketmaster.controller.response.EventoResponse;
import com.facketmaster.entity.Evento;
import com.facketmaster.exception.EventoNotFoundException;
import com.facketmaster.mapper.EventoMapper;
import com.facketmaster.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repository;
    private final EventoMapper mapper;
    //private final EventoPublisher publisher;

    @Transactional
    public EventoResponse criar(CriarEventoRequest request) {
        log.info("Criando evento | nome={}", request.getNome());
        Evento evento = mapper.toModel(request);
        Evento salvo = repository.save(evento);
        EventoResponse response = mapper.toResponse(salvo);
        //publisher.publicarEventoCriado(response);
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
        //publisher.publicarEventoAtualizado(response);
        log.info("Evento atualizado | id={}", id);
        return response;
    }

    @Transactional
    public EventoResponse atualizarQuantidade(Long id, Integer novaQuantidade) {
        log.info("Atualizando quantidade | id={} novaQtd={}", id, novaQuantidade);
        if (!repository.existsById(id)) throw new EventoNotFoundException(id);
        if (novaQuantidade < 0) throw new IllegalArgumentException("A quantidade não pode ser negativa");
        repository.atualizarQuantidade(id, novaQuantidade);
        Evento evento = repository.findById(id).orElseThrow(() -> new EventoNotFoundException(id));
        EventoResponse response = mapper.toResponse(evento);
        //publisher.publicarEventoAtualizado(response);
        return response;
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Cancelando evento | id={}", id);
        Evento evento = repository.findById(id)
                .orElseThrow(() -> new EventoNotFoundException(id));
        evento.setStatus(Evento.StatusEvento.CANCELADO);
        repository.save(evento);
        //publisher.publicarEventoDeletado(id);
        log.info("Evento cancelado | id={}", id);
    }
}
