package com.umc.pediupatrao.entity;

public enum StatusPedido {
    RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO;

    public boolean podeIrPara(StatusPedido novo) {
        return switch (this) {
            case RECEBIDO -> novo == EM_PREPARACAO;
            case EM_PREPARACAO -> novo == PRONTO;
            case PRONTO -> novo == SAIU_PARA_ENTREGA || novo == RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> novo == FINALIZADO;
            default -> false;
        };
    }

    public boolean antesDaSaida() {
        return this == RECEBIDO || this == EM_PREPARACAO || this == PRONTO;
    }
}
