let vendasChart;
let statusChart;
let produtosChart;

function filtrosDashboard() {
    const params = new URLSearchParams();
    ['dataInicio', 'dataFim', 'status', 'produtoId'].forEach(id => {
        const valor = document.getElementById(id).value;
        if (valor) {
            params.append(id, valor);
        }
    });
    return params.toString();
}

function dinheiro(valor) {
    const numero = typeof valor === 'object' && valor !== null && '$numberDecimal' in valor
        ? Number(valor.$numberDecimal)
        : Number(valor || 0);
    return (Number.isFinite(numero) ? numero : 0).toLocaleString('pt-BR', {
        style: 'currency',
        currency: 'BRL'
    });
}

async function carregarProdutosDashboard() {
    const produtos = await fetch('/api/produtos').then(resposta => resposta.json());
    const select = document.getElementById('produtoId');
    produtos.forEach(produto => {
        select.innerHTML += `<option value="${produto.id}">${produto.nome}</option>`;
    });
}

async function carregarDashboard() {
    const filtros = filtrosDashboard();
    const sufixo = filtros ? `?${filtros}` : '';
    const [resumo, vendas, status, produtos, pedidos] = await Promise.all([
        fetch(`/api/dashboard/resumo${sufixo}`).then(resposta => resposta.json()),
        fetch(`/api/dashboard/vendas?agrupamento=dia&${filtros}`).then(resposta => resposta.json()),
        fetch(`/api/dashboard/status${sufixo}`).then(resposta => resposta.json()),
        fetch(`/api/dashboard/produtos-top?limite=5&${filtros}`).then(resposta => resposta.json()),
        fetch(`/api/dashboard/pedidos-recentes${sufixo}`).then(resposta => resposta.json())
    ]);

    document.getElementById('faturamento-total').textContent = dinheiro(resumo.faturamentoTotal);
    document.getElementById('quantidade-pedidos').textContent = resumo.quantidadePedidos || 0;
    document.getElementById('ticket-medio').textContent = dinheiro(resumo.ticketMedio);
    document.getElementById('pedidos-cancelados').textContent = resumo.cancelados || 0;

    if (vendasChart) vendasChart.destroy();
    vendasChart = new Chart(document.getElementById('vendas-chart'), {
        type: 'line',
        data: {
            labels: vendas.map(item => item.periodo),
            datasets: [{
                label: 'Vendas',
                data: vendas.map(item => item.total),
                borderColor: '#007aff',
                backgroundColor: 'rgba(0, 122, 255, .08)',
                fill: true,
                tension: .35,
                pointRadius: 3,
                pointBackgroundColor: '#007aff'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                xAxes: [{ gridLines: { display: false }, ticks: { fontColor: '#8e8e93' } }],
                yAxes: [{ gridLines: { color: '#f0f0f2' }, ticks: { fontColor: '#8e8e93' } }]
            }
        }
    });

    if (statusChart) statusChart.destroy();
    statusChart = new Chart(document.getElementById('status-chart'), {
        type: 'doughnut',
        data: {
            labels: status.map(item => item.status),
            datasets: [{ data: status.map(item => item.quantidade), backgroundColor: ['#007aff', '#34c759', '#ff9500', '#5856d6', '#af52de', '#ff3b30', '#8e8e93'], borderWidth: 0 }]
        },
        options: { responsive: true, maintainAspectRatio: false, cutoutPercentage: 72, legend: { position: 'bottom', labels: { fontColor: '#6e6e73', usePointStyle: true, padding: 16 } } }
    });

    if (produtosChart) produtosChart.destroy();
    produtosChart = new Chart(document.getElementById('produtos-chart'), {
        type: 'bar',
        data: {
            labels: produtos.map(item => item.produto),
            datasets: [{ label: 'Quantidade', data: produtos.map(item => item.qtd), backgroundColor: '#007aff', borderRadius: 6, barPercentage: .55 }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            legend: { display: false },
            scales: {
                xAxes: [{ gridLines: { display: false }, ticks: { fontColor: '#8e8e93' } }],
                yAxes: [{ gridLines: { color: '#f0f0f2' }, ticks: { beginAtZero: true, fontColor: '#8e8e93' } }]
            }
        }
    });

    document.getElementById('pedidos-recentes').innerHTML = pedidos.map(pedido => `
        <tr>
            <td>${pedido.clienteNome || ''}</td>
            <td>${dinheiro(pedido.total)}</td>
            <td><span class="dashboard-status">${pedido.status}</span></td>
            <td>${new Date(pedido.dataHoraEntrada).toLocaleString('pt-BR')}</td>
        </tr>`).join('');
}

document.getElementById('filtrar-dashboard').addEventListener('click', carregarDashboard);
document.getElementById('atualizar-dashboard').addEventListener('click', carregarDashboard);
carregarProdutosDashboard().then(carregarDashboard);
