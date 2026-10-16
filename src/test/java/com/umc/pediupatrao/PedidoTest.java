package com.umc.pediupatrao;

import static org.junit.jupiter.api.Assertions.*;

import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PedidoTest {

    private Pedido novoPedido() {
        Pedido p = new Pedido();
        p.setStatus(StatusPedido.RECEBIDO);
        p.getItens().add(new ItemPedido("1", "Pizza", 2, new BigDecimal("50.00")));
        p.calcularTotal();
        return p;
    }

    @Test
    void calculaTotal() {
        assertEquals(0, new BigDecimal("100.00").compareTo(novoPedido().getTotal()));
    }

    @Test
    void aplicaDescontoDe10PorCento() {
        Pedido p = novoPedido();
        p.aplicarDesconto(new BigDecimal("10"));
        assertEquals(0, new BigDecimal("90.00").compareTo(p.getTotal()));
    }

    @Test
    void bloqueiaDescontoAcimaDe20() {
        assertThrows(IllegalArgumentException.class, () -> novoPedido().aplicarDesconto(new BigDecimal("25")));
    }

    @Test
    void bloqueiaTransicaoInvalida() {
        assertThrows(IllegalStateException.class, () -> novoPedido().mudarStatus(StatusPedido.FINALIZADO, "a"));
    }

    @Test
    void registraSaidaEBloqueiaDescontoECancelamento() {
        Pedido p = novoPedido();
        p.mudarStatus(StatusPedido.EM_PREPARACAO, "a");
        p.mudarStatus(StatusPedido.PRONTO, "a");
        p.mudarStatus(StatusPedido.SAIU_PARA_ENTREGA, "entregador");
        assertEquals("entregador", p.getResponsavelSaida());
        assertNotNull(p.getDataHoraSaida());
        assertThrows(IllegalStateException.class, () -> p.aplicarDesconto(BigDecimal.TEN));
        assertThrows(IllegalStateException.class, () -> p.cancelar("motivo", "g"));
    }

    @Test
    void canceladoNaoMudaDeStatus() {
        Pedido p = novoPedido();
        p.cancelar("cliente desistiu", "gerente");
        assertThrows(IllegalStateException.class, () -> p.mudarStatus(StatusPedido.EM_PREPARACAO, "a"));
    }

    @Test
    void cancelamentoExigeJustificativa() {
        assertThrows(IllegalArgumentException.class, () -> novoPedido().cancelar(" ", "g"));
    }
}