package com.tmkfolha.app.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Funcionario {
    // Atributos
    private String codigo;
    private String drt;
    private String nome;
   
    private double gratificacaoPercentual;
    private double gratificacaoValor;
    private String polo;
    private String tipoRegistro; // "FUNCIONARIO" ou "RESUMO_SETOR"

    private double recebimento;  // Usado como "valor" no ResumoSetor
    private double rendimento;   // Usado como "quantidade" no ResumoSetor


    // Construtor para Funcionário
    public Funcionario(String codigo, String drt, String nome, String polo) {
        this.tipoRegistro = "FUNCIONARIO";
        this.codigo = codigo;
        this.drt = drt;
        this.nome = nome;
        this.polo = polo;
    }

      // Método para carregar dados do fileData
    public static List<Funcionario> carregarFuncionarios(Map<String, Map<String, String>> fileData) {
        List<Funcionario> funcionarios = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : fileData.entrySet()) {
            Funcionario funcionario = Funcionario.fromMap(entry.getValue());
            funcionarios.add(funcionario);
        }
        return funcionarios;
    }
     // Construtor para Resumo de Setor
     public Funcionario(double rendimento, double recebimento) {
        this.tipoRegistro = "RESUMO_SETOR";
        this.rendimento = rendimento;
        this.recebimento = recebimento;
    }

    public static Funcionario fromMap(Map<String, String> data) {
        System.out.println("DATA " + data);
    
        // Mapeamento das colunas para os atributos
        Map<String, String> mapping = Map.of(
            "Column1", "codigo",
            "Column2", "drt",
            "Column3", "nome",
            "Column4", "polo",
            "Column5", "rendimento",
            "Column6", "recebimento"
        );
    
        String codigo = null, drt = null, nome = null, polo = null;
        double rendimento = 0, recebimento = 0;
    
        // Itera sobre os dados e preenche os atributos dinamicamente
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey().replaceAll(".*Column", "Column");
            String value = entry.getValue().trim();
    
            switch (mapping.getOrDefault(key, "")) {
                case "codigo" -> codigo = value;
                case "drt" -> drt = value;
                case "nome" -> nome = value;
                case "polo" -> polo = value;
                case "rendimento" -> {
                    rendimento = parseDoubleSafe(value, "rendimento", data);
                }
                case "recebimento" -> {
                    recebimento = parseDoubleSafe(value, "recebimento", data);
                }            
            }    
        }
             // Cria o objeto com base nos valores encontrados
             if (codigo != null) {
                return new Funcionario(codigo, drt, nome, polo);
            } else {
                return new Funcionario(rendimento, recebimento);
            }
    }

        // Método auxiliar para conversão segura
    private static double parseDoubleSafe(String value, String field, Map<String, String> data) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            System.err.printf("Aviso: Campo '%s' contém valor inválido: '%s'. Ignorando.%n", field, value);
            System.err.println("Dados problemáticos: " + data);
            return 0.0;
        }
    }

   /*  public static Funcionario fromMap(Map<String, String> data) {
        System.out.println("DATA " + data);
    
        // Mapeamento das colunas para os atributos
        Map<String, String> mapping = Map.of(
            "Column1", "codigo",
            "Column2", "drt",
            "Column3", "nome",
            "Column5", "polo",
            "Column5", "rendimento",
            "Column6", "recebimento"
        );
    
        String codigo = null, drt = null, nome = null, polo = null;
        double rendimento = 0, recebimento = 0;
    
        // Itera sobre os dados e preenche os atributos dinamicamente
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey().replaceAll(".*Column", "Column");
            String value = entry.getValue().trim();
    
            switch (mapping.getOrDefault(key, "")) {
                case "codigo" -> codigo = value;
                case "drt" -> drt = value;
                case "nome" -> nome = value;
                case "polo" -> polo = value;
                case "rendimento" -> rendimento = Double.parseDouble(value);
                case "recebimento" -> recebimento = Double.parseDouble(value);
            }
        }
    
        // Cria o objeto com base nos valores encontrados
        if (codigo != null) {
            return new Funcionario(codigo, drt, nome, polo);
        } else {
            return new Funcionario(rendimento, recebimento);
        }
    } */

     // Método para criar Funcionario a partir de um Map
  /*   public static Funcionario fromMap(Map<String, String> data) {
        System.out.println("DATA " + data);
        if (data.containsKey("Linha 1 - Column1")) {
            return new Funcionario(
                data.get("codigo"),
                data.get("drt"),
                data.get("nome"),
                data.get("polo")
            );
        } else {
            return new Funcionario(
                Double.parseDouble(data.getOrDefault("quantidade", "0")),
                Double.parseDouble(data.getOrDefault("valor", "0"))
            );
        }
    } */

    public String getPolo() {
        return polo;
    }
    public void setPolo(String polo) {
        this.polo = polo;
    }
    // Getters e Setters
    public String getTipoRegistro() { return tipoRegistro; }
    public void setTipoRegistro(String tipoRegistro) { this.tipoRegistro = tipoRegistro; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDrt() { return drt; }
    public void setDrt(String drt) { this.drt = drt; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public double getRecebimento() { return recebimento; }
    public void setRecebimento(double recebimento) { this.recebimento = recebimento; }

    public double getRendimento() { return rendimento; }
    public void setRendimento(double rendimento) { this.rendimento = rendimento; }

    public double getGratificacaoPercentual() { return gratificacaoPercentual; }
    public void setGratificacaoPercentual(double gratificacaoPercentual) { this.gratificacaoPercentual = gratificacaoPercentual; }

    public double getGratificacaoValor() { return gratificacaoValor; }
    public void setGratificacaoValor(double gratificacaoValor) { this.gratificacaoValor = gratificacaoValor; }

    // Método para logar os dados no console
    public void log() {
        if ("FUNCIONARIO".equals(tipoRegistro)) {
            System.out.printf("Funcionário - Código: %s, DRT: %s, Nome: %s, Polo: %s\n",
                codigo, drt, nome, polo);
        } else {
            System.out.printf("Resumo Setor - Quantidade: %.2f, Valor: %.2f\n",
                rendimento, recebimento);
        }
    }

    @Override
    public String toString() {
        if ("FUNCIONARIO".equals(tipoRegistro)) {
            return String.format("Código: %s, DRT: %s, Nome: %s, Polo: %s",
                codigo, drt, nome, polo);
        } else {
            return String.format("Resumo Setor - Quantidade: %.2f, Valor: %.2f",
                rendimento, recebimento);
        }
    }

}