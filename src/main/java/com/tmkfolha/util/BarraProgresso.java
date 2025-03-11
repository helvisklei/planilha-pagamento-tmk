package com.tmkfolha.util;

import javafx.scene.control.ProgressBar;

public class BarraProgresso {
    private ProgressBar progressBar;

    // Construtor com o ProgressBar passado como parâmetro
    public BarraProgresso(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    // Construtor sem parâmetros, inicializando o ProgressBar internamente
    public BarraProgresso() {
        this.progressBar = new ProgressBar(0);  // Inicializa com progresso 0
    }

    public void atualizarProgresso(int processedFiles, int totalFiles) {
        double progresso = (double) processedFiles / totalFiles;
        progressBar.setProgress(progresso); // Atualiza a barra de progresso
    }

    public ProgressBar getProgressBar() {
        return progressBar; // Para acessar o ProgressBar, se necessário
    }

}
