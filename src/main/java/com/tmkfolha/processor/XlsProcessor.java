package com.tmkfolha.processor;

import java.io.*;
import java.security.Key;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.tmkfolha.app.controllers.Funcionario;
import com.tmkfolha.app.controllers.GeradorRelatorio;
import com.tmkfolha.app.controllers.ProcessadorDados;
import com.tmkfolha.util.BarraProgresso;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Classe responsável por processar arquivos XLS e XLSX.
 * Lê os dados, processa e formata para saída em um novo Excel.
 */
public class XlsProcessor implements FileProcessor {

    private BarraProgresso barraProgresso;
    private List<Map<String, String>> extractedData = new ArrayList<>();
    private Map<String, List<String>> serra= new HashMap<>();
    private Map<String, List<String>> petrolina= new HashMap<>();
    private Map<String, List<String>> matriz= new HashMap<>();
    private Map<String, List<String>> caruaru= new HashMap<>();
    private Map<String, List<String>> garanhuns= new HashMap<>();
    private TreeMap<String, Map<String, String>> fileData = new TreeMap<>();
    private Map<String, List<String>> resultadosParaExcel = new HashMap<>();
    private DataProcessor dataProcessor = new DataProcessor(); // Integração com DataProcessor

    // Lista dos arquivos esperados no processamento
    private static final List<String> FILE_KEYS = List.of(
        "GERAL.XLS", "SETOR 2 SERRA.XLS", "SETOR 3 PETROLINA.XLS",
        "SETOR 4 MATRIZ.XLS", "SETOR 5 CARUARU.XLS", "SETOR 6 GARANHUNS.XLS", 
        "GERAL SITUAÇÃO MENS.xls","Nomes.xls","RECEBIDO POR OPERADOR.xls","Tabela-Gratificacao.xls"
    );

    private static final String TOTAL_RECEBIDO_C_C_BOLETOS = "Total  (Recebido-Cheque pré+Cheque Custódia+Boletos ) :";
    private static final String GERAL_SITUACAO_MENSAGEIRO = "Situação dos Mensageiros, contagem por recibos,Setor: 0";
    private static final String GRATIFICACAO = "FUNÇÃO";
    private static final String LISTA_NOME_NA_PLANILHA = "NOME";

    //Polos
    private static final String POLO_SERRA_TALHADA = "SERRA TALHADA";
    private static final String POLO_PETROLINA = "PETROLINA";
    private static final String POLO_MATRIZ= "MATRIZ";
    private static final String POLO_CARUARU = "CARUARU";
    private static final String POLO_GARANHUNS = "GARANHUNS";

    // Administrativo
    private static final String adm = "ADMINISTRAÇÃO";

    //OPERAÇÃO
    private static final String ope = "OPERADORA";


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
        //System.out.println("******************************************************************************************"); 
        //System.out.println("EXTRACTED DATA: " + extractedData);
        //System.out.println("******************************************************************************************");
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
      // System.out.println("L89 " + rowData);
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
        Map<String, Map<String, Map<String, String>>> mapaConsolidado = new HashMap<>(); 
        Map<String, Map<String, String>> petrolina =  new HashMap<>();  
        Map<String,Map<String,String>> tabelaNomeSerra = new HashMap<>(); 
        Map<String,Map<String,String>> tabelaNomePetrolina = new HashMap<>();
        Map<String,Map<String,String>> tabelaNomeMatriz = new HashMap<>();  
        Map<String,Map<String,String>> tabelaNomeCaruaru = new HashMap<>();
        Map<String,Map<String,String>> tabelaNomeGaranhuns = new HashMap<>(); 
        Map<String, Map<String, String>> geralCituacaoMensageiro = new HashMap<>(); 

        Map<String, Map<String, String>>  tabelaGratificacaoSerra = new HashMap<>();
        Map<String, Map<String, String>>  tabelaGratificacaoPetrolina = new HashMap<>(); 
        Map<String, Map<String, String>>  tabelaGratificacaoMatriz = new HashMap<>();
        Map<String, Map<String, String>>  tabelaGratificacaoCaruaru = new HashMap<>();
        Map<String, Map<String, String>>  tabelaGratificacaoGaranhuns = new HashMap<>();
        
