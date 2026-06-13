package com.facketmaster.payment.repository;

import com.facketmaster.payment.entity.Pagamento;
import com.facketmaster.payment.enums.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    Optional<Pagamento> findTopByPedidoIdOrderByCriadoEmDesc(UUID pedidoId);

    List<Pagamento> findByPedidoIdOrderByCriadoEmDesc(UUID pedidoId);

    List<Pagamento> findByStatusPagamento(StatusPagamento status);

    List<Pagamento> findByStatusPagamentoAndDataExpiracaoBefore(StatusPagamento status, LocalDateTime agora);
}
