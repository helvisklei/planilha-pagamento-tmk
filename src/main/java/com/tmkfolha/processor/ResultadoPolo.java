package com.tmkfolha.processor;

import java.text.DecimalFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResultadoPolo {
    private String nomePolo;
    private String nomeMsg; 
    private long quantidade;
    private long codigoMsg; 
    private double valor;
    private long drt;
    private double gratificacao;

    

    /**
     * @return String retorna o nome do Polo - nomePolo
     */
    public String getNomePolo(){
        return nomePolo;
    }
     /**
     * @return String retorna o nome do mensageiro - nomeMsg
     */
    public String getNomeMsg() {
        return nomeMsg;
    }    
    /**
     * @param nomeMsg the nomeMsg to set
     */
    public void setNomeMsg(String nomeMsg) {
        this.nomeMsg = nomeMsg;
    }
    public long getCodigoMsg() {
        return codigoMsg;
    }
    public void setCodigoMsg(Long codigoMsg) {
        this.codigoMsg = codigoMsg;
    }
    public void setNomePolo(String nomePolo){
        this.nomePolo = nomePolo;
    }  

    public long getDrt(){
        return drt;
    }

    public void setD(long drt){
        this.drt = drt;
    }
        
    public ResultadoPolo(long qtdInt,String poloNome,String nomeMsg,long codigoMsg,long drt, double valor, double gratificacao) {
        this.quantidade = qtdInt;
        this.nomePolo = poloNome;
        this.nomeMsg = nomeMsg;        
        this.codigoMsg = codigoMsg;
        this.drt = drt;
        this.valor = valor;
        this.gratificacao = gratificacao;
    }

    public double getGratificaco(){
        return gratificacao;
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
        System.out.println("NomeMsg -> " + nomeMsg +"NomePolo -> " + nomePolo +"Total: QTD -> " + quantidade + ", VALOR -> " + valor + ", GRATIFICAÇÃO -> " + gratificacao);
        return "NomeMsg -> " + nomeMsg +"NomePolo -> " + nomePolo +"Total: QTD -> " + quantidade + ", VALOR -> " + valor + ", GRATIFICAÇÃO -> " + gratificacao;
    }

    public static ResultadoPolo processarValores(String polo, String nomeMsg, Long codigoMsg, Long drt, List<String> valores, double gratificacao) {
        if (valores == null || valores.isEmpty()) {
            System.out.println("Erro: Lista " + polo + " está vazia ou nula.");
            return new ResultadoPolo(0L, "","", 0L, 0L, 0.0, 0.0);
        }
    
        // Remove linhas em branco
        valores.removeIf(String::isEmpty);
    
        if (valores.size() < 4) {
            System.out.println("Erro: Lista " + polo + " não contém dados suficientes após remoção de linhas vazias.");
            return new ResultadoPolo(0L, "","", 0L, 0L, 0.0, 0.0);
        }
    
        // Processamento dos valores
        String poloNome = valores.get(4);
        String msgNome = valores.get(3);
        Long msgCodigo = null;
        try {
            msgCodigo = Long.parseLong(valores.get(1).replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter código da mensagem para Long: " + e.getMessage());
            msgCodigo = 0L;
        }
        String matricula = valores.get(2);
    
        // Captura da quantidade (valores após "-> ")
        String textoQtd = valores.get(2);
        String qtd = textoQtd.contains("->") ? textoQtd.split("->")[1].trim() : "0";
        long qtdInt = 0;
        try {
            qtdInt = Long.parseLong(qtd.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter qtd para inteiro: " + e.getMessage());
        }
    
        // Captura do valor monetário com o padrão brasileiro
        String textoValor = valores.get(3);
        Pattern patternValor = Pattern.compile("([\\d.]+,\\d{2})");
        Matcher matcherValor = patternValor.matcher(textoValor);
        String valorParaConverter = matcherValor.find() ? matcherValor.group(1) : "0";
    
        double valor = 0.0;
        try {
            String valorStr = valorParaConverter.replace(".", "").replace(",", ".");
            valor = Double.parseDouble(valorStr);
        } catch (NumberFormatException e) {
            System.err.println("Erro ao converter valor para número: " + e.getMessage());
        }
    
        // Formatar o valor como moeda (com símbolo R$ e separadores de milhar)
        DecimalFormat formatoMoeda = new DecimalFormat("R$ #,##0.00");
        String valorFormatado = formatoMoeda.format(valor);
    
        System.out.println("L63 qtdInt: " + qtdInt + " Valor: " + valorFormatado + " Sem formato: " + valor);
    
        // Retorna o resultado encapsulado
        return new ResultadoPolo(qtdInt, poloNome, msgNome, msgCodigo, drt, valor, gratificacao);
    }
    

    /* public static ResultadoPolo processarValores(String polo, String nomeMsg,Long codigoMsg, Long drt, List<String> valores, double gratificacao) {
        if (valores == null || valores.size() < 4) { // Garante que há elementos suficientes na lista
            System.out.println("Erro: Lista " + polo + " não contém dados suficientes.");
            return new ResultadoPolo(0L," ",0L,0L, 0.0, 0.0); // Retorna um objeto vazio para evitar erros
        }
        // Remove linhas em branco antes de processar
        valores.removeIf(String::isEmpty);

        if (valores.size() < 4) {  // Garante que após a remoção de linhas em branco, ainda há dados suficientes
            System.out.println("Erro: Lista " + polo + " não contém dados suficientes após remoção de linhas vazias.");
            return new ResultadoPolo(0L,"",0L,0L, 0.0, 0.0); // Retorna um objeto vazio para evitar erros
        }

        // Nome do plo
        String poloNome = valores.toString();
        String msgNome = valores.get(3);
        Long msgCodigo = valores.get(1);
        String matricula = valores.get(2);

        // Pegamos os textos brutos das linhas
        String textoQtd = valores.get(2);  // Exemplo: "Linha 20 - Column7 -> 5538"
        System.out.println("L74 "+textoQtd);
       
        String textoValor = valores.get(3);  // Exemplo: "Linha 20 - Column9 -> R$ 166544,63"
        System.out.println("L77 " + textoValor);

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
        return new ResultadoPolo(qtdInt,msgNome,msgCodigo,drt, valor,gratificacao);
    }
 */
    /**
     * @param quantidade the quantidade to set
     */
    public void setQuantidade(long quantidade) {
        this.quantidade = quantidade;
    }

    /**
     * @param drt the drt to set
     */
    public void setDrt(Long drt) {
        this.drt = drt;
    }

    /**
     * @return double return the gratificacao
     */
    public double getGratificacao() {
        return gratificacao;
    }

    /**
     * @param gratificacao the gratificacao to set
     */
    public void setGratificacao(double gratificacao) {
        this.gratificacao = gratificacao;
    }   


    /**
     * @param valor the valor to set
     */
    public void setValor(double valor) {
        this.valor = valor;
    }

}
