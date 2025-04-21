package com.tmkfolha.app.controllers;

import java.math.BigDecimal;

public class Operadora {
    /* private String codigo; 
    private String nome;   

    private double quantidadeBoletos;
    private double valorBoletos;
    private double quantidadeRecibos;
    private double valorRecibos; */

    private String codigo;
    private String nome;
    private int quantidadeBoletos;
    private BigDecimal valorBoletos;
    private BigDecimal comissaoBoleto;
    private int quantidadeRecibos;
    private BigDecimal valorRecibos;
    private BigDecimal comissaoRecibo;
    
    // Construtor, getters e setters

    // Construtor que estava faltando
    // Construtor, getters e setters
    public Operadora(String codigo, String nome) {
        this.codigo = codigo;
        this.nome = nome;
        this.quantidadeBoletos = 0;
        this.valorBoletos = BigDecimal.ZERO;
        this.comissaoBoleto = BigDecimal.ZERO;
        this.quantidadeRecibos = 0;
        this.valorRecibos = BigDecimal.ZERO;
        this.comissaoRecibo = BigDecimal.ZERO;
    }
    
    // Construtor vazio (opcional)
    public Operadora() {
        this("", "");
    }

    // Métodos para adicionar valores
    public void adicionarBoleto(int quantidade, BigDecimal valor, BigDecimal comissao) {
        this.quantidadeBoletos += quantidade;
        this.valorBoletos = this.valorBoletos.add(valor != null ? valor : BigDecimal.ZERO);
        this.comissaoBoleto = this.comissaoBoleto.add(comissao != null ? comissao : BigDecimal.ZERO);
    }

    public void adicionarRecibo(int quantidade, BigDecimal valor, BigDecimal comissao) {
        this.quantidadeRecibos += quantidade;
        this.valorRecibos = this.valorRecibos.add(valor != null ? valor : BigDecimal.ZERO);
        this.comissaoRecibo = this.comissaoRecibo.add(comissao != null ? comissao : BigDecimal.ZERO);
    }
    
/*     public void addBoleto(double quantidade, double valor) {
        this.quantidadeBoletos += quantidade;
        this.valorBoletos += valor;
    } 
    
    public void addRecibo(double quantidade, double valor) {
        this.quantidadeRecibos += quantidade;
        this.valorRecibos += valor;
    }
*/ 
   /*  public double getTotalValor() {
        return valorBoletos + valorRecibos;
    } */   

   /*  public double getValorBoletos() {
        return valorBoletos;
    } */
 
    /* public double getValorRecibos() {
        return valorRecibos;
    } */

    // Getters
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }  

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

     /**
     * @param quantidadeBoletos the quantidadeBoletos to set
     */
    public void setQuantidadeBoleto(int quantidadeBoletos) { this.quantidadeBoletos = quantidadeBoletos;   }
    public int getQuantidadeBoleto() { return quantidadeBoletos; }

    /**
     * @param quantidadeBoletos the quantidadeBoletos to set
     */
    public void setQuantidadeBoletos(int quantidadeBoletos) { this.quantidadeBoletos = quantidadeBoletos; }   
    public double getQuantidadeBoletos() { return quantidadeBoletos; }

    public double getQuantidadeRecibos() { return quantidadeRecibos; }
    public double getTotalQuantidade() {  return quantidadeBoletos + quantidadeRecibos;  }

      /**
     * @param valorBoletos the valorBoletos to set
     */
    public void setValorBoletos(BigDecimal valorBoletos) { this.valorBoletos = valorBoletos;  }   
    public BigDecimal getValorBoleto() { return valorBoletos; }

     /**
     * @param comissaoBoleto the comissaoBoleto to set
     */
    public void setComissaoBoleto(BigDecimal comissaoBoleto) {  this.comissaoBoleto = comissaoBoleto;  }
    public BigDecimal getComissaoBoleto() { return comissaoBoleto; }

   /**
     * @param quantidadeRecibos the quantidadeRecibos to set
     */
    public void setQuantidadeRecibos(int quantidadeRecibos) { this.quantidadeRecibos = quantidadeRecibos; }
    public int getQuantidadeRecibo() { return quantidadeRecibos; }

     /**
     * @param valorRecibos the valorRecibos to set
     */
    public void setValorRecibos(BigDecimal valorRecibos) { this.valorRecibos = valorRecibos; }
    public BigDecimal getValorRecibo() { return valorRecibos; }

    /**
     * @param comissaoRecibo the comissaoRecibo to set
     */
    public void setComissaoRecibo(BigDecimal comissaoRecibo) { this.comissaoRecibo = comissaoRecibo;  }
    public BigDecimal getComissaoRecibo() { return comissaoRecibo; }


   

   

}
