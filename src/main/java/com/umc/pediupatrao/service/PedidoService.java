package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private AuditoriaService auditoria;

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataHoraEntrada"));
    }

    public Pedido criarPedido(PedidoRequest req) {
        Cliente cliente = clienteRepository.findById(req.clienteId()).orElseThrow();
        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        for (PedidoRequest.Item item : req.itens()) {
            Produto produto = produtoRepository.findById(item.produtoId()).orElseThrow();
            pedido.getItens().add(new ItemPedido(produto.getId(), produto.getNome(), item.quantidade(), BigDecimal.valueOf(produto.getPreco())));
        }
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setResponsavelEntrada(auditoria.usuarioAtual());
        pedido.setDataHoraEntrada(LocalDateTime.now());
        pedido.calcularTotal();
        Pedido salvo = pedidoRepository.save(pedido);
        auditoria.registrar("CRIACAO_PEDIDO", "Pedido", salvo.getId(), List.of(
                Alteracao.de("status", null, salvo.getStatus()),
                Alteracao.de("total", null, salvo.getTotal())), null);
        return salvo;
    }

    public Pedido atualizarStatus(String id, String status) {
        Pedido pedido = buscar(id);
        StatusPedido anterior = pedido.getStatus();
        pedido.mudarStatus(StatusPedido.valueOf(status), auditoria.usuarioAtual());
        pedidoRepository.save(pedido);
        auditoria.registrar("ALTERACAO_STATUS", "Pedido", id, List.of(
                Alteracao.de("status", anterior, pedido.getStatus())), null);
        return pedido;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = buscar(id);
        BigDecimal percentualAnterior = pedido.getPercentualDesconto();
        BigDecimal totalAnterior = pedido.getTotal();
        pedido.aplicarDesconto(percentual);
        pedidoRepository.save(pedido);
        auditoria.registrar("DESCONTO", "Pedido", id, List.of(
                Alteracao.de("percentualDesconto", percentualAnterior, pedido.getPercentualDesconto()),
                Alteracao.de("total", totalAnterior, pedido.getTotal())), null);
        return pedido;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        Pedido pedido = buscar(id);
        StatusPedido anterior = pedido.getStatus();
        pedido.cancelar(justificativa, auditoria.usuarioAtual());
        pedidoRepository.save(pedido);
        auditoria.registrar("CANCELAMENTO", "Pedido", id, List.of(
                Alteracao.de("status", anterior, pedido.getStatus())), justificativa);
        return pedido;
    }

    private Pedido buscar(String id) {
        return pedidoRepository.findById(id).orElseThrow();
    }
}