        List<String> demonstrativoCaixaSerra = new ArrayList<>();
        List<String> demonstrativoCaixaPetrolina = new ArrayList<>();
        List<String> demonstrativoCaixaMatriz = new ArrayList<>();
        List<String> demonstrativoCaixaCaruaru = new ArrayList<>();
        List<String> demonstrativoCaixaPetroGaranhuns = new ArrayList<>();
        List<String> demonstrativoCaixaGeral = new ArrayList<>();

        // Carregar os funcionários a partir do fileData
       /*  List<Funcionario> funcionarios = Funcionario.carregarFuncionarios(fileData);                   

        // Exibir os funcionários no log
        for (Funcionario func : funcionarios) {
            func.log();
        }
       
 */
        for (String key : FILE_KEYS) {
            if (fileData.containsKey(key)) {   // SETOR 3 PETROLINA  
              // System.out.println("******************************************************************************************"); 
               // System.out.println("FILEDATA: " + fileData);
               //System.out.println("******************************************************************************************"); 
               //Map<String, List<Funcionario>> dadosPorPolo = ProcessadorDados.processarDados(fileData);
               
              //  System.out.println("L174 fileData " + fileData);  
              if (key.equals("GERAL SITUAÇÃO MENS.xls")){ //"GERAL SITUAÇÃO MENS.xls"  GERAL_SITUACAO_MENSAGEIRO
                geralCituacaoMensageiro= JavaApp.buscarValoresPorNomeRetornarMapaDeMapa(fileData, key,GERAL_SITUACAO_MENSAGEIRO);
                    // Carregar os funcionários a partir do fileData
                    List<Funcionario> funcionariosMensageiros = Funcionario.carregarFuncionarios(geralCituacaoMensageiro); 
                      // Exibir os funcionários no log
                      for (Funcionario func : funcionariosMensageiros) {
                           func.log();
                           System.out.println(func);
                        }
               
                // Map<String, List<Funcionario>> dadosPorPolo = ProcessadorDados.processarDados(geralCituacaoMensageiro);
              
               // func.carregarFuncionarios(geralCituacaoMensageiro);
                //System.out.println("L180  "  + dadosPorPolo + "\n");
                GeradorRelatorio.gerarRelatorio(funcionariosMensageiros, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");
              }                
 
                if (key.equals("Nomes.xls")) { // SETOR 3 PETROLINA é igual a vazio não
                    // Trabalhamos a tabela nome
                   // List<Funcionario> funcionarios = Funcionario.carregarFuncionarios(fileData);

                    // Exibir os funcionários no log
                    /* for (Funcionario func : funcionarios) {
                        func.log();
                    } */
                    /* tabelaNomeSerra = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_SERRA_TALHADA);
                    //geralCituacaoMensageiro= JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, GERAL_SITUACAO_MENSAGEIRO);
                    Map<String, List<Funcionario>> dadosPorPolo = ProcessadorDados.processarDados(tabelaNomeSerra);
                    System.out.println("L180  "  + dadosPorPolo + "\n");
                    GeradorRelatorio.gerarRelatorio(dadosPorPolo, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx"); */

                    tabelaNomeSerra = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_SERRA_TALHADA);
                    // Carregar os funcionários a partir do fileData
                      List<Funcionario> funcionariosSerra = Funcionario.carregarFuncionarios(tabelaNomeSerra); 
                      // Exibir os funcionários no log
                      for (Funcionario func : funcionariosSerra) {
                           func.log();
                           System.out.println(func);
                        }
                        GeradorRelatorio.gerarRelatorio(funcionariosSerra, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");

                     
    
                   // exibirMapaFormatado(key, tabelaNomeSerra);
                    tabelaNomePetrolina = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_PETROLINA);                    
                    // Carregar os funcionários a partir do fileData
                    List<Funcionario> funcionariosPetrolina = Funcionario.carregarFuncionarios(tabelaNomePetrolina);
                    // Exibir os funcionários no log
                    for (Funcionario func : funcionariosPetrolina) {
                        func.log();
                        System.out.println(func);
                    }
                    GeradorRelatorio.gerarRelatorio(funcionariosPetrolina, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");

                     
                   // exibirMapaFormatado(key, tabelaNomePetrolina);              
                   //Map<String, List<Funcionario>> dadosPorPolo3 = ProcessadorDados.processarDados(tabelaNomePetrolina);
                   //System.out.println("L180  "  + dadosPorPolo3 + "\n");
                   //GeradorRelatorio.gerarRelatorio(dadosPorPolo3, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");
    
                    tabelaNomeMatriz= JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_MATRIZ);
                    // Carregar os funcionários a partir do fileData
                    List<Funcionario> funcionariosMatriz = Funcionario.carregarFuncionarios(tabelaNomeMatriz);
                    // Exibir os funcionários no log
                    for (Funcionario func : funcionariosMatriz) {
                        func.log();
                        System.out.println(func);
                    }
                    GeradorRelatorio.gerarRelatorio(funcionariosMatriz, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");

                    /* Map<String, List<Funcionario>> dadosPorPolo4 = ProcessadorDados.processarDados(tabelaNomeMatriz);
                    System.out.println("L180  "  + dadosPorPolo4 + "\n");
                    GeradorRelatorio.gerarRelatorio(dadosPorPolo4, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");
 */
                    //exibirMapaFormatado(key, tabelaNomeMatriz);
                    tabelaNomeCaruaru = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_CARUARU);
                    // Carregar os funcionários a partir do fileData
                    List<Funcionario> funcionariosCaruaru = Funcionario.carregarFuncionarios(tabelaNomeCaruaru);
                    // Exibir os funcionários no log
                    for (Funcionario func : funcionariosCaruaru) {
                        func.log();
                        System.out.println(func);
                    }
                    GeradorRelatorio.gerarRelatorio(funcionariosCaruaru, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");

                   /*  Map<String, List<Funcionario>> dadosPorPolo5 = ProcessadorDados.processarDados(tabelaNomeCaruaru);
                    System.out.println("L180  "  + dadosPorPolo5 + "\n");
                    GeradorRelatorio.gerarRelatorio(dadosPorPolo5, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");
 */
                    //exibirMapaFormatado(key, tabelaNomeCaruaru);                   
                    tabelaNomeGaranhuns = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_GARANHUNS);
                    // Carregar os funcionários a partir do fileData
                    List<Funcionario> funcionariosGaranhuns = Funcionario.carregarFuncionarios(tabelaNomeGaranhuns);
                    // Exibir os funcionários no log
                    for (Funcionario func : funcionariosGaranhuns) {
                        func.log();
                        System.out.println(func);
                    }

                    GeradorRelatorio.gerarRelatorio(funcionariosGaranhuns, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");

                    /* Map<String, List<Funcionario>> dadosPorPolo6 = ProcessadorDados.processarDados(tabelaNomeGaranhuns);
                    System.out.println("L180  "  + dadosPorPolo6 + "\n");
                    GeradorRelatorio.gerarRelatorio(dadosPorPolo6, System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");
  */
                   // exibirMapaFormatado(key, tabelaNomeGaranhuns); 
                }    
                 /*
                    /* if (!key.isEmpty()) {
                        ResultadoPolo resultadoNomesSerra = ResultadoPolo.processarValores(key,"",0L,0L,tabelaNomeSerra, 0.76);
                        resultadosParaExcel.put(key, List.of(
                            "Polo -> " + resultadoNomesSerra.getNomePolo(),
                            "CÓD -> "+ resultadoNomesSerra.getCodigoMsg(),
                            "DRT -> "+ resultadoNomesSerra.getDrt(),                        
                            "Nome -> " + resultadoNomesSerra.getNomeMsg(),                        
                            "Gratificação -> " + resultadoNomesSerra.getValor()                        
                        ));
                    } 
                    if (!key.isEmpty()) {
                        ResultadoPolo resultadoNomesPetrolina = ResultadoPolo.processarValores(key,"",0L,0L,tabelaNomePetrolina, 0.76);
                        resultadosParaExcel.put(key, List.of(
                            "Polo -> " + resultadoNomesPetrolina.getNomePolo(),
                            "CÓD -> "+ resultadoNomesPetrolina.getCodigoMsg(),
                            "DRT -> "+ resultadoNomesPetrolina.getDrt(),                        
                            "Nome -> " + resultadoNomesPetrolina.getNomeMsg(),                        
                            "Gratificação -> " + resultadoNomesPetrolina.getValor()                        
                        ));
                    }
                    if (!key.isEmpty()) {
                        ResultadoPolo resultadoNomesMatriz = ResultadoPolo.processarValores(key,"",0L,0L,tabelaNomeMatriz, 0.76);
                        resultadosParaExcel.put(key, List.of(
                            "Polo -> " + resultadoNomesMatriz.getNomePolo(),
                            "CÓD -> "+ resultadoNomesMatriz.getCodigoMsg(),
                            "DRT -> "+ resultadoNomesMatriz.getDrt(),                        
                            "Nome -> " + resultadoNomesMatriz.getNomeMsg(),                        
                            "Gratificação -> " + resultadoNomesMatriz.getValor()                        
                        ));
                    }
                    if (!key.isEmpty()) {
                        ResultadoPolo resultadoNomesCaruaru = ResultadoPolo.processarValores(key,"",0L,0L,tabelaNomeCaruaru, 0.76);
                        resultadosParaExcel.put(key, List.of(
                            "Polo -> " + resultadoNomesCaruaru.getNomePolo(),
                            "CÓD -> "+ resultadoNomesCaruaru.getCodigoMsg(),
                            "DRT -> "+ resultadoNomesCaruaru.getDrt(),                        
                            "Nome -> " + resultadoNomesCaruaru.getNomeMsg(),                        
                            "Gratificação -> " + resultadoNomesCaruaru.getValor()                        
                        ));
                    }
                    if (!key.isEmpty()) {
                        ResultadoPolo resultadoNomesGaranhuns = ResultadoPolo.processarValores(key,"",0L,0L,tabelaNomeGaranhuns, 0.76);
                        resultadosParaExcel.put(key, List.of(
                            "Polo -> " + resultadoNomesGaranhuns.getNomePolo(),
                            "CÓD -> "+ resultadoNomesGaranhuns.getCodigoMsg(),
                            "DRT -> "+ resultadoNomesGaranhuns.getDrt(),                        
                            "Nome -> " + resultadoNomesGaranhuns.getNomeMsg(),                        
                            "Gratificação -> " + resultadoNomesGaranhuns.getValor()                        
                        ));
                    }*/
                    
               /*  } else  if (key.equals("SETOR 2 SERRA.xls")){
                    demonstrativoCaixaSerra = buscarValoresPorNomeRetonarLista(fileData, key, TOTAL_RECEBIDO_C_C_BOLETOS);
                    //System.out.println("L221 Serra " + demonstrativoCaixaSerra + "\n"); 
                } else  if (key.equals("SETOR 3 PETROLINA.XLS")){
                    demonstrativoCaixaPetrolina = (buscarValoresPorNomeRetonarLista(fileData, key, TOTAL_RECEBIDO_C_C_BOLETOS));
                    //System.out.println("L224 Petrolina " + demonstrativoCaixaPetrolina + "\n");
                } else  if (key.equals("SETOR 4 MATRIZ.XLS")){
                    demonstrativoCaixaMatriz= buscarValoresPorNomeRetonarLista(fileData, key, TOTAL_RECEBIDO_C_C_BOLETOS);
                    System.out.println("L227 Matriz " + demonstrativoCaixaMatriz + "\n");
                    //exibirMapaFormatado(key, petrolina);
                } else  if (key.equals("SETOR 5 CARUARU.XLS")){
                    demonstrativoCaixaCaruaru = buscarValoresPorNomeRetonarLista(fileData, key, TOTAL_RECEBIDO_C_C_BOLETOS);
                   // System.out.println("L230 Caruaru " + demonstrativoCaixaCaruaru + "\n");
                } else  if (key.equals("SETOR 6 GARANHUNS.XLS")){
                    demonstrativoCaixaPetroGaranhuns = buscarValoresPorNomeRetonarLista(fileData, key, TOTAL_RECEBIDO_C_C_BOLETOS);
                   // System.out.println("L233 garanhuns " + demonstrativoCaixaPetroGaranhuns + "\n");
                } else  if (key.equals("GERAL.xls")){
                    demonstrativoCaixaGeral = buscarValoresPorNomeRetonarLista(fileData, key, TOTAL_RECEBIDO_C_C_BOLETOS);
                    //System.out.println("L236 Geral " + demonstrativoCaixaGeral + "\n");
                } else  if (key.equals("GERAL SITUAÇÃO MENS.xls")){
                    geralCituacaoMensageiro= JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, GERAL_SITUACAO_MENSAGEIRO);
                   
                   // System.out.println("L240 Geral situação mens " + geralCituacaoMensageiro +"\n");
                } else  if (key.equals("RECEBIDO POR OPERADOR.xls")){

                    //System.out.println("L171 recebido por operador " + key);
                } else  if (key.equals("Tabela-Gratificacao.xls")){
                    tabelaGratificacaoSerra = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_SERRA_TALHADA);
                    tabelaGratificacaoPetrolina = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_PETROLINA);
                    tabelaGratificacaoMatriz = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_MATRIZ);
                    tabelaGratificacaoCaruaru = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_CARUARU);
                    tabelaGratificacaoGaranhuns = JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, key, POLO_GARANHUNS);
                    //System.out.println("L173 Tabela-gratificaçaõ " + key);
                } */
              /*    // Consolidar no mapa do polo atual
                ConsolidacaoPolo.consolidarDadosPorPolo(mapaConsolidado, tabelaGratificacaoPetrolina, tabelaNomePetrolina, demonstrativoCaixaPetrolina);

                    // Adiciona o polo consolidado no mapa geral sem sobrescrever
                    String nomePolo = key.replace(".xls", "");
                    petrolina.put(nomePolo, mapaConsolidado);
                    }
                }

                // Exibe o mapa geral
                petrolina.forEach((polo, dados) -> {
                    System.out.println("Polo: " + polo);
                    exibirMapaFormatado(polo, dados);
                });
 */              
               /*  Map<String, Map<String, String>> serra = JavaApp.buscarValoresPorNomeRetornarMapaDeMapa(fileData, key, POLO_SERRA_TALHADA);
               
                Map<String, String> matriz = JavaApp.buscarValoresPorNomeRetornarMapa(fileData, key, POLO_MATRIZ);
                Map<String, String> caruaru = JavaApp.buscarValoresPorNomeRetornarMapa(fileData, key, POLO_CARUARU);
                Map<String, String> garanhuns = JavaApp.buscarValoresPorNomeRetornarMapa(fileData, key, POLO_GARANHUNS);
               */
            } else {
                System.out.println("Linha não encontrada.");
            }
               
        }
         // Consolidar tudo no mapa geral
        // mapaConsolidado.put("Petrolina", petrolina);
        // Consolidar dados após o loop
       // ConsolidacaoPolo.consolidarDadosPorPolo(mapaConsolidado.get("Petrolina"), tabelaGratificacao, tabelaNomes, demonstrativoCaixa);

    }
    
