 package com.tmkfolha.app.controllers;

 import org.apache.poi.ss.usermodel.*;
 import org.apache.poi.xssf.usermodel.XSSFWorkbook;
 
 import java.io.*;
 import java.util.*;
 
 public class GeradorRelatorio {
 
     public static void gerarRelatorio(List<Funcionario> funcionarios, String caminhoSaida) {
         Map<String, List<Funcionario>> polosMap = dividirPorPolo(funcionarios); // Método para dividir por Polo
 
         try (FileInputStream fis = new FileInputStream(caminhoSaida); 
              Workbook workbook = new XSSFWorkbook(fis)) {
 
             // Carrega a aba DP FOLHA ou cria se não existir
             Sheet folhaDPFolha = workbook.getSheet("DP FOLHA");
             if (folhaDPFolha == null) {
                 folhaDPFolha = workbook.createSheet("DP FOLHA");
             }
             preencherAbaDPFolha(folhaDPFolha, polosMap);
 
             // Carrega a aba DP FOLHA ORIGEM ou cria se não existir
             Sheet folhaDPFolhaOrigem = workbook.getSheet("DP FOLHA ORIGEM");
             if (folhaDPFolhaOrigem == null) {
                 folhaDPFolhaOrigem = workbook.createSheet("DP FOLHA ORIGEM");
             }
             preencherAbaDPFolhaOrigem(folhaDPFolhaOrigem, polosMap);
 
             // Salva o arquivo
             try (FileOutputStream fos = new FileOutputStream(caminhoSaida)) {
                 workbook.write(fos);
                 System.out.println("Relatório gerado com sucesso!");
             }
         } catch (IOException e) {
             e.printStackTrace();
         }
     }
 
     private static Map<String, List<Funcionario>> dividirPorPolo(List<Funcionario> funcionarios) {
         Map<String, List<Funcionario>> polosMap = new HashMap<>();
         for (Funcionario funcionario : funcionarios) {
             polosMap.computeIfAbsent(funcionario.getPolo(), k -> new ArrayList<>()).add(funcionario);
         }
         return polosMap;
     }
 
     private static void preencherAbaDPFolha(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
         int rowIndex = sheet.getLastRowNum() + 2; // Pega a última linha preenchida
 
         for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
             String polo = entry.getKey();
             List<Funcionario> funcionarios = entry.getValue();
 
             // Cabeçalho do polo
             Row rowPolo = sheet.createRow(rowIndex++);
             rowPolo.createCell(0).setCellValue(polo);
 
             // Cabeçalho da tabela
             Row rowCabecalho = sheet.createRow(rowIndex++);
             rowCabecalho.createCell(0).setCellValue("CÓD.");
             rowCabecalho.createCell(1).setCellValue("DRT");
             rowCabecalho.createCell(2).setCellValue("NOME");
             rowCabecalho.createCell(3).setCellValue("RECEBIMENTO (R$)");
             rowCabecalho.createCell(4).setCellValue("RENDIMENTO (%)");
             rowCabecalho.createCell(5).setCellValue("GRATIFICAÇÃO (%)");
             rowCabecalho.createCell(6).setCellValue("GRATIFICAÇÃO (R$)");
 
             // Dados dos funcionários
             for (Funcionario funcionario : funcionarios) {
                 Row row = sheet.createRow(rowIndex++);
                 row.createCell(0).setCellValue(funcionario.getCodigo());
                 row.createCell(1).setCellValue(funcionario.getDrt());
                 row.createCell(2).setCellValue(funcionario.getNome());
                 row.createCell(3).setCellValue(funcionario.getRecebimento());
                 row.createCell(4).setCellValue(funcionario.getRendimento() * 100);
                 row.createCell(5).setCellValue(funcionario.getGratificacaoPercentual() * 100);
                 row.createCell(6).setCellValue(funcionario.getGratificacaoValor());
             }
 
             // Linha em branco entre polos
             rowIndex++;
         }
     }
 
     private static void preencherAbaDPFolhaOrigem(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
         int rowIndex = sheet.getLastRowNum() + 2; // Pega a última linha preenchida
 
         for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
             String polo = entry.getKey();
             List<Funcionario> funcionarios = entry.getValue();
 
             // Cabeçalho do polo
             Row rowPolo = sheet.createRow(rowIndex++);
             rowPolo.createCell(0).setCellValue(polo);
 
             // Cabeçalho da tabela
             Row rowCabecalho = sheet.createRow(rowIndex++);
             rowCabecalho.createCell(0).setCellValue("CÓD.");
             rowCabecalho.createCell(1).setCellValue("DRT");
             rowCabecalho.createCell(2).setCellValue("NOME");
             rowCabecalho.createCell(3).setCellValue("GRATIFICAÇÃO (R$)");
 
             // Dados dos funcionários
             for (Funcionario funcionario : funcionarios) {
                 Row row = sheet.createRow(rowIndex++);
                 row.createCell(0).setCellValue(funcionario.getCodigo());
                 row.createCell(1).setCellValue(funcionario.getDrt());
                 row.createCell(2).setCellValue(funcionario.getNome());
                 row.createCell(3).setCellValue(funcionario.getGratificacaoValor());
             }
 
             // Linha em branco entre polos
             rowIndex++;
         }
     }
 }
 

