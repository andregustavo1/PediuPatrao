package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "pedidos")
public class Pedido {

    private static final BigDecimal LIMITE_DESCONTO = new BigDecimal("20");

    @Id
    private String id;
    private String clienteId;
    private String clienteNome;
    private List<ItemPedido> itens = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal percentualDesconto = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private StatusPedido status;
    private String responsavelEntrada;
    private LocalDateTime dataHoraEntrada;
    private String responsavelSaida;
    private LocalDateTime dataHoraSaida;
    private String canceladoPor;
    private LocalDateTime dataHoraCancelamento;
    private String justificativaCancelamento;

    public void calcularTotal() {
        subtotal = itens.stream().map(ItemPedido::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal desconto = subtotal.multiply(percentualDesconto).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        total = subtotal.subtract(desconto);
    }

    public void mudarStatus(StatusPedido novo, String usuario) {
        if (!status.podeIrPara(novo)) {
            throw new IllegalStateException("Transição inválida: " + status + " -> " + novo);
        }
        status = novo;
        if (novo == StatusPedido.SAIU_PARA_ENTREGA || novo == StatusPedido.RETIRADO) {
            responsavelSaida = usuario;
            dataHoraSaida = LocalDateTime.now();
        }
    }

    public void aplicarDesconto(BigDecimal percentual) {
        if (!status.antesDaSaida()) {
            throw new IllegalStateException("Desconto só é permitido antes da saída");
        }
        if (percentual == null || percentual.signum() <= 0 || percentual.compareTo(LIMITE_DESCONTO) > 0) {
            throw new IllegalArgumentException("Desconto deve ser maior que 0 e no máximo 20%");
        }
        percentualDesconto = percentual;
        calcularTotal();
    }

    public void cancelar(String justificativa, String usuario) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa obrigatória");
        }
        if (!status.antesDaSaida()) {
            throw new IllegalStateException("Pedido não pode mais ser cancelado");
        }
        status = StatusPedido.CANCELADO;
        justificativaCancelamento = justificativa;
        canceladoPor = usuario;
        dataHoraCancelamento = LocalDateTime.now();
    }
}
