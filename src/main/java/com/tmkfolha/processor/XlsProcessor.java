package com.tmkfolha.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.print.DocFlavor.STRING;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class XlsProcessor implements FileProcessor {

    private List<Map<String, String>> extractedData = new ArrayList<>();
    private TreeMap<String, Map<String, String>> fileData = new TreeMap<>();
    //private ExcelWrite excelWriter = new ExcelWrite();
    // Criando um mapa para armazenar os resultados
    Map<String, List<String>> resultadosParaExcel = new HashMap<>();

    @Override
    public void processFile(String filePath) throws IOException {
        // System.out.println("Processando arquivo L23: " + filePath);
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook;
        
        // **Chamar a classe ExcelWriter para salvar os dados**
        ExcelWriter excelWriter = new ExcelWriter();

        if (filePath.endsWith(".xlsx")) {
            workbook = new XSSFWorkbook(fis);
        } else {
            workbook = new HSSFWorkbook(fis);
        }

        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                Map<String, String> rowData = new HashMap<>();
                for (Cell cell : row) {
                    cell.setCellType(CellType.STRING);
                    rowData.put("Column" + cell.getColumnIndex(), cell.getStringCellValue());
                }
                //System.out.println(" XlsProcessor L40 "+rowData);
                extractedData.add(rowData);
            }
        }
       
        // Ordenar extractedData por uma coluna específica
             extractedData.sort(Comparator.comparing(row -> {
            // Extrair o número da coluna para garantir uma ordenação numérica
            String columnKey = row.getOrDefault("Column0", "");
            return extrairNumeroDaColuna(columnKey); // Função que extrai o número da coluna
        }));

        // Adicionar os dados ao fileData ordenado por nome do arquivo
        fileData.put(file.getName(), ordenarMapaInterno(mergeData(extractedData)));

        String GERAL = "GERAL.XLS"; // Demonstrativo geral 
        String SERRA = "SETOR 2 SERRA.XLS"; // Demonstrativo Serra Talhada 
        String PETROLINA = "SETOR 3 PETROLINA.XLS"; // Demonstrativo Petrolina
        String MATRIZ = "SETOR 4 MATRIZ.XLS"; // Demonstrativo Matriz
        String CARUARU = "SETOR 5 CARUARU.XLS"; // Demonstrativo Caruaru
        String GARANHUNS = "SETOR 6 GARANHUNS.XLS"; // Demonstrativo Garanhuns  
        String SITUACAO_MENSAGEIROS = "GERAL SITUAÇÃO MENS.xls"; // Situação Mensageiros
        String NOME_PROCURADO = "Total  (Recebido-Cheque pré+Cheque Custódia+Boletos ) :";
        String NOME_PROCURADO2 = "Cobrador";

        //Dados Petrolina
       /*  if (fileData.containsKey(PETROLINA)){
            Map<String, String> dadosPetrolina = fileData.get(PETROLINA);

            String nome = dadosPetrolina.get("Linha 3 - Column0");
            String qtd = dadosPetrolina.get("Linha 17 - Column7");            
            String valor4 = dadosPetrolina.get("Linha 17 - Column9");  
            String petrolinaCaixa = "Nome: " + nome + " - Qtd: " + qtd + " - Valor: " + valor4;         

            System.out.println(petrolinaCaixa);
            System.out.println("Valores completo de PETROLINA\n");
            System.out.println(fileData);
            System.out.println("\n"); 
            // Chamando a função para buscar um termo específico no mapa
            buscarValoresPorNome(fileData, PETROLINA, "Total  (Recebido-Cheque pré+Cheque Custódia+Boletos ) :");        
        }  */
        // Dados serra talhada
      /*   if (fileData.containsKey(SERRA)){
            Map<String, String> dadosSerra = fileData.get(SERRA);
            String nome = dadosSerra.get("Linha 3 - Column0");
            String qtd = dadosSerra.get("Linha 17 - Column7");            
            String valor4 = dadosSerra.get("Linha 17 - Column9");  
            String serraCaixa = "Nome: " + nome + " - Qtd: " + qtd + " - Valor: " + valor4;         

            System.out.println(serraCaixa);
            System.out.println("Valores completo de Serra Talhada\n");
            System.out.println(fileData);
            System.out.println("\n"); 
            // Chamando a função para buscar um termo específico no mapa
            buscarValoresPorNome(fileData, SERRA, "Total  (Recebido-Cheque pré+Cheque Custódia+Boletos ) :");      
        }   */
        
       // Verifica se os arquivos que você deseja estão no mapa 'fileData'
       if (fileData.containsKey(GERAL) || fileData.containsKey(SERRA) || fileData.containsKey(PETROLINA)|| fileData.containsKey(MATRIZ)|| fileData.containsKey(CARUARU)|| fileData.containsKey(GARANHUNS)|| fileData.containsKey(SITUACAO_MENSAGEIROS)) {
            // Buscar valores para o arquivo GERAL.XLS e insere no 'resultadosParaExcel'
            if (fileData.containsKey(GERAL)) {
                List<String> geralValores = buscarValoresPorNomeRetonarLista(fileData, GERAL, NOME_PROCURADO);
                ResultadoPolo resultadoGeral = ResultadoPolo.processarValores(GERAL, geralValores);
                List<String> valoresFormatados = List.of(            
                    "QTD -> " + resultadoGeral.getQuantidade(),
                    "VALOR -> " + resultadoGeral.getValor()
                );
                resultadosParaExcel.put(GERAL,valoresFormatados);  // Insere os valores encontrados

               /*  if (!geralValores.isEmpty()) {
                    resultadosParaExcel.put(GERAL, geralValores);  // Insere os valores encontrados
                    System.out.println("L115 "+resultadosParaExcel);
                } else {
                    System.out.println("Nenhum valor encontrado para GERAL.XLS");
                } */
            }

            // Buscar valores para o arquivo SETOR 2 SERRA.XLS e insere no 'resultadosParaExcel'
            if (fileData.containsKey(SERRA)) {
                List<String> serraValores = buscarValoresPorNomeRetonarLista(fileData, SERRA, NOME_PROCURADO);
                ResultadoPolo resultadoSerra = ResultadoPolo.processarValores(SERRA, serraValores);
                List<String> valoresFormatados = List.of(            
                    "QTD -> " + resultadoSerra.getQuantidade(),
                    "VALOR -> " + resultadoSerra.getValor()
                );
                resultadosParaExcel.put(SERRA,valoresFormatados);  // Insere os valores encontrados

               /*  if (!serraValores.isEmpty()) {
                    resultadosParaExcel.put(SERRA, serraValores);  // Insere os valores encontrados
                } else {
                    System.out.println("Nenhum valor encontrado para SETOR 2 SERRA.XLS");
                } */
            }

            // Buscar valores para o arquivo SETOR 3 PETROLINA.XLS e insere no 'resultadosParaExcel'
            if (fileData.containsKey(PETROLINA)) {
                List<String> petrolinaValores = buscarValoresPorNomeRetonarLista(fileData,PETROLINA, NOME_PROCURADO);
                ResultadoPolo resultadoPetrolina = ResultadoPolo.processarValores(PETROLINA, petrolinaValores);
                List<String> valoresFormatados = List.of(            
                    "QTD -> " + resultadoPetrolina.getQuantidade(),
                    "VALOR -> " + resultadoPetrolina.getValor()
                );
                resultadosParaExcel.put(PETROLINA,valoresFormatados);  // Insere os valores encontrados

                /* if (!petrolinaValores.isEmpty()) {
                    resultadosParaExcel.put(PETROLINA, petrolinaValores);  // Insere os valores encontrados
                } else {
                    System.out.println("Nenhum valor encontrado para SETOR 3 PETROLINA.XLS");
                } */
            }
            if(fileData.containsKey(MATRIZ)){
                List<String> matrizValores = buscarValoresPorNomeRetonarLista(fileData, MATRIZ, NOME_PROCURADO);
                ResultadoPolo resultadoMatriz = ResultadoPolo.processarValores(MATRIZ, matrizValores);
                List<String> valoresFormatados = List.of(            
                    "QTD -> " + resultadoMatriz.getQuantidade(),
                    "VALOR -> " + resultadoMatriz.getValor()
                );
                resultadosParaExcel.put(MATRIZ,valoresFormatados);  // Insere os valores encontrados

                /* if (!matrizValores.isEmpty()) {
                    resultadosParaExcel.put(MATRIZ, matrizValores);  // Insere os valores encontrados
                } else {
                    System.out.println("Nenhum valor encontrado para SETOR 4 MATRIZ.XLS");
                } */
            }
            if(fileData.containsKey(CARUARU)){
                List<String> caruaruValores = buscarValoresPorNomeRetonarLista(fileData, CARUARU, NOME_PROCURADO);
                ResultadoPolo resultadoCaruaru = ResultadoPolo.processarValores(CARUARU, caruaruValores);

                // Converte para List<String> e armazena no mapa
                List<String> valoresFormatados = List.of(            
                    "QTD -> " + resultadoCaruaru.getQuantidade(),
                    "VALOR -> " + resultadoCaruaru.getValor()
                );
                resultadosParaExcel.put(CARUARU,valoresFormatados);  // Insere os valores encontrados
                

              /*   if (!caruaruValores.isEmpty()) {
                    resultadosParaExcel.put(CARUARU, caruaruValores);  // Insere os valores encontrados
                } else {
                    System.out.println("Nenhum valor encontrado para SETOR 5 CARUARU.XLS");
                } */
            }

            if (fileData.containsKey(GARANHUNS)) {
            List<String> garanhunsValores = buscarValoresPorNomeRetonarLista(fileData, GARANHUNS, NOME_PROCURADO);           

            ResultadoPolo resultadoGaranhuns = ResultadoPolo.processarValores(GARANHUNS, garanhunsValores);

            // Converte para List<String> e armazena no mapa
            List<String> valoresFormatados = List.of(            
                "QTD -> " + resultadoGaranhuns.getQuantidade(),
                "VALOR -> " + resultadoGaranhuns.getValor()
            );
            resultadosParaExcel.put(GARANHUNS,valoresFormatados);  // Insere os valores encontrados
            
            
           /*  //System.out.println("L159 "+garanhunsValores.size() );
            
            if (garanhunsValores.size() < 4) { // Garante que há elementos suficientes na lista
                System.out.println("Erro: Lista garanhunsValores não contém dados suficientes.");
                return;
            }           

            // Pegamos os textos brutos das linhas
            String textoQtd = garanhunsValores.get(2);  // Exemplo: "Linha 20 - Column7 -> 5538"
            String textoValor = garanhunsValores.get(3);  // Exemplo: "Linha 20 - Column9 -> R$ 166544,63"

            // Captura apenas números inteiros da quantidade (valores após "-> ")
            String qtd = textoQtd.contains("->") ? textoQtd.split("->")[1].trim() : "0";

            // Converte a string para inteiro
            int qtdInt;
            try {
                qtdInt = Integer.parseInt(qtd.replaceAll("\\D", "")); // Remove qualquer caractere não numérico
            } catch (NumberFormatException e) {
                qtdInt = 0; // Define como 0 se a conversão falhar
                System.err.println("Erro ao converter qtd para inteiro: " + e.getMessage());
}

            // Captura do valor monetário com o padrão brasileiro
            Pattern patternValor = Pattern.compile("([\\d.]+,\\d{2})");
            Matcher matcherValor = patternValor.matcher(textoValor);
            String valorParaConverter = matcherValor.find() ? matcherValor.group(1) : "0";
            String valorStr = valorParaConverter.replace(",", "."); // Converte para o formato numérico
            double valor = Double.parseDouble(valorStr);

            List<Object> valoresObj = List.of("DEMONSTRATIVO -> Total","QTD -> "+ qtdInt, "VALOR -> "+valor); // Exemplo com tipos diferentes

            // Convertendo para List<String>
            List<String> garanhunsValoresDemonstrativo = valoresObj.stream()
                .map(Object::toString) // Converte cada elemento para String
                .collect(Collectors.toList());

           // resultadosParaExcel.put(GARANHUNS, garanhunsValoresDemonstrativo);  // Insere os valores encontrados


           // System.out.println("Valor extraído: " +  resultadosParaExcel);
            */
        }
        
        if(fileData.containsKey(SITUACAO_MENSAGEIROS)){
               /*  List<String> situacaoMensageirosValores = buscarValoresPorNomeRetonarLista(fileData, SITUACAO_MENSAGEIROS, NOME_PROCURADO2);
                if (!situacaoMensageirosValores.isEmpty()) {
                    ResultadoPolo resultadoSituacaoMensageiro = ResultadoPolo.processarValores(SITUACAO_MENSAGEIROS, situacaoMensageirosValores);

                    // Converte para List<String> e armazena no mapa
                    List<String> valoresFormatados = List.of(            
                        "QTD -> " + resultadoSituacaoMensageiro.getQuantidade(),
                        "VALOR -> " + resultadoSituacaoMensageiro.getValor()
                    );
                    resultadosParaExcel.put(SITUACAO_MENSAGEIROS,valoresFormatados);  // Insere os valores encontrados
            
                    System.out.println("221 " + situacaoMensageirosValores);
                    //resultadosParaExcel.put(SITUACAO_MENSAGEIROS, situacaoMensageirosValores);  // Insere os valores encontrados
                } else {
                    System.out.println("Nenhum valor encontrado para GERAL SITUAÇÃO MENS.xls");
                } */

                //Workbook workbook2 = new XSSFWorkbook(new FileInputStream(filePath));
                Sheet sheet =workbook.getSheetAt(0); // Assumindo que os dados estão na primeira aba
                System.out.println("L238 "+sheet.getSheetName());
                System.out.println(" L239 "+sheet);
                            
                List<String> situacaoMensageirosValores = new ArrayList<>();
            
                for (int rowIndex = 6; rowIndex <= sheet.getLastRowNum(); rowIndex++) { // Começa na linha 7 (índice 6)
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue; // Ignora linhas vazias
            
                    // Coluna 0 contém código do mensageiro
                    Cell cellCodigo = row.getCell(0);
                    if (cellCodigo == null || cellCodigo.getCellType() != CellType.NUMERIC) continue;
            
                    int codigoMensageiro = (int) cellCodigo.getNumericCellValue();
            
                    // Coluna 1 contém nome do mensageiro
                    Cell cellNome = row.getCell(1);
                    String nomeMensageiro = (cellNome != null) ? cellNome.getStringCellValue().trim() : "N/A";
            
                    // Coluna 2 contém um valor associado
                    Cell cellValor = row.getCell(2);
                    double valorMensageiro = (cellValor != null && cellValor.getCellType() == CellType.NUMERIC) ? cellValor.getNumericCellValue() : 0.0;
            
                    // Adiciona os valores extraídos à lista
                    situacaoMensageirosValores.add(codigoMensageiro + " - " + nomeMensageiro + " - " + valorMensageiro);
                    System.out.println("Linha " + rowIndex + " -> " + codigoMensageiro + " - " + nomeMensageiro + " - " + valorMensageiro);
                }
            
                //workbook.close(); // Fecha o arquivo após a leitura
            
                if (!situacaoMensageirosValores.isEmpty()) {
                    ResultadoPolo resultadoSituacaoMensageiro = ResultadoPolo.processarValores(SITUACAO_MENSAGEIROS, situacaoMensageirosValores);
            
                    // Formata os valores

                    List<String> valoresFormatados = List.of(            
                        "QTD -> " + resultadoSituacaoMensageiro.getQuantidade(),
                        "VALOR -> " + resultadoSituacaoMensageiro.getValor()
                    );
                    resultadosParaExcel.put(SITUACAO_MENSAGEIROS, valoresFormatados);
                    
                    System.out.println("Dados processados para Situação Mensageiros: " + valoresFormatados);
                } else {
                    System.out.println("Nenhum valor encontrado para GERAL SITUAÇÃO MENS.xls");
                }
            }
        } 
        // Fechando o arquivo             
        workbook.close();

        List<Map<String, Map<String, String>>> convertedData = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : resultadosParaExcel.entrySet()) {
            // Para cada arquivo (como 'GERAL.XLS'), vamos criar um Map<String, String> para armazenar os dados
            Map<String, String> registros = new HashMap<>();

            // Aqui você pode fazer o parsing das linhas e colunas conforme necessário
            for (String line : entry.getValue()) {
                String[] parts = line.split(" -> "); // Dividir pela chave '->'
                if (parts.length == 2) {
                    String key = parts[0].trim();  // Chave como "Linha 19 - Column1"
                    String value = parts[1].trim(); // Valor como "Total ..."
                    registros.put(key, value);
                   // System.out.println(" XlsProcessor L190 "+registros);
                }
            }

            // Agora, criamos o Map<String, Map<String, String>> para armazenar os registros de cada arquivo
            Map<String, Map<String, String>> section = new HashMap<>();
            section.put(entry.getKey(), registros); // Onde 'entry.getKey()' é o nome do arquivo, como 'GERAL.XLS'

            // Adicionamos a seção na lista de dados convertidos
            convertedData.add(section);
        }
       
      
        System.out.println("📌 Conteúdo que será salvo no Excel:");
        for (Map.Entry<String, List<String>> entry : resultadosParaExcel.entrySet()) {
            System.out.println("🔹 " + entry.getKey() + " -> " + entry.getValue());
        }
       excelWriter.escreverDados(convertedData); 

        fis.close();
    }
    
    @Override
    public Map<String, Map<String, String>> getData() {
       
        return fileData;
    }

    public List<Map<String, String>> getExtractedData() {
       // System.out.println(" XlsProcessor L58 "+extractedData);
        return extractedData;
    }

    public void clearExtractedData() {
        extractedData.clear();
    }

    public void generateFinalExcel(String outputPath) throws IOException {
        if (extractedData.isEmpty()) {
            System.out.println("Nenhum dado extraído para gerar o arquivo final.");
            return;
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("FOLHA");

        int rowNum = 0;
        for (Map<String, String> rowData : extractedData) {
            Row row = sheet.createRow(rowNum++);
            int cellNum = 0;
            for (String value : rowData.values()) {
                Cell cell = row.createCell(cellNum++);
                //System.out.println(" XlsProcessor L80 "+value);
                cell.setCellValue(value);
            }
        }

        FileOutputStream fos = new FileOutputStream(outputPath);
        workbook.write(fos);
        workbook.close();
        fos.close();

        System.out.println("Arquivo final gerado: " + outputPath);
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
    private int extrairNumeroDaColuna(String columnKey) {
        try {
            // Extrair o número da chave da coluna, considerando que o nome da coluna é algo como "Column10"
            return Integer.parseInt(columnKey.replaceAll("\\D", "")); // Remove os caracteres não numéricos
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Caso não consiga extrair o número, coloca um valor alto
        }
    }

}