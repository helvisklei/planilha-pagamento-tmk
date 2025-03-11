package com.tmkfolha.processor;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.tmkfolha.util.BarraProgresso;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Classe responsável por processar arquivos XLS e XLSX.
 * Lê os dados, processa e formata para saída em um novo Excel.
 */
public class XlsProcessor implements FileProcessor {

    private BarraProgresso barraProgresso;
    private List<Map<String, String>> extractedData = new ArrayList<>();
    private TreeMap<String, Map<String, String>> fileData = new TreeMap<>();
    private Map<String, List<String>> resultadosParaExcel = new HashMap<>();

    // Lista dos arquivos esperados no processamento
    private static final List<String> FILE_KEYS = List.of(
        "GERAL.XLS", "SETOR 2 SERRA.XLS", "SETOR 3 PETROLINA.XLS",
        "SETOR 4 MATRIZ.XLS", "SETOR 5 CARUARU.XLS", "SETOR 6 GARANHUNS.XLS", 
        "GERAL SITUAÇÃO MENS.xls","Nomes.xls","RECEBIDO POR OPERADOR.xls"
    );

    private static final String NOME_PROCURADO = "Total  (Recebido-Cheque pré+Cheque Custódia+Boletos ) :".trim();
    private static final String NOME_PROCURADO2 = "Situação dos Mensageiros, contagem por fichas ,Setor: 0";
    private static final String LISTA_NOME_NA_PLANILHA = "NOME".trim();


    public XlsProcessor(BarraProgresso barraProgresso) {
        this.barraProgresso = barraProgresso;
    }

