package com.tmkfolha.app;

import com.tmkfolha.processor.CsvProcessor;
import com.tmkfolha.processor.XlsProcessor;
import com.tmkfolha.processor.FileProcessor;
import com.tmkfolha.processor.ProcessXlsMapper;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ProgressBar;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Configuração da janela principal
        primaryStage.setTitle("Processador de Arquivos");

        // Carregar a imagem do logo
        Image image = new Image("file:src/main/resources/logo.png");
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(300); // Ajuste o tamanho conforme necessário

        // Criar um Rectangle para usar como clip e arredondar as bordas
        Rectangle clip = new Rectangle(300, 300); // Inicialmente, defina o tamanho do cli
       // clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
        clip.setArcWidth(30);  // Ajuste o valor para arredondar mais ou menos as bordas
        clip.setArcHeight(30); // Ajuste o valor para arredondar mais ou menos as bordas
        imageView.setClip(clip);

        // Adicionar listener para ajustar o tamanho do clip dinamicamente
        imageView.fitWidthProperty().addListener((observable, oldValue, newValue) -> {
            clip.setWidth(newValue.doubleValue());
        });
        imageView.fitHeightProperty().addListener((observable, oldValue, newValue) -> {
            clip.setHeight(newValue.doubleValue());
        });


        // Verifica se a imagem foi carregada corretamente
        if (image.isError()) {
            System.err.println("Erro ao carregar a imagem: " + image.getException().getMessage());
        }

        // Botão para carregar arquivos
        Button loadButton = new Button("Carregar Arquivos");
        Image loadIcon = new Image("file:src/main/resources/load.png");
        ImageView loadIconView = new ImageView(loadIcon);
        loadIconView.setFitHeight(25);
        loadIconView.setFitWidth(25);
        loadButton.setGraphic(loadIconView);
        loadButton.setOnAction(e -> processFiles(primaryStage));

        // Botão para finalizar
        Button finalizeButton = new Button("Finalizar");
        Image finalizeIcon = new Image("file:src/main/resources/finalize.png");
        ImageView finalizeIconView = new ImageView(finalizeIcon);
        finalizeIconView.setFitHeight(25);
        finalizeIconView.setFitWidth(20);
        finalizeButton.setGraphic(finalizeIconView);
        finalizeButton.setOnAction(e -> primaryStage.close());

        // Barra de progresso
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(0); // Inicia com 0%
        progressBar.setPrefWidth(300); // Largura da barra de progresso
        progressBar.setPrefHeight(25);

        // Layout com espaçamento ajustado
        VBox vbox = new VBox(10, imageView, loadButton, finalizeButton, progressBar);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color:rgb(238, 10, 10); -fx-padding: 10;");

        // Definindo o tamanho mínimo da cena para garantir boa visualização
        Scene scene = new Scene(vbox, 400, 400); // Tamanho maior para garantir uma boa visualização

        // Exibe a interface
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(300); // Definir largura mínima
        primaryStage.setMinHeight(300); // Definir altura mínima
        primaryStage.show();
    }

    private void processFiles(Stage primaryStage) {
        // Criação do JFileChooser para escolher múltiplos arquivos
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);  // Permitir seleção múltipla de arquivos

        // Filtra apenas arquivos CSV e XLS
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos CSV e XLS", "csv", "xls", "xlsx");
        fileChooser.setFileFilter(filter);

        // Exibe o dialog para selecionar arquivos
        int returnValue = fileChooser.showOpenDialog(null);

        // Verifica se o usuário selecionou arquivos
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();  // Obtém os arquivos selecionados
            int totalFiles = files.length; // Número total de arquivos a serem processados
            int processedFiles = 0; // Contador de arquivos processados

            // Barra de progresso
            ProgressBar progressBar = (ProgressBar) primaryStage.getScene().getRoot().getChildrenUnmodifiable().get(3);

            // Loop para processar os arquivos selecionados
            for (File file : files) {
                try {
                    // Verifica a extensão do arquivo
                    String fileName = file.getName().toLowerCase().toString().trim();
                    FileProcessor processor;

                    if (fileName.endsWith(".csv")) {
                        processor = new CsvProcessor();
                    } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                        processor = new XlsProcessor();
                       // System.out.println("Arquivo XLS ou XLSX:  L128 Mainapp" + processor);
                    } else {
                        System.err.println("Arquivo não suportado: " + file.getName());
                        continue; // Pula para o próximo arquivo se não for CSV ou XLS
                    }

                    //System.out.println("Processando arquivo: " + file.getAbsolutePath());
                                        
                    processor.processFile(file.getAbsolutePath());  // Processa o arquivo

                    // Atualiza a barra de progresso
                    processedFiles++;
                    progressBar.setProgress((double) processedFiles / totalFiles); // Atualiza o progresso

                    System.out.println("Arquivo processado com sucesso: " + file.getName());
                } catch (IOException e) {
                    System.err.println("Erro de I/O ao processar o arquivo " + file.getName() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Erro inesperado ao processar o arquivo " + file.getName() + ": " + e.getMessage());
                }
            }
            System.out.println("Processamento concluído!");
        } else {
            System.out.println("Nenhum arquivo foi selecionado.");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}