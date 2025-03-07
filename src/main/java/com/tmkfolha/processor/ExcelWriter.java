package com.tmkfolha.processor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWriter {
    private final String filePath;

    public ExcelWriter() {
        this.filePath = System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx";
        System.out.println("Salvando dados em: " + filePath);
    }

    public void escreverDados(List<Map<String, Map<String, String>>> dataList) {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs(); // Criar a pasta caso não exista

        Workbook workbook;
        Sheet sheet;
        int rowIndex = 0;

        // Verifica se o arquivo já existe
        if (outputFile.exists()) {
            try (FileInputStream fis = new FileInputStream(outputFile)) {
                workbook = new XSSFWorkbook(fis);
                sheet = workbook.getSheet("FOLHA PAGAMENTO");
                if (sheet == null) {
                    sheet = workbook.createSheet("FOLHA PAGAMENTO");
                } else {
                    rowIndex = sheet.getLastRowNum() + 1; // Continua da última linha
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo existente: " + e.getMessage());
                return;
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("FOLHA PAGAMENTO");
        }

        // Estilo para título
        CellStyle estiloTitulo = workbook.createCellStyle();
        estiloTitulo.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fonteBranca = workbook.createFont();
        fonteBranca.setColor(IndexedColors.WHITE.getIndex());
        fonteBranca.setBold(true);
        estiloTitulo.setFont(fonteBranca);

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
                        Cell cellCabecalho = rowCabecalho.createCell(coluna++);
                        cellCabecalho.setCellValue(chave);
                    }

                    // Criar linha de valores
                    Row rowValores = sheet.createRow(rowIndex++);
                    coluna = 0;
                    for (String valor : registros.values()) {
                        Cell cellDado = rowValores.createCell(coluna++);
                        cellDado.setCellValue(valor);
                    }
                }

                rowIndex++; // Linha em branco para separar seções
            }
        }

        // Escrever os dados no arquivo
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
}






























/* package com.tmkfolha.processor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelWriter {
    private final String filePath;

    public ExcelWriter() {
        // Define o caminho padrão na pasta do projeto
        String defaultPath = System.getProperty("user.dir") + "/output/dados_unificados.xlsx";
        this.filePath = defaultPath;
        System.out.println("Salvando dados em: " + filePath); // Verifique o caminho
    }

    public void escreverDados(List<Map<String, Map<String, String>>> dataList) {
        // Garante que a pasta existe
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs(); // Cria a pasta caso não exista

        try (Workbook workbook = new XSSFWorkbook()) {  // Criando novo workbook para salvar os dados
            Sheet sheet = workbook.createSheet("Dados Processados");

            int rowIndex = 0;
            // Estilos para o título
            CellStyle estiloTitulo = workbook.createCellStyle();
            estiloTitulo.setFillForegroundColor(IndexedColors.BLACK.getIndex());
            estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font fonteBranca = workbook.createFont();
            fonteBranca.setColor(IndexedColors.WHITE.getIndex());
            fonteBranca.setBold(true);
            estiloTitulo.setFont(fonteBranca);

            for (Map<String, Map<String, String>> data : dataList) {
                for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
                    for (String titulo : data.keySet()) {
                        Map<String, String> registros = data.get(titulo); 
                        Row row = sheet.createRow(rowIndex++);
                        Cell cellTitulo = row.createCell(0);
                        cellTitulo.setCellValue(titulo);
                        cellTitulo.setCellStyle(estiloTitulo); 
                        if (!registros.isEmpty()) {
                            // Criar cabeçalhos
                            Row rowCabecalho = sheet.createRow(rowIndex++);
                            int coluna = 0;
    
                            for (String chave : registros.keySet()) {
                                Cell cellCabecalho = rowCabecalho.createCell(coluna++);
                                cellCabecalho.setCellValue(chave);
                            }       
                            
                              // Criar linha de valores
                        Row rowValores = sheet.createRow(rowIndex++);
                        coluna = 0;
                        for (String valor : registros.values()) {
                            Cell cellDado = rowValores.createCell(coluna++);
                            cellDado.setCellValue(valor);
                        }
                    }
                    //rowIndex++; // Linha em branco para separar seções
                }
            }
                    
                 // /*   Cell cellCategoria = row.createCell(0);
                  //  cellCategoria.setCellValue(entry.getKey());

                  //  int cellIndex = 1;
                   // for (Map.Entry<String, String> subEntry : entry.getValue().entrySet()) {
                   //     Cell cellHeader = row.createCell(cellIndex++);
                    //    cellHeader.setCellValue(subEntry.getKey());
                    //    Cell cellValue = row.createCell(cellIndex++);
                    //    cellValue.setCellValue(subEntry.getValue());
                   // }/
               // }
            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            System.out.println("Dados gravados na planilha com sucesso!");

        } catch (IOException e) {
            System.err.println("Erro ao escrever no arquivo Excel: " + e.getMessage());
        }
    }
} */