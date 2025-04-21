package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GratificacaoCalculator extends XlsData {

    public GratificacaoCalculator(Map<String, String> rawData) {
        super(rawData);
    }
    
    @Override
    public String getCodigo() {
        return rawData.get("Column0"); // Código do funcionário
    }
    
    @Override
    public String getNome() {
        return rawData.get("Column1"); // Nome do funcionário
    }
    
    public BigDecimal getFatorGratificacao() {
        return parseMonetaryValue(rawData.get("Column2")); // Fator de gratificação
    }


   // Método corrigido para calcular a gratificação do Mensageiro
    public static BigDecimal calcularGratificacaoMensageiro(BigDecimal rendimento, String polo) {
        BigDecimal percentual;

        if (polo.equalsIgnoreCase("MATRIZ")) {
            if (rendimento.compareTo(BigDecimal.valueOf(0.91)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.88)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.85)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.80)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.75)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.70)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else percentual = BigDecimal.valueOf(0.0600);
        } else {
            if (rendimento.compareTo(BigDecimal.valueOf(0.91)) >= 0) percentual = BigDecimal.valueOf(0.0600);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.88)) >= 0) percentual = BigDecimal.valueOf(0.0550);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.85)) >= 0) percentual = BigDecimal.valueOf(0.0500);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.80)) >= 0) percentual = BigDecimal.valueOf(0.0450);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.75)) >= 0) percentual = BigDecimal.valueOf(0.0400);
            else if (rendimento.compareTo(BigDecimal.valueOf(0.70)) >= 0) percentual = BigDecimal.valueOf(0.0350);
            else percentual = BigDecimal.valueOf(0.0350);
        }

        return rendimento.multiply(percentual);
    }

    // Método para calcular a gratificação da Operadora
    public static BigDecimal calcularGratificacaoOperadora(BigDecimal recebido, BigDecimal percentual) {
        return recebido.multiply(percentual);
    }

    // Método para calcular a gratificação do Administrativo
    public static BigDecimal calcularGratificacaoAdministrativo(BigDecimal valorDemonstrativo, BigDecimal percentual) {
        return valorDemonstrativo.multiply(percentual);
    }

    // Método principal para processar os dados
    public static void processarDados(Map<String, Map<String, String>> dadosFiltrados, Map<String, Double> tabelaGratificacao) {
        BigDecimal subtotalAdministrativo = BigDecimal.ZERO;
        BigDecimal subtotalOperadora = BigDecimal.ZERO;
        BigDecimal subtotalMensageiro = BigDecimal.ZERO;

        for (Map.Entry<String, Map<String, String>> entry : dadosFiltrados.entrySet()) {
            String funcionarioId = entry.getKey();
            Map<String, String> dadosFuncionario = entry.getValue();

            String categoria = dadosFuncionario.get("Categoria");
            BigDecimal percentual = new BigDecimal(tabelaGratificacao.get(funcionarioId));

            switch (categoria.toUpperCase()) {
                case "ADMINISTRATIVO":
                    BigDecimal valorDemonstrativo =new BigDecimal(dadosFuncionario.get("ValorDemonstrativo"));
                    BigDecimal gratificacaoAdmin = calcularGratificacaoAdministrativo(valorDemonstrativo, percentual);
                    subtotalAdministrativo = subtotalAdministrativo.add(gratificacaoAdmin);
                    break;

                case "OPERADORA":
                    BigDecimal recebido = new BigDecimal(dadosFuncionario.get("Recebido"));
                    BigDecimal gratificacaoOperadora = calcularGratificacaoOperadora(recebido, percentual);
                    subtotalOperadora = subtotalOperadora.add(gratificacaoOperadora);
                    break;

                case "MENSAGEIRO":
                    BigDecimal rendimento =new BigDecimal(dadosFuncionario.get("Column14"));
                    String polo = dadosFuncionario.get("Polo");
                    BigDecimal gratificacaoMensageiro = calcularGratificacaoMensageiro(rendimento, polo);
                    subtotalMensageiro = subtotalMensageiro.add(gratificacaoMensageiro);
                    break;

                default:
                    System.out.println("Categoria não reconhecida: " + categoria);
                    break;
            }
        }        

        // Exibir subtotais
        System.out.println("Subtotal Administrativo: " + subtotalAdministrativo);
        System.out.println("Subtotal Operadora: " + subtotalOperadora);
        System.out.println("Subtotal Mensageiro: " + subtotalMensageiro);
    }

    // Nova sobrecarga para aceitar List<Funcionario>
    public static void processarDados(List<Funcionario> funcionarios, Map<String, Map<String, String>> tabelaGratificacao) {
        // Converte a lista de funcionários para o formato esperado
        Map<String, Map<String, String>> dadosFiltrados = converterFuncionariosParaMap(funcionarios);

        // Chama o método original
        processarDados(dadosFiltrados, extrairPercentuais(tabelaGratificacao));
    }

    // Método para extrair percentuais da tabela de gratificação
    private static Map<String, Double> extrairPercentuais(Map<String, Map<String, String>> tabelaGratificacao) {
        Map<String, Double> percentuais = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : tabelaGratificacao.entrySet()) {
            String funcionarioId = entry.getKey();
            double percentual = Double.parseDouble(entry.getValue().get("Percentual").toUpperCase());// coloquei maiusculo
            percentuais.put(funcionarioId, percentual);
        }

        return percentuais;
    }
        public static Map<String, Map<String, String>> converterFuncionariosParaMap(List<Funcionario> funcionarios) {
        Map<String, Map<String, String>> dadosFiltrados = new HashMap<>();

        for (Funcionario funcionario : funcionarios) {
            Map<String, String> dadosFuncionario = new HashMap<>();
            dadosFuncionario.put("codigo", funcionario.getCodigo());
            dadosFuncionario.put("drt", funcionario.getDrt());
            dadosFuncionario.put("nome", funcionario.getNome());
            dadosFuncionario.put("polo", funcionario.getPolo());
            dadosFuncionario.put("quantidade", String.valueOf(funcionario.getQuantidade()));
            dadosFuncionario.put("valor", String.valueOf(funcionario.getValor()));
            dadosFuncionario.put("rendimento", String.valueOf(funcionario.getRendimento()));
            dadosFuncionario.put("recebimento", String.valueOf(funcionario.getRecebimento()));
            dadosFuncionario.put("gratificacaoPercentual", String.valueOf(funcionario.getGratificacaoPercentual()));

            dadosFiltrados.put(funcionario.getCodigo(), dadosFuncionario);
        }

        return dadosFiltrados;
    }
}