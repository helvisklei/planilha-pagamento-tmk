package com.tmkfolha.service;

import java.util.List;

public class DataValidator {

    public static boolean validateData(List<String[]> data) {
        // Implementar as validações necessárias
        for (String[] row : data) {
            if (row.length < 3) {
                return false; // Exemplo de validação: uma linha precisa ter pelo menos 3 colunas
            }
        }
        return true;
    }
}

