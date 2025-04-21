package com.tmkfolha.processor;

import java.util.*;
import java.util.stream.Collectors;

    public class JavaApp {
        public static Map<String, String> buscarValoresPorNomeRetornarMapaOrdenado(
        Map<String, Map<String, String>> fileData,
        String arquivo,
        String criterioOrdenacao,
        String... nomesProcurados
) {
    Map<String, String> resultados = new LinkedHashMap<>();

    if (!fileData.containsKey(arquivo)) {
        System.out.println("Arquivo não encontrado.");
        return resultados;
    }

    Map<String, String> dadosArquivo = fileData.get(arquivo);
    Set<String> nomesProcuradosSet = new HashSet<>(Arrays.asList(nomesProcurados));

    // Filtra apenas os nomes procurados
    dadosArquivo.entrySet().stream()
            .filter(entry -> nomesProcuradosSet.isEmpty() || nomesProcuradosSet.contains(entry.getKey()))
            .forEach(entry -> resultados.put(entry.getKey(), entry.getValue()));

    // Ordenação conforme o critério
    Comparator<Map.Entry<String, String>> comparator;
    switch (criterioOrdenacao.toLowerCase()) {
        case "nome":
            comparator = Comparator.comparing(entry -> {
                String[] campos = entry.getValue().split(";|,|\\|");
                return campos.length > 3 ? campos[3] : ""; // Assume que o nome está na 4ª coluna
            });
            break;
        case "codigo":
            comparator = Comparator.comparing(entry -> {
                String[] campos = entry.getValue().split(";|,|\\|");
                return campos.length > 2 ? Integer.parseInt(campos[2].trim()) : Integer.MAX_VALUE;
            });
            break;
        case "linha":
            comparator = Comparator.comparing(entry -> Integer.parseInt(entry.getKey().replaceAll("[^0-9]", "")));
            break;
        default:
            throw new IllegalArgumentException("Critério de ordenação inválido: " + criterioOrdenacao);
    }

    // Ordena as entradas do mapa conforme o comparador definido
    return resultados.entrySet().stream()
            .sorted(comparator)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new // Mantém a ordem
            ));
}

    // Método para exibição formatada
   /*  public static void exibirResultados(Map<String, String> resultados) {
        for (Map.Entry<String, String> entry : resultados.entrySet()) {
            String[] colunas = entry.getValue().split(";|,|\\|");
            System.out.println("Linha " + entry.getKey() + ":");
            for (int i = 0; i < colunas.length; i++) {
                System.out.println("  Column" + i + " -> " + colunas[i].trim());
            }
            System.out.println();
        }
    }*/

    public static Map<String, String> buscarValoresPorNomeRetornarMapa(Map<String, Map<String, String>> fileData, String arquivo, String... nomesProcurados) {
        Map<String, String> resultados = new LinkedHashMap<>();
    
        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return resultados;  // Retorna mapa vazio se o arquivo não for encontrado
        }
    
        Map<String, String> dadosArquivo = fileData.get(arquivo);
    
        // Normaliza os nomes procurados (remover acentos, espaços extras, etc.)
        Set<String> nomesProcuradosNormalizados = Arrays.stream(nomesProcurados)
            .map(XlsProcessor::normalizarTexto)
            .collect(Collectors.toSet());
    
    // Identifica todas as linhas existentes no arquivo
    Set<String> linhas = dadosArquivo.keySet().stream()
            .map(chave -> chave.split(" - ")[0])  // Pega apenas a parte "Linha X"
            .collect(Collectors.toSet());

        // Percorre cada linha do arquivo
        for (String linha : linhas) {
            StringBuilder linhaCompleta = new StringBuilder();

            // Monta a linha completa unindo todas as colunas
            for (int i = 0; ; i++) {
                String chave = linha + " - Column" + i;
                if (!dadosArquivo.containsKey(chave)) break;
                linhaCompleta.append(dadosArquivo.get(chave)).append(", ");
            }

            // Verifica se a linha completa contém algum dos nomes procurados
            String linhaNormalizada = normalizarTexto(linhaCompleta.toString());
            for (String nomeProcurado : nomesProcuradosNormalizados) {
                if (linhaNormalizada.contains(nomeProcurado)) {
                    resultados.put(linha, linhaCompleta.toString());
                    break;
                }
            }
        }

        //return resultados;
        // Ordena os resultados por chave (exemplo: "Linha 2", "Linha 174", ...)
        return resultados.entrySet().stream()
        .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(l -> Integer.parseInt(l.replaceAll("[^0-9]", "")))))
        .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
        ));
    } 
        
    /**
     * Faz uma busca personalizada para o conjuto de dados retornando o conjunto mapeado.
     * @param  fileData onde o mapa dos dados estão.
     * @param arquivo nome da chave do mapa
     * @param nomesProcurados o nome que será procurado na coluna especifica do arquivo
     * @return retorna dinamicamente o valor especificamente na coluna 4.
     */
    public static Map<String, Map<String, String>> buscarValoresPorNomeRetornarMapaDeMapa(
        Map<String, Map<String, String>> fileData,
        String arquivo,
        String... nomesProcurados
    ) {        
        Map<String, Map<String, String>> resultados = new LinkedHashMap<>();

        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return resultados;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        Set<String> nomesProcuradosSet = new HashSet<>(Arrays.asList(nomesProcurados));

        // Busca pelo valor em Column4
        dadosArquivo.entrySet().stream()
        .filter(entry -> entry.getKey().contains("Column4") && nomesProcuradosSet.contains(entry.getValue().trim()))
        .map(entry -> entry.getKey().split(" - ")[0])  // Pega a linha onde o polo foi encontrado
        .distinct()
        .forEach(linha -> {
            // Adiciona todas as colunas dessa linha ao resultado
            dadosArquivo.entrySet().stream()
                .filter(e -> e.getKey().startsWith(linha + " - "))
                .forEach(e -> resultados.computeIfAbsent(linha, k -> new LinkedHashMap<>()).put(e.getKey(), e.getValue()));
        });

        // Ordena as entradas do mapa conforme a chave
        return resultados.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(l -> {
                String numeros = l.replaceAll("\\D", "");
                return numeros.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(numeros);
            })))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    public static void exibeResultado(){
        
    }
     // Normaliza o texto removendo espaços extras e caracteres invisíveis
     public static String normalizarTexto(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[\\s\u00A0\u200B]+", " ").trim().toLowerCase();
    }
    
    // Método para exibição formatada
    public static void exibirResultados(Map<String, Map<String, String>> resultados) {
        for (Map.Entry<String, Map<String, String>> entry : resultados.entrySet()) {
            System.out.println("Linha " + entry.getKey() + ":");
            for (Map.Entry<String, String> coluna : entry.getValue().entrySet()) {
                System.out.println("  " + coluna.getKey() + " -> " + coluna.getValue().trim());
            }
            System.out.println();
        }
    }  
    /**
     * Busca em todo o arquivo, retornando todas as linhas que contêm o valor procurado em qualquer coluna
     * Faz uma busca personalizada para o conjuto de dados retornando mapeado de mapa.
     * @param  fileData onde o mapa dos dados estão.
     * @param arquivo nome da chave do mapa
     * @param nomesProcurados o nome que será procurado na coluna especifica do arquivo
     * @return retorna dinamicamente o valor especifico que esteja dentro do arquivo em qualquer coluna.
     * 
     */

    public static Map<String, Map<String, String>> buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(
            Map<String, Map<String, String>> fileData,
            String arquivo,
            String... nomesProcurados ) {
            Map<String, Map<String, String>> resultados = new LinkedHashMap<>();

        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return resultados;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        Set<String> nomesProcuradosSet = new HashSet<>(Arrays.asList(nomesProcurados));

        System.out.println("🔍 Buscando por: " + nomesProcuradosSet);

        // Filtra todas as linhas que possuem o valor procurado em qualquer coluna
        dadosArquivo.entrySet().stream()
            .filter(entry -> nomesProcuradosSet.isEmpty() || nomesProcuradosSet.contains(entry.getValue().trim()))
            .map(entry -> entry.getKey().split(" - ")[0])  // Pega a linha onde o valor foi encontrado
            .distinct()
            .forEach(linha -> {
               // Obtém o valor do "PÓLO" (coluna 4)
                String chavePolo = dadosArquivo.getOrDefault(linha + " - Column4", "DESCONHECIDO").trim();

                System.out.println("✔ Encontrado PÓLO: " + chavePolo + " na linha " + linha);

                // Adiciona todas as colunas dessa linha ao resultado agrupado pelo PÓLO
                dadosArquivo.entrySet().stream()
                    .filter(e -> e.getKey().startsWith(linha + " - "))
                    .forEach(e -> {
                    resultados.computeIfAbsent(chavePolo, k -> new LinkedHashMap<>())
                    .put(e.getKey(), e.getValue());
                        System.out.println("📌 Adicionando ao PÓLO " + chavePolo + ": " + e.getKey() + " -> " + e.getValue());
                });
            });

            System.out.println("📊 Mapa final: " + resultados);
            return resultados;
    }

      

    /**
     * Busca em todo o arquivo, retornando todas as linhas que contêm o valor procurado em qualquer coluna
     * Faz uma busca personalizada para o conjuto de dados retornando mapeado de mapa.
     * @param  fileData onde o mapa dos dados estão.
     * @param arquivo nome da chave do mapa
     * @param nomesProcurados o nome que será procurado na coluna especifica do arquivo
     * @return retorna dinamicamente o valor especifico que esteja dentro do arquivo em qualquer coluna.
     * A linha nesse codigo é a chave do mapa
     */
   /*  public static Map<String, Map<String, String>> buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(
        Map<String, Map<String, String>> fileData,
        String arquivo,
        String... nomesProcurados
        ) {
        Map<String, Map<String, String>> resultados = new LinkedHashMap<>();

        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return resultados;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        Set<String> nomesProcuradosSet = new HashSet<>(Arrays.asList(nomesProcurados));
       // System.out.println("L217 Japp"+nomesProcuradosSet);

        // Filtra todas as linhas que possuem o valor procurado em qualquer coluna
        dadosArquivo.entrySet().stream()
            .filter(entry -> nomesProcuradosSet.isEmpty() || nomesProcuradosSet.contains(entry.getValue().trim()))
            .map(entry -> entry.getKey().split(" - ")[0])  // Pega a linha onde o valor foi encontrado
           .distinct()
           .forEach(linha -> {
             // Adiciona todas as colunas dessa linha ao resultado
             dadosArquivo.entrySet().stream()
                .filter(e -> e.getKey().startsWith(linha + " - "))
                .forEach(e -> resultados.computeIfAbsent(linha, k -> new LinkedHashMap<>()).put(e.getKey(), e.getValue()));
                //System.out.println("L229 Japp "+linha);
            });

        // Ordena as entradas do mapa conforme a chave
        return resultados.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparingInt(l -> {
                String numeros = l.replaceAll("\\D", "");
                return numeros.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(numeros);
                })))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
        } */

    
    public static String calcularPercentualPorPolo(Map<String, Map<String, String>> fileData, String key, String polo, double rendimento) {
     // Localiza o polo
        Map<String, Map<String, String>> gratificacaoTabela = buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, polo);
        if (gratificacaoTabela == null || gratificacaoTabela.isEmpty()) {
            return "Polo não encontrado";
        }
        
        String percentual = "";
        for (Map<String, String> linha : gratificacaoTabela.values()) {
                String nome = linha.getOrDefault(key + " - Column3", "");
                String percentualLinha = linha.getOrDefault(key + " - Column2", "");
        
                if (nome.equalsIgnoreCase("Maior ou igual") && rendimento >= 0.91) {
                    percentual = percentualLinha;
                    break;
                } else if (nome.equalsIgnoreCase("Menor que") && rendimento < 0.91 && rendimento >= 0.88) {
                    percentual = percentualLinha;
                    break;
                } else if (nome.equalsIgnoreCase("Menor que") && rendimento < 0.88 && rendimento >= 0.85) {
                    percentual = percentualLinha;
                    break;
                } else if (nome.equalsIgnoreCase("Menor que") && rendimento < 0.85 && rendimento >= 0.80) {
                    percentual = percentualLinha;
                    break;
                } else if (nome.equalsIgnoreCase("Menor que") && rendimento < 0.80 && rendimento >= 0.75) {
                    percentual = percentualLinha;
                    break;
                } else if (nome.equalsIgnoreCase("Menor que") && rendimento < 0.75 && rendimento >= 0.70) {
                    percentual = percentualLinha;
                    break;
                } else if (nome.equalsIgnoreCase("Menor que") && rendimento < 0.70) {
                    percentual = percentualLinha;
                    break;
                }
            }
        
            return percentual.isEmpty() ? "Percentual não encontrado" : percentual;
    }
}
