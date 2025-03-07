package com.tmkfolha.service;

import com.tmkfolha.processor.CsvProcessor;
import com.tmkfolha.processor.XlsxProcessor;
import com.tmkfolha.processor.FileProcessor;
import com.tmkfolha.processor.XlsProcessor;
import com.tmkfolha.processor.ExcelWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FileService {

    public void processFiles(List<String> filePaths, String outputPath) throws Exception {
        List<Map<String, Map<String, String>>> allData = new ArrayList<>(); // Corrigido para armazenar os dados processados

        for (String filePath : filePaths) {
            FileProcessor processor;

            if (filePath.endsWith(".csv")) {
                processor = new CsvProcessor();
            } else if (filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
                processor = new XlsProcessor(); // Usar o XlsProcessor aqui
            } else {
                throw new Exception("Tipo de arquivo não suportado: " + filePath);
            }

            processor.processFile(filePath);
           // System.out.println(" FileService dados coletado L31"+processor.getData());
           // allData.add(processor.getData());  // Coleta os dados processados
        }

        // Chama o ExcelWriter para escrever os dados no arquivo Excel unificado
        //ExcelWriter excelWriter = new ExcelWriter();
        //System.out.println(" L36 fileservice "+allData);
        //excelWriter.escreverDados(allData);  // Passa todos os dados coletados
    }
}