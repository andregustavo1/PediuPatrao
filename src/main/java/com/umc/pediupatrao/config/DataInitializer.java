package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.ProdutoRepository;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
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
                repo.saveAll(List.of(
                        produto("Pizza Calabresa", "Pizza", 45.0),
                        produto("Pizza Mussarela", "Pizza", 42.0),
                        produto("Refrigerante 2L", "Bebida", 12.0)));
            }
        };
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
}
