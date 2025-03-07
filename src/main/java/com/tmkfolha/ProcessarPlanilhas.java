package com.tmkfolha;

import com.tmkfolha.processor.ExcelWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ProcessarPlanilhas {
    
    private final ExcelWriter excelWriter;

    public ProcessarPlanilhas() {
        this.excelWriter = new ExcelWriter();
    }

    public void enviarParaProcessamento(Map<String, Map<String, String>> dados) {
        // Verifica se há dados processados
        if (dados == null || dados.isEmpty()) {
            System.out.println("Nenhum dado processado para exibição.");
            return;
        }        

        // Exibe os dados no console para verificação
        System.out.println("\nDados extraídos:");
        for (Map.Entry<String, Map<String, String>> entry : dados.entrySet()) {
            System.out.println("Arquivo: " + entry.getKey());
            entry.getValue().forEach((chave, valor) -> 
                    System.out.println(chave + ": " + valor));
           // System.out.println("-------------------");
        }

        // Envia os dados para serem escritos no Excel
       // System.out.println("Em Procesar Planilha L32: " + dados);
        List<Map<String, Map<String, String>>> listaDeDados = new ArrayList<>();
        //listaDeDados.add(dados);
        
       // excelWriter.escreverDados(listaDeDados);
        //excelWriter.escreverDados(dados);
    }
    private void tratarDados(Map<String, Map<String, String>> dados) {
        // Realize o tratamento necessário nos dados aqui
        // Por exemplo, você pode iterar sobre o mapa e modificar os valores conforme necessário
        for (Map.Entry<String, Map<String, String>> entry : dados.entrySet()) {
            String identificador = entry.getKey();
            Map<String, String> valores = entry.getValue();

            // Exemplo de tratamento: adicionar um sufixo aos valores
            valores.replaceAll((k, v) -> v + " Tratado");
        }
    }

    private void exibirDadosTratados(Map<String, Map<String, String>> dados) {
        // Exiba os dados tratados no console
        for (Map.Entry<String, Map<String, String>> entry : dados.entrySet()) {
            String identificador = entry.getKey();
            Map<String, String> valores = entry.getValue();

            System.out.println("Identificador: " + identificador);
            for (Map.Entry<String, String> valorEntry : valores.entrySet()) {
                System.out.println("  " + valorEntry.getKey() + ": " + valorEntry.getValue());
            }
        }
    }
}
