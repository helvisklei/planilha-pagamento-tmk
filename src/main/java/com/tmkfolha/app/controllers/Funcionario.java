package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;


public class Funcionario {

    private Operadora operadora; // null para não-operadoras
    private boolean administrativo;

     // Atributos básicos
     private String codigo;
     private String drt;
     private String nome;
     private String polo;
     private String setor;
     private String tipo;
    
    private String tipoRegistro; //= "FUNCIONARIO";
     
     // Dados de desempenho
     private Double quantidade = 0.0;
     private double valor = 0.0;
     private BigDecimal rendimento = BigDecimal.ZERO;
     private BigDecimal recebimento = BigDecimal.ZERO;
     private BigDecimal gratificacaoPercentual = BigDecimal.ZERO;
     private BigDecimal gratificacaoValor = BigDecimal.ZERO;   
     
     // Dados do polo
     private double quantidadePolo = 0;     
     private BigDecimal valorPolo = BigDecimal.ZERO;
     private BigDecimal metaOperadora = BigDecimal.ZERO;

     private SituacaoOperadora situacaoOperadora;  

   

    // Getter e Setter para SituacaoOperadora
    public SituacaoOperadora getSituacaoOperadora() {
        return situacaoOperadora;
    }

    public void setSituacaoOperadora(SituacaoOperadora situacaoOperadora) {
        this.situacaoOperadora = situacaoOperadora;
    } 

    // Dados adicionais dos outros arquivos
    private BigDecimal metaRec = BigDecimal.ZERO;
    private BigDecimal recebidoOperador = BigDecimal.ZERO;
    private BigDecimal comissao = BigDecimal.ZERO;
    private BigDecimal gratificacao = BigDecimal.ZERO;
    private Map<String, Double> demonstrativosCaixa = new HashMap<>();
    private SituacaoMensal situacaoMensal;
    private BigDecimal comissaoMensageiro;
    private Map<String, BigDecimal> operacoes = new HashMap<>(); // Mapeia tipo de operação para valor
     
     // Construtores
     public Funcionario() {}
     
     public Funcionario(String codigo, String drt, String nome, String polo) {
        this.codigo = codigo;
        this.drt = drt;
        this.nome = nome;
        this.polo = polo;
        this.demonstrativosCaixa = new HashMap<>();// 🔥 GARANTINDO QUE NUNCA É NULL
     }      

   /*  public Optional<Operadora> getOperadora() {
        return Optional.ofNullable(operadora);
    } */

    public Operadora getOperadora() {
        return operadora;
    }
    
    public void setOperadora(Operadora operadora) {
        this.operadora = operadora;
    }

    // Método de conveniência para verificar se é operadora
   /*  public boolean isOperadora() {
        return operadora != null;
    } */
   /*  public boolean isOperadora() {
        return "OPE".equalsIgnoreCase(this.tipo) || "OPERADORA".equalsIgnoreCase(this.tipo);
    } */
    public boolean isOperadora() {
        if (this.operadora != null) {
            return true;
        }
        
        if (this.tipo == null) {
            return false;
        }
        
        return "OPE".equalsIgnoreCase(this.tipo) || 
               "OPERADORA".equalsIgnoreCase(this.tipo);
    }
    // Lista global de funcionários
    //private static List<Funcionario> funcionarios = new ArrayList<>(); 

    // Construtor para Resumo de Setor
    public Funcionario(BigDecimal rendimento, BigDecimal recebimento, double quantidade, double valor) {
        this.tipoRegistro = "RESUMO_SETOR";
        this.rendimento = rendimento;
        this.recebimento = recebimento;
        this.quantidade = quantidade;
        this.valor = valor;
    }
    // Versão que aceita tanto Map<String, String> quanto Map<String, Map<String, String>>
    public static List<Funcionario> carregarFuncionarios(Object dadosPolo) {
        List<Funcionario> funcionarios = new ArrayList<>();
        
        if (dadosPolo instanceof Map) {
            if (((Map<?, ?>) dadosPolo).values().stream().allMatch(v -> v instanceof String)) {
                // Caso Map<String, String>
                Map<String, String> dadosSimples = (Map<String, String>) dadosPolo;
                Funcionario func = fromMap(dadosSimples);
                if (func != null) {
                    funcionarios.add(func);
                }
            } else if (((Map<?, ?>) dadosPolo).values().stream().allMatch(v -> v instanceof Map)) {
                // Caso Map<String, Map<String, String>>
                Map<String, Map<String, String>> dadosComplexos = (Map<String, Map<String, String>>) dadosPolo;
                for (Map<String, String> dados : dadosComplexos.values()) {
                    Funcionario func = fromMap(dados);
                    if (func != null) {
                        funcionarios.add(func);
                    }
                }
            }
        }
        
        return funcionarios;
    }

