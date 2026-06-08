package com.facketmaster.payment.entity;

import com.facketmaster.payment.enums.StatusIngresso;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingressos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ingresso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Column(nullable = false)
    private Long eventoId;

    @Column(nullable = false, length = 100)
    private String usuarioId;

    @Column(nullable = false, unique = true, length = 100)
    private String codigoIngresso;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private StatusIngresso statusIngresso = StatusIngresso.ATIVO;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime emitidoEm;

    private LocalDateTime utilizadoEm;
}