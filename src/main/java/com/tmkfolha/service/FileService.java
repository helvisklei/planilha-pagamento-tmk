package com.tmkfolha.service;

import com.tmkfolha.processor.FileProcessor;
import com.tmkfolha.processor.XlsProcessor;
import com.tmkfolha.processor.ExcelWriter;
import com.tmkfolha.util.BarraProgresso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável pelo processamento de arquivos CSV e Excel (.xls, .xlsx).
 */
public class FileService {

    /**
     * Processa uma lista de arquivos e realiza o processamento de acordo com o tipo de arquivo.
     *
     * @param filePaths Lista de caminhos dos arquivos a serem processados.
     * @param outputPath Caminho de saída para salvar os dados processados.
     * @throws Exception Caso ocorra um erro durante o processamento dos arquivos.
     */
    public void processFiles(List<String> filePaths, String outputPath,String mes, int ano) throws Exception {
        // Lista para armazenar os dados processados de todos os arquivos
        List<Map<String, Map<String, String>>> allData = new ArrayList<>();

        // Criação da BarraProgresso para monitoramento do progresso
        BarraProgresso barraProgresso = new BarraProgresso(); // Ajustar conforme a implementação necessária

        // Loop para processar cada arquivo da lista
        for (String filePath : filePaths) {
            FileProcessor processor;

            // Verifica a extensão do arquivo e seleciona o processador apropriado
           /*  if (filePath.endsWith(".csv")) {
                processor = new CsvProcessor();
            } else */ if (filePath.endsWith(".xlsx") || filePath.endsWith(".xls")) {
                processor = new XlsProcessor(barraProgresso); // Processador de arquivos Excel
            } else {
                throw new Exception("Tipo de arquivo não suportado: " + filePath);
            }

            // Executa o processamento do arquivo
            processor.processFile(filePath,mes,ano);
        }
    }
}
