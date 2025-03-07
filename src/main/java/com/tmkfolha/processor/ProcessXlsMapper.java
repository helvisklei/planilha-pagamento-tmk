package com.tmkfolha.processor;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ProcessXlsMapper {
    private final String caminhoArquivo;
    private final Map<String, String> dadosProcessados;

    // Construtor recebe o caminho e já processa o arquivo
    public ProcessXlsMapper(String caminhoArquivo) throws IOException {
        this.caminhoArquivo = caminhoArquivo;
        this.dadosProcessados = processarArquivoExcel();
    }

    // Método para retornar os dados processados
    public Map<String, String> getData() {
        return dadosProcessados;
    }

    // Método privado para processar o arquivo XLS
    private Map<String, String> processarArquivoExcel() throws IOException {
        Map<String, String> mapaDados = new LinkedHashMap<>();
        File file = new File(caminhoArquivo);

        if (!file.exists()) {
            throw new IOException("Arquivo não encontrado: " + caminhoArquivo);
        }

        try (FileInputStream fileStream = new FileInputStream(file);
             Workbook workbook = new HSSFWorkbook(fileStream)) {

            for (Sheet sheet : workbook) {
                for (Row row : sheet) {
                    String chave = "";
                    String valor = "";

                    for (Cell cell : row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                if (chave.isEmpty()) {
                                    chave = cell.getStringCellValue().trim();
                                } else {
                                    valor = cell.getStringCellValue().trim();
                                }
                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    valor = cell.getDateCellValue().toString();
                                } else {
                                    valor = String.valueOf(cell.getNumericCellValue());
                                }
                                break;
                            case BOOLEAN:
                                valor = String.valueOf(cell.getBooleanCellValue());
                                break;
                            default:
                                break;
                        }
                    }

                    if (!chave.isEmpty() && !valor.isEmpty()) {
                        mapaDados.put(chave, valor);
                    }
                }
            }
        }
        return mapaDados;
    }
}
