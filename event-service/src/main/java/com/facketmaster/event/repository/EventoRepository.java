package com.facketmaster.event.repository;

import com.facketmaster.event.entity.Evento;
import com.facketmaster.event.entity.Evento.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByStatusOrderByDataEventoAsc(StatusEvento status);

    @Query("""
                SELECT e FROM Evento e
                WHERE e.status = 'ATIVO'
                  AND e.dataEvento > :agora
                ORDER BY e.dataEvento ASC
            """)
    List<Evento> findEventosDisponivels(@Param("agora") LocalDateTime agora);

    @Query("""
                SELECT e FROM Evento e
                WHERE LOWER(e.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
                  AND e.status != 'CANCELADO'
            """)
    List<Evento> findByNomeContainingIgnoreCase(@Param("nome") String nome);

    boolean existsByIdAndStatus(Long id, StatusEvento status);

    @Query("SELECT e FROM Evento e WHERE e.id = :id AND e.status != 'CANCELADO'")
    Optional<Evento> findByIdAtivo(@Param("id") Long id);
}