    public void calcularCamposDerivados() {
        try {
            // Calcula rendimento (recebimento/meta)
            if (this.metaOperadora.compareTo(BigDecimal.ZERO) > 0) {
                this.rendimento = this.recebimento.divide(this.metaOperadora);
            } else {
                this.rendimento = BigDecimal.ZERO;
            }
            
            // Calcula gratificação conforme o tipo
            if ("OPERADORA".equalsIgnoreCase(this.tipoRegistro)) {
                this.gratificacaoValor = GratificacaoCalculator.calcularGratificacaoOperadora(
                    this.recebimento, 
                    this.gratificacaoPercentual
                );
            } else if ("MENSAGEIRO".equalsIgnoreCase(this.tipoRegistro)) {
                this.gratificacaoValor = GratificacaoCalculator.calcularGratificacaoMensageiro(
                    this.rendimento, 
                    this.polo
                );
            } else {
                // Padrão para administrativos ou outros tipos
                this.gratificacaoValor = BigDecimal.ZERO;
            }
        } catch (Exception e) {
            System.err.println("Erro ao calcular campos para " + this.nome + ": " + e.getMessage());
            this.rendimento = BigDecimal.ZERO;
            this.gratificacaoValor = BigDecimal.ZERO;
        }
    }

   /*  public void calcularCamposDerivados() {
        // Calcular rendimento se necessário
        if (this.metaOperadora > 0) {
            this.rendimento = this.recebimento / this.metaOperadora;
        }
        
        // Calcular gratificação conforme tipo
        if (this.tipoRegistro.equalsIgnoreCase("OPERADORA")) {
            this.gratificacaoValor = GratificacaoCalculator.calcularGratificacaoOperadora(
                this.recebimento, 
                this.gratificacaoPercentual
            );
        } else if (this.tipoRegistro.equalsIgnoreCase("MENSAGEIRO")) {
            this.gratificacaoValor = GratificacaoCalculator.calcularGratificacaoMensageiro(
                this.rendimento, 
                this.polo
            );
        }
        // ... outros cálculos conforme necessário
    } */

    // Método para adicionar valor e quantidade
    public void adicionarValorEQuantidade(double valor, double quantidade) {
        this.valor = valor;
        this.quantidade = quantidade;
    }  

    // Método para criar um Funcionario a partir de um mapa de dados
    public static Funcionario fromMap(Map<String, String> data) {
        if (data == null || data.isEmpty()) return null;

        String codigo = data.getOrDefault("Column0", "").trim();
        String drt = data.getOrDefault("Column1", "").trim();
        String nome = data.getOrDefault("Column2", "").trim();
        String polo = data.getOrDefault("Column3", "").trim();
        
        if (codigo.isEmpty() || nome.isEmpty()) return null;

        Funcionario func = new Funcionario(codigo, drt, nome, polo);
        func.atualizarDados(data);
        return func;
    }

    public void atualizarDados(Map<String, String> dados) {
        if (dados.containsKey("Column6")) {
            this.quantidade = parseDoubleSafe(dados.get("Column6"), "quantidade", dados);
        }
        if (dados.containsKey("Column7")) {
            this.valor = parseDoubleSafe(
                dados.get("Column7").replace("R$", "").replace(".", "").replace(",", "."), 
                "valor", dados);
        }
        // ... outros campos conforme necessário
    }

