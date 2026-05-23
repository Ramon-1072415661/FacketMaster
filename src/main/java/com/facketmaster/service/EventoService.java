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
import java.util.Optional;

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
    public Optional<EventoResponse> atualizarQuantidade(Long id, Integer novaQuantidade) {
        log.info("Atualizando quantidade | id={} novaQtd={}", id, novaQuantidade);
        Optional<Evento> optEvento = repository.findById(id);
        if(optEvento.isPresent()){
            Evento evento = optEvento.get();

            if(novaQuantidade == 0){
                evento.setQuantidadeDisponivel(novaQuantidade);
                evento.setStatus(Evento.StatusEvento.ESGOTADO);
            }
            if(novaQuantidade > 0){
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
        //publisher.publicarEventoDeletado(id);
        log.info("Evento cancelado | id={}", id);
    }

    // Verifica se o evento esta disponivel (quantidade diferente de 0)
    public boolean isAvailable(Long id){
        Optional<Evento> optEvento = repository.findById(id);
        if(optEvento.isPresent() && optEvento.get().getQuantidadeDisponivel() != 0){
            return true;
        } else return false;
    }
}
