package com.tmkfolha.processor;

import java.util.List;
import java.util.Map;

import com.tmkfolha.app.controllers.Funcionario;

public interface FileProcessor {
    List<Funcionario> processFile(String filePath, String mes, int ano) throws Exception;
    Map<String, Map<String, String>> getData();
}



