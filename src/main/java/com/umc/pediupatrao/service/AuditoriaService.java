package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Alteracao;
import com.umc.pediupatrao.entity.RegistroAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    public String usuarioAtual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public void registrar(String operacao, String entidade, String entidadeId, List<Alteracao> alteracoes, String justificativa) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.setUsuario(auth.getName());
        registro.setPerfil(auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .findFirst().orElse(null));
        registro.setDataHora(LocalDateTime.now());
        registro.setOperacao(operacao);
        registro.setEntidade(entidade);
        registro.setEntidadeId(entidadeId);
        registro.setAlteracoes(alteracoes);
        registro.setJustificativa(justificativa);
        auditoriaRepository.insert(registro);
    }

    public List<RegistroAuditoria> listar() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }
}