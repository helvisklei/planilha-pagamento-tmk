package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class XlsData {
    protected static final Logger logger = LogManager.getLogger(XlsData.class);
    protected Map<String, String> rawData;
    
    public XlsData(Map<String, String> rawData) {
        this.rawData = rawData;
        logger.debug("Criando novo XlsData com dados: {}", rawData);
    }
    
    // Métodos comuns a todos os tipos de dados
    public abstract String getCodigo();
    public abstract String getNome();
    
    // Método para converter valores monetários
    protected BigDecimal parseMonetaryValue(String value) {
        if (value == null || value.isEmpty()) return BigDecimal.ZERO;
        try {
            String cleaned = value.replace("R$", "").replace(".", "").replace(",", ".");
            return new BigDecimal(cleaned.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
    
    // Método para converter valores inteiros
    protected Integer parseInteger(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Cria uma instância de {@code XlsData} baseada no nome do arquivo informado.
     * Este método identifica o tipo do arquivo a partir do nome e retorna um objeto correspondente.
     *
     * @param fileName Nome do arquivo a ser processado. O nome será convertido para letras maiúsculas para padronização.
     * @param data Mapa contendo os dados do arquivo, onde a chave representa um identificador e o valor é o dado correspondente.
     * @return Uma instância de {@code XlsData} correspondente ao tipo de arquivo identificado.
     * @throws IllegalArgumentException Se o tipo de arquivo não for suportado.
     */
    public static XlsData createFromFile(String fileName, Map<String, String> data) {
        fileName = fileName.toUpperCase();
        
        logger.info("Criando objeto para arquivo: {}", fileName);
        if (fileName.contains("RECEBIDO POR OPERADOR")) {
            logger.debug("Identificado como RecebidoOperador");
            return new RecebidoOperador(data);
        } 
        else if (fileName.contains("META REC")) {
            logger.debug("Identificado como meta por Operador");
            return new MetaParaReciboPorOperador(data);
            
        }        
        else if (fileName.contains("GERAL SITUAÇÃO MENS.XLS")) {
            String setor = determinarSetor(fileName); // Implemente este método
            return new GeralSituacaoMensal(data, setor);
        }
        else if (fileName.contains("Tabela-Gratificacao.xls") || fileName.contains("Tabela-Gratificacao")) {
            return new GratificacaoCalculator(data);
        }
        // outros else if para outros tipos...
        logger.error("Tipo de arquivo não suportado: {}", fileName);
        
        throw new IllegalArgumentException("Tipo de arquivo não suportado: " + fileName);
    }

    public static String determinarSetor(String fileName) {

        if (fileName.toUpperCase().contains("SETOR 2") || fileName.toUpperCase().contains("SERRA TALHADA")) {
            return "SETOR 2";
        } else if (fileName.toUpperCase().contains("SETOR 3") || fileName.toUpperCase().contains("PETROLINA")) {
            return "SETOR 3";
        } else if (fileName.toUpperCase().contains("SETOR 4") || fileName.toUpperCase().contains("MATRIZ")) {
            return "SETOR 4";
        } else if (fileName.toUpperCase().contains("SETOR 5") || fileName.toUpperCase().contains("CARUARU")) {
            return "SETOR 5";
        } else if (fileName.toUpperCase().contains("SETOR 6") || fileName.toUpperCase().contains("GARANHUNS")) {
            return "SETOR 6";
        }
        // Adicione outros setores conforme necessário
        return "OUTRO";
    }
    
}
