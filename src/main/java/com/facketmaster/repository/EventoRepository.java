package com.facketmaster.repository;

import com.ingressos.eventservice.model.Evento;
import com.ingressos.eventservice.model.Evento.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("""
        UPDATE Evento e
        SET e.quantidadeDisponivel = :quantidade,
            e.status = CASE
                WHEN :quantidade = 0 THEN com.ingressos.eventservice.model.Evento$StatusEvento.ESGOTADO
                ELSE com.ingressos.eventservice.model.Evento$StatusEvento.ATIVO
            END
        WHERE e.id = :id
    """)
    int atualizarQuantidade(@Param("id") Long id, @Param("quantidade") Integer quantidade);

    boolean existsByIdAndStatus(Long id, StatusEvento status);

    @Query("SELECT e FROM Evento e WHERE e.id = :id AND e.status != 'CANCELADO'")
    Optional<Evento> findByIdAtivo(@Param("id") Long id);
}
