package com.tmkfolha.app.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FiltradorDados {

    /**
     * Filtra e retorna os dados relevantes de fileData.
     *
     * @param fileData          Mapa contendo os dados dos arquivos.
     * @param colunasRelevantes Lista de colunas relevantes a serem filtradas.
     * @return Mapa contendo os dados filtrados.
     */
    public static Map<String, Map<String, String>> filtrarColunasRelevantes(
            Map<String, Map<String, String>> fileData, List<String> colunasRelevantes) {
        Map<String, Map<String, String>> dadosFiltrados = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> arquivoEntry : fileData.entrySet()) {
            String nomeArquivo = arquivoEntry.getKey();
            Map<String, String> dadosLinhas = arquivoEntry.getValue();

            // Filtra as colunas relevantes
            Map<String, String> dadosFiltradosPorArquivo = new HashMap<>();
            for (Map.Entry<String, String> linhaEntry : dadosLinhas.entrySet()) {
                String chave = linhaEntry.getKey();
                String valor = linhaEntry.getValue();

                // Verifica se a chave contém alguma das colunas relevantes
                for (String coluna : colunasRelevantes) {
                    if (chave.contains(coluna)) {
                        dadosFiltradosPorArquivo.put(chave, valor);
                    }
                }
            }

            // Adiciona ao mapa de dados filtrados
            dadosFiltrados.put(nomeArquivo, dadosFiltradosPorArquivo);
        }
        return dadosFiltrados;
    }

    /**
     * Exibe os dados filtrados no console.
     *
     * @param dadosFiltrados Mapa contendo os dados filtrados.
     */
    public static void exibirDadosFiltrados(Map<String, Map<String, String>> dadosFiltrados) {
        for (Map.Entry<String, Map<String, String>> arquivoEntry : dadosFiltrados.entrySet()) {
            String nomeArquivo = arquivoEntry.getKey();
            Map<String, String> dadosLinhas = arquivoEntry.getValue();

            System.out.println("Arquivo: " + nomeArquivo);
            System.out.println("----------------------------------------");

            for (Map.Entry<String, String> linhaEntry : dadosLinhas.entrySet()) {
                String chave = linhaEntry.getKey();
                String valor = linhaEntry.getValue();
                System.out.println(chave + ": " + valor);
            }

            System.out.println(); // Linha em branco para separar arquivos
        }
    } 

    public static Map<String, Double> transformarMetasEmLista(Map<String, Map<String, String>> metasDasOperadoras) {
        Map<String, Double> metasPorCodigo = new HashMap<>();
    
        // Verifica se o mapa não está vazio
        if (metasDasOperadoras == null || metasDasOperadoras.isEmpty()) {
            System.out.println("O mapa de metas das operadoras está vazio.");
            return metasPorCodigo; // Retorna um mapa vazio
        }
    
            // Percorre o mapa de metas
            for (Map.Entry<String, Map<String, String>> entry : metasDasOperadoras.entrySet()) {
                Map<String, String> dadosLinha = entry.getValue();
        
                // Extrai o código e a meta da linha
                String coluna0 = dadosLinha.get("Column0"); // CÓD + NOME
                String metaStr = dadosLinha.get("Column3"); // META REC

                // Extrai o código da coluna0
                String codigo = extrairCodigo(coluna0);

                // Verifica se metaStr não é nulo ou vazio
            if (codigo != null && metaStr != null && !metaStr.trim().isEmpty()) {
                try {
                    // Converte a meta para double (substitui vírgula por ponto)
                    double meta = Double.parseDouble(metaStr.replace(",", "."));
                    metasPorCodigo.put(codigo, meta); // Adiciona ao mapa
                } catch (NumberFormatException e) {
                    System.out.println("Erro ao converter meta para double: " + metaStr);
                }
            } else {
                System.out.println("Meta não encontrada ou vazia para o código: " + codigo);
            }
        }

        return metasPorCodigo;
    }

    public static String extrairCodigo(String coluna0) {
        if (coluna0 == null || coluna0.trim().isEmpty()) {
            return null; // Retorna null se a coluna estiver vazia
        }
    
        // Remove espaços em branco e divide a string pelo primeiro espaço
        String[] partes = coluna0.trim().split("\\s+", 2); // Divide em no máximo 2 partes
    
        // Extrai o código (primeira parte) e remove zeros à esquerda
        String codigoComZeros = partes[0];
        String codigo = codigoComZeros.replaceFirst("^0+(\\d+)$", "$1"); // Remove zeros à esquerda
    
        return codigo;
    }    

       /* public static List<String> transformarMetasEmLista(Map<String, Map<String, String>> metasDasOperadoras) {
        List<String> listaMetas = new ArrayList<>();

        // Verifica se o mapa não está vazio
        if (metasDasOperadoras == null || metasDasOperadoras.isEmpty()) {
            System.out.println("O mapa de metas das operadoras está vazio.");
            return listaMetas; // Retorna uma lista vazia
        }

        // Percorre o mapa externo (operadoras)
        for (Map.Entry<String, Map<String, String>> entryOperadora : metasDasOperadoras.entrySet()) {
            String nomeOperadora = entryOperadora.getKey(); // Nome da operadora
            Map<String, String> metas = entryOperadora.getValue(); // Metas da operadora

            // Verifica se o mapa interno (metas) não está vazio
            if (metas == null || metas.isEmpty()) {
                System.out.println("Nenhuma meta encontrada para a operadora: " + nomeOperadora);
                continue; // Pula para a próxima operadora
            }

            // Percorre o mapa interno (metas)
            for (Map.Entry<String, String> entryMeta : metas.entrySet()) {
                String chaveMeta = entryMeta.getKey(); // Chave da meta (ex: "Meta1")
                String valorMeta = entryMeta.getValue(); // Valor da meta (ex: "1000")

                // Formata a meta como uma string e adiciona à lista
                String metaFormatada = nomeOperadora + " - " + chaveMeta + ": " + valorMeta;
                listaMetas.add(metaFormatada);
            }
        }

        return listaMetas;
    }
         */
       
}    
