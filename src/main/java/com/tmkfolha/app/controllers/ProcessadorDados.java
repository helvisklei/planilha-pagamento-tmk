package com.tmkfolha.app.controllers;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

public class ProcessadorDados {

    public static Map<String, List<Funcionario>> processarDados(Map<String, Map<String, String>> fileData) {
        Map<String, List<Funcionario>> dadosPorPolo = new HashMap<>();
        Map<String, Funcionario> funcionariosPorCodigo = new HashMap<>();
        for (String chave : fileData.keySet()) {
            System.out.println("Chave encontrada: " + chave);
        }
        

        processarNomes(fileData.get("Nomes.xls"), dadosPorPolo, funcionariosPorCodigo);
        processarGeral(fileData.get("GERAL SITUAÇÃO MENS.xls"), funcionariosPorCodigo);

        return dadosPorPolo;
    }

    private static void processarNomes(Map<String, String> nomesData, 
                                       Map<String, List<Funcionario>> dadosPorPolo,
                                       Map<String, Funcionario> funcionariosPorCodigo) {
        if (nomesData == null) return;

        System.out.println("=================================================================");
        System.out.println("           PROCESSAMENTO DE DADOS DE NOMES");
        System.out.println("=================================================================");

        for (Map.Entry<String, String> entry : nomesData.entrySet()) {
            String linha = entry.getKey();
            String valor = entry.getValue();
            
            System.out.println("\n-----------------------------------------------------------------");
            System.out.println("Processando Linha: " + linha);
            System.out.println("Conteúdo da Linha: " + valor);
            
            if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
                Pattern pattern = Pattern.compile("Column\\d+=(.*?)(,\\s*|$)");
                Matcher matcher = pattern.matcher(valor);
                
                List<String> colunas = new ArrayList<>();
                while (matcher.find()) {
                    colunas.add(matcher.group(1).trim());
                }
                
                if (colunas.size() >= 5) {
                    String codigo = colunas.get(1);
                    String drt = colunas.get(2);
                    String nome = colunas.get(3);
                    String polo = colunas.get(4);

                    nome = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

                    Funcionario funcionario = new Funcionario(codigo, drt, nome, polo);
                    funcionario.setCodigo(codigo);
                    funcionario.setDrt(drt);
                    funcionario.setNome(nome);
                    funcionario.setPolo(polo);

                    dadosPorPolo.computeIfAbsent(polo, k -> new ArrayList<>()).add(funcionario);
                    funcionariosPorCodigo.put(codigo, funcionario);

                    System.out.printf("Funcionario Processado: | %-10s | %-20s | %-30s | %-15s |\n", codigo, drt, nome, polo);
                } else {
                    System.err.println("Linha inválida ou com colunas insuficientes: " + valor);
                }
            }
        }
    }

    private static void processarGeral(Map<String, String> geralData, Map<String, Funcionario> funcionariosPorCodigo) {
        if (geralData == null) return;

        System.out.println("=================================================================");
        System.out.println("            PROCESSAMENTO DE DADOS GERAIS");
        System.out.println("=================================================================");

        for (Map.Entry<String, String> entry : geralData.entrySet()) {
            String linha = entry.getKey();
            String valor = entry.getValue();

            if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
                String[] valores = valor.split(", ");
                try {
                    if (valores.length > 4) {
                        String codigo = valores[1].split("=")[1];
                        double recebimento = parseDouble(valores[10]);
                        double rendimento = parsePercentage(valores[14]);
                        double gratificacaoPercentual = parsePercentage(valores[14]);
                        double gratificacaoValor = recebimento * gratificacaoPercentual;

                        Funcionario funcionario = funcionariosPorCodigo.get(codigo);
                        if (funcionario != null) {
                            funcionario.setRecebimento(recebimento);
                            funcionario.setRendimento(rendimento);
                            funcionario.setGratificacaoPercentual(gratificacaoPercentual);
                            funcionario.setGratificacaoValor(gratificacaoValor);

                            System.out.printf("| %-10s | %-15s | %-15s | %-20s | %-15s |\n", 
                                    funcionario.getCodigo(), 
                                    recebimento, 
                                    rendimento, 
                                    gratificacaoPercentual, 
                                    gratificacaoValor);
                        }
                    } else {
                        System.err.println("Erro ao processar linha de Geral: " + linha + " - Menos de 5 colunas encontradas.");
                    }
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    System.err.println("Erro ao processar linha de Geral: " + linha + " - " + e.getMessage());
                }
            }
        }
    }

    private static double parseDouble(String valor) {
        return Double.parseDouble(valor.split("=")[1].replaceAll("[^\\d.]", ""));
    }

    private static double parsePercentage(String valor) {
        return Double.parseDouble(valor.split("=")[1].replace("%", "").trim()) / 100;
    }
}




