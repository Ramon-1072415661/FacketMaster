package com.facketmaster.event.repository;

import com.facketmaster.event.entity.Evento;
import com.facketmaster.event.entity.Evento.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Reserva {@code qtd} ingressos de forma atômica (compare-and-swap no banco).
     * Só decrementa se o evento estiver ATIVO e houver disponibilidade suficiente.
     * Marca o evento como ESGOTADO se a disponibilidade chegar a zero.
     *
     * @return quantidade de linhas afetadas (0 = sem disponibilidade/evento inexistente/inativo)
     */
    @Modifying
    @Transactional
    @Query(value = """
                UPDATE eventos
                SET quantidade_disponivel = quantidade_disponivel - :qtd,
                    status = CASE
                        WHEN quantidade_disponivel - :qtd = 0 THEN 'ESGOTADO'
                        ELSE status
                    END
                WHERE id = :id
                  AND status = 'ATIVO'
                  AND quantidade_disponivel >= :qtd
            """, nativeQuery = true)
    int reservarQuantidade(@Param("id") Long id, @Param("qtd") Integer qtd);

    /**
     * Libera {@code qtd} ingressos previamente reservados, devolvendo-os à
     * disponibilidade do evento. Se o evento estava ESGOTADO e volta a ter
     * disponibilidade, retorna para ATIVO.
     *
     * @return quantidade de linhas afetadas (0 = evento inexistente)
     */
    @Modifying
    @Transactional
    @Query(value = """
                UPDATE eventos
                SET quantidade_disponivel = LEAST(quantidade_disponivel + :qtd, quantidade_total),
                    status = CASE
                        WHEN status = 'ESGOTADO' AND quantidade_disponivel + :qtd > 0 THEN 'ATIVO'
                        ELSE status
                    END
                WHERE id = :id
            """, nativeQuery = true)
    int liberarQuantidade(@Param("id") Long id, @Param("qtd") Integer qtd);
}
