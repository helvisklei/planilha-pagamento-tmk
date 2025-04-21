package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class IntegradorQuantidadeValor {
    public static void integrarQuantidadeEValor(List<Funcionario> funcionarios, List<String> dadosPolo) {
        for (Funcionario funcionario : funcionarios) {
            for (String linha : dadosPolo) {
                // Suponha que cada linha seja um CSV (exemplo: "codigo,quantidade,valor")
                String[] partes = linha.split(",");
                if (partes.length >= 3) { // Verifica se a linha tem pelo menos 3 partes
                    String codigo = partes[0];
                    int quantidade = parseIntSafe(partes[1], "quantidade", Map.of());
                    double valor = parseDoubleSafe(partes[2].replace("R$", "").replace(".", "").replace(",", "."), "valor", Map.of());
    
                    // Verifica se o código do funcionário corresponde ao código na linha
                    if (funcionario.getCodigo().equals(codigo)) {
                        funcionario.adicionarValorEQuantidade(valor, quantidade);
                        break; // Sai do loop após encontrar o funcionário correspondente
                    }
                }
            }
        }
        
    }

    public static void integrarDadosDoPolo(List<Funcionario> funcionarios, List<String> dadosPolo, String nomePolo) {
        // Extrai os valores de quantidade e valor do polo
        String quantidadePolo = "";
        String valorPolo = "";
       // System.out.println("Dados do polo: " + dadosPolo);
    
        for (String item : dadosPolo) {
            if (item.startsWith("DemonsTrativoQtd")) {
                quantidadePolo = item.split(" -> ")[1];
            } else if (item.startsWith("DemonsTrativoValor")) {
                valorPolo = item.split(" -> ")[1];
            }
        }
    
        if (!quantidadePolo.isEmpty() && !valorPolo.isEmpty()) {
            // Percorre a lista de funcionários para encontrar o último do polo
            Funcionario ultimoFuncionarioDoPolo = null;
            for (Funcionario funcionario : funcionarios) {
                if (funcionario.getPolo().equalsIgnoreCase(nomePolo)) {
                    ultimoFuncionarioDoPolo = funcionario; // Atualiza o último funcionário do polo
                }
            }
    
            // Atribui os valores ao último funcionário do polo
            if (ultimoFuncionarioDoPolo != null) {
                System.out.println("Atribuindo valores ao último funcionário do polo: " + ultimoFuncionarioDoPolo.getNome());
                ultimoFuncionarioDoPolo.setQuantidadePolo(Integer.parseInt(quantidadePolo));
                ultimoFuncionarioDoPolo.setValorPolo(new BigDecimal(valorPolo.replace("R$", "").replace(".", "").replace(",", ".")));
            } else {
                System.out.println("Nenhum funcionário do polo " + nomePolo + " encontrado.");
            }
        } else {
            System.out.println("Valores de quantidade ou valor do polo não encontrados.");
        }
    }

    /* public static void integrarDadosDoPolo(List<Funcionario> funcionarios, List<String> dadosPolo, String nomePolo) {
        // Extrai os valores de quantidade e valor do polo
        String quantidadePolo = "";
        String valorPolo = "";
        System.out.println("L32 " + nomePolo);
    
        for (String item : dadosPolo) {
            if (item.startsWith("DemonsTrativoQtd")) {
                quantidadePolo = item.split(" -> ")[1];
            } else if (item.startsWith("DemonsTrativoValor")) {
                valorPolo = item.split(" -> ")[1];
            }
        }
    
        if (!quantidadePolo.isEmpty() && !valorPolo.isEmpty()) {
            // Adiciona os valores ao primeiro funcionário do polo (ou a um objeto específico para o polo)
            for (Funcionario funcionario : funcionarios) {
                if (funcionario.getPolo().equalsIgnoreCase(nomePolo)) {
                    funcionario.setQuantidadePolo(Integer.parseInt(quantidadePolo));
                    funcionario.setValorPolo(Double.parseDouble(valorPolo.replace("R$", "").replace(".", "").replace(",", ".")));
                    break; // Sai do loop após adicionar os valores ao polo
                }
            }
        }
    } */

    public static int parseIntSafe(String valor, String campo, Map<String, String> contexto) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("Erro ao converter campo '" + campo + "' para int: " + valor);
            return 0;
        }
    }
    
    public static double parseDoubleSafe(String valor, String campo, Map<String, String> contexto) {
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException | NullPointerException e) {
            System.err.println("Erro ao converter campo '" + campo + "' para double: " + valor);
            return 0.0;
        }
    }


}    
    
    

    
/*     public static void integrarQuantidadeEValor(List<Funcionario> funcionarios, List<String> quantidadeEValor) {
        for (String linha : quantidadeEValor) {
            String[] partes = linha.split(" -> ");
            if (partes.length != 2) continue;

            String[] identificador = partes[0].split(" - ");
            if (identificador.length != 2) continue;

            String coluna = identificador[1].trim();
            String valorStr = partes[1].trim().replace("R$", "").replace(".", "").replace(",", ".");

            try {
                if ("Column7".equals(coluna)) {
                    int quantidade = Integer.parseInt(valorStr);
                    //System.out.println("26 em integrador " + quantidade);
                    System.out.println("L21 INTEGRADOR - Tamanho da lista de funcionários: " + funcionarios.size());
                    for (Funcionario funcionario : funcionarios) {                       
                        funcionario.adicionarValorEQuantidade(0, quantidade);                        
                        //System.out.println("L24 INTEGRADOR " + funcionario);
                    }
                } else if ("Column9".equals(coluna)) {
                    double valor = Double.parseDouble(valorStr);
                    //System.out.println("26 em integrador " + valor);
                    for (Funcionario funcionario : funcionarios) {                        
                        funcionario.adicionarValorEQuantidade(valor, 0);
                        //System.out.println("L31 INTEGRADOR " + funcionario);
                    }
                }
               
            } catch (NumberFormatException e) {
                System.err.println("Erro ao converter valor: " + valorStr);
            }
        }
    } */