/*comb 1 
package com.tmkfolha.app.controllers;

import java.text.Normalizer;
import java.util.*;

public class ProcessadorDados {

    public static Map<String, List<Funcionario>> processarDados(Map<String, Map<String, String>> fileData) {
        Map<String, List<Funcionario>> dadosPorPolo = new HashMap<>();
        Map<String, Funcionario> funcionariosPorCodigo = new HashMap<>();
        //System.out.println("L10 " + fileData);

        // Processa o arquivo Nomes.xls
        processarNomes(fileData.get("Nomes.xls"), dadosPorPolo, funcionariosPorCodigo);

        // Processa o arquivo GERAL SITUAÇÃO MENS.xls
        processarGeral(fileData.get("GERAL SITUAÇÃO MENS.xls"), funcionariosPorCodigo);

        System.out.println("Linha 33 " + fileData);
        System.out.println("Linha 34 " + funcionariosPorCodigo);

        return dadosPorPolo;
    }

    private static void processarNomes(Map<String, String> nomesData, 
                                   Map<String, List<Funcionario>> dadosPorPolo,
                                   Map<String, Funcionario> funcionariosPorCodigo) {
        if (nomesData == null) return;
        System.out.println("Linha 29 - geralData " + dadosPorPolo);
        System.out.println("Linha 30 -  geralData " + funcionariosPorCodigo);

        // Cabeçalho para melhorar a visualização no log
        System.out.println("=================================================================");
        System.out.println("           PROCESSAMENTO DE DADOS DE NOMES");
        System.out.println("=================================================================");

        for (Map.Entry<String, String> entry : nomesData.entrySet()) {
            String linha = entry.getKey();
            String valor = entry.getValue();
            System.out.println("\n-----------------------------------------------------------------");
            System.out.println("Processando Linha: " + linha);
            System.out.println("Conteúdo da Linha: " + valor);  

            if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
                String[] valores = valor.split(", ");

                // Conta as colunas válidas (não vazias e não nulas)
                int colunasValidas = 0;
                for (String v : valores) {
                    if (v != null && !v.trim().isEmpty()) { // Desconsidera valores nulos ou vazios
                        colunasValidas++;
                    }
                }

                // Debug: Exibe a quantidade de colunas válidas
                System.out.println("Número de colunas válidas: " + colunasValidas);
                System.out.println("-----------------------------------------------------------------");

                // Caso haja pelo menos uma coluna válida, processamos
                if (colunasValidas > 0) {
                    try {
                        String codigo = "";
                        String drt = "";
                        String nome = "";
                        String polo = "";

                        // Verifica quantas colunas válidas existem e preenche as variáveis
                        if (valores.length > 0 && valores[1] != null && !valores[0].trim().isEmpty()) {
                            codigo = valores[1].split("=")[1].trim();
                        }
                        if (valores.length > 1 && valores[2] != null && !valores[1].trim().isEmpty()) {
                            drt = valores[2].split("=")[1].trim();
                        }
                        if (valores.length > 2 && valores[3] != null && !valores[2].trim().isEmpty()) {
                            nome = valores[3].split("=")[1].trim();
                        }
                        if (valores.length > 3 && valores[4] != null && !valores[3].trim().isEmpty()) {
                            polo = valores[4].split("=")[1].trim();
                        }

                        // Normaliza o nome, removendo acentos e caracteres especiais
                        nome = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

                        Funcionario funcionario = new Funcionario();
                        funcionario.setCodigo(codigo);
                        System.out.println("Linha 81 " + codigo);
                        funcionario.setDrt(drt);
                        System.out.println("Linha 83 " + drt);
                        funcionario.setNome(nome);
                        System.out.println("Linha 85 " + nome);
                        System.out.println("Linha 86 " + polo);

                        // Armazenamento por polo
                        if (!polo.isEmpty()) {
                            dadosPorPolo.computeIfAbsent(polo, k -> new ArrayList<>()).add(funcionario);
                        }
                        funcionariosPorCodigo.put(codigo, funcionario);

                        // Log de sucesso
                        System.out.println("Funcionario Processado: ");
                        System.out.printf("| %-10s | %-20s | %-30s | %-15s |\n", codigo, drt, nome, polo);

                    } catch (Exception e) {
                        System.err.println("Erro ao processar linha de Nomes: " + linha + " - " + e.getMessage());
                    }
                } else {
                    // Log de erro para linhas inválidas
                    System.err.println("Erro ao processar linha de Nomes: Nenhuma coluna válida encontrada.");
                    System.out.println("-----------------------------------------------------------------");
                }
            }
        }
        System.out.println("=================================================================");
        System.out.println("Processamento concluído.");
        System.out.println("=================================================================");
    } */

   /*  private static void processarNomes(Map<String, String> nomesData, 
                                   Map<String, List<Funcionario>> dadosPorPolo,
                                   Map<String, Funcionario> funcionariosPorCodigo) {
    if (nomesData == null) return;

    for (Map.Entry<String, String> entry : nomesData.entrySet()) {
        String linha = entry.getKey();
        String valor = entry.getValue();
        System.out.println("L28 " + linha + " - " + valor);  

        if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
            String[] valores = valor.split(", ");

            // Conta as colunas válidas (não vazias e não nulas)
            int colunasValidas = 0;
            for (String v : valores) {
                if (v != null && !v.trim().isEmpty()) { // Desconsidera valores nulos ou vazios
                    colunasValidas++;
                }
            }

            // Debug: Exibe a quantidade de colunas válidas
            System.out.println("Número de colunas válidas: " + colunasValidas);

            // Caso haja pelo menos uma coluna válida, processamos
            if (colunasValidas > 0) {
                try {
                    String codigo = "";
                    String drt = "";
                    String nome = "";
                    String polo = "";

                    // Verifica quantas colunas válidas existem e preenche as variáveis
                    if (valores.length > 0 && valores[0] != null && !valores[0].trim().isEmpty()) {
                        codigo = valores[0].split("=")[1].trim();
                    }
                    if (valores.length > 1 && valores[1] != null && !valores[1].trim().isEmpty()) {
                        drt = valores[1].split("=")[1].trim();
                    }
                    if (valores.length > 2 && valores[2] != null && !valores[2].trim().isEmpty()) {
                        nome = valores[2].split("=")[1].trim();
                    }
                    if (valores.length > 3 && valores[3] != null && !valores[3].trim().isEmpty()) {
                        polo = valores[3].split("=")[1].trim();
                    }

                    // Normaliza o nome, removendo acentos e caracteres especiais
                    nome = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

                    Funcionario funcionario = new Funcionario();
                    funcionario.setCodigo(codigo);
                    funcionario.setDrt(drt);
                    funcionario.setNome(nome);

                    // Armazena o funcionário por polo
                    if (!polo.isEmpty()) {
                        dadosPorPolo.computeIfAbsent(polo, k -> new ArrayList<>()).add(funcionario);
                    }
                    funcionariosPorCodigo.put(codigo, funcionario);

                } catch (Exception e) {
                    System.err.println("Erro ao processar linha de Nomes: " + linha + " - " + e.getMessage());
                }
            } else {
                System.err.println("Erro ao processar linha de Nomes: Nenhuma coluna válida encontrada.");
            }
        }
    }
} */

 
    /*  comb 1
    private static void processarGeral(Map<String, String> geralData, Map<String, Funcionario> funcionariosPorCodigo) {
        if (geralData == null) return;
        System.out.println("Linha 189 - geralData " + geralData);
        System.out.println("Linha 200 -  geralData " + funcionariosPorCodigo);

        // Cabeçalho para melhorar a visualização no log
        System.out.println("=================================================================");
        System.out.println("            PROCESSAMENTO DE DADOS GERAIS");
        System.out.println("=================================================================");

        System.out.println("\n\n\n "+ "Linha 188 geralData " + geralData +"\n\n");
        System.out.println("\n\n\n "+ "Linha 189 geralData " + funcionariosPorCodigo +"\n\n");


        for (Map.Entry<String, String> entry : geralData.entrySet()) {
            String linha = entry.getKey();
            System.out.println("\n-----------------------------------------------------------------");
            System.out.println("L195 Geral "+entry.getKey() +" Processando Linha: " + linha);
            //System.out.println("Conteúdo da Linha: " + entry.getValue());
            
            if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
                String[] valores = entry.getValue().split(", ");
                System.out.println("Linha " + linha + " - Valores: " + Arrays.toString(valores));
                // Conta as colunas válidas
            int colunasValidas = 0;
            for (String v : valores) {
                if (v != null && !v.trim().isEmpty()) { // Desconsidera valores nulos ou vazios
                    colunasValidas++;
                }
            }

            // Debug: Exibe a quantidade de colunas válidas
            System.out.println("Número de colunas válidas: " + colunasValidas);
            System.out.println("-----------------------------------------------------------------");


                try {
                    if (valores.length > 4) {
                        String codigo = valores[1].split("=")[1];
                        System.out.println("Linha 217 " + codigo);
                        double recebimento = parseDouble(valores[10]);
                        double rendimento = parsePercentage(valores[14]);
                        double gratificacaoPercentual = parsePercentage(valores[14]);
                        double gratificacaoValor = recebimento * gratificacaoPercentual;
                    
                        Funcionario funcionario = funcionariosPorCodigo.get(codigo);
                        if (funcionario != null) {
                            funcionario.setRecebimento(recebimento);
                            funcionario.setRendimento(rendimento);
                            funcionario.setGratificacaoPercentual(gratificacaoPercentual);
                            funcionario.setGratificacaoValor(gratificacaoValor);
                             // Log de sucesso - Funcionario processado
                            System.out.println("Funcionario Processado: ");
                            System.out.printf("| %-10s | %-15s | %-15s | %-20s | %-15s |\n", 
                                            funcionario.getCodigo(), 
                                            recebimento, 
                                            rendimento, 
                                            gratificacaoPercentual, 
                                            gratificacaoValor);
                        }
                    } else {
                        System.err.println("Erro ao processar linha de Geral: " + linha + " - Menos de 5 colunas encontradas.");
                        System.out.println("-----------------------------------------------------------------");
                    }
                   
                } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                    System.err.println("Erro ao processar linha de Geral: " + linha + " - " + e.getMessage());
                }
            }
        }
        System.out.println("=================================================================");
        System.out.println("Processamento concluído.");
        System.out.println("=================================================================");
    }

    private static double parseDouble(String valor) {
        return Double.parseDouble(valor.split("=")[1].replace("R$", "").replace(".", "").replace(",", ".").trim());
    }

    private static double parsePercentage(String valor) {
        return Double.parseDouble(valor.split("=")[1].replace("%", "").trim()) / 100;
    }
}

 */


