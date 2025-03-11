package com.tmkfolha.app;

import com.tmkfolha.processor.CsvProcessor;
import com.tmkfolha.processor.XlsProcessor;
import com.tmkfolha.util.BarraProgresso;
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
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Classe principal da aplicação que inicializa a interface gráfica e gerencia o processamento de arquivos.
 * Utiliza JavaFX para a interface do usuário e permite a seleção e processamento de arquivos CSV e XLS.
 */
public class MainApp extends Application {
    private ProgressBar progressBar; // Barra de progresso para indicar o progresso do processamento

    /**
     * Método de inicialização da interface gráfica da aplicação.
     * @param primaryStage Janela principal da aplicação.
     */
    @Override    
    public void start(Stage primaryStage) {
        this.progressBar = new ProgressBar(); // Inicializa a barra de progresso
        primaryStage.setTitle("Processador de Arquivos");

        // Carregar a imagem do logo
        Image image = new Image("file:src/main/resources/logo.png");
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(300);

        // Criar um Rectangle para usar como clip e arredondar as bordas da imagem
        Rectangle clip = new Rectangle(300, 300);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        imageView.setClip(clip);

        // Ajuste dinâmico do clip da imagem conforme necessário
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

        // Botão para finalizar a aplicação
        Button finalizeButton = new Button("Finalizar");
        Image finalizeIcon = new Image("file:src/main/resources/finalize.png");
        ImageView finalizeIconView = new ImageView(finalizeIcon);
        finalizeIconView.setFitHeight(25);
        finalizeIconView.setFitWidth(20);
        finalizeButton.setGraphic(finalizeIconView);
        finalizeButton.setOnAction(e -> primaryStage.close());

        // Configuração da barra de progresso
        progressBar.setProgress(0);
        progressBar.setPrefWidth(300);
        progressBar.setPrefHeight(25);

        // Layout principal
        VBox vbox = new VBox(10, imageView, loadButton, finalizeButton, progressBar);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color:rgb(236, 94, 94); -fx-padding: 10;");

        Scene scene = new Scene(vbox, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(300);
        primaryStage.setMinHeight(300);
        primaryStage.show();
    }

    /**
     * Método responsável por abrir um seletor de arquivos e processá-los.
     * @param primaryStage Janela principal da aplicação.
     */
    private void processFiles(Stage primaryStage) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos CSV e XLS", "csv", "xls", "xlsx");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            int totalFiles = files.length;
            int processedFiles = 0;

            // Objeto para atualizar a barra de progresso
            BarraProgresso barraProgresso = new BarraProgresso(progressBar);

            for (File file : files) {
                try {
                    String fileName = file.getName().toLowerCase().trim();
                    FileProcessor processor = null;

                    if (fileName.endsWith(".csv")) {
                        processor = new CsvProcessor();
                    } else if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {
                        if (!isValidXls(file)) {
                            System.err.println("O arquivo " + file.getName() + " não é um XLS válido. Pulando...");
                            continue;
                        }
                        processor = new XlsProcessor(barraProgresso);
                    } else {
                        System.err.println("Arquivo não suportado: " + file.getName());
                        continue;
                    }

                    processor.processFile(file.getAbsolutePath());
                    processedFiles++;
                    barraProgresso.atualizarProgresso(processedFiles, totalFiles);
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

    /**
     * Método principal que inicia a aplicação JavaFX.
     * @param args Argumentos da linha de comando.
     */
    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Verifica se um arquivo XLS é um documento OLE2 válido.
     * @param file Arquivo a ser verificado.
     * @return true se o arquivo for um XLS válido, false caso contrário.
     */
    private static boolean isValidXls(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            new POIFSFileSystem(fis).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}