    /**
     * Processa um arquivo XLS ou XLSX, extraindo e formatando os dados.
     */
    @Override
    public void processFile(String filePath) throws IOException {
        File file = new File(filePath);

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = filePath.endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            processWorkbook(workbook);
            fileData.put(file.getName(), ordenarMapaInterno(mergeData(extractedData)));
            processResults();

            ExcelWriter excelWriter = new ExcelWriter();
            excelWriter.escreverDados(convertToExcelFormat());
        }
    }

    /**
     * Percorre as planilhas do workbook e extrai os dados necessários.
     */
    private void processWorkbook(Workbook workbook) {
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                Map<String, String> rowData = processRow(row);
                if (!rowData.isEmpty() && rowData.values().stream().anyMatch(val -> !val.isBlank())) {
                    extractedData.add(rowData);
                }
            }
        }
        extractedData.sort(Comparator.comparing(row -> extrairNumeroDaColuna(row.getOrDefault("Column0", ""))));
    }

    /**
     * Processa uma linha da planilha e retorna um mapa de valores.
     */
    private Map<String, String> processRow(Row row) {
        Map<String, String> rowData = new HashMap<>();
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.BLANK) continue;
            rowData.put("Column" + cell.getColumnIndex(), processCell(cell));
        }
        return rowData;
    }

    /**
     * Processa uma célula, extraindo seu conteúdo de acordo com o tipo.
     */
    private String processCell(Cell cell) {
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> DateUtil.isCellDateFormatted(cell) ?
                        new SimpleDateFormat("dd-MM-yyyy").format(cell.getDateCellValue()) :
                        formatNumericValue(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> processFormulaCell(cell);
                default -> "";
            };
        } catch (Exception e) {
            System.err.println("Erro ao processar célula: " + cell);
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Processa células do tipo fórmula, obtendo o resultado correto.
     */
    private String processFormulaCell(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> formatNumericValue(cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue();
            default -> "";
        };
    }

    /**
     * Formata valores numéricos, removendo casas decimais se não forem necessárias.
     */
    private String formatNumericValue(double value) {
        return (value == (long) value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    /**
     * Processa os resultados finais e os formata para a saída.
     */
    private void processResults() {
        for (String key : FILE_KEYS) {
            if (fileData.containsKey(key)) {
                List<String> nomes = buscarValoresPorNomeRetonarLista(fileData, key, LISTA_NOME_NA_PLANILHA);
                List<String> demonstrativoCaixa = buscarValoresPorNomeRetonarLista(fileData, key, NOME_PROCURADO);
                
                if (!nomes.isEmpty()) {
                    ResultadoPolo resultadoNomes = ResultadoPolo.processarValores(key, nomes);
                    resultadosParaExcel.put(key, List.of(
                        "CÓD -> " + resultadoNomes.getClass(),
                        "DRT -> " + resultadoNomes.getValor()
                    ));
                }
            }
        }
    }

    /**
     * Converte os resultados para um formato adequado para escrita em Excel.
     */
    private List<Map<String, Map<String, String>>> convertToExcelFormat() {
        return resultadosParaExcel.entrySet().stream()
            .map(entry -> Map.of(entry.getKey(), entry.getValue().stream()
                .map(line -> line.split(" -> "))
                .filter(parts -> parts.length == 2)
                .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()))))
            .collect(Collectors.toList());
    }

    /**
     * Formata valores numéricos para exibição como moeda.
     */
    private String formatCurrency(double value) {
        return new DecimalFormat("R$ #,##0.00").format(value);
    }

    @Override
    public Map<String, Map<String, String>> getData() {       
         return fileData;
    }

    public List<Map<String, String>> getExtractedData() {   
        return extractedData;    }

    public void clearExtractedData() {
        extractedData.clear();
    }
    
    private Map<String, String> formatarDadosDoMapa(Map<String, String> dados, String linhaReferencia) {
        Map<String, String> dadosFormatados = new HashMap<>();
    
        dadosFormatados.put("Caixa", dados.get(linhaReferencia + " - Column1"));
        dadosFormatados.put("QTD", dados.get(linhaReferencia + " - Column7"));
        dadosFormatados.put("Valor", dados.get(linhaReferencia + " - Column9"));
    
        return dadosFormatados;
    }

    private Map<String, String> mergeData(List<Map<String, String>> data) {
        Map<String, String> merged = new HashMap<>();
        int rowIndex = 1;
        for (Map<String, String> row : data) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                merged.put("Linha " + rowIndex + " - " + entry.getKey(), entry.getValue());
            }
            rowIndex++;
        }
        //System.out.println(" XlsProcessor L116 "+merged);
        return merged;
    }
    private TreeMap<String, String> ordenarMapaInterno(Map<String, String> mapa) {
        return new TreeMap<>(mapa);
    }
    // Função para localizar e exibir valores com base no nome procurado
    public static void buscarValoresPorNome(Map<String, Map<String, String>> fileData, String arquivo, String nomeProcurado) {
        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        boolean encontrou = false;
        nomeProcurado = normalizarTexto(nomeProcurado);

        // Percorrendo todas as chaves do mapa para encontrar correspondências
        for (Map.Entry<String, String> entry : dadosArquivo.entrySet()) {
            String chave = entry.getKey();
            String valor = normalizarTexto(entry.getValue().trim());

            // Verificando se a chave contém o nome procurado
            if (valor.equals(nomeProcurado)) {
                //System.out.println("Encontrado: " + chave + " -> " + valor);
                encontrou = true;

                // Extraindo o número da linha corretamente
                String[] partes =  chave.split(" - ");
                String linhaBase = partes.length > 1 ? partes[0] : ""; // Exemplo: "Linha 17"
                //System.out.println("Valores completos da " + linhaBase + ":");

                for (Map.Entry<String, String> linhaEntry : dadosArquivo.entrySet()) {
                    if (linhaEntry.getKey().startsWith(linhaBase)) {
                        //System.out.println("  " + linhaEntry.getKey() + " -> " + linhaEntry.getValue());
                    }
                }
                break; // Interrompe a busca após encontrar a primeira ocorrência
            }            
        }

        if (!encontrou) {
            System.out.println("Nenhum resultado encontrado para: " + nomeProcurado);
        }
    }
    public static List<String> buscarValoresPorNomeRetonarLista(Map<String, Map<String, String>> fileData, String arquivo, String nomeProcurado) {
        List<String> resultados = new ArrayList<>();
    
        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return resultados; // Retorna lista vazia caso o arquivo não seja encontrado
        }
    
        Map<String, String> dadosArquivo = fileData.get(arquivo);
        nomeProcurado = normalizarTexto(nomeProcurado);
    
        // Usando TreeMap para garantir ordenação sem sobrescrever valores repetidos
        TreeMap<String, List<String>> resultadosOrdenados = new TreeMap<>();
    
        for (Map.Entry<String, String> entry : dadosArquivo.entrySet()) {
            String chave = entry.getKey();
            String valor = normalizarTexto(entry.getValue().trim());
    
            if (valor.equals(nomeProcurado)) {
                //System.out.println("Encontrado: " + chave + " -> " + valor);
    
                // Extraindo a linha base (exemplo: "Linha 17")
                String[] partes = chave.split(" - ");
                String linhaBase = partes.length > 1 ? partes[0] : "";
    
                // Adiciona chave e valor a uma lista para evitar sobrescrita
                resultadosOrdenados.putIfAbsent(linhaBase, new ArrayList<>());
                resultadosOrdenados.get(linhaBase).add(chave + " -> " + valor);
    
                for (Map.Entry<String, String> linhaEntry : dadosArquivo.entrySet()) {
                    if (linhaEntry.getKey().startsWith(linhaBase)) {
                        resultadosOrdenados.get(linhaBase).add(linhaEntry.getKey() + " -> " + linhaEntry.getValue());
                    }
                }
            }
        }
    
        if (resultadosOrdenados.isEmpty()) {
            System.out.println("Nenhum resultado encontrado para: " + nomeProcurado);
        } else {
            for (List<String> lista : resultadosOrdenados.values()) {
                resultados.addAll(lista);
            }
        }
    
        return resultados; // Retorna os resultados sem sobrescrever
    }

    // Normaliza o texto removendo espaços extras e caracteres invisíveis
    public static String normalizarTexto(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("[\\s\u00A0\u200B]+", " ").trim().toLowerCase();
    }

    // Método para extrair o número da coluna

    private static long extrairNumeroDaColuna(String coluna) {
        String numeros = coluna.replaceAll("[^0-9]", "");
        return numeros.isEmpty() ? 0 : Long.parseLong(numeros);//coluna.replaceAll("[^0-9]", "").isEmpty() ? 0 : Integer.parseInt(coluna.replaceAll("[^0-9]", ""));
    }
   /*  private int extrairNumeroDaColuna(String columnKey) {
        try {
            // Extrair o número da chave da coluna, considerando que o nome da coluna é algo como "Column10"
            return Integer.parseInt(columnKey.replaceAll("\\D", "")); // Remove os caracteres não numéricos
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Caso não consiga extrair o número, coloca um valor alto
        }
    } */

    public void processar() {
        int totalFiles = 100;
        int processedFiles = 0;

        for (int i = 0; i < totalFiles; i++) {
            // Lógica de processamento...

            // Atualiza a barra de progresso
            barraProgresso.atualizarProgresso(processedFiles++, totalFiles);

             // Simulação de processamento (substitua por seu código real)
             try {
                Thread.sleep(500);  // Simula um pequeno delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}