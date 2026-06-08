package com.facketmaster.payment.controller.request;

import com.facketmaster.payment.enums.MetodoPagamento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CriarPedidoRequest {

    @NotNull(message = "O ID do evento é obrigatório")
    private Long eventoId;

    @NotBlank(message = "O ID do usuário é obrigatório")
    @Size(max = 100)
    private String usuarioId;

    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade mínima é 1")
    @Max(value = 10, message = "Limite de 10 ingressos por pedido")
    private Integer quantidade;

    @NotNull(message = "O valor unitário é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor mínimo é R$ 0,01")
    private BigDecimal valorUnitario;

    @NotNull(message = "O método de pagamento é obrigatório")
    private MetodoPagamento metodoPagamento;

    @Valid
    private DadosCartaoRequest dadosCartao;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DadosCartaoRequest {

        @NotBlank(message = "O número do cartão é obrigatório")
        @Pattern(regexp = "\\d{4}\\.\\d{4}\\.\\d{4}\\.\\d{4}",
                message = "Formato inválido. Use: XXXX.XXXX.XXXX.XXXX")
        private String numero;

        @NotBlank(message = "O nome do titular é obrigatório")
        @Size(max = 100)
        private String nomeTitular;

        @NotBlank(message = "A validade é obrigatória")
        @Pattern(regexp = "\\d{2}/\\d{2}", message = "Formato inválido. Use: MM/AA")
        private String validade;

        @NotBlank(message = "O CVV é obrigatório")
        @Pattern(regexp = "\\d{3,4}", message = "CVV inválido")
        private String cvv;

        @NotNull(message = "O número de parcelas é obrigatório")
        @Min(value = 1, message = "Mínimo de 1 parcela")
        @Max(value = 12, message = "Máximo de 12 parcelas")
        private Integer parcelas;

        @NotBlank(message = "A bandeira é obrigatória")
        private String bandeira;
    }
}