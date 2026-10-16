package com.umc.pediupatrao.entity;

import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alteracao {

    private String campo;
    private String valorAnterior;
    private String valorNovo;

    public static Alteracao de(String campo, Object anterior, Object novo) {
        return new Alteracao(campo, Objects.toString(anterior, null), Objects.toString(novo, null));
    }

    public static void comparar(List<Alteracao> lista, String campo, String anterior, String novo) {
        if (!Objects.toString(anterior, "").equals(Objects.toString(novo, ""))) {
            lista.add(new Alteracao(campo, anterior, novo));
        }
    }
}