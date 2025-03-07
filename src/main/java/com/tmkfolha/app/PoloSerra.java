package com.tmkfolha.app;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PoloSerra {

    private static String lerArquivoExcel(String caminhoArquivo) throws IOException {
        FileInputStream file = new FileInputStream(new File(caminhoArquivo));
        Workbook workbook = new XSSFWorkbook(file);
        Sheet sheet = workbook.getSheetAt(0); // Primeira aba

        Row row = sheet.getRow(1); // Segunda linha (índice 1)
        Cell cell = row.getCell(1); // Segunda coluna (índice 1)

        String valor = cell.toString();
        workbook.close();
        return valor;
    }

    public static void main(String[] args) {
        // Criação do JFileChooser para selecionar o arquivo
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecione o arquivo Polo2.XLS");

        // Filtra apenas arquivos .xls e .xlsx
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos Excel", "xls", "xlsx");
        fileChooser.setFileFilter(filter);

        // Exibe o diálogo de seleção
        int returnValue = fileChooser.showOpenDialog(null);

        // Verifica se o usuário selecionou um arquivo
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();  // Obtém o arquivo selecionado
            String caminhoArquivo = file.getAbsolutePath();  // Obtém o caminho completo do arquivo

            System.out.println("Lendo arquivo: " + caminhoArquivo);

            try {
                // Chama a função para ler o arquivo e imprime o valor selecionado
                String valorSelecionado = lerArquivoExcel(caminhoArquivo);
                System.out.println("Valor selecionado do arquivo: " + valorSelecionado);
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo!");
                e.printStackTrace();
            }
        } else {
            System.out.println("Nenhum arquivo foi selecionado.");
        }
    }
}
