package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Pedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.DateOperators;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final MongoTemplate mongoTemplate;

    public DashboardService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Map<String, Object> resumo(LocalDate dataInicio, LocalDate dataFim, String status, String produtoId) {
        List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> etapas = new ArrayList<>();
        adicionarFiltro(etapas, dataInicio, dataFim, status, produtoId);
        etapas.add(Aggregation.group()
                .sum("total").as("faturamentoTotal")
                .count().as("quantidadePedidos")
                .avg("total").as("ticketMedio")
                .sum(org.springframework.data.mongodb.core.aggregation.ConditionalOperators
                        .when(Criteria.where("status").is("CANCELADO")).then(1).otherwise(0))
                .as("cancelados"));

        Document resultado = mongoTemplate.aggregate(
                Aggregation.newAggregation(etapas), "pedidos", Document.class).getUniqueMappedResult();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("faturamentoTotal", resultado == null ? BigDecimal.ZERO
                : numeroMonetario(resultado.get("faturamentoTotal")));
        resposta.put("quantidadePedidos", resultado == null ? 0 : resultado.get("quantidadePedidos"));
        resposta.put("ticketMedio", resultado == null ? BigDecimal.ZERO
                : numeroMonetario(resultado.get("ticketMedio")));
        resposta.put("cancelados", resultado == null ? 0 : resultado.get("cancelados"));
        return resposta;
    }

    private BigDecimal numeroMonetario(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof Decimal128 decimal128) {
            return decimal128.bigDecimalValue();
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        if (valor instanceof Number numero) {
            return BigDecimal.valueOf(numero.doubleValue());
        }
        return new BigDecimal(valor.toString());
    }

    public List<Document> vendas(LocalDate dataInicio, LocalDate dataFim, String status, String produtoId,
            String agrupamento) {
        String formato = agrupamento.equals("mes") ? "%Y-%m"
                : agrupamento.equals("semana") ? "%G-W%V" : "%Y-%m-%d";
        List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> etapas = new ArrayList<>();
        adicionarFiltro(etapas, dataInicio, dataFim, status, produtoId);
        etapas.add(Aggregation.project("total")
                .and(DateOperators.DateToString.dateOf("dataHoraEntrada").toString(formato)).as("periodo"));
        etapas.add(Aggregation.group("periodo").sum("total").as("total").count().as("quantidade"));
        etapas.add(Aggregation.project("total", "quantidade").and("_id").as("periodo").andExclude("_id"));
        etapas.add(Aggregation.sort(Sort.Direction.ASC, "periodo"));
        return mongoTemplate.aggregate(Aggregation.newAggregation(etapas), "pedidos", Document.class)
                .getMappedResults();
    }

    public List<Document> status(LocalDate dataInicio, LocalDate dataFim, String status, String produtoId) {
        List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> etapas = new ArrayList<>();
        adicionarFiltro(etapas, dataInicio, dataFim, status, produtoId);
        etapas.add(Aggregation.group("status").count().as("quantidade"));
        etapas.add(Aggregation.project("quantidade").and("_id").as("status").andExclude("_id"));
        etapas.add(Aggregation.sort(Sort.Direction.ASC, "status"));
        return mongoTemplate.aggregate(Aggregation.newAggregation(etapas), "pedidos", Document.class)
                .getMappedResults();
    }

    public List<Document> produtosTop(LocalDate dataInicio, LocalDate dataFim, String status, String produtoId,
            int limite) {
        List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> etapas = new ArrayList<>();
        adicionarFiltro(etapas, dataInicio, dataFim, status, produtoId);
        etapas.add(Aggregation.unwind("itens"));
        etapas.add(Aggregation.group("itens.nomeProduto")
                .sum("itens.quantidade").as("qtd")
                .sum(org.springframework.data.mongodb.core.aggregation.ArithmeticOperators.Multiply
                        .valueOf("itens.quantidade").multiplyBy("itens.precoUnitario")).as("receita"));
        etapas.add(Aggregation.project("qtd", "receita").and("_id").as("produto").andExclude("_id"));
        etapas.add(Aggregation.sort(Sort.Direction.DESC, "qtd"));
        etapas.add(Aggregation.limit(limite));
        return mongoTemplate.aggregate(Aggregation.newAggregation(etapas), "pedidos", Document.class)
                .getMappedResults();
    }

    public List<Pedido> pedidosRecentes(LocalDate dataInicio, LocalDate dataFim, String status, String produtoId) {
        Query query = new Query(filtro(dataInicio, dataFim, status, produtoId));
        query.with(Sort.by(Sort.Direction.DESC, "dataHoraEntrada"));
        query.limit(10);
        return mongoTemplate.find(query, Pedido.class);
    }

    private void adicionarFiltro(List<org.springframework.data.mongodb.core.aggregation.AggregationOperation> etapas,
            LocalDate dataInicio, LocalDate dataFim, String status, String produtoId) {
        etapas.add(Aggregation.match(filtro(dataInicio, dataFim, status, produtoId)));
    }

    private Criteria filtro(LocalDate dataInicio, LocalDate dataFim, String status, String produtoId) {
        List<Criteria> filtros = new ArrayList<>();
        if (dataInicio != null) {
            filtros.add(Criteria.where("dataHoraEntrada").gte(dataInicio.atStartOfDay()));
        }
        if (dataFim != null) {
            LocalDateTime fim = dataFim.plusDays(1).atStartOfDay();
            filtros.add(Criteria.where("dataHoraEntrada").lt(fim));
        }
        if (status != null && !status.isBlank()) {
            filtros.add(Criteria.where("status").is(status));
        }
        if (produtoId != null && !produtoId.isBlank()) {
            filtros.add(Criteria.where("itens.produtoId").is(produtoId));
        }
        return filtros.isEmpty() ? new Criteria() : new Criteria().andOperator(filtros);
    }
}
