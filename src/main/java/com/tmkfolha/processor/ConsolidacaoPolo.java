package com.tmkfolha.processor;

import java.util.*;

public class ConsolidacaoPolo {

    public static void consolidarDadosPorPolo(
            Map<String, Map<String, String>> mapaConsolidado,
            Map<String, Map<String, String>> tabelaGratificacao,
            List<String> tabelaNomes,
            List<String> demonstrativoCaixa) {

        // Obtém o maior número atual para continuar a numeração
        int maxNumero = mapaConsolidado.keySet().stream()
                .filter(k -> k.matches("Linha \\d+"))
                .mapToInt(k -> Integer.parseInt(k.split(" ")[1]))
                .max().orElse(0);

        // Consolida Tabela Gratificação e outros
        for (Map.Entry<String, Map<String, String>> entry : tabelaGratificacao.entrySet()) {
            maxNumero++;
            String novaChave = "Linha " + maxNumero;
            mapaConsolidado.put(novaChave, entry.getValue());
        }

        // Consolida Nomes
        if (!tabelaNomes.isEmpty()) {
            Map<String, String> nomesMap = new HashMap<>();
            for (int i = 0; i < tabelaNomes.size(); i++) {
                nomesMap.put("Nome " + (i + 1), tabelaNomes.get(i));
            }
            mapaConsolidado.put("Nomes", nomesMap);
        }

        // Consolida Demonstrativo de Caixa
        if (!demonstrativoCaixa.isEmpty()) {
            Map<String, String> demonstrativoMap = new HashMap<>();
            for (int i = 0; i < demonstrativoCaixa.size(); i++) {
                demonstrativoMap.put("Demonstrativo " + (i + 1), demonstrativoCaixa.get(i));
            }
            mapaConsolidado.put("Demonstrativo de Caixa", demonstrativoMap);
        }
    }

   /*  public static void consolidarDadosPorPolo(
            Map<String, Map<String, String>> mapaConsolidado,
            Map<String, Map<String, String>> tabelaGratificacao,
            List<String> tabelaNomes,
            List<String> demonstrativoCaixa) {

        // Consolida Tabela Gratificação e outros
        for (Map.Entry<String, Map<String, String>> entry : tabelaGratificacao.entrySet()) {
            String linha = entry.getKey();
            String novaChave = gerarChaveUnica(mapaConsolidado, linha);
            mapaConsolidado.put(novaChave, entry.getValue());
        }

        // Consolida Nomes
        if (!tabelaNomes.isEmpty()) {
            Map<String, String> nomesMap = new HashMap<>();
            for (int i = 0; i < tabelaNomes.size(); i++) {
                nomesMap.put("Nome " + (i + 1), tabelaNomes.get(i));
            }
            mapaConsolidado.put("Nomes", nomesMap);
        }

        // Consolida Demonstrativo de Caixa
        if (!demonstrativoCaixa.isEmpty()) {
            Map<String, String> demonstrativoMap = new HashMap<>();
            for (int i = 0; i < demonstrativoCaixa.size(); i++) {
                demonstrativoMap.put("Demonstrativo " + (i + 1), demonstrativoCaixa.get(i));
            }
            mapaConsolidado.put("Demonstrativo de Caixa", demonstrativoMap);
        }
    }

    private static String gerarChaveUnica(Map<String, Map<String, String>> mapa, String chave) {
        int contador = 1;
        String novaChave = chave;
        while (mapa.containsKey(novaChave)) {
            novaChave = chave + " (" + contador + ")";
            contador++;
        }
        return novaChave;
    } */
}