    // Métodos auxiliares para conversão segura
    private static double parseDoubleSafe(String value, String field, Map<String, String> data) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.printf("Aviso: Campo '%s' contém valor inválido: '%s'. Ignorando.%n", field, value);
            System.err.println("Dados problemáticos: " + data);
            return 0.0;
        }
    }
    private static int parseIntSafe(String value, String field, Map<String, String> data) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            System.err.printf("Aviso: Campo '%s' contém valor inválido: '%s'. Ignorando.%n", field, value);
            System.err.println("Dados problemáticos: " + data);
            return 0;
        }
    }
    // Método para logar os dados no console
    public void log() {
        if ("FUNCIONARIO".equals(tipoRegistro)) {
            System.out.printf(
            "Funcionário - Código: %s, DRT: %s, Nome: %s, Polo: %s, Quantidade: %d, Valor: %.2f, Rendimento: %.2f, Gratificação: %.2f, QuantidadePolo: %d, ValorPolo: %.2f, MetaOperadora: %.2f\n",
            codigo, drt, nome, polo, quantidade, valor, rendimento, gratificacaoPercentual, quantidadePolo, valorPolo, metaOperadora
            );
        } else {
            System.out.printf("Resumo Setor - Quantidade: %.2f, Valor: %.2f\n", quantidade, valor);
        }
    }
    /**
    * Exibe o log formatado de uma lista de funcionários.
    *
    * @param funcionarios Lista de funcionários a ser logada.
    */
    public static void logFuncionarios(List<Funcionario> funcionarios) {
        if (funcionarios == null || funcionarios.isEmpty()) {
            System.out.println("Nenhum funcionário encontrado para exibir o log.");
            return;
        }
        String nome_do_polo = ""; 
    
        System.out.println("=== LOG DE FUNCIONÁRIOS ===");
        for (int i = 0; i < funcionarios.size(); i++) {
            Funcionario funcionario = funcionarios.get(i);
            System.out.println("\nFuncionário #" + (i + 1) + ":");
            System.out.println("  ID: " + funcionario.codigo);
            System.out.println("  Nome: " + funcionario.nome);
            System.out.println("  Setor: " + funcionario.polo);
            System.out.println("  Meta: " + funcionario.metaOperadora);
            System.out.println("  Valor Recebido: " + funcionario.recebimento);
            System.out.println("  Valor Rendimento: " + funcionario.rendimento);
            System.out.println("  Gratificação: " + funcionario.gratificacaoValor);
            System.out.println("  Gratificação %: " + funcionario.gratificacaoPercentual);
            System.out.println("  Valor Polo: " + funcionario.quantidadePolo);
            System.out.println("  Quantidade Polo: " + funcionario.quantidadePolo);
            System.out.println("  Tipo Registro: " + funcionario.tipoRegistro);
            // Adicione outros atributos relevantes do funcionário aqui
            nome_do_polo = funcionario.nome;
        }
        System.out.println("=== FIM DO LOG === " + nome_do_polo);
    }
    @Override
    public String toString() {
        if ("FUNCIONARIO".equals(tipoRegistro)) {
            return String.format(
                "Código: %s, DRT: %s, Nome: %s, Polo: %s, Quantidade: %d, Valor: %.2f, Rendimento: %.2f, Gratificação: %.2f, QuantidadePolo: %d, ValorPolo: %.2f, MetaOperadora: %.2f",
                codigo, drt, nome, polo, quantidade, valor, rendimento, gratificacaoPercentual, quantidadePolo, valorPolo, metaOperadora
            );
        } else {
            return String.format("Resumo Setor - Quantidade: %.2f, Valor: %.2f", quantidade, valor);
        }
    }    
    public static void processarDadosFiltrados(Map<String, Map<String, String>> dadosFiltrados, List<Funcionario> funcionarios) {
        for (Map.Entry<String, Map<String, String>> arquivoEntry : dadosFiltrados.entrySet()) {
            Map<String, String> dadosLinhas = arquivoEntry.getValue();                

            // Verifica se os dados são de resumo de setor (presença de campos específicos)
            if (dadosLinhas.containsKey("Column14") && dadosLinhas.containsKey("Column15")) {
                // Processa dados de resumo de setor
                processarDadosResumoSetor(funcionarios, dadosLinhas);
            } else if (dadosLinhas.containsKey("Column0") && dadosLinhas.containsKey("Column1")) {
                // Extrai o código da coluna Column0
                String coluna0 = dadosLinhas.get("Column0");
                String codigo = FiltradorDados.extrairCodigo(coluna0);

                // Verifica se o código é válido
                if (codigo != null) {
                    // Atualiza o valor de Column0 no mapa para o código formatado
                    dadosLinhas.put("Column0", codigo);

                    // Processa os dados do funcionário
                    atualizarOuCriarFuncionario(funcionarios, dadosLinhas);
                } else {
                    System.out.println("Código inválido ou não encontrado na linha: " + coluna0);
                }
            } else {
                System.out.println("L199 Processar Dados Filtrados Funcionario - Dados não reconhecidos: " + dadosLinhas);
            }
        }
    }
    public static void atribuirMetaOperadora(List<Funcionario> funcionarios, Map<String, Double> metasPorCodigo) {
        for (Funcionario funcionario : funcionarios) {
            String codigoFuncionario = funcionario.getCodigo();

            // Remove zeros à esquerda do código do funcionário
            String codigoFuncionarioFormatado = codigoFuncionario.replaceFirst("^0+(\\d+)$", "$1");
    
            // Verifica se há uma meta para o código do funcionário
            if (metasPorCodigo.containsKey(codigoFuncionarioFormatado)) {
                BigDecimal metaOperadora = BigDecimal.valueOf(metasPorCodigo.get(codigoFuncionarioFormatado));
                funcionario.setMetaOperadora(metaOperadora); // Atribui a meta ao funcionário
                System.out.println("Meta da operadora atribuída ao funcionário: " + funcionario.getNome() + " - Meta: " + metaOperadora);
            } else {
                System.out.println("Nenhuma meta encontrada para o funcionário: " + funcionario.getNome());
            }
        }
    }

    public static void processarDadosFiltradosMSG(Map<String, Map<String, String>> dadosFiltrados, List<Funcionario> funcionarios) {
        for (Map.Entry<String, Map<String, String>> arquivoEntry : dadosFiltrados.entrySet()) {
            Map<String, String> dadosLinhas = arquivoEntry.getValue();                

            // Verifica se os dados são de resumo de setor (presença de campos específicos)
            if (dadosLinhas.containsKey("Column14") && dadosLinhas.containsKey("Column15")) {
                // Processa dados de resumo de setor
                processarDadosResumoSetor(funcionarios, dadosLinhas);
            } else if (dadosLinhas.containsKey("Column0") && dadosLinhas.containsKey("Column1")) {
                // Extrai o código da coluna Column0
                String coluna0 = dadosLinhas.get("Column0");
                String codigo = FiltradorDados.extrairCodigo(coluna0);

                // Verifica se o código é válido
                if (codigo != null) {
                    // Atualiza o valor de Column0 no mapa para o código formatado
                    dadosLinhas.put("Column0", codigo);

                    // Processa os dados do funcionário
                    atualizarOuCriarFuncionario(funcionarios, dadosLinhas);
                } else {
                    System.out.println("Código inválido ou não encontrado na linha: " + coluna0);
                }
            } else {
                System.out.println("L199 Processar Dados Filtrados Funcionario - Dados não reconhecidos: " + dadosLinhas);
            }
        }
    }
    public static void atualizarOuCriarFuncionario(List<Funcionario> funcionarios, Map<String, String> dadosFiltrados) {
        // Extrai o código e nome do funcionário
        String codigo = dadosFiltrados.get("Column0");
        String nome = dadosFiltrados.get("Column1");

        // Verifica se o funcionário já existe na lista
        Funcionario funcionarioExistente = null;
        for (Funcionario func : funcionarios) {
            if (codigo.equals(func.getCodigo())) {
                funcionarioExistente = func;
                break;
            }
        }
        // Se o funcionário não existir, cria um novo
        if (funcionarioExistente == null) {
            funcionarioExistente = new Funcionario(codigo, dadosFiltrados.get("Column2"), nome, dadosFiltrados.get("Column4"));
            funcionarios.add(funcionarioExistente);
        }
        // Atualiza os dados do funcionário
        funcionarioExistente.atualizarDados(dadosFiltrados);
    }
    public static void processarDadosResumoSetor(List<Funcionario> funcionarios, Map<String, String> dadosLinhas) {

        //System.out.println("L219 Funcionario -> " + dadosLinhas);
        // Extrai os dados de resumo de setor
        Double rendimento = Funcionario.parseDoubleSafe(dadosLinhas.getOrDefault("Column14", "0").replace("%", "").trim(), "rendimento", dadosLinhas);
        Double recebimento = Funcionario.parseDoubleSafe(dadosLinhas.getOrDefault("Column15", "0").replace("%", "").trim(), "recebimento", dadosLinhas);
        int quantidade = Funcionario.parseIntSafe(dadosLinhas.getOrDefault("Column6", "0"), "quantidade", dadosLinhas);
        double valor = Funcionario.parseDoubleSafe(dadosLinhas.getOrDefault("Column7", "0").replace("R$", "").replace(".", "").replace(",", "."), "valor", dadosLinhas);

        /* // Cria um objeto Funcionario do tipo RESUMO_SETOR
        Funcionario resumoSetor = new Funcionario(rendimento, recebimento, quantidade, valor);
        funcionarios.add(resumoSetor); */
    }

    public static class SituacaoOperadora {

        // Faixa Nova (FN)
        private BigDecimal fnRecVal;
        private BigDecimal fnBolVal;
        private BigDecimal fnDebVal;
        private BigDecimal fnPayVal;
        private BigDecimal fnHipVal;
    
        // Recorrente (RC)
        private BigDecimal rcRecVal;
        private BigDecimal rcBolVal;
        private BigDecimal rcDebVal;
        private BigDecimal rcPayVal;
        private BigDecimal rcHipVal;
    
        // Construtor completo
        public SituacaoOperadora(BigDecimal fnRecVal, BigDecimal fnBolVal, BigDecimal fnDebVal,
                                 BigDecimal fnPayVal, BigDecimal fnHipVal,
                                 BigDecimal rcRecVal, BigDecimal rcBolVal, BigDecimal rcDebVal,
                                 BigDecimal rcPayVal, BigDecimal rcHipVal) {
            this.fnRecVal = fnRecVal;
            this.fnBolVal = fnBolVal;
            this.fnDebVal = fnDebVal;
            this.fnPayVal = fnPayVal;
            this.fnHipVal = fnHipVal;
            this.rcRecVal = rcRecVal;
            this.rcBolVal = rcBolVal;
            this.rcDebVal = rcDebVal;
            this.rcPayVal = rcPayVal;
            this.rcHipVal = rcHipVal;
        }
    
        // Construtor vazio
        public SituacaoOperadora() {}

        public static SituacaoOperadora fromMapOpe(Map<String, String> map) {
            SituacaoOperadora so = new SituacaoOperadora();
        
            so.setFnRecVal(toBigDecimal(map.get("fnRecVal")));
            so.setFnBolVal(toBigDecimal(map.get("fnBolVal")));
            so.setFnDebVal(toBigDecimal(map.get("fnDebVal")));
            so.setFnPayVal(toBigDecimal(map.get("fnPayVal")));
            so.setFnHipVal(toBigDecimal(map.get("fnHipVal")));
        
            so.setRcRecVal(toBigDecimal(map.get("rcRecVal")));
            so.setRcBolVal(toBigDecimal(map.get("rcBolVal")));
            so.setRcDebVal(toBigDecimal(map.get("rcDebVal")));
            so.setRcPayVal(toBigDecimal(map.get("rcPayVal")));
            so.setRcHipVal(toBigDecimal(map.get("rcHipVal")));
        
            return so;
        }
        
        // Método auxiliar interno para conversão segura
        private static BigDecimal toBigDecimal(String valor) {
            if (valor == null || valor.isBlank() || valor.trim().isEmpty()) return BigDecimal.ZERO;
            try {
                // Remove pontos de milhar e substitui vírgula por ponto, se necessário
                String normalized = valor.replace(".", "").replace(",", ".");
                return new BigDecimal(normalized);
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }

        @Override
        public String toString() {
            return "SituacaoOperadora{" +
                "fnRecVal=" + fnRecVal +
                ", fnBolVal=" + fnBolVal +
                ", fnDebVal=" + fnDebVal +
                ", fnPayVal=" + fnPayVal +
                ", fnHipVal=" + fnHipVal +
                ", rcRecVal=" + rcRecVal +
                ", rcBolVal=" + rcBolVal +
                ", rcDebVal=" + rcDebVal +
                ", rcPayVal=" + rcPayVal +
                ", rcHipVal=" + rcHipVal +
                '}';
        }

    
        // Getters e setters
        public BigDecimal getFnRecVal() { return fnRecVal; }
        public void setFnRecVal(BigDecimal fnRecVal) { this.fnRecVal = fnRecVal; }
    
        public BigDecimal getFnBolVal() { return fnBolVal; }
        public void setFnBolVal(BigDecimal fnBolVal) { this.fnBolVal = fnBolVal; }
    
        public BigDecimal getFnDebVal() { return fnDebVal; }
        public void setFnDebVal(BigDecimal fnDebVal) { this.fnDebVal = fnDebVal; }
    
        public BigDecimal getFnPayVal() { return fnPayVal; }
        public void setFnPayVal(BigDecimal fnPayVal) { this.fnPayVal = fnPayVal; }
    
        public BigDecimal getFnHipVal() { return fnHipVal; }
        public void setFnHipVal(BigDecimal fnHipVal) { this.fnHipVal = fnHipVal; }
    
        public BigDecimal getRcRecVal() { return rcRecVal; }
        public void setRcRecVal(BigDecimal rcRecVal) { this.rcRecVal = rcRecVal; }
    
        public BigDecimal getRcBolVal() { return rcBolVal; }
        public void setRcBolVal(BigDecimal rcBolVal) { this.rcBolVal = rcBolVal; }
    
        public BigDecimal getRcDebVal() { return rcDebVal; }
        public void setRcDebVal(BigDecimal rcDebVal) { this.rcDebVal = rcDebVal; }
    
        public BigDecimal getRcPayVal() { return rcPayVal; }
        public void setRcPayVal(BigDecimal rcPayVal) { this.rcPayVal = rcPayVal; }
    
        public BigDecimal getRcHipVal() { return rcHipVal; }
        public void setRcHipVal(BigDecimal rcHipVal) { this.rcHipVal = rcHipVal; }
    }

    // Classe interna para situação mensal mensageiro
    public static class SituacaoMensal {
    /*     private BigDecimal saldoAnterior;
        private BigDecimal entradas;
        private BigDecimal recebido;
        private BigDecimal percentualDevolvido;
        private BigDecimal devolvido;
        private BigDecimal saldoDia;
        private BigDecimal rendimento; */

        private Integer saldoAnteriorQtd;
        private BigDecimal saldoAnterior;

        private Integer entradaQtd;
        private BigDecimal entradaValor;

        private Integer recebidoQtd;
        private BigDecimal recebidoValor;

        private Integer devolvidoQtd;
        private BigDecimal devolvidoValor;

        //private BigDecimal percentualDevolvido;

        private BigDecimal rendimentoQtd;
        private BigDecimal rendimentoValor;

        private BigDecimal saldoDiaValor;
       // private BigDecimal recebidoValor;
        private BigDecimal comissao;
        

         // ✅ Adicione este construtor completo abaixo:
    public SituacaoMensal(
        Integer saldoAnteriorQtd, 
        BigDecimal saldoAnterior,

        Integer entradaQtd, 
        BigDecimal entradaValor,

        Integer recebidoQtd, 
        BigDecimal recebidoValor,

        Integer devolvidoQtd, 
        BigDecimal devolvidoValor,

        BigDecimal saldoDiaValor, 

        //BigDecimal percentualDevolvido, 
        BigDecimal rendimentoQtd, 
        BigDecimal rendimentoValor,
        
        BigDecimal comissao
        )
        {
        this.saldoAnteriorQtd = saldoAnteriorQtd;
        this.saldoAnterior = saldoAnterior;
        this.entradaQtd = entradaQtd;
        this.entradaValor = entradaValor;
        this.recebidoQtd = recebidoQtd;
        this.recebidoValor = recebidoValor;
        this.devolvidoQtd = devolvidoQtd;
        this.devolvidoValor = devolvidoValor;        
        this.rendimentoQtd = rendimentoQtd;
        this.rendimentoValor = rendimentoValor;
        this.saldoDiaValor = saldoDiaValor;
        this.comissao = comissao;
    }
     // ✅ Construtor vazio
    public SituacaoMensal() {}

    public BigDecimal getComissao() {
        return comissao;
    }
    /* public void setRendimentoValor(BigDecimal valor) {
        this.rendimentoValor = valor;
    } */
    public BigDecimal getRendimentoValor() {
        return rendimentoValor;
    }    

    /**
     * @return Integer return the saldoAnteriorQtd
     */
    public Integer getSaldoAnteriorQtd() { return saldoAnteriorQtd; }
    /**
     * @param saldoAnteriorQtd the saldoAnteriorQtd to set
     */
    public void setSaldoAnteriorQtd(Integer saldoAnteriorQtd) { this.saldoAnteriorQtd = saldoAnteriorQtd;  }
    /**
     * @return Integer return the entradasQtd
     */
    public Integer getEntradaQtd() { return entradaQtd;  }
    /**
     * @param entradasQtd the entradasQtd to set
     */
    public void setEntradaQtd(Integer entradaQtd) { this.entradaQtd = entradaQtd;  }
    /**
     * @return Integer return the recebidoQtd
     */
    public Integer getRecebidoQtd() { return recebidoQtd;  }
    /**
     * @param recebidoQtd the recebidoQtd to set
     */
    public void setRecebidoQtd(Integer recebidoQtd) { this.recebidoQtd = recebidoQtd;  }
    /**
     * @return Integer return the devolvidoQtd
     */
    public Integer getDevolvidoQtd() { return devolvidoQtd;  }
    /**
     * @param devolvidoQtd the devolvidoQtd to set
     */
    public void setDevolvidoQtd(Integer devolvidoQtd) { this.devolvidoQtd = devolvidoQtd;  }            
    /**
     * @return double return the saldoAnterior
    */
    public BigDecimal getSaldoAnterior() { return saldoAnterior; }
    /**
     * @param saldoAnterior the saldoAnterior to set
    */
    public void setSaldoAnterior(BigDecimal saldoAnterior) { this.saldoAnterior = saldoAnterior; } 
    /**
    * @return double return the entradas
    */
    public BigDecimal getEntradaValor() { return entradaValor; }
    /**
    * @param entradas the entradas to set
    */
    public void setEntradaValor(BigDecimal entradaValor) { this.entradaValor = entradaValor; }
    /**
    * @return Bigdecimal return the recebido
    */
    public BigDecimal getRecebidoValor() { return recebidoValor; }
    /**
    * @param recebido the recebido to set
    */
    public void setRecebidoValor(BigDecimal recebidoValor) { this.recebidoValor = recebidoValor; }
    /**
    * @return double return the percentualDevolvido
    */
  //  public BigDecimal getPercentualDevolvido() { return percentualDevolvido; }
    /**
    * @param percentualDevolvido the percentualDevolvido to set
    */
   // public void setPercentualDevolvido(BigDecimal percentualDevolvido) { this.percentualDevolvido = percentualDevolvido; }
    /**
    * @return double return the devolvido
    */
    public BigDecimal getDevolvidoValor() { return devolvidoValor; }
    /**
    * @param devolvido the devolvido to set
    */
    public void setDevolvidoValor(BigDecimal devolvidoValor) { this.devolvidoValor = devolvidoValor;  }
    /**
    * @return double return the saldoDia
    */
     public BigDecimal getSaldoDia() { return saldoDiaValor; }
    /**
    * @param saldoDia the saldoDia to set
    */
    public void setSaldoDia(BigDecimal saldoDiaValor) { this.saldoDiaValor = saldoDiaValor; }
    /**
    * @return double return the rendimento
    */
    public BigDecimal getRendimentoQtd() { return rendimentoQtd; }
    /**
    * @param rendimento the rendimento to set
    */
    public void setRendimentoQtd(BigDecimal rendimentoQtd) { this.rendimentoQtd = rendimentoQtd; }

    public void setRendimentoValor(BigDecimal rendimentoValor) { this.rendimentoValor = rendimentoValor; }

     /**
     * @return BigDecimal return the percentualQtd
     */
    public BigDecimal getComissaoMsg() {  return comissao; }

    /**
     * @param percentualQtd the percentualQtd to set
     */
    public void setComissaoMsg(BigDecimal comissao) {  this.comissao = comissao;   }
       /**
     * @return BigDecimal return the recebidoValor
     */
   // public BigDecimal getRecebidoValor() { return recebidoValor;  }

    /**
     * @param recebidoValor the recebidoValor to set
     */
   // public void setRecebidoValor(BigDecimal recebidoValor) { this.recebidoValor = recebidoValor;  }  
    
     @Override
    public String toString() {
        NumberFormat moedaFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(2);
        
        return String.format(
            "SituacaoMensal[\n" +
            "  saldoAnteriorQtd=%d, saldoAnteriorValor=%s\n" +
            "  entradasQtd=%d, entradasValor=%s\n" +
            "  recebidoQtd=%d, recebidoValor=%s\n" +
            "  devolvidoQtd=%d, devolvidoValor=%s\n" +
            "  percentualDevolvido=%s, rendimento=%s\n" +
            "  saldoDia=%s, comissao=%s\n" +
            "  recebidoValor=%s\n" +
            "]",
            saldoAnteriorQtd, moedaFormat.format(saldoAnterior),
            entradaQtd, moedaFormat.format(entradaValor),
            recebidoQtd, moedaFormat.format(recebidoValor),
            devolvidoQtd, moedaFormat.format(devolvidoValor),
            percentFormat.format(rendimentoValor != null ? rendimentoValor.divide(new BigDecimal(100)) : BigDecimal.ZERO),
            percentFormat.format(rendimentoQtd != null ? rendimentoQtd.divide(new BigDecimal(100)) : BigDecimal.ZERO),
            moedaFormat.format(saldoDiaValor),
            moedaFormat.format(comissao),
            moedaFormat.format(recebidoValor)
        );
    }

    }

    private String formatarPercentual(BigDecimal valor) {
        try {
            if (valor == null) return "0%";
            NumberFormat percentFormat = NumberFormat.getPercentInstance();
            percentFormat.setMaximumFractionDigits(2);
            return percentFormat.format(valor.divide(BigDecimal.valueOf(100)));
        } catch (Exception e) {
            return "0%";
        }
    }

    public static void visualizarFuncionarios(List<Funcionario> funcionarios) {
        if (funcionarios == null || funcionarios.isEmpty()) {
            System.out.println("Nenhum funcionário disponível para exibição.");
            return;
        }        

        System.out.println("=== LISTA DE FUNCIONÁRIOS ===");
        for (Funcionario funcionario : funcionarios) {
            System.out.println("Código: " + funcionario.getCodigo());
            System.out.println("Nome: " + funcionario.getNome());
            System.out.println("DRT: " + funcionario.getDrt());
            System.out.println("Polo: " + funcionario.getPolo());
            System.out.println("Setor: " + funcionario.getSetor());
            System.out.println("Tipo: " + funcionario.getTipo());
            System.out.println("Tipo Registro: " + funcionario.getTipoRegistro());
            System.out.println("Quantidade: " + funcionario.getQuantidade());
            System.out.println("Valor: " + funcionario.getValor());
            System.out.println("Rendimento: " + funcionario.getRendimento());
            System.out.println("Recebimento: " + funcionario.getRecebimento());
            System.out.println("Gratificação (%): " + funcionario.getGratificacaoPercentual());
            System.out.println("Gratificação (Valor): " + funcionario.getGratificacaoValor());
            System.out.println("Quantidade Polo: " + funcionario.getQuantidadePolo());
            System.out.println("Valor Polo: " + funcionario.getValorPolo());
            System.out.println("Meta Operadora: " + funcionario.getMetaOperadora());
            System.out.println("===================================");
        }
    }    
    
    // Getters e Setters
    public String getPolo() { return polo; }
    public void setPolo(String polo) { this.polo = polo != null ? polo.trim() : ""; } // coloquei aqui tratamento
    public String getTipoRegistro() { return tipoRegistro; }
    public void setTipoRegistro(String tipoRegistro) { this.tipoRegistro = tipoRegistro; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo != null ? codigo.trim() : ""; }
    public String getDrt() { return drt; }
    public void setDrt(String drt) { this.drt = drt; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public BigDecimal getRecebimento() { return Objects.requireNonNullElse(recebimento, BigDecimal.ZERO); }
    public void setRecebimento(BigDecimal recebimento) { this.recebimento = recebimento; }

    public BigDecimal getRendimento() { return Objects.requireNonNullElse(rendimento, BigDecimal.ZERO); }
    public void setRendimento(BigDecimal rendimento) { this.rendimento = rendimento; }

    public BigDecimal getGratificacaoPercentual() { return Objects.requireNonNullElse(gratificacaoPercentual, BigDecimal.ZERO); }
    public void setGratificacaoPercentual(BigDecimal gratificacaoPercentual) { this.gratificacaoPercentual = gratificacaoPercentual; }
   
    public BigDecimal getGratificacaoValor() { return Objects.requireNonNullElse(gratificacaoValor, BigDecimal.ZERO); }
    public void setGratificacaoValor(BigDecimal gratificacaoValor) { this.gratificacaoValor = gratificacaoValor; }
    public Double getValor() { return valor; }
    public void setValor(Double valor) { this.valor = valor; }
    public Double getQuantidade() { return quantidade; }
    public void setQuantidade(Double quantidade) { this.quantidade = quantidade; } 
    public void setQuantidadePolo(double quantidadePolo) { this.quantidadePolo = quantidadePolo; }
    public double getQuantidadePolo() { return quantidadePolo; }    
    public BigDecimal getValorPolo() { return Objects.requireNonNullElse(valorPolo, BigDecimal.ZERO); }
    public void setValorPolo(BigDecimal valorPolo2) { this.valorPolo = valorPolo2; } 
    public BigDecimal getMetaOperadora() { return Objects.requireNonNullElse(metaOperadora, BigDecimal.ZERO);  }
    public void setMetaOperadora(BigDecimal metaOperadora) { this.metaOperadora = metaOperadora; }
    public String getSetor() { return setor;  }
    public void setSetor(String setor) { this.setor = setor;  }

    public boolean isAdministrativo() {  return administrativo; }

    public void setAdministrativo(boolean administrativo) { this.administrativo = administrativo;  }

    /**
     * @return double return the recebidoOperador
     */
    public BigDecimal getRecebidoOperador() { return Objects.requireNonNullElse(recebidoOperador, BigDecimal.ZERO); }

    /**
     * @param recebidoOperador the recebidoOperador to set
     */
    public void setRecebidoOperador(BigDecimal recebidoOperador) { this.recebidoOperador = recebidoOperador; }

    /**
     * @return double return the comissao
     */
    public BigDecimal getComissao() { return comissao != null ? comissao : BigDecimal.ZERO;  }

    /**
     * @param comissao the comissao to set
     */
    public void setComissao(BigDecimal comissao) { this.comissao = comissao; }

    /**
     * @return double return the gratificacao
     */
    public BigDecimal getGratificacao() { return Objects.requireNonNullElse(gratificacao, BigDecimal.ZERO);  }

    /**
     * @param gratificacao the gratificacao to set
     */
    public void setGratificacao(BigDecimal gratificacao) { this.gratificacao = gratificacao;  }

    /**
     * @return Map<String, Double> return the demonstrativosCaixa
     */
    public Map<String, Double> getDemonstrativosCaixa() {
        if (this.demonstrativosCaixa == null || this.demonstrativosCaixa.isEmpty()) {
            this.demonstrativosCaixa = new HashMap<>();
            //System.out.println("🚨 Mapa demonstrativosCaixa está vazio ou não inicializado!");
        } else {
            System.out.println("✅ Conteúdo de demonstrativosCaixa:");
            for (Map.Entry<String, Double> entry : demonstrativosCaixa.entrySet()) {
                System.out.println("Arquivo: " + entry.getKey() + " | Valor: " + entry.getValue());
            }
        }
        return this.demonstrativosCaixa;
    }

    /**
     * @param demonstrativosCaixa the demonstrativosCaixa to set
     */
    public void setDemonstrativosCaixa(Map<String, Double> demonstrativosCaixa) { this.demonstrativosCaixa = demonstrativosCaixa; }

    /**
     * @return SituacaoMensal return the situacaoMensal
     */
    public SituacaoMensal getSituacaoMensal() { return situacaoMensal;  }

    /**
     * @param situacaoMensal the situacaoMensal to set
     */
    public void setSituacaoMensal(SituacaoMensal situacaoMensal) { this.situacaoMensal = situacaoMensal;  }

    public BigDecimal getComissaoMensageiro(){return comissaoMensageiro != null ? comissaoMensageiro : BigDecimal.ZERO;}
    public void setComissaoMensageiro(BigDecimal comissaoMensageiro) { this.comissaoMensageiro = comissaoMensageiro; }

    public Map<String, BigDecimal> getOperacoes() { return operacoes;  }
    
    public void setOperacoes(Map<String, BigDecimal> operacoes) { this.operacoes = operacoes; }
    
    // Método auxiliar para adicionar uma operação específica
    public void adicionarOperacao(String tipo, BigDecimal valor) { this.operacoes.merge(tipo, valor, BigDecimal::add); }

    // Getter
    public String getTipo() { return tipo;  }

    // Setter
    public void setTipo(String tipo) { this.tipo = tipo; }    

    /**
     * @return double return the metaRec
     */
    public BigDecimal getMetaRec() { return Objects.requireNonNullElse(metaRec, BigDecimal.ZERO);  }

    /**
     * @param metaRec the metaRec to set
     */
    public void setMetaRec(BigDecimal metaRec) { this.metaRec = metaRec;  }

   

}