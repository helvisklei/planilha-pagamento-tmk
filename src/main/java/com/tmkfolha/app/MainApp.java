package com.tmkfolha.app;

import com.tmkfolha.app.controllers.Funcionario;
import com.tmkfolha.app.controllers.GeradorRelatorio;
import com.tmkfolha.app.controllers.RelatorioGeral;
import com.tmkfolha.processor.XlsProcessor;
import com.tmkfolha.util.BarraProgresso;
import com.tmkfolha.processor.FileProcessor;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.ProgressBar;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JProgressBar;

/**
 * Classe principal da aplicação que inicializa a interface gráfica e gerencia o processamento de arquivos.
 * Utiliza JavaFX para a interface do usuário e permite a seleção e processamento de arquivos CSV e XLS.
 */
public class MainApp extends Application {
    private ProgressBar progressBar; // Barra de progresso para indicar o progresso do processamento
    private ComboBox<String> monthComboBox;
    private ComboBox<Integer> yearComboBox;

    /**
     * Método de inicialização da interface gráfica da aplicação.
     * @param primaryStage Janela principal da aplicação.
     */
    @Override    
    public void start(Stage primaryStage) {
        this.progressBar = new ProgressBar(); // Inicializa a barra de progresso
        primaryStage.setTitle("Processador de Arquivos");

         // Configuração dos ComboBox para mês e ano
        monthComboBox = new ComboBox<>();
        monthComboBox.getItems().addAll(
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
        );
        monthComboBox.setPromptText("Mês");
        monthComboBox.setPrefWidth(100);

        yearComboBox = new ComboBox<>();
        int currentYear = Year.now().getValue();
        for (int year = currentYear - 10; year <= currentYear + 5; year++) {
            yearComboBox.getItems().add(year);
        }
        yearComboBox.setValue(currentYear); // Ano atual como padrão
        yearComboBox.setPromptText("Ano");
        yearComboBox.setPrefWidth(70);

        // Layout para os novos controles
        HBox dateSelectionBox = new HBox(5, monthComboBox, yearComboBox);
        dateSelectionBox.setAlignment(Pos.CENTER);


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
        VBox vbox = new VBox(10, imageView, loadButton, dateSelectionBox, finalizeButton, progressBar);
        vbox.setAlignment(Pos.CENTER);
        vbox.setStyle("-fx-background-color:rgb(255, 28, 28); -fx-padding: 10;");

        Scene scene = new Scene(vbox, 400, 400);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(500);
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
            // Ordena os arquivos conforme o mapa de prioridade
            List<File> arquivosOrdenados = new ArrayList<>();
            for (File f : files) {
                arquivosOrdenados.add(f);
            }
            arquivosOrdenados.sort(Comparator.comparingInt(
                f -> ordemPrioridade.getOrDefault(f.getName().toLowerCase().trim(), Integer.MAX_VALUE)));
            
            int totalFiles = files.length;
            BarraProgresso barraProgresso = new BarraProgresso(progressBar);
            XlsProcessor xlsProcessor = new XlsProcessor(barraProgresso);
            List<Funcionario> funcionarios = new ArrayList<>();           
            
            for (int i = 0; i < arquivosOrdenados.size(); i++) {
                File file = arquivosOrdenados.get(i);
                try {                    
                    String fileName = file.getName().toLowerCase().trim();
                    FileProcessor processor = null;
                    
                    if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {                        
                        if (!isValidXls(file)) {
                            System.err.println("O arquivo " + file.getName() + " não é um XLS válido. Pulando...");
                            continue;
                        }
                        
                        String mes = monthComboBox.getValue() != null ? 
                            monthComboBox.getValue() : 
                            Month.of(LocalDate.now().getMonthValue()).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                        int ano = yearComboBox.getValue() != null ? 
                            yearComboBox.getValue() : 
                            Year.now().getValue();
                        
                        // Atualiza progresso
                        int progresso = (int) (((i + 1) / (double) arquivosOrdenados.size()) * 100);
                        barraProgresso.atualizarProgresso(progresso, 100);
                        
                        List<Funcionario> result = xlsProcessor.processFile(file.getAbsolutePath(), mes, ano);
                        funcionarios = result;//funcionarios.addAll(result)
                        
                        System.out.println("Arquivo processado com sucesso: " + file.getName());
                    } else {
                        System.err.println("Arquivo não suportado: " + file.getName());
                        continue;
                    }
                } catch (IOException e) {
                    System.err.println("Erro de I/O ao processar o arquivo " + file.getName() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Erro inesperado ao processar o arquivo " + file.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            if (!funcionarios.isEmpty()) {
                GeradorRelatorio gerador = new GeradorRelatorio();
                gerador.gerarRelatorio(funcionarios, "RelatorioConsolidado.xlsx");

                // Gera também o relatório específico do mês/ano
                String mes = monthComboBox.getValue() != null ? 
                    monthComboBox.getValue() : 
                    Month.of(LocalDate.now().getMonthValue()).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                int ano = yearComboBox.getValue() != null ? yearComboBox.getValue() : Year.now().getValue();
                
                String nomeRelatorio = String.format("Relatorio_%s_%d.xlsx", mes.toLowerCase(), ano);
                RelatorioGeral.gerarRelatorio(funcionarios, 
                    System.getProperty("user.dir") + "/Saida/FOLHA_PAGAMENT_TMK_" + nomeRelatorio, mes, ano);
                System.out.println("Relatório gerado com sucesso!");
            } else {
                System.out.println("Nenhum dado foi processado.");
            }
            System.out.println("Processamento concluído!");
        } else {
            System.out.println("Nenhum arquivo foi selecionado.");
        }
    }
/*     private void processFiles(Stage primaryStage) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Arquivos CSV e XLS", "csv", "xls", "xlsx");
        fileChooser.setFileFilter(filter);

        int returnValue = fileChooser.showOpenDialog(null);

        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            // Ordena os arquivos conforme o mapa de prioridade
            List<File> arquivosOrdenados = new ArrayList<>();
            for (File f : files) {
                arquivosOrdenados.add(f);
            }
            arquivosOrdenados.sort(Comparator.comparingInt(
                f -> ordemPrioridade.getOrDefault(f.getName().toLowerCase().trim(), Integer.MAX_VALUE)));
            int totalFiles = files.length;
            int processedFiles = 0;

            // Objeto para atualizar a barra de progresso
            BarraProgresso barraProgresso = new BarraProgresso(progressBar);
            XlsProcessor xlsProcessor = new XlsProcessor(barraProgresso);
            List<Funcionario> funcionarios = new ArrayList<>();

           
            for (File file : arquivosOrdenados) {
                try {                    
                    String fileName = file.getName().toLowerCase().trim();
                    FileProcessor processor = null; */
                   /*  if (fileName.endsWith(".csv")) {
                        processor = new CsvProcessor();
                    } else */
/*                     if (fileName.endsWith(".xls") || fileName.endsWith(".xlsx")) {                        
                        if (!isValidXls(file)) {
                            System.err.println("O arquivo " + file.getName() + " não é um XLS válido. Pulando...");
                            continue;
                        } // Obter valores dos ComboBox antes do processamento
                        String mes = monthComboBox.getValue() != null ? 
                            monthComboBox.getValue() : 
                            Month.of(LocalDate.now().getMonthValue()).getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
                        int ano = yearComboBox.getValue() != null ? 
                            yearComboBox.getValue() : 
                            Year.now().getValue();

                            int totalArquivos = arquivosOrdenados.size();
                            for (int i = 0; i < totalArquivos; i++) {
                                 file = arquivosOrdenados.get(i);
                            
                                // Atualiza barra de progresso
                                int progresso = (int) (((i + 1) / (double) totalArquivos) * 100);
                               // progressBar.setViewOrder(progresso); //setValue(progresso);
                                barraProgresso.atualizarProgresso(progresso, 100);
                            
                                // Processa arquivo
                                List<Funcionario> result = xlsProcessor.processFile(file.getAbsolutePath(), mes, ano);
                                funcionarios = result;
                                //funcionarios.addAll(result);
                            }    
                             */

                       /*  // Processa o arquivo e atualiza a lista de funcionários
                        List<Funcionario> result = xlsProcessor.processFile(file.getAbsolutePath(),mes, ano);
                        if (result != null && !result.isEmpty()) {
                            // Se for o arquivo de nomes, substitui a lista
                            if (fileName.equals("nomes.xls")) {
                                funcionarios = result;
                            }
                        } */
/*                         processedFiles++;
                        //barraProgresso.atualizarProgresso(processedFiles, totalFiles);
                        System.out.println("Arquivo processado com sucesso: " + file.getName());
                    } else {
                        System.err.println("Arquivo não suportado: " + file.getName());
                        continue;
                    } */
                    /*  // Ajuste o limite de segurança para evitar "Zip bomb detected"
                    ZipSecureFile.setMinInflateRatio(0.009); // ◀️ SOLUÇÃO

                    processor.processFile(file.getAbsolutePath());
                    processedFiles++;
                    barraProgresso.atualizarProgresso(processedFiles, totalFiles); */
                    //System.out.println("Arquivo processado com sucesso: " + file.getName());
   /*              } catch (IOException e) {
                    System.err.println("Erro de I/O ao processar o arquivo " + file.getName() + ": " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("Erro inesperado ao processar o arquivo " + file.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
             // Após processar todos os arquivos, gere o relatório consolidado
            if (!funcionarios.isEmpty()) {
                GeradorRelatorio gerador = new GeradorRelatorio();
                gerador.gerarRelatorio(funcionarios, "RelatorioConsolidado.xlsx");
                System.out.println("Relatório gerado com sucesso!");
            } else {
                System.out.println("Nenhum dado foi processado.");
            }
            System.out.println("Processamento concluído!");
        } else {
            System.out.println("Nenhum arquivo foi selecionado.");
        }
    }
 */
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
        private static final Map<String, Integer> ordemPrioridade = Map.ofEntries(
            Map.entry("nomes.xls", 1),
            Map.entry("setor 2 serra.xls", 2),
            Map.entry("setor 3 petrolina.xls", 3),
            Map.entry("setor 4 matriz.xls", 4),
            Map.entry("setor 5 caruaru.xls", 5),
            Map.entry("setor 6 garanhuns.xls", 6),
            Map.entry("geral.xls", 7),
            Map.entry("meta rec.xls", 8),
            Map.entry("recebido por operador.xls", 9),
            Map.entry("fn.xls", 10),
            Map.entry("geral situação mens.xls", 11),
            Map.entry("tabela-gratificacao.xls", 12)
        );
        private static Comparator<File> comparadorPrioridade = Comparator.comparingInt(file -> {
            String nome = file.getName().toLowerCase();
            return ordemPrioridade.entrySet().stream()
                .filter(entry -> nome.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(Integer.MAX_VALUE); // Arquivos não mapeados vão pro final
        });
}