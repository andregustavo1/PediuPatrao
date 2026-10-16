package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Perfil;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
}