package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class Funcionarios {
    // Atributos básicos (do NOME.XLS)
    private String tipo;
    private String codigo;
    private String drt;
    private String nome;
    private String polo;
    private String setor;  

    // Atributos calculados
    private BigDecimal quantidadePolo;
    private BigDecimal valorPolo;
    private BigDecimal metaQuantidade;
    private BigDecimal metaValor;
    private BigDecimal metaRecebido;
    private BigDecimal percentualQuantidade;
    private BigDecimal percentualValor;

    private Map<String, BigDecimal> operacoes = new HashMap<>(); // Mapeia tipo de operação para valor

    // Construtor padrão
    public Funcionarios() {}

    // Construtor com campos básicos (opcional)
    public Funcionarios(String tipo, String codigo, String drt, String nome, String polo, String setor) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.drt = drt;
        this.nome = nome;
        this.polo = polo;
        this.setor = setor;
    }

    // --- Getters e Setters ---
    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDrt() {
        return drt;
    }

    public void setDrt(String drt) {
        this.drt = drt;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getPolo() {
        return polo;
    }

    public void setPolo(String polo) {
        this.polo = polo;
    }

    public String getSetor() {
        return setor;
    }

    public void setSetor(String setor) {
        this.setor = setor;
    }

    public BigDecimal getQuantidadePolo() {
        return quantidadePolo;
    }

    public void setQuantidadePolo(BigDecimal quantidadePolo) {
        this.quantidadePolo = quantidadePolo;
    }

    public BigDecimal getValorPolo() {
        return valorPolo;
    }

    public void setValorPolo(BigDecimal valorPolo) {
        this.valorPolo = valorPolo;
    }

    public BigDecimal getMetaQuantidade() {
        return metaQuantidade;
    }

    public void setMetaQuantidade(BigDecimal metaQuantidade) {
        this.metaQuantidade = metaQuantidade;
    }

    public BigDecimal getMetaValor() {
        return metaValor;
    }

    public void setMetaValor(BigDecimal metaValor) {
        this.metaValor = metaValor;
    }

    public BigDecimal getMetaRecebido() {
        return metaRecebido;
    }

    public void setMetaRecebido(BigDecimal metaRecebido) {
        this.metaRecebido = metaRecebido;
    }

    public BigDecimal getPercentualQuantidade() {
        return percentualQuantidade;
    }

    public void setPercentualQuantidade(BigDecimal percentualQuantidade) {
        this.percentualQuantidade = percentualQuantidade;
    }

    public BigDecimal getPercentualValor() {
        return percentualValor;
    }

    public void setPercentualValor(BigDecimal percentualValor) {
        this.percentualValor = percentualValor;
    }

    // --- Métodos auxiliares ---
    @Override
    public String toString() {
        return "Funcionario{" +
                "tipo='" + tipo + '\'' +
                ", codigo='" + codigo + '\'' +
                ", drt='" + drt + '\'' +
                ", nome='" + nome + '\'' +
                ", polo='" + polo + '\'' +
                ", setor='" + setor + '\'' +
                ", quantidadePolo=" + quantidadePolo +
                ", valorPolo=" + valorPolo +
                ", metaQuantidade=" + metaQuantidade +
                ", metaValor=" + metaValor +
                ", metaRecebido=" + metaRecebido +
                ", percentualQuantidade=" + percentualQuantidade +
                ", percentualValor=" + percentualValor +
                '}';
    }
    

}