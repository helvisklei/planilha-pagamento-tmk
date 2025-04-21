package com.tmkfolha.app.controllers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.tmkfolha.app.controllers.Operadora;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ProcessadorOperadoras {
    public static Map<String, Operadora> processarArquivoOperadoras(String caminhoArquivo) throws IOException {
        Map<String, Operadora> operadoras = new HashMap<>();
        
        try (Workbook workbook = WorkbookFactory.create(new File(caminhoArquivo))) {
            Sheet sheet = workbook.getSheetAt(0);
            
            for (Row row : sheet) {
                if (row.getRowNum() < 6) continue; // Pular cabeçalho
                
                Cell cellNome = row.getCell(0);
                if (cellNome == null || cellNome.getStringCellValue().isEmpty()) continue;
                
                String[] partesNome = cellNome.getStringCellValue().split(" ", 3);
                if (partesNome.length < 3) continue;
                
                String codigo = partesNome[0];
                String nome = partesNome[1] + (partesNome.length > 2 ? " " + partesNome[2] : "");
                String tipoRecebimento = partesNome[partesNome.length-1]; // BOL ou REC
                
                // Verificar se já existe a operadora no mapa
                Operadora operadora = operadoras.getOrDefault(codigo, new Operadora(codigo, nome));
                
                // Processar valores
                if (row.getCell(1) != null) {
                    double quantidade = row.getCell(1).getNumericCellValue();
                    double valor = row.getCell(2) != null ? 
                        row.getCell(2).getNumericCellValue() : 0;
                    
                    if ("BOL".equals(tipoRecebimento)) {
                       // operadora.addBoleto(quantidade, valor);
                    } else if ("REC".equals(tipoRecebimento)) {
                       // operadora.addRecibo(quantidade, valor);
                    }
                }
                
                operadoras.put(codigo, operadora);
            }
        }
        
        return operadoras;
    }
}
