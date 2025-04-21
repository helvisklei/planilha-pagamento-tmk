package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DemonstrativoDados {
    private int quantidade;
    private BigDecimal valor;

    public DemonstrativoDados() {
        this.quantidade = 0;
        this.valor = BigDecimal.ZERO;
    }

     public DemonstrativoDados(int quantidade, BigDecimal valor) {
        this.quantidade = quantidade;
        this.valor = valor;
    } 

    // Novo Construtor que recebe Strings formatadas e faz o parsing
    public DemonstrativoDados(String quantidadeStr, String valorStr) {
        this.quantidade = Integer.parseInt(quantidadeStr.split("->")[1].trim());
        this.valor = new BigDecimal(
            valorStr.split("->")[1]
                .replace("R$", "")  // Remove o símbolo de moeda
                .replace(".", "")   // Remove pontos separadores de milhar
                .replace(",", ".")  // Troca vírgula por ponto para conversão numérica
                .trim()
        ).setScale(2, RoundingMode.HALF_UP); // Mantém duas casas decimais
    }

    public int getQuantidade() {
        return quantidade;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }   

    @Override
    public String toString() {
        return "Quantidade: " + quantidade + ", Valor: " + valor;
    }
}
