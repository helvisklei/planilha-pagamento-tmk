package com.tmkfolha.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Classe responsável por escrever dados em um arquivo Excel.
 * Suporta adição de cores, formatação e múltiplas abas.
 */
public class ExcelWriter {
    private final String filePath;

    /**
     * Construtor que define o caminho padrão do arquivo de saída.
     */
    public ExcelWriter() {
        this.filePath = System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx";
        System.out.println("Salvando dados em: " + filePath);
    }

    /**
     * Escreve os dados em uma planilha Excel.
     * @param dataList Lista de mapas contendo os dados estruturados a serem escritos.
     */
    public void escreverDados(List<Map<String, Map<String, String>>> dataList) {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs(); // Garante que o diretório exista

        Workbook workbook;
        Sheet sheet;
        int rowIndex = 0;

        // Tenta abrir o arquivo se já existir
        if (outputFile.exists()) {
            try (FileInputStream fis = new FileInputStream(outputFile)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheet("DP FOLHA");
                if (sheet == null) {
                    sheet = workbook.createSheet("DP FOLHA ORIGEM");
                } else {
                    rowIndex = sheet.getLastRowNum() + 1; // Continua da última linha
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo existente: " + e.getMessage());
                return;
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("DP FOLHA"); // Aba principal
            workbook.createSheet("DP FOLHA ORIGEM"); // Criando outra aba
        }

        // Criando estilo para título
        CellStyle estiloTitulo = criarEstiloTitulo(workbook);

        for (Map<String, Map<String, String>> data : dataList) {
            for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
                String titulo = entry.getKey();
                Map<String, String> registros = entry.getValue();

                // Criar linha para o título
                Row rowTitulo = sheet.createRow(rowIndex++);
                Cell cellTitulo = rowTitulo.createCell(0);
                cellTitulo.setCellValue(titulo);
                cellTitulo.setCellStyle(estiloTitulo);

                if (!registros.isEmpty()) {
                    // Criar cabeçalhos
                    Row rowCabecalho = sheet.createRow(rowIndex++);
                    int coluna = 0;
                    for (String chave : registros.keySet()) {
                        System.out.println("L78 " + chave);
                        Cell cellCabecalho = rowCabecalho.createCell(coluna++);
                        cellCabecalho.setCellValue(chave);
                    }

                    // Criar linha de valores
                    Row rowValores = sheet.createRow(rowIndex++);
                    coluna = 0;
                    for (String valor : registros.values()) {
                        Cell cellDado = rowValores.createCell(coluna++);
                        CellStyle style = workbook.createCellStyle();
                        DataFormat format = workbook.createDataFormat();
                        style.setDataFormat(format.getFormat("R$ #,##0.00"));  // Formatação de moeda
                        cellDado.setCellValue(valor);
                        cellDado.setCellStyle(style);
                    }
                }

                rowIndex++; // Adiciona uma linha em branco para separação
            }
        }

        // Escreve os dados no arquivo Excel
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            workbook.write(fos);
            System.out.println("Dados adicionados à planilha com sucesso!");
        } catch (IOException e) {
            System.err.println("Erro ao salvar no arquivo Excel: " + e.getMessage());
        } finally {
            try {
                workbook.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o arquivo Excel: " + e.getMessage());
            }
        }
    }

    /**
     * Cria um estilo personalizado para os títulos na planilha.
     * @param workbook Workbook onde o estilo será aplicado.
     * @return CellStyle configurado.
     */
    private CellStyle criarEstiloTitulo(Workbook workbook) {
        CellStyle estilo = workbook.createCellStyle();
        estilo.setFillForegroundColor(IndexedColors.BLUE.getIndex()); // Define a cor de fundo
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        Font fonteBranca = workbook.createFont();
        fonteBranca.setColor(IndexedColors.WHITE.getIndex()); // Define a cor do texto
        fonteBranca.setBold(true);
        
        estilo.setFont(fonteBranca);
        return estilo;
    }
}