/* import java.util.*;

public class ProcessadorDados {
    public static Map<String, List<Funcionario>> processarDados(Map<String, Map<String, String>> fileData) {
        Map<String, List<Funcionario>> dadosPorPolo = new HashMap<>();

        // Processar Nomes.xls
        Map<String, String> nomesData = fileData.get("Nomes.xls");
        if (nomesData != null) {
            for (Map.Entry<String, String> entry : nomesData.entrySet()) {
                String linha = entry.getKey();
                if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
                    String[] valores = entry.getValue().split(", ");
                    String codigo = valores[1].split("=")[1];
                    String drt = valores[2].split("=")[1];
                    String nome = valores[3].split("=")[1];
                    String polo = valores[4].split("=")[1];

                    Funcionario funcionario = new Funcionario();
                    funcionario.setCodigo(codigo);
                    funcionario.setDrt(drt);
                    funcionario.setNome(nome);

                    dadosPorPolo.computeIfAbsent(polo, k -> new ArrayList<>()).add(funcionario);
                }
            }
        }

        // Processar GERAL SITUAÇÃO MENS.xls
        Map<String, String> geralData = fileData.get("GERAL SITUAÇÃO MENS.xls");
        if (geralData != null) {
            for (Map.Entry<String, String> entry : geralData.entrySet()) {
                String linha = entry.getKey();
                if (linha.startsWith("Linha ") && !linha.contains("Column0")) { // Ignorar cabeçalho
                    String[] valores = entry.getValue().split(", ");
                    String codigo = valores[4].split("=")[1];
                    double recebimento = Double.parseDouble(valores[5].split("=")[1].replace("R$", "").replace(".", "").replace(",", "."));
                    double rendimento = Double.parseDouble(valores[14].split("=")[1].replace("%", "").trim()) / 100;
                    double gratificacaoPercentual = Double.parseDouble(valores[15].split("=")[1].replace("%", "").trim()) / 100;
                    double gratificacaoValor = recebimento * gratificacaoPercentual;

                    // Atualizar funcionário correspondente
                    for (List<Funcionario> funcionarios : dadosPorPolo.values()) {
                        for (Funcionario funcionario : funcionarios) {
                            if (funcionario.getCodigo().equals(codigo)) {
                                funcionario.setRecebimento(recebimento);
                                funcionario.setRendimento(rendimento);
                                funcionario.setGratificacaoPercentual(gratificacaoPercentual);
                                funcionario.setGratificacaoValor(gratificacaoValor);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return dadosPorPolo;
    }
} */