/*
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeradorRelatorio {

    public static void gerarRelatorio(List<Funcionario> funcionarios, String caminhoSaida) {
        Map<String, List<Funcionario>> polosMap = dividirPorPolo(funcionarios); // Método para dividir por Polo

        try (Workbook workbook = new XSSFWorkbook()) {
            // Cria a aba DP FOLHA
            Sheet folhaDPFolha = workbook.createSheet("DP FOLHA");
            preencherAbaDPFolha(folhaDPFolha, polosMap);

            // Cria a aba DP FOLHA ORIGEM
            Sheet folhaDPFolhaOrigem = workbook.createSheet("DP FOLHA ORIGEM");
            preencherAbaDPFolhaOrigem(folhaDPFolhaOrigem, polosMap);

            // Salva o arquivo
            try (FileOutputStream fos = new FileOutputStream(caminhoSaida)) {
                workbook.write(fos);
                System.out.println("Relatório gerado com sucesso!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, List<Funcionario>> dividirPorPolo(List<Funcionario> funcionarios) {
        Map<String, List<Funcionario>> polosMap = new HashMap<>();
        for (Funcionario funcionario : funcionarios) {
            polosMap.computeIfAbsent(funcionario.getPolo(), k -> new ArrayList<>()).add(funcionario);
        }
        return polosMap;
    }

    private static void preencherAbaDPFolha(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        // Pega a última linha já preenchida
        int rowIndex = sheet.getLastRowNum() + 1;
    
        for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
            String polo = entry.getKey();
            List<Funcionario> funcionarios = entry.getValue();
    
            // Cabeçalho do polo
            Row rowPolo = sheet.createRow(rowIndex++);
            rowPolo.createCell(0).setCellValue(polo);
    
            // Cabeçalho da tabela
            Row rowCabecalho = sheet.createRow(rowIndex++);
            rowCabecalho.createCell(0).setCellValue("CÓD.");
            rowCabecalho.createCell(1).setCellValue("DRT");
            rowCabecalho.createCell(2).setCellValue("NOME");
            rowCabecalho.createCell(3).setCellValue("RECEBIMENTO (R$)");
            rowCabecalho.createCell(4).setCellValue("RENDIMENTO (%)");
            rowCabecalho.createCell(5).setCellValue("GRATIFICAÇÃO (%)");
            rowCabecalho.createCell(6).setCellValue("GRATIFICAÇÃO (R$)");
    
            // Dados dos funcionários
            for (Funcionario funcionario : funcionarios) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(funcionario.getCodigo());
                row.createCell(1).setCellValue(funcionario.getDrt());
                row.createCell(2).setCellValue(funcionario.getNome());
                row.createCell(3).setCellValue(funcionario.getRecebimento());
                row.createCell(4).setCellValue(funcionario.getRendimento() * 100);
                row.createCell(5).setCellValue(funcionario.getGratificacaoPercentual() * 100);
                row.createCell(6).setCellValue(funcionario.getGratificacaoValor());
            }
    
            // Linha em branco entre polos
            rowIndex++;
        }
    }
    
    private static void preencherAbaDPFolhaOrigem(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        // Pega a última linha já preenchida
        int rowIndex = sheet.getLastRowNum() + 1;
    
        for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
            String polo = entry.getKey();
            List<Funcionario> funcionarios = entry.getValue();
    
            // Cabeçalho do polo
            Row rowPolo = sheet.createRow(rowIndex++);
            rowPolo.createCell(0).setCellValue(polo);
    
            // Cabeçalho da tabela
            Row rowCabecalho = sheet.createRow(rowIndex++);
            rowCabecalho.createCell(0).setCellValue("CÓD.");
            rowCabecalho.createCell(1).setCellValue("DRT");
            rowCabecalho.createCell(2).setCellValue("NOME");
            rowCabecalho.createCell(3).setCellValue("GRATIFICAÇÃO (R$)");
    
            // Dados dos funcionários
            for (Funcionario funcionario : funcionarios) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(funcionario.getCodigo());
                row.createCell(1).setCellValue(funcionario.getDrt());
                row.createCell(2).setCellValue(funcionario.getNome());
                row.createCell(3).setCellValue(funcionario.getGratificacaoValor());
            }
    
            // Linha em branco entre polos
            rowIndex++;
        }
    }
 */
    /* private static void preencherAbaDPFolha(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        int rowIndex = 0;

        for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
            String polo = entry.getKey();
            List<Funcionario> funcionarios = entry.getValue();

            // Cabeçalho do polo
            Row rowPolo = sheet.createRow(rowIndex++);
            rowPolo.createCell(0).setCellValue(polo);

            // Cabeçalho da tabela
            Row rowCabecalho = sheet.createRow(rowIndex++);
            rowCabecalho.createCell(0).setCellValue("CÓD.");
            rowCabecalho.createCell(1).setCellValue("DRT");
            rowCabecalho.createCell(2).setCellValue("NOME");
            rowCabecalho.createCell(3).setCellValue("RECEBIMENTO (R$)");
            rowCabecalho.createCell(4).setCellValue("RENDIMENTO (%)");
            rowCabecalho.createCell(5).setCellValue("GRATIFICAÇÃO (%)");
            rowCabecalho.createCell(6).setCellValue("GRATIFICAÇÃO (R$)");

            // Dados dos funcionários
            for (Funcionario funcionario : funcionarios) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(funcionario.getCodigo());
                row.createCell(1).setCellValue(funcionario.getDrt());
                row.createCell(2).setCellValue(funcionario.getNome());
                row.createCell(3).setCellValue(funcionario.getRecebimento());
                row.createCell(4).setCellValue(funcionario.getRendimento() * 100);
                row.createCell(5).setCellValue(funcionario.getGratificacaoPercentual() * 100);
                row.createCell(6).setCellValue(funcionario.getGratificacaoValor());
            }

            // Linha em branco entre polos
            rowIndex++;
        }
    }

    private static void preencherAbaDPFolhaOrigem(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        int rowIndex = 0;

        for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
            String polo = entry.getKey();
            List<Funcionario> funcionarios = entry.getValue();

            // Cabeçalho do polo
            Row rowPolo = sheet.createRow(rowIndex++);
            rowPolo.createCell(0).setCellValue(polo);

            // Cabeçalho da tabela
            Row rowCabecalho = sheet.createRow(rowIndex++);
            rowCabecalho.createCell(0).setCellValue("CÓD.");
            rowCabecalho.createCell(1).setCellValue("DRT");
            rowCabecalho.createCell(2).setCellValue("NOME");
            rowCabecalho.createCell(3).setCellValue("GRATIFICAÇÃO (R$)");

            // Dados dos funcionários
            for (Funcionario funcionario : funcionarios) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(funcionario.getCodigo());
                row.createCell(1).setCellValue(funcionario.getDrt());
                row.createCell(2).setCellValue(funcionario.getNome());
                row.createCell(3).setCellValue(funcionario.getGratificacaoValor());
            }

            // Linha em branco entre polos
            rowIndex++;
        }
    } */
//}
