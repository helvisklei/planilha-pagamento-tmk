package com.tmkfolha.app.controllers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class GeralSituacaoMensal extends XlsData {
    private String setor;
    private Map<String, String> dadosMensageiro;
    private static final Logger logger = LogManager.getLogger(GeralSituacaoMensal.class);
    
    // Construtor com setor padrão
    public GeralSituacaoMensal(Map<String, String> rawData) {
        this(rawData, "SETOR DESCONHECIDO"); // Define um valor padrão
    }
    
    // Construtor com setor específico
    public GeralSituacaoMensal(Map<String, String> rawData, String setor) {
        super(processarRawData(rawData)); // Usamos um mapa vazio pois os dados serão reorganizados
        this.setor = setor;
        this.dadosMensageiro = new LinkedHashMap<>(rawData);
        processarDados(rawData);
    }

    private static Map<String, String> processarRawData(Map<String, String> rawData) {
        Map<String, String> processed = new LinkedHashMap<>();
        if (rawData != null) {
            rawData.forEach((key, value) -> {
                // Remove espaços em branco extras
                String cleanedValue = value != null ? value.trim() : "";
                processed.put(key, cleanedValue);
            });
        }
        return processed;
    }

    private void processarDados(Map<String, String> rawData) {

         // Verifica se os dados estão no formato esperado
         if (rawData == null || rawData.isEmpty()) {
            logger.warn("Dados brutos vazios para mensageiro");
            return;
        }

        // Mapeamento correto das colunas
        for (Map.Entry<String, String> entry : rawData.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            // Transforma "LinhaX - ColumnY" em "ColumnY"
            if (key.startsWith("Linha")) {
                String newKey = key.split(" - ")[1];
                dadosMensageiro.put(newKey, value);
            } else {
                dadosMensageiro.put(key, value);
            }
        }
        // Extrai e organiza os dados do mensageiro
        dadosMensageiro.put("Column0", rawData.get("Linha6 - Column0")); // Código
        dadosMensageiro.put("Column1", rawData.get("Linha6 - Column1")); // Nome
        
        // Adiciona os dados das colunas da linha 7
        for (int i = 2; i <= 15; i++) {
            if (i != 8 && i != 11) { // Pula colunas 8 e 11 se não existirem
                String chave = "Linha7 - Column" + i;
                if (rawData.containsKey(chave)) {
                    dadosMensageiro.put("Column" + i, rawData.get(chave));
                }
            }
        }
    }
    
    @Override
    //public String getCodigo() {
    //    return dadosMensageiro.getOrDefault("Column0",""); // Código do mensageiro
    //
    public String getCodigo() {
        String codigo = dadosMensageiro.get("Column0");
        // Remove qualquer caractere não numérico
        return codigo != null ? codigo.replaceAll("[^0-9]", "") : "";
    } // código
    
    @Override
    public String getNome() {
        return dadosMensageiro.getOrDefault("Column1",""); // Nome do mensageiro
    } // nome
    
    // Métodos para acessar os valores das colunas
    public Integer getSaldoAnteriorQtd() { return parseInteger(dadosMensageiro.get("Column2"));  }
    
    public BigDecimal getSaldoAnteriorValor() { return parseMonetaryValue(dadosMensageiro.get("Column3")); }
    
    public Integer getEntradasPeriodoQtd() { return parseInteger(dadosMensageiro.get("Column4")); }
    
    public BigDecimal getEntradasPeriodoValor() { return parseMonetaryValue(dadosMensageiro.get("Column5")); }
    
    public Integer getRecebidoMsgQtd() { return parseInteger(dadosMensageiro.get("Column6"));  }
    
    public BigDecimal getRecebidoMsgValor() {  return parseMonetaryValue(dadosMensageiro.get("Linha7 - Column7"));  }
    
    public Integer getDevolvidoMsgQtd() { return parseInteger(dadosMensageiro.get("Column9")); }
    
    public BigDecimal getDevolvidoMsgValor() { return parseMonetaryValue(dadosMensageiro.get("Column10")); }

     public BigDecimal getSaldoDia() {
        return parseMonetaryValue(dadosMensageiro.get("Column12")); 
    } 
    
    public BigDecimal getPercentualRendimentoQtd() { return parsePercentualValue(dadosMensageiro.get("Column14")); }
    
    public BigDecimal getPercentualRendimentoValor() { return parsePercentualValue(dadosMensageiro.get("Column15")); }
    
    /* // Método para calcular a gratificação conforme as regras específicas
    public BigDecimal calcularGratificacao() {
        BigDecimal percentual = getPercentualRendimentoValor();
        
        if (setor.equals("SETOR 4")) { // MATRIZ
            if (percentual.compareTo(new BigDecimal("91.00")) >= 0) {
                return new BigDecimal("6.0");
            } else if (percentual.compareTo(new BigDecimal("87.50")) >= 0) {
                return new BigDecimal("6.0");
            } else if (percentual.compareTo(new BigDecimal("85.00")) >= 0) {
                return new BigDecimal("6.0");
            } else if (percentual.compareTo(new BigDecimal("80.00")) >= 0) {
                return new BigDecimal("6.0");
            } else if (percentual.compareTo(new BigDecimal("75.00")) >= 0) {
                return new BigDecimal("6.0");
            } else if (percentual.compareTo(new BigDecimal("70.00")) >= 0) {
                return new BigDecimal("6.0");
            } else {
                return new BigDecimal("6.0");
            }
        } else { // Demais polos (ex: CARUARU - SETOR 5)
            if (percentual.compareTo(new BigDecimal("91.00")) >= 0) {
                return new BigDecimal("6.0");
            } else if (percentual.compareTo(new BigDecimal("87.50")) >= 0) {
                return new BigDecimal("5.5");
            } else if (percentual.compareTo(new BigDecimal("85.00")) >= 0) {
                return new BigDecimal("5.0");
            } else if (percentual.compareTo(new BigDecimal("80.00")) >= 0) {
                return new BigDecimal("4.5");
            } else if (percentual.compareTo(new BigDecimal("75.00")) >= 0) {
                return new BigDecimal("4.0");
            } else if (percentual.compareTo(new BigDecimal("70.00")) >= 0) {
                return new BigDecimal("3.5");
            } else {
                return new BigDecimal("3.5");
            }
        }
    } */
    public static BigDecimal calcularGratificacaoMensageiro(BigDecimal valorRecebido, BigDecimal rendimento) {
        BigDecimal percentual;
        
        // Lógica única para todos os mensageiros
        if (rendimento.compareTo(BigDecimal.valueOf(0.91)) >= 0) {
            percentual = BigDecimal.valueOf(0.0600); // 6%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.88)) >= 0) {
            percentual = BigDecimal.valueOf(0.0550); // 5.5%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.85)) >= 0) {
            percentual = BigDecimal.valueOf(0.0500); // 5%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.80)) >= 0) {
            percentual = BigDecimal.valueOf(0.0450); // 4.5%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.75)) >= 0) {
            percentual = BigDecimal.valueOf(0.0400); // 4%
        } else {
            percentual = BigDecimal.valueOf(0.0350); // 3.5% (para rendimento < 0.75)
        }
        
        // Calcula o valor da comissão: valorRecebido * percentual
        return valorRecebido.multiply(percentual);
    }
    
   /*  // Método para calcular o valor total da gratificação
    public BigDecimal calcularValorGratificacao() {
        return getRecebidoMsgValor().multiply(calcularGratificacao().divide(new BigDecimal("100")));
    }
    
    @Override
    public String toString() {
        return String.format("Mensageiro[%s - %s - Setor: %s - Rendimento: %.2f%% - Gratificação: %.2f%%]",
                getCodigo(), getNome(), setor, getPercentualRendimentoValor(), calcularGratificacao());
    } */

    public Map<String, String> processarColumn0(Map<String, String> dadosBrutos) {
        if (!dadosBrutos.containsKey("Column0")) {
            return dadosBrutos;
        }

        String valorOriginal = dadosBrutos.get("Column0").trim();
        Map<String, String> dadosProcessados = new HashMap<>(dadosBrutos);
        
        // Padrão 1: Código seguido de nome e tipo (ex: "37 CARLOS EDUARDO FABRICIO M")
        Pattern padraoCompleto = Pattern.compile("^(\\d+)\\s+([A-Za-zÀ-ú\\s]+?)\\s+(REC|BOL|DEP|OUT|CHE|PIX|[A-Z])$");
        // Padrão 2: Apenas código numérico (ex: "37")
        Pattern padraoApenasCodigo = Pattern.compile("^(\\d+)$");
        
        Matcher matcherCompleto = padraoCompleto.matcher(valorOriginal);
        Matcher matcherApenasCodigo = padraoApenasCodigo.matcher(valorOriginal);
        
        if (matcherCompleto.find()) {
            // Caso 1: Código + Nome + Tipo
            dadosProcessados.put("Column0", matcherCompleto.group(1).trim());
            dadosProcessados.put("NomeOPE", matcherCompleto.group(2).trim());
            dadosProcessados.put("TipoOpe", matcherCompleto.group(3).trim());
        } 
        else if (matcherApenasCodigo.find()) {
            // Caso 2: Apenas código numérico
            dadosProcessados.put("Column0", matcherApenasCodigo.group(1).trim());
            // Mantém os outros campos como estão ou vazios
            dadosProcessados.putIfAbsent("NomeOPE", "");
            dadosProcessados.putIfAbsent("TipoOpe", "");
        }
        else {
            // Caso 3: Formato não reconhecido - tenta extrair o código de qualquer forma
            String[] partes = valorOriginal.split("\\s+");
            if (partes.length > 0 && partes[0].matches("\\d+")) {
                dadosProcessados.put("Column0", partes[0]);
                if (partes.length > 1) {
                    dadosProcessados.put("NomeOPE", valorOriginal.substring(partes[0].length()).trim());
                }
            } else {
                // Se não conseguir extrair, mantém o valor original
                dadosProcessados.put("Column0", valorOriginal);
                logger.warn("Formato não reconhecido para Column0: '{}'", valorOriginal);
            }
        }
        
        return dadosProcessados;
    }

     // Método para gerar a estrutura JSON correta
     public Map<String, Object> toJsonMap() {
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        jsonMap.put("setor", this.setor);
        
        Map<String, String> dadosFormatados = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : dadosMensageiro.entrySet()) {
            dadosFormatados.put(entry.getKey(), entry.getValue());
        }
        
        jsonMap.put("GERAL SITUAÇÃO MENS", dadosFormatados);
        return jsonMap;
    }

    public void visualizarTodosMensageirosComPaginacao(List<XlsData> objectsList) {
        final int PAGE_SIZE = 5; // Itens por página
        Scanner scanner = new Scanner(System.in);
        int totalPages = (int) Math.ceil((double) objectsList.size() / PAGE_SIZE);
        
        for (int page = 0; page < totalPages; page++) {
            System.out.println("\n=== Página " + (page + 1) + " de " + totalPages + " ===");
            
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, objectsList.size());
            
            for (int i = start; i < end; i++) {
                XlsData data = objectsList.get(i);
                if (data instanceof GeralSituacaoMensal) {
                    GeralSituacaoMensal mensageiro = (GeralSituacaoMensal) data;
                    System.out.println("\n" + mensageiro.toString());
                    System.out.println("----------------------------------------");
                    mensageiro.toJsonMap().forEach((key, value) -> {
                        if (value instanceof Map) {
                            System.out.println(key + ":");
                            ((Map<?, ?>) value).forEach((k, v) -> 
                                System.out.println("  " + k + ": " + v));
                        } else {
                            System.out.println(key + ": " + value);
                        }
                    });
                }
            }
            
            if (page < totalPages - 1) {
                System.out.println("\nPressione Enter para continuar...");
                scanner.nextLine();
            }
        }
        scanner.close();
    }

    public void exportarParaArquivoTemporario(List<XlsData> objectsList) throws IOException {
        Path tempFile = Files.createTempFile("mensageiros_", ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile)) {
            for (XlsData data : objectsList) {
                if (data instanceof GeralSituacaoMensal) {
                    GeralSituacaoMensal mensageiro = (GeralSituacaoMensal) data;
                    writer.write("=== Dados do Mensageiro ===\n");
                    writer.write("Código: " + mensageiro.getCodigo() + "\n");
                    writer.write("Nome: " + mensageiro.getNome() + "\n");
                    writer.write("Setor: " + mensageiro.toJsonMap().get("setor") + "\n");
                    
                    Map<?, ?> dados = (Map<?, ?>) mensageiro.toJsonMap().get("GERAL SITUAÇÃO MENS");
                    dados.forEach((k, v) -> {
                        try {
                            writer.write(k + ": " + v + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                    writer.write("\n");
                }
            }
        }
        System.out.println("Dados exportados para: " + tempFile.toAbsolutePath());
        System.out.println("Use o comando: less " + tempFile.toAbsolutePath() + " para visualizar com paginação");
    }

     // Métodos para converter valores com tratamento de erros
     @Override
     protected Integer parseInteger(String value) {
         if (value == null || value.trim().isEmpty()) return 0;
         try {
             return Integer.parseInt(value.trim().replaceAll("\\s+", ""));
         } catch (NumberFormatException e) {
             logger.warn("Erro ao converter valor inteiro: '{}'", value);
             return 0;
         }
    }

    protected BigDecimal parsePercentualValue(String value) {
        if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String cleaned = value.replace("%", "")
                                  .replace(".", "")
                                  .replace(",", ".")
                                  .trim();
            BigDecimal percentual = new BigDecimal(cleaned);
            return percentual.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter valor percentual: '{}'", value);
            return BigDecimal.ZERO;
        }
    }

    @Override
    protected BigDecimal parseMonetaryValue(String value) {
        if (value == null || value.trim().isEmpty()) return BigDecimal.ZERO;
        try {
            String cleaned = value.replace("R$", "")
                                .replace(".", "")
                                .replace(",", ".")
                                .trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter valor monetário: '{}'", value);
            return BigDecimal.ZERO;
        }
    }

     public static Map<String, Map<String, String>> agruparDadosMensageiros(Map<String, Map<String, String>> dadosBrutos) {
        Map<String, Map<String, String>> dadosAgrupados = new LinkedHashMap<>();
        
        List<Integer> linhas = dadosBrutos.keySet().stream()
            .filter(k -> k.startsWith("Linha "))
            .map(k -> Integer.parseInt(k.replace("Linha ", "")))
            .sorted()
            .collect(Collectors.toList());

        for (int i = 0; i < linhas.size(); i++) {
            int linhaAtual = linhas.get(i);
            
            if (linhaAtual % 2 == 0) {
                int linhaDados = linhaAtual + 1;
                
                if (linhas.contains(linhaDados)) {
                    Map<String, String> linhaCodigo = dadosBrutos.get("Linha " + linhaAtual);
                    Map<String, String> linhaDetalhes = dadosBrutos.get("Linha " + linhaDados);
                    
                    if (linhaCodigo != null && linhaDetalhes != null) {
                        String codigo = linhaCodigo.get("Column0");
                        String nome = linhaCodigo.get("Column1");
                        
                        if (codigo != null && nome != null && !codigo.isEmpty() && !nome.isEmpty()) {
                            Map<String, String> mensageiro = new LinkedHashMap<>();
                            mensageiro.put("Código", codigo.trim());
                            mensageiro.put("Nome", nome.trim());
                            
                            linhaDetalhes.forEach((key, value) -> {
                                if (value != null && !value.trim().isEmpty()) {
                                    mensageiro.put(key, value.trim());
                                }
                            });
                            
                            dadosAgrupados.put(codigo + " - " + nome, mensageiro);
                        }
                    }
                }
            }
        }
        logger.debug("L391 Dados agrupados de mensageiros: {}", dadosAgrupados);
        return dadosAgrupados;
    }

    public static Map<String, String> processarDadosMensageiro(Map<String, String> dadosBrutos) {
        Map<String, String> dadosProcessados = new LinkedHashMap<>();
        
        dadosBrutos.forEach((key, value) -> {
            String chaveCorrigida = key.replace("Código", "Código");//C├│digo
            dadosProcessados.put(chaveCorrigida, (value != null) ? value.trim() : "");
        });
        
        dadosProcessados.putIfAbsent("Setor", "");
        dadosProcessados.putIfAbsent("Rendimento", "0,00%");
        dadosProcessados.putIfAbsent("Gratificação", "3,50%");
        
        return dadosProcessados;
    }


}