package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repo, PasswordEncoder encoder) {
        return args -> {
            criarSeNaoExistir(repo, encoder, "admin", Perfil.ADMIN);
            criarSeNaoExistir(repo, encoder, "gerente", Perfil.GERENTE);
            criarSeNaoExistir(repo, encoder, "atendente", Perfil.ATENDENTE);
        };
    }

    @Bean
    CommandLineRunner initProdutos(ProdutoRepository repo) {
        return args -> {
            if (repo.count() == 0) {
                repo.saveAll(produtosPadrao());
            }
        };
    }

    @Bean
    CommandLineRunner initPedidos(ClienteRepository clienteRepo, ProdutoRepository produtoRepo,
                                  PedidoRepository pedidoRepo) {
        return args -> {
            if (pedidoRepo.countByResponsavelEntrada("DATA_INITIALIZER") > 0) {
                return;
            }

            List<Produto> produtos = produtoRepo.findAll();
            if (produtos.isEmpty()) {
                produtos = produtoRepo.saveAll(produtosPadrao());
            }

            List<Cliente> clientes = clienteRepo.findAll();
            if (clientes.isEmpty()) {
                clientes = clienteRepo.saveAll(clientesPadrao());
            }

            ThreadLocalRandom random = ThreadLocalRandom.current();
            List<Pedido> pedidos = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                Cliente cliente = clientes.get(random.nextInt(clientes.size()));
                Pedido pedido = new Pedido();
                pedido.setClienteId(cliente.getId());
                pedido.setClienteNome(cliente.getNome());
                pedido.setResponsavelEntrada("DATA_INITIALIZER");
                pedido.setDataHoraEntrada(LocalDateTime.now().minusDays(random.nextInt(0, 130))
                        .minusHours(random.nextInt(0, 24)));
                pedido.setStatus(statusAleatorio(random));

                int quantidadeItens = random.nextInt(1, Math.min(3, produtos.size()) + 1);
                for (int j = 0; j < quantidadeItens; j++) {
                    Produto produto = produtos.get(random.nextInt(produtos.size()));
                    pedido.getItens().add(new ItemPedido(produto.getId(), produto.getNome(),
                            random.nextInt(1, 4), BigDecimal.valueOf(produto.getPreco())));
                }
                pedido.calcularTotal();
                pedidos.add(pedido);
            }
            pedidoRepo.saveAll(pedidos);
        };
    }

    private List<Produto> produtosPadrao() {
        return List.of(
                produto("Pizza Calabresa", "Pizza", 45.0),
                produto("Pizza Mussarela", "Pizza", 42.0),
                produto("Refrigerante 2L", "Bebida", 12.0));
    }

    private void criarSeNaoExistir(UsuarioRepository repo, PasswordEncoder encoder,
                                String username, Perfil perfil) {
        Usuario u = repo.findByUsername(username).orElseGet(() -> {
            Usuario novo = new Usuario();
            novo.setUsername(username);
            novo.setPassword(encoder.encode("teste123"));
            return novo;
        });
        if (u.getRole() == null) {
            u.setRole(perfil);
        }
        repo.save(u);
    }

    private Produto produto(String nome, String tipo, double preco) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setTipo(tipo);
        p.setPreco(preco);
        p.setAtivo(true);
        return p;
    }

    private List<Cliente> clientesPadrao() {
        List<Cliente> clientes = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Cliente cliente = new Cliente();
            cliente.setNome("Cliente Teste " + i);
            cliente.setTelefone("1199999000" + i);
            clientes.add(cliente);
        }
        return clientes;
    }

    private StatusPedido statusAleatorio(ThreadLocalRandom random) {
        StatusPedido[] status = StatusPedido.values();
        return status[random.nextInt(status.length)];
    }
}
