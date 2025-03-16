package com.tmkfolha.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class DataProcessor {
    private Map<String, List<Map<String, String>>> dadosNomes = new HashMap<>();
    private Map<String, Double> gratificacaoPercentual = new HashMap<>();

    /**
     * Lê a planilha Nomes.xls e armazena os dados por setor.
     */
    public void carregarDadosNomes(String caminhoArquivo) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(caminhoArquivo));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Assume a primeira aba
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pular cabeçalho

                String setor = row.getCell(5).getStringCellValue().trim(); // Coluna SETOR
                Map<String, String> dados = new HashMap<>();
                dados.put("TIPO", row.getCell(0).getStringCellValue().trim());
                dados.put("CÓD.", row.getCell(1).getStringCellValue().trim());
                dados.put("DRT", row.getCell(2).getStringCellValue().trim());
                dados.put("NOME", row.getCell(3).getStringCellValue().trim());
                dados.put("PÓLO", row.getCell(4).getStringCellValue().trim());
                dados.put("SETOR", setor);

                dadosNomes.computeIfAbsent(setor, k -> new ArrayList<>()).add(dados);
            }
        }
    }

    /**
     * Lê a planilha Tabela-Gratificacao.xls e armazena os percentuais.
     */
    public void carregarPercentuaisGratificacao(String caminhoArquivo) throws IOException {
        try (FileInputStream fis = new FileInputStream(new File(caminhoArquivo));
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); 
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pular cabeçalho
                String setor = row.getCell(0).getStringCellValue().trim();
                double percentual = row.getCell(1).getNumericCellValue();
                gratificacaoPercentual.put(setor, percentual);
            }
        }
    }

    /**
     * Associa os dados dos setores e calcula as gratificações.
     */
    public Map<String, List<Map<String, String>>> processarSetores(String caminhoSetores) throws IOException {
        Map<String, List<Map<String, String>>> dadosFinais = new HashMap<>();

        File diretorio = new File(caminhoSetores);
        for (File file : Objects.requireNonNull(diretorio.listFiles())) {
            if (!file.getName().endsWith(".xls") && !file.getName().endsWith(".xlsx")) continue;
            
            String setor = file.getName().replace(".xls", "").replace(".xlsx", "").trim();
            if (!dadosNomes.containsKey(setor)) continue;

            try (FileInputStream fis = new FileInputStream(file);
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);
                for (Row row : sheet) {
                    String codigo = row.getCell(0).getStringCellValue().trim();
                    double valorBase = row.getCell(1).getNumericCellValue();
                    
                    for (Map<String, String> dados : dadosNomes.get(setor)) {
                        if (dados.get("CÓD.").equals(codigo)) {
                            double percentual = gratificacaoPercentual.getOrDefault(setor, 0.0);
                            double gratificacao = valorBase * percentual;
                            dados.put("GRATIFICAÇÃO (R$)", String.format("%.2f", gratificacao));
                            dadosFinais.computeIfAbsent(setor, k -> new ArrayList<>()).add(dados);
                        }
                    }
                }
            }
        }
        return dadosFinais;
    }
}
