package com.tmkfolha.processor;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;  // Importação da exceção CsvException
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvProcessor implements FileProcessor {

    private List<String[]> data;

    @Override
    public void processFile(String filePath) throws IOException {
        try (CSVReader reader = new CSVReader(new FileReader(filePath))) {
            data = reader.readAll();  // Lê todas as linhas do CSV
        } catch (CsvException e) {
            System.err.println("Erro ao processar o arquivo CSV: " + e.getMessage());
            throw new IOException("Erro ao processar o arquivo CSV", e);  // Re-lança a exceção como IOException
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
