package com.tmkfolha.processor;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultadoPolo {
    private long quantidade;
    private double valor;
    
    public ResultadoPolo(long qtdInt, double valor) {
        this.quantidade = qtdInt;
        this.valor = valor;
    }
    
    public long getQuantidade() {
        return quantidade;
    }
    
    public double getValor() {
        return valor;
    }
    
    public void somar(ResultadoPolo outro) {
        this.quantidade += outro.quantidade;
        this.valor += outro.valor;
    }
    
    @Override
    public String toString() {
        return "Demonstrativo -> Total " + ", QTD -> " + quantidade + ", VALOR -> " + valor;
    }

    public static ResultadoPolo processarValores(String polo, List<String> valores) {
        if (valores == null || valores.size() < 4) { // Garante que há elementos suficientes na lista
            System.out.println("Erro: Lista " + polo + " não contém dados suficientes.");
            return new ResultadoPolo(0L, 0.0); // Retorna um objeto vazio para evitar erros
        }
        // Remove linhas em branco antes de processar
        valores.removeIf(String::isEmpty);

        if (valores.size() < 4) {  // Garante que após a remoção de linhas em branco, ainda há dados suficientes
            System.out.println("Erro: Lista " + polo + " não contém dados suficientes após remoção de linhas vazias.");
            return new ResultadoPolo(0L, 0.0); // Retorna um objeto vazio para evitar erros
        }

        // Pegamos os textos brutos das linhas
        String textoQtd = valores.get(2);  // Exemplo: "Linha 20 - Column7 -> 5538"
        String textoValor = valores.get(3);  // Exemplo: "Linha 20 - Column9 -> R$ 166544,63"

        // Captura apenas números inteiros da quantidade (valores após "-> ")
        String qtd = textoQtd.contains("->") ? textoQtd.split("->")[1].trim() : "0";

        // Converte a string para inteiro
        long qtdInt = 0;
        try {
            qtdInt = Long.parseLong(qtd.replaceAll("\\D", "")); // Remove qualquer caractere não numérico
        } catch (NumberFormatException e) {
            qtdInt = 0; // Define como 0 se a conversão falhar
            System.err.println("Erro ao converter qtd para inteiro: " + "QTD "+ qtd + "QTDINT"+qtdInt+ "-" + e.getMessage());
        }

        // Captura do valor monetário com o padrão brasileiro
        Pattern patternValor = Pattern.compile("([\\d.]+,\\d{2})");
        Matcher matcherValor = patternValor.matcher(textoValor);
        String valorParaConverter = matcherValor.find() ? matcherValor.group(1) : "0";

        String valorStr = valorParaConverter.replace(",", "."); // Converte para o formato numérico
        double valor = 0;
        try {
            valor = Double.parseDouble(valorStr);// converte para double // Remove tudo que não for número esse metodo valorStr.replaceAll("[^0-9]", "")
            
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter valor para número: " + e.getMessage());
        } 
        // Formatar o valor como moeda (com símbolo R$ e separadores de milhar)
        DecimalFormat formatoMoeda = new DecimalFormat("R$ #,##0.00"); // Formato de moeda brasileiro
        String valorFormatado = formatoMoeda.format(valor);
        System.out.println("L63  qtdInt"+ qtdInt + "Valor "+ valorFormatado + "Sem formato " + valor);

        // Retorna o resultado encapsulado
        return new ResultadoPolo(qtdInt, valor);
    }

}
