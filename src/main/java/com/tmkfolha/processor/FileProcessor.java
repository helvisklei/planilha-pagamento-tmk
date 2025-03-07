package com.tmkfolha.processor;

import java.util.Map;

public interface FileProcessor {
    void processFile(String filePath) throws Exception;
    Map<String, Map<String, String>> getData();
}



