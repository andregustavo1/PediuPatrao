package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public Map<String, Object> resumo(@RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim, @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return dashboardService.resumo(dataInicio, dataFim, status, produtoId);
    }

    @GetMapping("/vendas")
    public List<Document> vendas(@RequestParam(defaultValue = "dia") String agrupamento,
            @RequestParam(required = false) LocalDate dataInicio, @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String status, @RequestParam(required = false) String produtoId) {
        return dashboardService.vendas(dataInicio, dataFim, status, produtoId, agrupamento);
    }

    @GetMapping("/status")
    public List<Document> status(@RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim, @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return dashboardService.status(dataInicio, dataFim, status, produtoId);
    }

    @GetMapping("/produtos-top")
    public List<Document> produtosTop(@RequestParam(defaultValue = "5") int limite,
            @RequestParam(required = false) LocalDate dataInicio, @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String status, @RequestParam(required = false) String produtoId) {
        return dashboardService.produtosTop(dataInicio, dataFim, status, produtoId, limite);
    }

    @GetMapping("/pedidos-recentes")
    public List<Pedido> pedidosRecentes(@RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim, @RequestParam(required = false) String status,
            @RequestParam(required = false) String produtoId) {
        return dashboardService.pedidosRecentes(dataInicio, dataFim, status, produtoId);
    }
}
