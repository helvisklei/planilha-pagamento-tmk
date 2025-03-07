package com.tmkfolha.processor;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XlsxProcessor implements FileProcessor {

    private List<String[]> data;

    @Override
    public void processFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             XSSFWorkbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Lê a primeira aba
            data = new ArrayList<>();
            for (Row row : sheet) {
                List<String> rowData = new ArrayList<>();
                for (Cell cell : row) {
                    rowData.add(cell.toString()); // Adiciona o valor da célula à lista
                }
                data.add(rowData.toArray(new String[0])); // Converte para array e adiciona à lista final
            }
        }
    }

    @Override
    public Map<String, Map<String, String>> getData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getData'");
    }

   /*  @Override
    public List<String[]> getData() {
        return data;
    } */
}

