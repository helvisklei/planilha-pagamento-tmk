package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MetaParaReciboPorOperador extends XlsData {
    private static final Logger logger = LogManager.getLogger(RecebidoOperador.class);    

    public MetaParaReciboPorOperador(Map<String, String> rawData) {
        super(rawData);
        logger.debug("Criando RecebidoOperador com dados: {}", rawData);
    }

    //CÓD 0 DRT 1	NOMES 2	META REC 3
       
    @Override
    public String getCodigo() {
        String codigo = rawData.get("Column0");
        logger.trace("Obtendo código: {}", codigo);
        return codigo;
    }

    public Integer getDrt() {
        return parseInteger(rawData.get("Column1"));
    }

    @Override
    public String getNome() {
       
        return rawData.get("Column2");
    }

    public BigDecimal getValorLiquido() {
        BigDecimal metaRecebidoOpe = parseMonetaryValue(rawData.get("Column3"));
        logger.debug("Meta de recebido para {}: {}", getNome(), metaRecebidoOpe);
        return metaRecebidoOpe;
    }    

    @Override
    public String toString() {
        return String.format("Meta Recebido Por Operdora [%s - %s (%s) - %s]", getCodigo(), getDrt(),getNome(), getValorLiquido());
    }

}