/* import java.util.*;

public class ProcessadorDados {
    public static Map<String, List<Funcionario>> processarDados(Map<String, Map<String, String>> fileData) {
        Map<String, List<Funcionario>> dadosPorPolo = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : fileData.entrySet()) {
            String nomeArquivo = entry.getKey();
            Map<String, String> dadosLinha = entry.getValue();

            // Extrair informações da linha
            String codigo = dadosLinha.get("CÓD.");
            String drt = dadosLinha.get("DRT");
            String nome = dadosLinha.get("NOME");
            String polo = dadosLinha.get("POLO");
            double recebimento = Double.parseDouble(dadosLinha.get("RECEBIMENTO (R$)").replace("R$", "").replace(".", "").replace(",", "."));
            double rendimento = Double.parseDouble(dadosLinha.get("RENDIMENTO (%)").replace("%", "")) / 100;
            double gratificacaoPercentual = Double.parseDouble(dadosLinha.get("GRATIFICAÇÃO (%)").replace("%", "")) / 100;
            double gratificacaoValor = Double.parseDouble(dadosLinha.get("GRATIFICAÇÃO (R$)").replace("R$", "").replace(".", "").replace(",", "."));

            // Criar objeto Funcionario
            Funcionario funcionario = new Funcionario();
            funcionario.setCodigo(codigo);
            funcionario.setDrt(drt);
            funcionario.setNome(nome);
            funcionario.setRecebimento(recebimento);
            funcionario.setRendimento(rendimento);
            funcionario.setGratificacaoPercentual(gratificacaoPercentual);
            funcionario.setGratificacaoValor(gratificacaoValor);

            // Adicionar ao mapa de dados por polo
            dadosPorPolo.computeIfAbsent(polo, k -> new ArrayList<>()).add(funcionario);
        }

        return dadosPorPolo;
    }
}
 */