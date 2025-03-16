package com.tmkfolha.app;

import org.junit.jupiter.api.Test;

import com.tmkfolha.app.controllers.Funcionario;
import com.tmkfolha.app.controllers.ProcessadorDados;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class Testes {

    @Test
    public void testProcessarDados() {
        // Dados simulados
        Map<String, Map<String, String>> fileData = new HashMap<>();

        // Simular dados de Nomes.xls
        Map<String, String> nomesData = new HashMap<>();
        nomesData.put("Linha 10 - Column0", "MSG");
        nomesData.put("Linha 10 - Column1", "129");
        nomesData.put("Linha 10 - Column2", "8388");
        nomesData.put("Linha 10 - Column3", "DIOGO ROGERIO DO NASCIMENTO P");
        nomesData.put("Linha 10 - Column4", "PETROLINA");
        nomesData.put("Linha 10 - Column5", "3");
        fileData.put("Nomes.xls", nomesData);

        // Simular dados de GERAL SITUAÇÃO MENS.xls
        Map<String, String> geralData = new HashMap<>();
        geralData.put("Linha 10 - Column4", "129");
        geralData.put("Linha 10 - Column5", "R$ 16.986,00");
        geralData.put("Linha 10 - Column14", "91,65 %");
        geralData.put("Linha 10 - Column15", "6,00 %");
        fileData.put("GERAL SITUAÇÃO MENS.xls", geralData);

        // Processar dados
        Map<String, List<Funcionario>> dadosPorPolo = ProcessadorDados.processarDados(fileData);

        // Verificar resultados
        assertNotNull(dadosPorPolo);
        assertTrue(dadosPorPolo.containsKey("PETROLINA"));

        List<Funcionario> funcionarios = dadosPorPolo.get("PETROLINA");
        assertEquals(1, funcionarios.size());

        Funcionario funcionario = funcionarios.get(0);
        assertEquals("129", funcionario.getCodigo());
        assertEquals("8388", funcionario.getDrt());
        assertEquals("DIOGO ROGERIO DO NASCIMENTO P", funcionario.getNome());
        assertEquals(16986.0, funcionario.getRecebimento());
        assertEquals(0.9165, funcionario.getRendimento());
        assertEquals(0.06, funcionario.getGratificacaoPercentual());
        assertEquals(1019.16, funcionario.getGratificacaoValor(), 0.01); // Tolerância para valores decimais
    }
}