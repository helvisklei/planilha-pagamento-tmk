package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecebidoOperador extends XlsData {
    private static final Logger logger = LogManager.getLogger(RecebidoOperador.class);
   
    public RecebidoOperador(Map<String, String> rawData) {
        super(rawData);
        logger.debug("Criando RecebidoOperador com dados: {}", rawData);
    }
    
    @Override
    public String getCodigo() {
        String codigo = rawData.get("Column0"); // normalizarCodigo(rawData.get("Column0"));
        logger.trace("Obtendo código: {}", codigo);
        return codigo;
    }
    
    @Override
    public String getNome() {
       // String[] parts = rawData.get("Column0").split(" ");
        return rawData.get("NomeOPE");//String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
    }

    public String getTipoOperacao() {
        return rawData.get("TipoOpe"); // BOL, REC e PIX etc..,
    }
    
    // Métodos específicos para este tipo de arquivo
    public BigDecimal getValorBruto() {
        // Prioriza Column6, mas faz fallback para Column8 se necessário
        String valorStr = rawData.getOrDefault("Column6", rawData.get("Column8"));
        if (valorStr == null) {
            return BigDecimal.ZERO;
        }
        return parseMonetaryValue(valorStr);
    }
    
    public BigDecimal getValorLiquido() {
        BigDecimal comissao = parseMonetaryValue(rawData.get("Column8"));
        logger.debug("Comissão para {}: {}", getNome(), comissao);
        return comissao;
    }
    
    public BigDecimal getComissao() {
        return parseMonetaryValue(rawData.get("Column12"));
    }
    
    public Integer getQuantidade() {
        return parseInteger(rawData.get("Column3"));
    }
    
    public String getCargo() {
        return rawData.get("Column2");
    }


    @Override
    public String toString() {
        return String.format("RecebidoOperador[%s - %s (%s)]", getCodigo(), getNome(),getTipoOperacao());
    }

     public Map<String, String> processarColumn0(Map<String, String> dadosBrutos) {
        if (!dadosBrutos.containsKey("Column0")) {
            return dadosBrutos;
        }

        String valorOriginal = dadosBrutos.get("Column0").trim();
        Map<String, String> dadosProcessados = new HashMap<>(dadosBrutos);
        
        // Expressão regular atualizada:
        // ^(\d+) - código (1 ou mais dígitos)
        // \s+ - espaços
        // ([A-Za-zÀ-ú\s]+?) - nome (letras e espaços, mínimo possível)
        // \s+ - espaços
        // (REC|BOL|DEP|OUT|CHE|PIX)$ - tipo (3 letras, pode adicionar mais)
        Pattern pattern = Pattern.compile("^(\\d+)\\s+([A-Za-zÀ-ú\\s]+?)\\s+(REC|BOL|DEP|OUT|CHE|PIX)$");
        Matcher matcher = pattern.matcher(valorOriginal);
        
        if (matcher.find()) {
            dadosProcessados.put("Column0", matcher.group(1).trim());  // Código (pode ter mais de 3 dígitos)
            dadosProcessados.put("NomeOPE", matcher.group(2).trim());  // Nome (sem espaços extras)
            dadosProcessados.put("TipoOpe", matcher.group(3).trim());  // Tipo
        } else {
            logger.warn("Formato inválido para Column0: '{}'", valorOriginal);
            // Tenta fallback mais simples para códigos longos
            String[] partes = valorOriginal.split("\\s+", 3);
            if (partes.length >= 3) {
                dadosProcessados.put("Column0", partes[0]);
                dadosProcessados.put("NomeOPE", partes[1]);
                dadosProcessados.put("TipoOpe", partes[2]);
            } else {
                // Mantém o original se não conseguir parsear
                dadosProcessados.put("Column0", valorOriginal);
            }
        }
        
        return dadosProcessados;
    }

    public void debugDados() {
        logger.info("=== DETALHES DO OPERADOR ===");
        logger.info("Código: {}", getCodigo());
        logger.info("Nome: {}", getNome());
        logger.info("Tipo: {}", getTipoOperacao());
        logger.info("Valor Bruto: {}", getValorBruto());
        logger.info("Valor Líquido: {}", getValorLiquido());
        logger.info("Dados brutos completos: {}", rawData);
    }
     /**
     * Normaliza códigos removendo zeros à esquerda e espaços
     */
    private String normalizarCodigo(String codigoOriginal) {
        if (codigoOriginal == null) return "";
        // Remove espaços e zeros à esquerda
        return codigoOriginal.trim().replaceFirst("^0+", "");
    }

}