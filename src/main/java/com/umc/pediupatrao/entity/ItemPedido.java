package com.umc.pediupatrao.entity;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

    private String produtoId;
    private String nomeProduto;
    private int quantidade;
    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal precoUnitario;

    public BigDecimal getSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
