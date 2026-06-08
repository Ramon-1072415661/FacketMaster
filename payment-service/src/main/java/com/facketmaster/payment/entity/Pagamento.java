package com.facketmaster.payment.entity;

import com.facketmaster.payment.enums.MetodoPagamento;
import com.facketmaster.payment.enums.StatusPagamento;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagamentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetodoPagamento metodoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;

    @Column(length = 100)
    private String codigoTransacao;

    @Column(length = 500)
    private String codigoPagamento;

    @Column(length = 1000)
    private String urlPagamento;

    private LocalDateTime dataExpiracao;

    private LocalDateTime dataAprovacao;

    @Column(length = 500)
    private String motivoRecusa;

    @Column(nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime atualizadoEm;
}