    public void processarSetores(String caminhoSetores) throws IOException {
        Map<String, List<Map<String, String>>> dadosFinais = dataProcessor.processarSetores(caminhoSetores);
        ExcelWriter excelWriter = new ExcelWriter();
        excelWriter.escreverDados((List<Map<String, Map<String, String>>>) resultadosParaExcel);
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

    // Método para consolidar dados no mapa do polo
    private void consolidarDadosPorPolo(Map<String, Map<String, String>> polo, Map<String, Map<String, String>> gratificacaoTabela, List<String> nomes) {
        polo.putAll(gratificacaoTabela);
        Map<String, String> nomesMap = new HashMap<>();
        for (int i = 0; i < nomes.size(); i++) {
            nomesMap.put("Nome " + (i + 1), nomes.get(i));
        }
        polo.put("Nomes", nomesMap);
    }

    // Método para exibir o mapa formatado
    private void exibirMapaFormatado(String polo, Map<String, Map<String, String>> mapa) {
        System.out.println("Polo: " + polo);
        for (Map.Entry<String, Map<String, String>> entry : mapa.entrySet()) {
            System.out.println("Linha " + entry.getKey() + ":");
            Map<String, String> valores = entry.getValue();
            for (Map.Entry<String, String> valor : valores.entrySet()) {
                System.out.println("  " + valor.getKey() + " -> " + valor.getValue());
            }
            System.out.println();
        }
    }

}