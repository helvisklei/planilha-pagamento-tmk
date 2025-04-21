package com.tmkfolha.processor;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.CodingErrorAction;
import java.text.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.tmkfolha.app.controllers.CacheDadosFuncionarios;
import com.tmkfolha.app.controllers.FiltradorDados;
import com.tmkfolha.app.controllers.Funcionario;
import com.tmkfolha.app.controllers.Funcionario.SituacaoOperadora;
import com.tmkfolha.app.controllers.Funcionarios;
import com.tmkfolha.app.controllers.GeradorRelatorio;
import com.tmkfolha.app.controllers.GeralSituacaoMensal;
import com.tmkfolha.app.controllers.GratificacaoCalculator;
import com.tmkfolha.app.controllers.IntegradorQuantidadeValor;
import com.tmkfolha.app.controllers.MetaParaReciboPorOperador;
import com.tmkfolha.app.controllers.Operadora;
import com.tmkfolha.app.controllers.RecebidoOperador;
import com.tmkfolha.app.controllers.RelatorioGeral;
import com.tmkfolha.app.controllers.XlsData;
import com.tmkfolha.util.BarraProgresso;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.math.BigDecimal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.tmkfolha.app.controllers.DemonstrativoDados;

/**
 * Classe responsável por processar arquivos XLS e XLSX.
 * Lê os dados, processa e formata para saída em um novo Excel.
 */
public class XlsProcessor implements FileProcessor {
    private static boolean nomesProcessados = false; // Flag para controlar o processamento
    private List<Funcionario> listaFuncionarios = new ArrayList<>();   
    private XlsProcessor xlsProcessor; 

    private Map<String, Map<String, String>> tabelaGratificacao = new HashMap<>(); // mama tabela de gratificação
    private Map<String, BigDecimal> recebidoOperadorPorCodigo = new HashMap<>();
    private Map<String, BigDecimal> gratificacaoPorCodigo = new HashMap<>();
    private Map<String, String> situacaoMensageirosPorCodigo = new HashMap<>();


    //private static final java.util.logging.Logger logger = LogManager.getLogger(XlsProcessor.class);
    private static final Logger logger = LogManager.getLogger(XlsProcessor.class);
    private CacheDadosFuncionarios cache;

    private BarraProgresso barraProgresso;
    private List<Map<String, String>> extractedData = new ArrayList<>();

    private TreeMap<String, Map<String, String>> fileData = new TreeMap<>();
    private Map<String, List<XlsData>> processedData = new TreeMap<>();

    private Map<String, List<String>> resultadosParaExcel = new HashMap<>();
    private DataProcessor dataProcessor = new DataProcessor(); // Integração com DataProcessor
    
    private static Map<String, Double> metasPorCodigo = new HashMap<>();    

    private static Map<String, Map<String, String>> dadosFiltradosOperadoras = new HashMap<>();
    Map<String, Map<String, String>> dadosFiltradosGeralSituacaoMens = new HashMap<>();
    
    private static List<String> colunasRelevantesSituacaoMensageiros = Arrays.asList(
            "Column0", "Column1", "Column6", "Column7", "Column14", "Column15"); 

    private static List<String> colunasRelevantesOperadoras = Arrays.asList(
            "Column0", "Column3", "Column8"); //

    private static List<String> colunasDeMetasRelevantesOperadoras = Arrays.asList(
            "Column0", "Column3");    
            

    private static final String TOTAL_RECEBIDO_C_C_BOLETOS = "Total  (Recebido-Cheque pré+Cheque Custódia+Boletos ) :";    

    // Polos
    private static final String POLO_SERRA_TALHADA = "SERRA TALHADA";
    private static final String POLO_PETROLINA = "PETROLINA";
    private static final String POLO_MATRIZ = "MATRIZ";
    private static final String POLO_CARUARU = "CARUARU";
    private static final String POLO_GARANHUNS = "GARANHUNS";

    private static String nomeDoArquivo = "";

    // Administrativo
    private static final String ADM = "ADM";

    // OPERAÇÃO
    private static final String OPE = "OPERADORA";

    public XlsProcessor(BarraProgresso barraProgresso) {
        this.barraProgresso = barraProgresso;
    }  

    public List<Funcionario> processFile(String filePath,String mesSelecionado, int anoSelecionado) throws Exception {
        File arquivo = new File(filePath);
        String nomeArquivo = arquivo.getName().toLowerCase();
        //String mes = mesSelecionado;
        //int ano = anoSelecionado;
        
        // Verificação inicial da lista de funcionários
        if (listaFuncionarios == null) {
            listaFuncionarios = new ArrayList<>();
        }   
        
        // Se já processou nomes.xls e está tentando processar de novo, ignora
        if (nomeArquivo.equals("nomes.xls") && nomesProcessados) {
            logger.info("Arquivo Nomes.xls já foi processado anteriormente - ignorando");
            return listaFuncionarios;
        }        
        
        try {

            if (nomeArquivo.equals("nomes.xls") && !nomesProcessados) {            
                listaFuncionarios = processarNomes(arquivo);
                nomesProcessados = true;
                logger.info("Arquivo Nomes.xls processado com {} funcionários", listaFuncionarios.size());
               // return listaFuncionarios;       
            }  // Verifica se nomes foram processados para arquivos que dependem dele
            // Processamento dos demais arquivos
            else if (nomeArquivo.startsWith("setor") || nomeArquivo.equals("geral.xls")) {
                // Garante que os nomes foram processados primeiro
                if (!nomesProcessados) {
                    File arquivoNomes = new File(arquivo.getParentFile(), "Nomes.xls");
                    if (arquivoNomes.exists()) {
                        listaFuncionarios = processarNomes(arquivoNomes);
                        nomesProcessados = true;
                        logger.info("Nomes.xls carregado automaticamente antes de {}", nomeArquivo);
                    } else {
                        throw new IllegalStateException("Arquivo Nomes.xls não encontrado para processar " + nomeArquivo);
                    }
                }
                
                if (listaFuncionarios.isEmpty()) {
                    logger.warn("Lista de funcionários vazia ao processar {}", nomeArquivo);
                } else {
                    processarDemonstrativosCaixa(arquivo, listaFuncionarios);
                }
                
            }  else if (nomeArquivo.equals("meta rec.xls") || 
                      nomeArquivo.equals("geral situação mens.xls") || 
                      nomeArquivo.equals("recebido por operador.xls") || 
                      nomeArquivo.equals("tabela-gratificacao.xls") ||
                      nomeArquivo.equals("fn.xls")) {
                
                // Verificação rigorosa para arquivos que dependem dos nomes
                if (!nomesProcessados) {
                    File arquivoNomes = new File(arquivo.getParentFile(), "Nomes.xls");
                    if (arquivoNomes.exists()) {
                        listaFuncionarios = processarNomes(arquivoNomes);
                        nomesProcessados = true;
                        logger.info("Nomes.xls carregado automaticamente antes de {}", nomeArquivo);
                    } else {
                        throw new IllegalStateException("Arquivo Nomes.xls deve ser processado primeiro");
                    }
                }
                
                 if (listaFuncionarios.isEmpty()) {
                    logger.error("Lista de funcionários vazia - não é possível processar {}", nomeArquivo);
                    return listaFuncionarios;
                }
                
                // Processa o arquivo específico
                if (nomeArquivo.equals("meta rec.xls")) {
                    processarMetaRec(arquivo, listaFuncionarios);
                } else if (nomeArquivo.equals("geral situação mens.xls")) {
                    processarSituacaoMensal(arquivo, listaFuncionarios);
                }else if (nomeArquivo.equals("recebido por operador.xls")) {
                    processarRecebidoOperador(arquivo, listaFuncionarios);
                } else if (nomeArquivo.equalsIgnoreCase("FN.xls") || nomeArquivo.equalsIgnoreCase("fn.xls") || nomeArquivo.equalsIgnoreCase("FN RECEBIDA GERENCIAL.xls")) {
                    processarFichaNova(arquivo, listaFuncionarios);
                } else if (nomeArquivo.equals("tabela-gratificacao.xls")) {
                    processarTabelaGratificacao(arquivo, listaFuncionarios);
                }
            } else {
                logger.warn("Tipo de arquivo não reconhecido: {}", nomeArquivo);
            } 

            logger.info("Arquivo processado com sucesso");

           // String nomeRelatorio = String.format("Relatorio_%s_%d.xlsx", mes.toLowerCase(), ano);
           // RelatorioGeral.gerarRelatorio(listaFuncionarios, System.getProperty("user.dir") + "/Saida/FOLHA_PAGAMENT_TMK_" + nomeRelatorio,mes, ano);
 
            return listaFuncionarios; 
            
         } catch (Exception e) {
            logger.error("Erro ao processar arquivo {}: {} ", nomeArquivo, e.getMessage(), e);
            throw e;
        } 
        
    }

   private void processarTabelaGratificacao(File arquivo, List<Funcionario> funcionarios) throws Exception {
        try (FileInputStream fis = new FileInputStream(arquivo);
            Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Processa cada linha a partir do cabeçalho (linha 0)
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pula cabeçalho
                
                Map<String, String> rawData = new HashMap<>();
                rawData.put("Column0", getCellStringValue(row, 0)); // FUNÇÃO
                BigDecimal percentual = new BigDecimal(rawData.put("Column1", getCellStringValue(row, 1))); // PERCENTUAL
                rawData.put("Column2", getCellStringValue(row, 2)); // CARGOS
                rawData.put("Column3", getCellStringValue(row, 3)); // SETOR

                System.out.println("L257 " + rawData);

                GratificacaoCalculator calculator = new GratificacaoCalculator(rawData);
                
                // Armazena os dados para processamento posterior
                Map<String, String> dadosGratificacao = new HashMap<>();
                dadosGratificacao.put("Funcao", calculator.getCodigo());
                dadosGratificacao.put("Percentual", calculator.getFatorGratificacao().toString());
                dadosGratificacao.put("Cargos", calculator.getNome());
                dadosGratificacao.put("Setor", rawData.get("Column3"));

                //System.out.println("TESTE TESTE");
                
                //tabelaGratificacao.put(calculator.getCodigo(), dadosGratificacao);
                for (Funcionario func : funcionarios) {        
                    // Aplica a gratificação
                   /*  if (gratificacaoPorCodigo.containsKey(func.getSetor())) {
                        func.setGratificacaoPercentual(gratificacaoPorCodigo.get(func.getSetor()));            
                    } */
                   Boolean p = gratificacaoPorCodigo.containsKey(func.getSetor());
                    System.out.println("TESTE " + p);
                    System.out.println("Codigo Funcionario "+ func.getCodigo() + "Percentual "+  percentual);
                    // Armazena no mapa intermediário
                    gratificacaoPorCodigo.put(func.getCodigo(), percentual);
                }  
                
                
                XlsData gratificacao = XlsData.createFromFile(arquivo.getName(), rawData);

                logger.debug("TESTE {} ", gratificacao.getNome());
                logger.debug("TESTE2 {} ", gratificacao.getCodigo());
                logger.info("TAMANHO3 {} ", dadosGratificacao.size());
            }

            for (Funcionario func : funcionarios) {        
                // Aplica a gratificação
                if (gratificacaoPorCodigo.containsKey(func.getSetor())) {
                    func.setGratificacaoPercentual(gratificacaoPorCodigo.get(func.getSetor()));
        
                }
            }    
            
            // Aplica as gratificações aos funcionários
            aplicarGratificacoes(funcionarios, tabelaGratificacao);
            
        } catch (Exception e) {
            logger.error("Erro ao processar arquivo Tabela-Gratificacao", e);
            throw e;
        }
    }

    private void aplicarGratificacoes(List<Funcionario> funcionarios, Map<String, Map<String, String>> tabelaGratificacao) {
        // Primeiro, converte a tabela para o formato esperado pelo GratificacaoCalculator
        Map<String, Double> percentuais = new HashMap<>();
        Map<String, Map<String, String>> dadosParaCalculo = new HashMap<>();
        
        for (Funcionario func : funcionarios) {
            Map<String, String> dadosFunc = new HashMap<>();
            dadosFunc.put("Categoria", func.getTipo());
            dadosFunc.put("Polo", func.getPolo());
            
            // Adiciona dados específicos por categoria
            if ("MENSAGEIRO".equalsIgnoreCase(func.getTipo())) {
                dadosFunc.put("Column14", String.valueOf(func.getRendimento()));
            } else if ("OPERADORA".equalsIgnoreCase(func.getTipo())) {
                dadosFunc.put("Recebido", String.valueOf(func.getRecebidoOperador()));
            } else if ("ADMINISTRATIVO".equalsIgnoreCase(func.getTipo())) {
                // Supondo que o valor do demonstrativo está no mapa de demonstrativos
                double valorDemonstrativo = func.getDemonstrativosCaixa().values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
                dadosFunc.put("ValorDemonstrativo", String.valueOf(valorDemonstrativo));
            }
            
            dadosParaCalculo.put(func.getCodigo(), dadosFunc);
            
            // Obtém o percentual correto da tabela
            String funcaoTabela = determinarFuncaoTabela(func, tabelaGratificacao);
            if (funcaoTabela != null && tabelaGratificacao.containsKey(funcaoTabela)) {
                percentuais.put(func.getCodigo(), 
                    Double.parseDouble(tabelaGratificacao.get(funcaoTabela).get("Percentual")));
            }
        }
        
        // Usa o GratificacaoCalculator para processar os dados
        GratificacaoCalculator.processarDados(dadosParaCalculo, percentuais);
        
        // Atualiza os funcionários com os valores calculados
        for (Funcionario func : funcionarios) {
            String funcaoTabela = determinarFuncaoTabela(func, tabelaGratificacao);
            if (funcaoTabela != null && tabelaGratificacao.containsKey(funcaoTabela)) {
                BigDecimal percentual =new BigDecimal(tabelaGratificacao.get(funcaoTabela).get("Percentual"));
                func.setGratificacaoPercentual(percentual);
                
                // Calcula o valor específico conforme a categoria
                BigDecimal gratificacao = BigDecimal.ZERO;
                switch (func.getTipo().toUpperCase()) {
                    case "MENSAGEIRO":
                        gratificacao = GratificacaoCalculator.calcularGratificacaoMensageiro(
                            func.getRendimento(), func.getPolo());
                        break;
                    case "OPERADORA":
                        gratificacao = GratificacaoCalculator.calcularGratificacaoOperadora(
                            func.getRecebidoOperador(),percentual);
                        break;
                    case "ADMINISTRATIVO":
                        BigDecimal valorDemonstrativo = func.getDemonstrativosCaixa().values().stream()                   
                            .map(BigDecimal::valueOf) // Converte Double para BigDecimal                                             
                            .reduce(BigDecimal.ZERO,BigDecimal::add);// Soma corretamente como BigDecimal
                        gratificacao = GratificacaoCalculator.calcularGratificacaoAdministrativo(
                            valorDemonstrativo, percentual);
                        break;
                }
                
                func.setGratificacao(gratificacao);
            }
        }
    }

    private String determinarFuncaoTabela(Funcionario func, Map<String, Map<String, String>> tabelaGratificacao) {
        // Lógica para mapear o tipo do funcionário para a função na tabela
        String tipo = func.getTipo().toUpperCase();
        
        if ("MENSAGEIRO".equals(tipo)) {
            return "MSG";
        } else if ("OPERADORA".equals(tipo)) {
            return "OPE";
        } else {
            // Para administrativos, verifica o setor/cargo
            for (Map.Entry<String, Map<String, String>> entry : tabelaGratificacao.entrySet()) {
                String cargos = entry.getValue().get("Cargos");
                if (cargos != null && cargos.contains(func.getSetor())) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
   /**
    * Para usar este método, basta chamá-lo após carregar os funcionários básicos    
    * List<Funcionario> funcionarios = processarNomes(arquivoNomes);
      processarRecebidoOperador(arquivoRecebido, funcionarios);
   */
    private void processarRecebidoOperador(File arquivo, List<Funcionario> funcionarios)  throws Exception {
        try (FileInputStream fis = new FileInputStream(arquivo);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            // Processa cada linha a partir da linha 8 (índice 7)
            for (int i = 7; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // 1. Extrai dados brutos da linha
                Map<String, String> dadosBrutos = extrairDadosRow(row);
                
                // 2. Processa e valida dados do operador
                RecebidoOperador operador = processarDadosOperador(arquivo.getName(), dadosBrutos);
                if (operador == null) {
                    logger.info("❌ Erro: Objeto operador não foi criado. {}",operador); 
                    return;
                }
                if (operador.getCodigo() == null || operador.getCodigo().isEmpty()) {
                    System.out.println("⚠️ Aviso: Código do operador não foi carregado.");
                    logger.info("⚠️ Aviso: Código do operador não foi carregado. {}",operador); 
                }
                
                if (operador.getValorBruto() == null) {
                    System.out.println("⚠️ Aviso: Valor bruto não foi carregado.");
                    logger.info("⚠️ Aviso: Valor bruto não foi carregado. {}",operador); 
                }
                
                if (operador.getComissao() == null) {
                    System.out.println("⚠️ Aviso: Comissão não foi carregada.");
                    logger.info("⚠️ Aviso: Comissão não foi carregada. {}",operador); 
                }

                logger.debug("L358 {}", operador);
                //if (operador == null) continue;
                
                // 3. Encontra e atualiza funcionário correspondente
                vincularOperadorAFuncionario(operador, funcionarios);

                dadosBrutos.clear();
            }
        } catch (Exception e) {
            logger.error("Erro ao processar arquivo RECEBIDO POR OPERADOR", e);
            throw e;
        }
    }

    private Map<String, String> extrairDadosRow(Row row) {
        Map<String, String> dados = new HashMap<>();
        for (int j = 0; j < row.getLastCellNum(); j++) {
            dados.put("Column" + j, getCellStringValue(row, j));
        }
        return dados;
    }
    private RecebidoOperador processarDadosOperador(String nomeArquivo, Map<String, String> dadosBrutos) {
        try {
            // Processa a coluna principal para extrair código, nome e tipo
            dadosBrutos = processarColumn0(dadosBrutos);
            
            // Cria objeto RecebidoOperador
            XlsData data = XlsData.createFromFile(nomeArquivo, dadosBrutos);
            if (!(data instanceof RecebidoOperador)) {
                logger.warn("Dados não correspondem a um operador válido");
                return null;
            }
            
            RecebidoOperador operador = (RecebidoOperador) data;
            operador.debugDados(); // Log para depuração
            
            return operador;
        } catch (Exception e) {
            logger.error("Erro ao processar dados do operador", e);
            return null;
        }
    }

    private void vincularOperadorAFuncionario(RecebidoOperador operador, List<Funcionario> funcionarios) {
         // Verifica se o código do operador é válido
        if (operador.getCodigo() == null || operador.getCodigo().trim().isEmpty()) {
            logger.error("Código do operador é nulo ou vazio. Não é possível vincular.");
            return;
        }
        String codigoRecebidoOperador = (operador.getCodigo()).trim().toString();
        String codigoNormalizadoRecebidoOperador = normalizarCodigo(codigoRecebidoOperador);    

        try {   
            // Normaliza e converte o código do operador para BigDecimal
            BigDecimal codigoOperadorBD = new BigDecimal(codigoNormalizadoRecebidoOperador);               
                
            for (Funcionario func : funcionarios) {
               // Verifica se o código do funcionário é válido
                if (func.getCodigo() == null) {
                    continue;
                }
                //logger.warn("L481 {}",func.getTipo());
                   
                // Comparação segura entre códigos
                BigDecimal codigoFuncBD;
                try {
                    codigoFuncBD = new BigDecimal(func.getCodigo().trim().toString());
                } catch (NumberFormatException e) {
                    logger.warn("Código inválido para funcionário {}: {}", func.getNome(), func.getCodigo());
                    continue;
                }
                    
                /* if (codigoOperadorBD.compareTo(codigoFuncBD) == 0 && 
                    "OPE".equalsIgnoreCase(func.getTipo()) ) { */
                if (codigoFuncBD != null && codigoOperadorBD.compareTo(codigoFuncBD) == 0 && "OPE".equalsIgnoreCase(func.getTipo())){        
                            
                    // Restante da lógica de vinculação...
                    func.setRecebidoOperador(operador.getValorBruto());
                    func.setComissao(operador.getComissao());
                    func.setTipoRegistro(operador.getTipoOperacao());
                        
                    Map<String, BigDecimal> operacoes = func.getOperacoes();
                    if (operacoes == null) {
                        operacoes = new HashMap<>();
                        func.setOperacoes(operacoes);
                    }
                    operacoes.put(operador.getTipoOperacao(), operador.getValorBruto());
                    if ("OPE".equalsIgnoreCase(func.getTipo())) {
                        logger.info("Operador {} atualizado - Valor: {}, Comissão: {}",
                        func.getNome(), operador.getValorBruto(), operador.getComissao());                            
                    }                       
                        
                    return;                        
                }
                if ("OPE".equalsIgnoreCase(func.getTipo())) {
                    // Debug: lista todos os funcionários do tipo OPERADORA
                    logger.debug("-> L507 Código: {} | Nome: {} | Polo: {} | Recebido: {} | Comissão: {} | TIPO: {}",
                    func.getCodigo(), func.getNome(),func.getPolo(),func.getRecebidoOperador(),func.getComissao(),func.getTipoRegistro());                        
                }
            }
                
                logger.warn("Nenhum funcionário OPERADORA encontrado com o código {}", codigoOperadorBD );
        } catch (NumberFormatException e) {
            logger.error("Erro ao processar código do operador {}", e.getMessage());
        }      
    }
    /**
     * Normaliza códigos removendo zeros à esquerda e espaços
     */
    private String normalizarCodigo(String codigoOriginal) {
        if (codigoOriginal == null) return "";
        // Remove espaços e zeros à esquerda
        return codigoOriginal.trim().replaceFirst("^0+", "");
    }

    public List<Funcionario> processarNomes(File arquivo) throws Exception {
        List<Funcionario> funcionariosNovos = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(arquivo);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pula cabeçalho
                
                Funcionario func = new Funcionario();                
                func.setTipo(getCellStringValue(row, 0));
                func.setCodigo(getCellStringValue(row, 1));
                func.setDrt(getCellStringValue(row, 2));
                func.setNome(getCellStringValue(row, 3));
                func.setPolo(getCellStringValue(row, 4));
                func.setSetor(getCellStringValue(row, 5));

                if (func.getDemonstrativosCaixa() == null) {
                    func.setDemonstrativosCaixa(new HashMap<>()); // Evita erro de mapa nulo
                }
                listaFuncionarios.add(func);
            }
            System.out.println("🔍 Funcionários e seus setores:");
            for (Funcionario f : listaFuncionarios) {
                System.out.println(f.getNome() + " -> " + f.getPolo());
            }
        }
        if (listaFuncionarios.isEmpty()) {
            logger.warn("🚨 Nenhum funcionário carregado a partir de {}!", arquivo.getName());
        } else {
            logger.info("✅ {} funcionários carregados com sucesso!", listaFuncionarios.size());
        }

        //logger.info("L341 Funcionarios {}", funcionarios);
        return listaFuncionarios;
    }

    public void processarMetaRec(File arquivo, List<Funcionario> funcionarios) throws Exception {
        logger.info("Processando arquivo de metas: {}", arquivo.getName());
        logger.debug("Total de funcionários: {}", funcionarios.size());
        
        // Mapa para otimizar a busca de funcionários por código
        Map<String, Funcionario> funcionariosPorCodigo = funcionarios.stream()
            .filter(f -> f.getCodigo() != null)
            .collect(Collectors.toMap(
                f -> normalizarCodigo(f.getCodigo().toString()),
                f -> f,
                (existente, novo) -> existente // Em caso de códigos duplicados, mantém o existente
            ));
    
        try (FileInputStream fis = new FileInputStream(arquivo);
             Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            int linhasProcessadas = 0;
            int metasAtribuidas = 0;
            
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pula cabeçalho
                
                linhasProcessadas++;
                String codigo = getCellStringValue(row, 0); // CÓD
                if (codigo == null || codigo.trim().isEmpty()) {
                    logger.warn("Linha {} - Código vazio ou nulo", row.getRowNum() + 1);
                    continue;
                }
                
                String codigoNormalizado = normalizarCodigo(codigo);
                BigDecimal metaRec;
                
                try {
                    metaRec = BigDecimal.valueOf(getCellNumericValue(row, 3)); // META REC
                } catch (Exception e) {
                    logger.error("Linha {} - Valor inválido para meta: {}", row.getRowNum() + 1, getCellStringValue(row, 3));
                    continue;
                }
                
                logger.debug("Processando código: {} - Meta: {}", codigoNormalizado, metaRec);
                
                // Busca otimizada usando o mapa
                Funcionario funcionario = funcionariosPorCodigo.get(codigoNormalizado);
                
                if (funcionario != null) {
                    // Verifica se é um operador antes de atribuir
                    if ("OPE".equalsIgnoreCase(funcionario.getTipo())) {
                        funcionario.setMetaRec(metaRec);
                        metasAtribuidas++;
                        logger.info("Meta atribuída para {} (Código: {}): {}", 
                                    funcionario.getNome(), codigoNormalizado, metaRec);
                    } else {
                        logger.debug("Funcionário encontrado não é operador: {}", funcionario.getTipo());
                    }
                } else {
                    logger.warn("Nenhum funcionário encontrado para o código: {}", codigoNormalizado);
                }
            }
            
            logger.info("Processamento concluído. Linhas: {}, Metas atribuídas: {}", 
                       linhasProcessadas, metasAtribuidas);
            
        } catch (Exception e) {
            logger.error("Erro ao processar arquivo de metas", e);
            throw e;
        }
    }

    private Map<String, Double> demonstrativosPorSetor = new HashMap<>(); // armazenar quantidade e valor por setor
    public Map<String, Double> getDemonstrativosPorSetor() {
        return this.demonstrativosPorSetor;
    }

    public void processarDemonstrativosCaixa(File arquivo, List<Funcionario> funcionarios) throws Exception { 
        if (funcionarios == null || funcionarios.isEmpty()) {
            System.out.println("❌ Nenhum funcionário disponível para processar demonstrativo.");
            return;
        }        
        System.out.println("📌 Número de funcionários carregados: " + funcionarios.size());
    
        try (FileInputStream fis = new FileInputStream(arquivo);
            Workbook workbook = WorkbookFactory.create(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            String setor = extrairSetorDoNomeArquivo(arquivo.getName());
    
            System.out.println("📌 Processando demonstrativo para setor: " + setor);
            
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING && 
                        cell.getStringCellValue().trim().contains(TOTAL_RECEBIDO_C_C_BOLETOS)) {
    
                        // ✅ Chamando o método corrigido para processar a linha
                        processarLinhaDemonstrativo(row, setor, arquivo, funcionarios);
                        break; // Sai do loop da linha após encontrar
                    }
                }
            }
        }
    }
    
    public void processarSituacaoMensal(File arquivo, List<Funcionario> funcionarios) throws Exception {
        // Validação inicial
        if (arquivo == null || !arquivo.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado: " + (arquivo != null ? arquivo.getName() : "null"));
        }
        try (FileInputStream fis = new FileInputStream(arquivo);
             Workbook workbook = WorkbookFactory.create(fis)) {          
            
            Sheet sheet = workbook.getSheetAt(0);

            // 🧾 Log completo da planilha - linha a linha, célula a célula
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    logger.debug("Linha {}: (vazia)", i);
                    continue;
                }

                StringBuilder sb = new StringBuilder("Linha " + i + ": ");
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String valor = getCellStringValue(cell);
                    sb.append("[Coluna ").append(j).append(": ").append(valor).append("] ");
                }
                //logger.debug(sb.toString());
            }

            Map<String, Map<String, String>> dadosBrutosMsg = extrairDadosBrutos(sheet);//new LinkedHashMap<>();
           
            atualizarSituacaoFuncionarios(funcionarios, dadosBrutosMsg);
            logContagemAtualizacoes(funcionarios);

            // Depois visualiza
            //visualizarMensageiros(funcionarios);                
           
            }

           /*   // Primeiro, coletamos todos os dados brutos do arquivo
            for (Row row : sheet) {
                if (row.getRowNum() < 2) continue; // Pula cabeçalhos
                
                Map<String, String> linhaDados = new LinkedHashMap<>();
                for (Cell cell : row) {
                    String columnName = "Column" + cell.getColumnIndex();
                    String cellValue = getCellStringValue(cell);
                    linhaDados.put(columnName, cellValue);
                }
                dadosBrutosMsg.put("Linha" + row.getRowNum(), linhaDados);
               // logger.debug("L806 Linhas dados Mensageiros {}", dadosBrutosMsg);
            }

            // Agora processamos os dados usando a classe GeralSituacaoMensal
            Map<String, Map<String, String>> dadosAgrupadosMsg = GeralSituacaoMensal.agruparDadosMensageiros(dadosBrutosMsg);
            logger.debug("L811 Linhas dados Mensageiros {}", dadosAgrupadosMsg);

            for (Map.Entry<String, Map<String, String>> entry : dadosAgrupadosMsg.entrySet()) {
                Map<String, String> dadosProcessados = GeralSituacaoMensal.processarDadosMensageiro(entry.getValue());
                GeralSituacaoMensal situacaoMensal = new GeralSituacaoMensal(dadosProcessados);

                logger.debug("L808 DEBUGANDO {}",situacaoMensal);
                
                // Encontrar funcionário correspondente
                for (Funcionario func : funcionarios) {
                    logger.info("L812 Codigo Mensageiro {} - Codigo Situação Mensal {} è Falso ou Verdade? {}", 
                    func.getCodigo(), situacaoMensal.getCodigo(),func.getCodigo().equals(situacaoMensal.getCodigo()));
                    if (func.getCodigo().equals(situacaoMensal.getCodigo())) {
                        Funcionario.SituacaoMensal situacao = new Funcionario.SituacaoMensal();
                        
                        // Mapeando os valores da classe GeralSituacaoMensal para a estrutura do Funcionario
                        situacao.setSaldoAnterior(situacaoMensal.getSaldoAnteriorValor());
                        situacao.setEntradas(situacaoMensal.getEntradasPeriodoValor());
                        situacao.setRecebido(situacaoMensal.getRecebidoMsgValor());
                        situacao.setPercentualDevolvido(situacaoMensal.getPercentualRendimentoQtd());
                        situacao.setDevolvido(situacaoMensal.getDevolvidoMsgValor());
                        situacao.setSaldoDia(BigDecimal.ZERO); // Ajuste conforme necessário
                        situacao.setRendimento(situacaoMensal.getPercentualRendimentoValor());
                        
                        func.setSituacaoMensal(situacao);
                        break;
                    }
                }
            }

            // Log de verificação
            int count = 0;
            for (Funcionario func : funcionarios) {
                if (func.getSituacaoMensal() != null) {
                    count++;
                }
            }
            logger.info("Dados de situação mensal integrados para {} funcionários", count);

        } */
    }

    private Map<String, Map<String, String>> extrairDadosBrutos(Sheet sheet) {       
        Map<String, Map<String, String>> dados = new LinkedHashMap<>();
        
        // Processa a partir da linha 6 (índice 5), pulando de 2 em 2 linhas
        for (int i = 5; i < sheet.getLastRowNum(); i += 2) {
            Row idRow = sheet.getRow(i);
            Row valuesRow = sheet.getRow(i+1);
            
            if (idRow == null || valuesRow == null) continue;
            
            // Pega código e nome do mensageiro
            String codigo = getCellStringValue(idRow.getCell(0));
            String nome = getCellStringValue(idRow.getCell(1));
            
            if (codigo == null || codigo.isEmpty()) continue;
            
            Map<String, String> valores = new HashMap<>();
            // Extrai os valores da linha seguinte
            valores.put("saldoAnteriorQtd", getCellStringValue(valuesRow.getCell(2)));//10
            valores.put("saldoAnteriorValor", getCellStringValue(valuesRow.getCell(3)));//11
            valores.put("entradasQtd", getCellStringValue(valuesRow.getCell(4)));//8
            valores.put("entradasValor", getCellStringValue(valuesRow.getCell(5)));//7
            valores.put("recebidoQtd", getCellStringValue(valuesRow.getCell(6)));//6
            valores.put("recebidoValor", getCellStringValue(valuesRow.getCell(7)));//5
            // valores.put("percentualEmQtd", getCellStringValue(valuesRow.getCell(8)));//9
            valores.put("devolvidoQtd", getCellStringValue(valuesRow.getCell(9)));//3
            valores.put("devolvidoValor", getCellStringValue(valuesRow.getCell(10)));// 1
            // valores.put("saldoDiaQtd", getCellStringValue(valuesRow.getCell(12)));
            valores.put("saldoDiaValor", getCellStringValue(valuesRow.getCell(13))); // posicao 0
            valores.put("rendimentoQtd", getCellStringValue(valuesRow.getCell(14)));//4
            valores.put("rendimentoValor", getCellStringValue(valuesRow.getCell(15)));//2             
            
            dados.put(codigo, valores);
            //logger.debug("L926 Dados extraídos para {} - {}: {}", codigo,nome, valores);
        }   
        
        //logger.debug("L924 DADOS MSG {} ", dados);
        
        return dados;
    }   
    
    private void atualizarSituacaoFuncionarios(List<Funcionario> funcionarios, 
                                         Map<String, Map<String, String>> dadosBrutosMsg) {
        // Cria um mapa de funcionários por código para acesso rápido
        Map<String, Funcionario> funcionariosPorCodigo = funcionarios.stream()
            .filter(f -> "MSG".equalsIgnoreCase(f.getTipo()))  // Filtra apenas mensageiros
            .collect(Collectors.toMap(
                f -> String.valueOf(f.getCodigo()),  // Converte código para String para match com os dados brutos
                Function.identity(),
                (f1, f2) -> f1));  // Em caso de duplicatas, mantém o primeiro
        
        // Itera sobre os dados brutos dos mensageiros
        for (Map.Entry<String, Map<String, String>> entry : dadosBrutosMsg.entrySet()) {
            String codigoMsg = entry.getKey();
            Map<String, String> dadosMsg = entry.getValue();
            
            // Encontra o funcionário correspondente
            Funcionario funcionario = funcionariosPorCodigo.get(codigoMsg);
            
            if (funcionario != null) {

                BigDecimal valorRecebido = parseBigDecimal(dadosMsg.get("recebidoValor"));
                BigDecimal rendimento = parsePercentual(dadosMsg.get("rendimentoQtd"));
                
                // Calcula a comissão mensageiro
                BigDecimal comissao = GeralSituacaoMensal.calcularGratificacaoMensageiro(
                    valorRecebido, 
                    rendimento
                );

                Funcionario.SituacaoMensal situacao = new Funcionario.SituacaoMensal(
                    parseInteger(dadosMsg.get("saldoAnteriorQtd")),
                    parseBigDecimal(dadosMsg.get("saldoAnteriorValor")),
                    parseInteger(dadosMsg.get("entradasQtd")),
                    parseBigDecimal(dadosMsg.get("entradasValor")),
                    parseInteger(dadosMsg.get("recebidoQtd")),
                    parseBigDecimal(dadosMsg.get("recebidoValor")),  
                    //parsePercentual(dadosMsg.get("percentualEmQtd")),                  
                    parseInteger(dadosMsg.get("devolvidoQtd")),
                    parseBigDecimal(dadosMsg.get("devolvidoValor")),                   
                    parseBigDecimal(dadosMsg.get("saldoDiaValor")),
                    parsePercentual(dadosMsg.get("rendimentoQtd")),
                    parsePercentual(dadosMsg.get("rendimentoValor")),
                    comissao
                     
                );
               
                String comissaoFormatada = formatarParaReal(comissao); //  convertendo para moeda Brasileira
                //logger.info("L843 Comissao do Mês do Mensageiro {}", comissaoFormatada);
                logger.info("L844 rendimentoQtd {}   - Recebido Valor {} - Comissao do Mês MSG {}", parsePercentual(dadosMsg.get("rendimentoQtd")),valorRecebido, comissaoFormatada); 
                                         
                // Atualiza o funcionário
                funcionario.setSituacaoMensal(situacao);

                //logger.debug("L977 {}",situacao);
                
                //logger.debug("Atualizado mensageiro {} - Código: {} - {}", funcionario.getNome(), codigoMsg,situacao);
            } else {
                logger.warn("Mensageiro não encontrado na lista de funcionários - Código: {}", codigoMsg);
            }
        }
    }  
    
    private void atualizarFuncionariosOperadoras(List<Funcionario> funcionarios, 
        Map<String, Map<String, String>> dadosBrutosMsgOperadora) {

            logger.debug("L886 {} ", dadosBrutosMsgOperadora);
            System.out.println("L887 " + dadosBrutosMsgOperadora);
    
        // Cria um mapa de funcionários por código
        Map<String, Funcionario> funcionariosPorCodigo = funcionarios.stream()
            .filter(f -> "OPE".equalsIgnoreCase(f.getTipo()))
            .collect(Collectors.toMap(
                f -> String.valueOf(f.getCodigo()), 
                Function.identity(),
                (f1, f2) -> f1)); // Mantém o primeiro em caso de duplicidade

        // Itera sobre os dados brutos extraídos da planilha
        for (Map.Entry<String, Map<String, String>> entry : dadosBrutosMsgOperadora.entrySet()) {
            String codigoOpe = entry.getKey();
            Map<String, String> dadosOpe = entry.getValue();

            Funcionario funcionario = funcionariosPorCodigo.get(codigoOpe);
            if (funcionario != null) {
                 SituacaoOperadora situacao = SituacaoOperadora.fromMapOpe(dadosOpe);
                funcionario.setSituacaoOperadora(situacao);

                Funcionario f = funcionarios.get(0);
                System.out.println(f.getSituacaoOperadora());


            } else {
                //logger.warn("Funcionário com código {} não encontrado na lista", codigoOpe);
                System.err.println("Funcionário com código " + codigoOpe + " não encontrado na lista");
            }
        }
    }


    public void processarFichaNova(File arquivo, List<Funcionario> funcionarios) throws Exception {
        logger.info("L917 Processando arquivo de Ficha Nova: {}", arquivo.getName());
        // Validação inicial
        if (arquivo == null || !arquivo.exists()) {
            throw new FileNotFoundException("Arquivo não encontrado: " + (arquivo != null ? arquivo.getName() : "null"));
        }
         // Verifica se a lista de funcionários está preenchida
        if (funcionarios == null || funcionarios.isEmpty()) {
            logger.error("Lista de funcionários vazia para processar FN.xls");
            throw new IllegalStateException("Lista de funcionários não pode ser vazia");
        }
        try (FileInputStream fis = new FileInputStream(arquivo);
             Workbook workbook = WorkbookFactory.create(fis)) {          
            
            Sheet sheet = workbook.getSheetAt(0);

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 🧾 Log completo da planilha - linha a linha, célula a célula
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    logger.debug("Linha {}: (vazia)", i);
                    continue;
                }

                StringBuilder sb = new StringBuilder("Linha " + i + ": ");
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    String valor = getCellStringOpeValue(cell, evaluator);
                    sb.append("[Coluna ").append(j).append(": ").append(valor).append("] ");
                }
                logger.debug(sb.toString());
            }

            Map<String, Map<String, String>> dadosBrutosOperadora = extrairDadosFichaNovaOperadora(sheet, evaluator);//new LinkedHashMap<>();
            logger.info("L909 DADOS OPE {} ", dadosBrutosOperadora);
            System.out.println("L910" + dadosBrutosOperadora);
           
            atualizarFuncionariosOperadoras(funcionarios, dadosBrutosOperadora);
        }
    }

    private Map<String, Map<String, String>> extrairDadosFichaNovaOperadora(Sheet sheet, FormulaEvaluator evaluator) {
        Map<String, Map<String, String>> dados = new LinkedHashMap<>();

         // Verifica se a sheet é válida
        if (sheet == null) {
            logger.error("Planilha inválida - sheet é null");
            return dados;
        }
    
        // Define os índices das colunas relevantes
        Map<String, Integer> colunasFN = Map.of(
            "fnRecVal", 14,
            "fnBolVal", 16,
            "fnDebVal", 18,
            "fnPayVal", 20,
            "fnHipVal", 22
        );
    
        Map<String, Integer> colunasRC = Map.of(
            "rcRecVal", 24,
            "rcBolVal", 26,
            "rcDebVal", 28,
            "rcPayVal", 30,
            "rcHipVal", 32
        );
    
        for (int i = 1; i <= sheet.getLastRowNum(); i ++) {
            Row row = sheet.getRow(i);
            //Row valuesRow = sheet.getRow(i + 1);
    
            if (row == null ) continue;
    
            String codigo = getCellStringOpeValue(row.getCell(0),evaluator);
            logger.info("L978 processando linha {} - Codigo: {} ",i,codigo);
            if (codigo == null || codigo.isEmpty()) continue;
    
            Map<String, String> valores = new HashMap<>();
    
            // Adiciona valores da ficha nova (FN)
            for (Map.Entry<String, Integer> entry : colunasFN.entrySet()) {
                valores.put(entry.getKey(), getCellStringOpeValue(row.getCell(entry.getValue()), evaluator));
            }
    
            // Adiciona valores da faixa recorrente (RC)
            for (Map.Entry<String, Integer> entry : colunasRC.entrySet()) {
                valores.put(entry.getKey(), getCellStringOpeValue(row.getCell(entry.getValue()), evaluator));
            }
    
            dados.put(codigo, valores);
            logger.debug("L1010 xlsProcessor Dados extraídos para código {}: {}", codigo, valores);
        }       
    
        return dados;
    }
    

    /*  private Map<String, Map<String, String>> extrairDadosFichaNovaOperadora(Sheet sheet) {       
        Map<String, Map<String, String>> dados = new LinkedHashMap<>();
        
        // Processa a partir da linha 6 (índice 5), pulando de 2 em 2 linhas
        for (int i = 5; i < sheet.getLastRowNum(); i += 2) {
            Row idRow = sheet.getRow(i);
            Row valuesRow = sheet.getRow(i+1);
            
            if (idRow == null || valuesRow == null) continue;
            
            // Pega código e nome do mensageiro
            String codigo = getCellStringValue(idRow.getCell(0));
            //String nome = getCellStringValue(idRow.getCell(1));
            
            if (codigo == null || codigo.isEmpty()) continue;

            //FN  = posição 14 (O) e 16 (Q) e 18 (S) e 20 (U) e 22 (W)
             // FR = posição 24 (Y)e 26 (AA) e 28 (AC) e 30 (AE) e 32 (AG)
                       
            Map<String, String> valores = new HashMap<>();
            // Extrai os valores da linha seguinte
            valores.put("fnRecVal", getCellStringValue(valuesRow.getCell(14)));
            valores.put("fnBolValor", getCellStringValue(valuesRow.getCell(16)));
            valores.put("fnDebValor", getCellStringValue(valuesRow.getCell(18)));
            valores.put("fnPayValor", getCellStringValue(valuesRow.getCell(20)));
            valores.put("fnHipValor ", getCellStringValue(valuesRow.getCell(22)));

            valores.put("rcRecValor", getCellStringValue(valuesRow.getCell(24)));
            // valores.put("percentualEmQtd", getCellStringValue(valuesRow.getCell(8)));//9
            valores.put("rcBolValor", getCellStringValue(valuesRow.getCell(26)));//3
            valores.put("rcDebValor", getCellStringValue(valuesRow.getCell(28)));// 1
            // valores.put("saldoDiaQtd", getCellStringValue(valuesRow.getCell(12)));
            valores.put("rcPayValor ", getCellStringValue(valuesRow.getCell(30))); // posicao 0
            valores.put("rcHipValor", getCellStringValue(valuesRow.getCell(32)));//4
           // valores.put("rendimentoValor", getCellStringValue(valuesRow.getCell(15)));//2             
            
            dados.put(codigo, valores);
            //logger.debug("L926 Dados extraídos para {} - {}: {}", codigo,nome, valores);
        }   
        
        //logger.debug("L924 DADOS MSG {} ", dados);
        
        return dados;
    }  */
      
    public void visualizarMensageiros(List<Funcionario> funcionarios) {
        // Filtra apenas mensageiros
        List<Funcionario> mensageiros = funcionarios.stream()
            .filter(f -> "MSG".equalsIgnoreCase(f.getTipo()))
            .sorted(Comparator.comparing(Funcionario::getCodigo))
            .collect(Collectors.toList());
        
        logger.info("\n=== RELATÓRIO DE MENSAGEIROS ===");
        logger.info("Total: {}", mensageiros.size());
        logger.info("=================================");
        
        // Cabeçalho da tabela
        logger.info(String.format("%-6s | %-30s | %12s | %12s | %10s", 
            "Código", "Nome", "Recebido", "Comissão", "Rendimento"));
        logger.info("-------------------------------------------------------------------------------");
        
        for (Funcionario msg : mensageiros) {
            try {
                // Obtém os valores com tratamento seguro para nulos
                BigDecimal recebido = safeGetRecebidoValor(msg);
                BigDecimal comissao = safeGetComissao(msg);
                BigDecimal rendimento = safeGetRendimento(msg);
                
                // Formata os valores
                String recebidoStr = formatarParaReal(recebido);
                String comissaoStr = formatarParaReal(comissao);
                String rendimentoStr = rendimento != null ? 
                    String.format("%.2f%%", rendimento.multiply(BigDecimal.valueOf(100))) : "N/A";
                
                // Loga a linha formatada
                logger.info(String.format("%-6s | %-30s | %12s | %12s | %10s",
                    msg.getCodigo(),
                    msg.getNome(),
                    recebidoStr,
                    comissaoStr,
                    rendimentoStr));
            } catch (Exception e) {
                logger.error("Erro ao processar mensageiro {}: {}", msg.getCodigo(), e.getMessage());
            }
        }
    }
    
    // Métodos auxiliares para tratamento seguro de valores
    private BigDecimal safeGetRecebidoValor(Funcionario msg) {
        try {
            return msg.getSituacaoMensal() != null ? 
                msg.getSituacaoMensal().getRecebidoValor() : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal safeGetComissao(Funcionario msg) {
        try {
            return msg.getComissaoMensageiro() != null ? 
                msg.getComissaoMensageiro() : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    private BigDecimal safeGetRendimento(Funcionario msg) {
        try {
            return msg.getSituacaoMensal() != null ? 
                msg.getSituacaoMensal().getRendimentoQtd() : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    // Método formatarParaReal robusto
    private String formatarParaReal(BigDecimal valor) {
        try {
            if (valor == null) {
                return "R$ 0,00";
            }
            
            // Usa Locale brasileiro
            NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);
            
            return formato.format(valor);
        } catch (Exception e) {
            logger.error("Erro ao formatar valor monetário: {}", e.getMessage());
            return "R$ [ERRO]";
        }
    }
    
    private Funcionario.SituacaoMensal criarSituacaoMensal(GeralSituacaoMensal situacaoMensal) {
        Funcionario.SituacaoMensal situacao = new Funcionario.SituacaoMensal();
        situacao.setSaldoAnterior(situacaoMensal.getSaldoAnteriorValor());
        situacao.setEntradaValor(situacaoMensal.getEntradasPeriodoValor());
        situacao.setRecebidoValor(situacaoMensal.getRecebidoMsgValor());
        situacao.setRendimentoQtd(situacaoMensal.getPercentualRendimentoQtd());
        situacao.setDevolvidoValor(situacaoMensal.getDevolvidoMsgValor());
        situacao.setSaldoDia(BigDecimal.ZERO);
        situacao.setRendimentoValor(situacaoMensal.getPercentualRendimentoValor());
        return situacao;
    }

    /* public static String formatarParaReal(BigDecimal valor) {
        // Cria um formatador para o locale brasileiro
        NumberFormat formatoBrasileiro = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        
        // Configura para usar 2 casas decimais (centavos)
        formatoBrasileiro.setMinimumFractionDigits(2);
        formatoBrasileiro.setMaximumFractionDigits(2);
        
        // Formata o valor
        return formatoBrasileiro.format(valor);
    } */
    // Métodos auxiliares para conversão
    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty() || "       %".equals(value.trim())) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.replace(".", "").replace(",", "."));
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter valor para double: {}", value);
            return 0.0;
        }
    }
    // Métodos auxiliares para conversão
    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty() || "       %".equals(value.trim())||value.trim().equals("%")) {
            return 0;
        }
        try {
            // Remove parte decimal se existir (transforma "37.0" em 37)
            String clean = value.split("\\.")[0];
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            logger.warn("Valor inválido para inteiro: {}", value);
            return 0;
        }
    }
    private BigDecimal parsePercentual(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("       %")) {
            return BigDecimal.ZERO;
        }
        try {
            // Remove o % e espaços, substitui vírgula por ponto
            String clean = value.replace("%", "")
                              .trim()
                              .replace(",", ".");
            return new BigDecimal(clean)
                    .divide(new BigDecimal(100)); // Converte 85,01 para 0.8501
        } catch (NumberFormatException e) {
            logger.warn("Percentual inválido: {}", value);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().equals("R$ 0,00") || value.trim().equals("%") || value.trim().equals("       %")) {
            return BigDecimal.ZERO;
        }
        try {
            // Remove "R$ " e substitui vírgula por ponto
            String cleanValue = value.replace("R$ ", "")
                                      .replace(".", "")
                                      .replace(",", ".");
            return new BigDecimal(cleanValue);
        } catch (NumberFormatException e) {
            logger.warn("Erro ao converter valor para BigDecimal: {}", value);
            return BigDecimal.ZERO;
        }
    }
    
    private void logContagemAtualizacoes(List<Funcionario> funcionarios) {
        long count = funcionarios.stream()
            .filter(func -> func.getSituacaoMensal() != null)
            .count();
        logger.info("Dados de situação mensal integrados para {} funcionários", count);
    }

    /**
     * Ler valores de células para Situação dos mensageiros
    */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

     /**
     * Ler valores de células para Situação das Operadoras FN e RC
     * @param cell
    */
    private String getCellStringOpeValue(Cell cell,FormulaEvaluator evaluator) {
        if (cell == null) return "0.0";
    
        switch (cell.getCellType()) {
            case NUMERIC:
                // Remove .0 de valores inteiros para ficar mais limpo
                double num = cell.getNumericCellValue();
                if (num == (int) num) {
                    return String.valueOf((int) num);
                }
                return String.valueOf(num);
            case STRING:
                return cell.getStringCellValue().trim();
            case BLANK:
                return "0.0";
            case FORMULA:
               // Avalia a fórmula antes de extrair o valor
            CellValue cellValue = evaluator.evaluate(cell);
            switch (cellValue.getCellType()) {
                case NUMERIC:
                    double formulaNum = cellValue.getNumberValue();
                    return (formulaNum == (int) formulaNum) ? 
                           String.valueOf((int) formulaNum) : 
                           String.valueOf(formulaNum);
                case STRING:
                    return cellValue.getStringValue();
                default:
                    return "0.0";
            }
            default:
                return "0.0";
        }
    }

    

    private String getCellStringValue(Row row, int column) {
        if (row == null) return "";
        Cell cell = row.getCell(column);
        
        if (cell == null) {
            logger.trace("Célula {}-{} está vazia", row.getRowNum(), column);
            return "";
        }
        
        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        return String.valueOf(cell.getNumericCellValue());
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return cell.getCellFormula();
                default:
                    return "";
            }
        } catch (Exception e) {
            logger.warn("Erro ao ler célula {}-{}: {}", row.getRowNum(), column, e.getMessage());
            return "";
        }
    }
    // Métodos auxiliares
    /* private String getCellStringValue(Row row, int cellNum) {
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return (cell == null) ? "" : cell.toString().trim();
    } */

    private double getCellNumericValue(Row row, int cellNum) {
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return 0.0; // Se a célula for vazia, retorna 0
        
        switch (cell.getCellType()) {
            case NUMERIC:
                return cell.getNumericCellValue();
            case STRING:
                try {
                    return Double.parseDouble(cell.getStringCellValue().replace(",", "."));
                } catch (NumberFormatException e) {
                    logger.warn("❌ Erro ao converter '{}' para número. Linha: {}", cell.getStringCellValue(), row.getRowNum());
                    return 0.0;
                }
            default:
                return 0.0;
        }
    }

   /*  private double getCellNumericValue(Row row, int cellNum) {
        Cell cell = row.getCell(cellNum, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return (cell == null) ? 0.0 : cell.getNumericCellValue();
    } */

    private String extrairSetorDoNomeArquivo(String nomeArquivo) {
        nomeArquivo = nomeArquivo.toUpperCase().trim(); // Padroniza
        
        if (nomeArquivo.contains("SETOR 2")) return "SERRA TALHADA";
        if (nomeArquivo.contains("SETOR 3")) return "PETROLINA";
        if (nomeArquivo.contains("SETOR 4")) return "MATRIZ";
        if (nomeArquivo.contains("SETOR 5")) return "CARUARU";
        if (nomeArquivo.contains("SETOR 6")) return "GARANHUNS";
        return "GERAL";
    }

    private double parseCurrency(String currencyValue) {
        if (currencyValue == null || currencyValue.isEmpty()) {
            return 0;
        }
        try {
            // Remove R$, espaços e converte corretamente
            String cleaned = currencyValue.replaceAll("[R\\$\\s]", "") // remove R$ e espaços
                                          .replaceAll("\\.(?=\\d{3})", "") // remove ponto só se for separador de milhar
                                          .replace(",", ".")               // troca vírgula decimal
                                          .trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            logger.error("Erro ao converter valor monetário: {}", currencyValue, e);
            return 0;
        }
    }
    
    /* private double parseCurrency(String currencyValue) {
        if (currencyValue == null || currencyValue.isEmpty()) {
            return 0;
        }
        try {
            // Remove R$, pontos e troca vírgula por ponto
            String cleaned = currencyValue.replaceAll("[R\\$\\s]", "")
                                         .replace(".", "")
                                         .replace(",", ".")
                                         .trim();
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            logger.error("Erro ao converter valor monetário: {}", currencyValue, e);
            return 0;
        }
    } */


    /**
     * Percorre as planilhas do workbook e extrai os dados necessários.
     */
    private void processWorkbook(Workbook workbook) {
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                Map<String, String> rowData = processRow(row);
                if (!rowData.isEmpty() && rowData.values().stream().anyMatch(val -> !val.isBlank())) {
                    extractedData.add(rowData);
                }
            }
        }

        extractedData.sort(Comparator.comparing(row -> extrairNumeroDaColuna(row.getOrDefault("Column0", ""))));
    }

    /**
     * Processa uma linha da planilha e retorna um mapa de valores.
     */
    private Map<String, String> processRow(Row row) {
        Map<String, String> rowData = new HashMap<>();
        for (Cell cell : row) {
            if (cell.getCellType() == CellType.BLANK)
                continue;
            rowData.put("Column" + cell.getColumnIndex(), processCell(cell));
        }
        // System.out.println("L89 " + rowData);
        return rowData;
    }
    
    /**
     * Processa uma célula, extraindo seu conteúdo de acordo com o tipo.
     */
    private String processCell(Cell cell) {
        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                        ? new SimpleDateFormat("dd-MM-yyyy").format(cell.getDateCellValue())
                        : formatNumericValue(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> processFormulaCell(cell);
                default -> "";
            };
        } catch (Exception e) {
            System.err.println("Erro ao processar célula: " + cell);
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Processa células do tipo fórmula, obtendo o resultado correto.
     */
    private String processFormulaCell(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> formatNumericValue(cell.getNumericCellValue());
            case STRING -> cell.getStringCellValue();
            default -> "";
        };
    }

    /**
     * Formata valores numéricos, removendo casas decimais se não forem necessárias.
     */
    private String formatNumericValue(double value) {
        return (value == (long) value) ? String.valueOf((long) value) : String.valueOf(value);
    }    

    /**
    * 📌 Método para extrair valores de uma linha específica do demonstrativo.
    */
    private void processarLinhaDemonstrativo(Row row, String setor, File arquivo, List<Funcionario> funcionarios) {
        List<Object> valoresNaLinha = new ArrayList<>();

        System.out.print("🔍 Linha " + row.getRowNum() + ": ");
        for (Cell cell : row) {
            String cellValue = cell.toString().trim();
            System.out.print("| " + cellValue + " ");
            if (!cellValue.isEmpty()) {  
                valoresNaLinha.add(cellValue);
            }
        }
        System.out.println("|");

        // 🔄 Procurar os valores dentro da linha
        int quantidadePolo = 0;
        double valorPolo = 0;
        boolean encontrouQuantidade = false;
        boolean encontrouValor = false;

        for (Object obj : valoresNaLinha) {
            if (!encontrouQuantidade) {
                try {
                    quantidadePolo = Integer.parseInt(obj.toString().replaceAll("[^0-9]", ""));
                    encontrouQuantidade = true;
                    continue;
                } catch (NumberFormatException ignored) {}
            }

            if (encontrouQuantidade && !encontrouValor) {
                try {
                    valorPolo = parseCurrency(obj.toString());  // Converte "R$ 65.963,00" para número
                    encontrouValor = true;
                    break;
                } catch (NumberFormatException e) {
                    logger.warn("⚠️ Formato inválido para valor: {}", obj);
                }
            }
            demonstrativosPorSetor.put(setor, valorPolo);
        }

        // ✅ Se encontrou os valores, associar ao funcionário correspondente
        if (encontrouQuantidade && encontrouValor) {
            logger.info("✅ Dados extraídos - QuantidadePolo: {}, ValorPolo: {}", quantidadePolo, valorPolo);

            for (Funcionario func : funcionarios) {                             
                if (func.getPolo().equalsIgnoreCase(setor)) {
                    System.out.println("📌 Adicionando ao funcionário: " + func.getNome() + " | Setor: " + func.getPolo());

                    // ✅ EVITA O ERRO DE MAPA NÃO INICIALIZADO
                    if (func.getDemonstrativosCaixa() == null) {
                        func.setDemonstrativosCaixa(new HashMap<>()); 
                    }
                    func.getDemonstrativosCaixa().put(extrairSetorDoNomeArquivo(arquivo.getName()), valorPolo);
                    demonstrativosPorSetor.put(func.getPolo(), valorPolo);
                    System.out.println("📌 Valor total do setor armazenado: " + setor + " -> " + valorPolo);
                    logger.info("✅ Demonstrativo atualizado -> {} | ValorPolo: {} | Setor {}", func.getNome(), valorPolo,func.getPolo());                           
                    break;
                }
            }
        } else {
            logger.warn("⚠️ Não foi possível extrair valores válidos da linha {}", row.getRowNum());
        }

        // ✅ Exibir os dados finais após processamento
        System.out.println("\n📌 RESUMO FINAL DOS DEMONSTRATIVOS 📌");
        for (Funcionario func : funcionarios) {
 
            if ("ADM".equals(func.getTipo()) && ("SERRA TALHADA".equals(func.getPolo()) || "PETROLINA".equals(func.getPolo()) ||
             "MATRIZ".equals(func.getPolo()) || "CARUARU".equals(func.getPolo()) || "GARANHUNS".equals(func.getPolo()))) {
                System.out.println("📌 Funcionário: " + func.getNome() + " | Setor: " + func.getPolo()+ " | QuantidadePolo: " + func.getQuantidadePolo()+ " | ValorPolo: " + func.getValorPolo()+ " | Comissão: " + func.getGratificacao());
                System.out.println("📊 Demonstrativos Caixa: " + func.getDemonstrativosCaixa());
            }
            //System.out.println("📌 Funcionário: " + func.getNome() + " | Setor: " + func.getPolo()+ " | QuantidadePolo: " + func.getQuantidadePolo()+ " | ValorPolo: " + func.getValorPolo()+ " | Comissão: " + func.getGratificacao());
           // System.out.println("📊 Demonstrativos Caixa: " + func.getDemonstrativosCaixa());
        }

        // for (Funcionario func : funcionarios) {
           // if (func.getPolo().equalsIgnoreCase("ADM") && func.getSetor().equalsIgnoreCase("GARANHUNS")) { 
               // BigDecimal percentualBigDecimal = new BigDecimal("0.0090");//func.getGratificacao(); // Obtém o percentual como BigDecimal
              //  double percentual = percentualBigDecimal.doubleValue();   // Converte para double
                
               // double valorSetor = demonstrativosPorSetor.get("GARANHUNS"); // Obtém o valor do setor
               // double valorCalculado = valorSetor * (percentual / 100);  // Calcula a comissão
                
                // Armazena a comissão no mapa de demonstrativos do funcionário
                //func.getDemonstrativosCaixa().put("SETOR 6 GARANHUNS.XLS", valorCalculado);
                
                // Exibe os dados calculados
               // System.out.println("✅ Comissão aplicada a " + func.getNome() + 
               //     " | Polo: " + func.getPolo() + 
               //     " | Setor: " + func.getSetor() + 
               //     " | Percentual: " + percentual + "% | Valor: " + valorCalculado);
           // }
        //} 
    }
    
    /**
     * Converte um valor monetário para double, removendo "R$" e formatando corretamente.
     */
    private double parseValorMonetarioParaDemonstrativo(String valorStr) throws NumberFormatException {
        String cleanStr = valorStr.replace("R$", "").replace(".", "").replace(",", ".").trim();
        return Double.parseDouble(cleanStr);
    }

    // Método auxiliar para converter para lista (para o cache)
    private List<String> converterParaLista(Map<String, Map<String, String>> dadosOperadoras) {
        List<String> linhas = new ArrayList<>();
        
        for (Map.Entry<String, Map<String, String>> entry : dadosOperadoras.entrySet()) {
            String codigo = entry.getKey();
            Map<String, String> dados = entry.getValue();
            
            StringBuilder sb = new StringBuilder();
            sb.append("Codigo:").append(codigo).append("|");
            sb.append("Nome:").append(dados.get("Nome")).append("|");
            sb.append("quantidadeBoleto:").append(dados.get("quantidadeBoleto")).append("|");
            sb.append("valorBoleto:").append(dados.get("valorBoleto")).append("|");
            sb.append("comissaoBoleto:").append(dados.get("comissaoBoleto")).append("|");
            sb.append("quantidadeRecibo:").append(dados.get("quantidadeRecibo")).append("|");
            sb.append("valorRecibo:").append(dados.get("valorRecibo")).append("|");
            sb.append("comissaoRecibo:").append(dados.get("comissaoRecibo"));
            
            linhas.add(sb.toString());
        }
        
        return linhas;
    }
    // Método auxiliar para converter a estrutura
    private TreeMap<String, Map<String, String>> converterParaEstruturaProcessavel(Map<String, String> dadosBrutos) {
        TreeMap<String, Map<String, String>> resultado = new TreeMap<>();
        
        // Agrupar por linha (ex: "Linha 11")
        Map<String, Map<String, String>> linhas = new HashMap<>();
        
        for (Map.Entry<String, String> entry : dadosBrutos.entrySet()) {
            String[] partes = entry.getKey().split(" - ");
            if (partes.length == 2) {
                String linha = partes[0]; // "Linha 11"
                String coluna = partes[1]; // "Column0"
                
                if (!linhas.containsKey(linha)) {
                    linhas.put(linha, new HashMap<>());
                }
                linhas.get(linha).put(coluna, entry.getValue());
            }
        }
        
        // Converter para TreeMap
        resultado.putAll(linhas);
        return resultado;
    }

    /**
     * Converte um Map<String, Map<String, String>> em TreeMap<String, Map<String, String>>.
     * Mantém a ordenação natural das chaves (String).
     * 
     * @param inputMap O mapa original a ser convertido.
     * @return Um novo TreeMap com os mesmos dados, ordenado pelas chaves.
     */
    public static TreeMap<String, Map<String, String>> toTreeMap(Map<String, Map<String, String>> inputMap) {
        TreeMap<String, Map<String, String>> treeMap = new TreeMap<>();
        
        if (inputMap != null) {
            treeMap.putAll(inputMap); // Copia todos os dados mantendo a ordenação
        }
        
        return treeMap;
    }

    private Map<String, Map<String, String>> processarRecebidoPorOperador(TreeMap<String, Map<String, String>> fileData) {
        Map<String, Map<String, String>> dadosOperadoras = new HashMap<>();
        
        for (Map.Entry<String, Map<String, String>> entry : fileData.entrySet()) {
            Map<String, String> linha = entry.getValue();
            String coluna0 = linha.get("Column0");
            //logger.info("Processando linha: {}", linha);
            
            if (coluna0 != null) {
                String[] partes = coluna0.split("\\s+", 3);
                
                if (partes.length >= 2) { // Garantir que temos pelo menos Código e Nome
                    String codigo = partes[0];
                    String nome = partes[1];
    
                    // Criar estrutura inicial se não existir
                    if (!dadosOperadoras.containsKey(codigo)) {
                        Map<String, String> dadosOpe = new HashMap<>();
                        dadosOpe.put("Codigo", codigo);
                        dadosOpe.put("Nome", nome);
                        // 🔹 Adicionar valores padrão para evitar campos vazios no JSON
                        dadosOpe.put("quantidadeBoleto", "0");
                        dadosOpe.put("valorBoleto", "0.00");
                        dadosOpe.put("comissaoBoleto", "0.00");
                        dadosOpe.put("quantidadeRecibo", "0");
                        dadosOpe.put("valorRecibo", "0.00");
                        dadosOpe.put("comissaoRecibo", "0.00");
                        dadosOperadoras.put(codigo, dadosOpe);
                    }
    
                    Map<String, String> dadosOpe = dadosOperadoras.get(codigo);
    
                    // Se houver tipo (BOL ou REC), atualizar os dados corretamente
                    if (partes.length == 3) {
                        String tipo = partes[2].trim();
    
                        if (tipo.equals("BOL")) {
                            dadosOpe.put("quantidadeBoleto", linha.getOrDefault("Column3", "0"));
                            dadosOpe.put("valorBoleto", formatarValor(linha.get("Column6")));
                            dadosOpe.put("comissaoBoleto", formatarValor(linha.get("Column12")));
                        } else if (tipo.equals("REC")) {
                            dadosOpe.put("quantidadeRecibo", linha.getOrDefault("Column3", "0"));
                            dadosOpe.put("valorRecibo", formatarValor(linha.get("Column6")));
                            dadosOpe.put("comissaoRecibo", formatarValor(linha.get("Column12")));
                        }
                    }
                }
            }
        }
        return dadosOperadoras;
    }

    // Método original (sem tabelaNomesPorPolo como parâmetro)
    private TreeMap<String, Map<String, String>> converterParaEstruturaProcessavelMetas(Map<String, String> dadosBrutos) {
        TreeMap<String, Map<String, String>> resultado = new TreeMap<>();
        Map<String, Map<String, String>> linhas = new HashMap<>();
        
        for (Map.Entry<String, String> entry : dadosBrutos.entrySet()) {
            String[] partes = entry.getKey().split(" - ");
            if (partes.length == 2) {
                String linha = partes[0]; // "Linha 11"
                String coluna = partes[1]; // "Column0", "Column3", etc.
                
                if (!linhas.containsKey(linha)) {
                    linhas.put(linha, new HashMap<>());
                }
                linhas.get(linha).put(coluna, entry.getValue());
            }
        }
        
        resultado.putAll(linhas);
        return resultado;
    }

    private void vincularMetas(
        TreeMap<String, Map<String, String>> dadosProcessados,
        Map<String, Map<String, Map<String, String>>> tabelaNomesPorPolo
    ) {
         // Foca apenas no polo OPE
        Map<String, Map<String, String>> poloOpe = tabelaNomesPorPolo.get("OPE");
        
        if (poloOpe == null) {
            logger.error("Polo OPE não encontrado na tabelaNomesPorPolo!");
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : dadosProcessados.entrySet()) {
            String chave = entry.getKey();
            Map<String, String> dados = entry.getValue();
            
            // Extrai código da operadora (adaptar conforme necessário)
            String codigoOperadora = dados.get("Column0");  // Ou use extrairCodigoOperadora(chave)
            
            if (codigoOperadora != null && poloOpe.containsKey(codigoOperadora)) {
                // Atualiza a operadora no polo OPE
               // Map<String, String> operadora = poloOpe.get(codigoOperadora);
                
                // Atualiza a meta se existir nos dados processados
                if (dados.containsKey("metaRecOperadora")) {
                    poloOpe.get(codigoOperadora).put("metaRecOperadora", dados.get("metaRecOperadora"));
                } else if (dados.containsKey("Column3")) {
                    poloOpe.get(codigoOperadora).put("metaRecOperadora", dados.get("Column3"));
                }
                
                logger.info("Meta atualizada para operadora {}: {}", codigoOperadora, 
                        poloOpe.get(codigoOperadora).get("metaRecOperadora"));
            }
        }
    }
    
    // Método auxiliar melhorado para extração do código
    private String extrairCodigoOperadora(String chave) {
        // Exemplos de tratamento:
        // 1. Se a chave for "Operadora 014" → "014"
        // 2. Se a chave for "014 - Vivo" → "014"
        // 3. Se a chave for "OPE-014" → "014"
        return chave.replaceAll("[^0-9]", "").trim();
    } 

    // Novo método para extrair metas e inserir em tabelaNomesPorPolo
    private void extrairMetasParaTabela(TreeMap<String, Map<String, String>> dadosProcessados,
     Map<String, String> tabelaNomesPorPolo ) {// tabela que vai receber as metas
    
        for (Map.Entry<String, Map<String, String>> linhaEntry : dadosProcessados.entrySet()) {
            String linha = linhaEntry.getKey(); // Ex: "Linha 11"
            Map<String, String> colunas = linhaEntry.getValue();
            
            // Verifica se existe "Column3" (meta) e insere na tabelaNomesPorPolo
            if (colunas.containsKey("Column3")) {
                tabelaNomesPorPolo.put(linha, colunas.get("Column3"));
            }
        }
    }
     
    private Map<String, Map<String, String>> processarMetaPorOperador(TreeMap<String, Map<String, String>> fileData) {
        TreeMap<String, Map<String, String>> dadosMetasOperadoras = new TreeMap<>();
    
        for (Map.Entry<String, Map<String, String>> entry : fileData.entrySet()) {
            String chaveOriginal = entry.getKey();  // Mantém a chave original (ex: "Linha 11")
            Map<String, String> valores = entry.getValue();
            
            // Cria uma nova entrada com a mesma estrutura
            Map<String, String> dadosOperadora = new HashMap<>();
            
            // Copia todos os valores originais
            dadosOperadora.putAll(valores);
            
            // Adiciona campo metaRecOperadora se Column3 existir
            if (valores.containsKey("Column3")) {
                dadosOperadora.put("metaRecOperadora", valores.get("Column3"));
            }
            
            dadosMetasOperadoras.put(chaveOriginal, dadosOperadora);
        }
        
        return dadosMetasOperadoras;
    }
     
    // Método para formatar valores monetários (ex: "R$ 1.520,61" -> "1520.61")
    private String formatarValor(String valor) {
        if (valor == null) return "0";
        return valor.replace("R$", "").replace(".", "").replace(",", ".").trim();
    }

    public class OperadoraProcessor {
        private static final Logger logger = LogManager.getLogger(OperadoraProcessor.class);
    
        public static List<String> processarOperadoras(TreeMap<String, Map<String, String>> fileData) {
            List<String> dadosFormatados = new ArrayList<>();
            Map<String, Map<String, String>> operadorasConsolidadas = new HashMap<>();
    
            for (Map.Entry<String, Map<String, String>> entry : fileData.entrySet()) {
                Map<String, String> linha = entry.getValue();
                
                // Processa a linha para extrair código, nome e tipo
                Map<String, String> linhaProcessada = processarLinhaOperadora(linha);
                
                if (linhaProcessada.containsKey("Codigo") && linhaProcessada.containsKey("Tipo")) {
                    String codigo = linhaProcessada.get("Codigo");
                    String tipo = linhaProcessada.get("Tipo");
                    
                    // Obtém ou cria a operadora no mapa consolidado
                    Map<String, String> operadora = operadorasConsolidadas.computeIfAbsent(codigo, k -> new HashMap<>());
                    
                    // Adiciona informações básicas
                    operadora.put("Codigo", codigo);
                    operadora.put("Nome", linhaProcessada.get("Nome"));
                    
                    // Processa valores conforme o tipo (BOL/REC)
                    processarValoresOperadora(operadora, linhaProcessada, tipo);
                }
            }    
            // Formata os dados para retorno
            for (Map.Entry<String, Map<String, String>> entry : operadorasConsolidadas.entrySet()) {
                Map<String, String> operadora = entry.getValue();
                dadosFormatados.add(formatarDadosOperadora(operadora));
            }
    
            logger.info("Total de operadoras processadas: {}", operadorasConsolidadas.size());
            return dadosFormatados;
        }
    
        private static Map<String, String> processarLinhaOperadora(Map<String, String> linha) {
            Map<String, String> linhaProcessada = new HashMap<>();
            
            if (linha.containsKey("Column0")) {
                String[] partes = linha.get("Column0").split("\\s+", 3);
                if (partes.length >= 3) {
                    linhaProcessada.put("Codigo", partes[0]);
                    linhaProcessada.put("Nome", partes[1]);
                    linhaProcessada.put("Tipo", partes[2]);
                    
                    // Copia os demais valores da linha
                    linha.forEach((key, value) -> {
                        if (!key.equals("Column0")) {
                            linhaProcessada.put(key, value);
                        }
                    });
                }
            }
            
            return linhaProcessada;
        }
    
        private static void processarValoresOperadora(Map<String, String> operadora, 
                                                     Map<String, String> linha, 
                                                     String tipo) {
            if ("BOL".equals(tipo)) {
                operadora.put("QtdBoletos", linha.getOrDefault("Column3", "0"));
                operadora.put("ValorBoletos", formatarValor(linha.getOrDefault("Column6", "0")));
                operadora.put("ComissaoBoletos", formatarValor(linha.getOrDefault("Column12", "0")));
            } 
            else if ("REC".equals(tipo)) {
                operadora.put("QtdRecibos", linha.getOrDefault("Column3", "0"));
                operadora.put("ValorRecibos", formatarValor(linha.getOrDefault("Column6", "0")));
                operadora.put("ComissaoRecibos", formatarValor(linha.getOrDefault("Column12", "0")));
            }
        }
    
        private static String formatarValor(String valor) {
            return valor.replace("R$", "").replace(".", "").replace(",", ".").trim();
        }
    
        private static String formatarDadosOperadora(Map<String, String> operadora) {
            return String.format(
                "Codigo: %s | Nome: %s | " +
                "QtdBoletos: %s | ValorBoletos: %s | ComissaoBoletos: %s | " +
                "QtdRecibos: %s | ValorRecibos: %s | ComissaoRecibos: %s",
                operadora.getOrDefault("Codigo", ""),
                operadora.getOrDefault("Nome", ""),
                operadora.getOrDefault("QtdBoletos", "0"),
                operadora.getOrDefault("ValorBoletos", "0.00"),
                operadora.getOrDefault("ComissaoBoletos", "0.00"),
                operadora.getOrDefault("QtdRecibos", "0"),
                operadora.getOrDefault("ValorRecibos", "0.00"),
                operadora.getOrDefault("ComissaoRecibos", "0.00")
            );
        }
    }

    public void processarRecebidoPorOperador(List<Map<String, String>> dadosLinhas, Map<String, Map<String, String>> tabelaNomePorPolo) {
        // Estrutura temporária para agrupar dados BOL e REC
        Map<String, Map<String, String>> dadosOperadoras = new HashMap<>();
    
        for (Map<String, String> linha : dadosLinhas) {
            String coluna0 = linha.get("Column0");
            
            // Verifica se é uma linha de operadora (BOL ou REC)
            if (coluna0 != null && (coluna0.contains("BOL") || coluna0.contains("REC"))) {
                // Extrai código, nome e tipo
                String[] partes = coluna0.split("\\s+");
                String codigo = partes[0];
                String nome = partes[1];
                String tipo = partes[2];
                
                // Cria chave única (código + nome)
                String chave = codigo + "_" + nome;
                
                // Inicializa a estrutura se não existir
                if (!dadosOperadoras.containsKey(chave)) {
                    dadosOperadoras.put(chave, new HashMap<>());
                    dadosOperadoras.get(chave).put("codigoOpe", codigo);
                    dadosOperadoras.get(chave).put("nomeOpe", nome);
                }
                
                // Processa os valores conforme o tipo (BOL ou REC)
                if (tipo.equals("BOL")) {
                    dadosOperadoras.get(chave).put("quantidadeBoleto", linha.get("Column3"));
                    dadosOperadoras.get(chave).put("valorBoleto", linha.get("Column6"));
                    dadosOperadoras.get(chave).put("comissaoBoleto", linha.get("Column12"));
                } else if (tipo.equals("REC")) {
                    dadosOperadoras.get(chave).put("quantidadeRecibo", linha.get("Column3"));
                    dadosOperadoras.get(chave).put("valorRecibo", linha.get("Column6"));
                    dadosOperadoras.get(chave).put("comissaoRecibo", linha.get("Column12"));
                }
            }
        }
        
        // Agora transfere os dados para a tabelaNomePorPolo
        for (Map<String, String> dadosOpe : dadosOperadoras.values()) {
            String codigo = dadosOpe.get("codigoOpe");
            String nome = dadosOpe.get("nomeOpe");
            
            // Cria a estrutura final na tabela principal
            if (!tabelaNomePorPolo.containsKey(codigo)) {
                tabelaNomePorPolo.put(codigo, new HashMap<>());
            }
            
            // Adiciona todos os campos
            tabelaNomePorPolo.get(codigo).put("Nome", nome);
            tabelaNomePorPolo.get(codigo).put("quantidadeBoleto", dadosOpe.getOrDefault("quantidadeBoleto", "0"));
            tabelaNomePorPolo.get(codigo).put("valorBoleto", dadosOpe.getOrDefault("valorBoleto", "0"));
            tabelaNomePorPolo.get(codigo).put("comissaoBoleto", dadosOpe.getOrDefault("comissaoBoleto", "0"));
            tabelaNomePorPolo.get(codigo).put("quantidadeRecibo", dadosOpe.getOrDefault("quantidadeRecibo", "0"));
            tabelaNomePorPolo.get(codigo).put("valorRecibo", dadosOpe.getOrDefault("valorRecibo", "0"));
            tabelaNomePorPolo.get(codigo).put("comissaoRecibo", dadosOpe.getOrDefault("comissaoRecibo", "0"));
        }
    }

    // Método para verificar os dados de uma operadora específica
    public void verificarOperadoraNoCache(String codigoOperadora) {
        Map<String, Map<String, String>> poloOPE = cache.getTabelaNomesPorPolo().get("OPE");
        
        if (poloOPE != null) {
            for (Map.Entry<String, Map<String, String>> entry : poloOPE.entrySet()) {
                Map<String, String> funcionario = entry.getValue();
                
                if (funcionario.get("Column1").equals(codigoOperadora)) {
                    System.out.println("\n=== DADOS DA OPERADORA " + codigoOperadora + " ===");
                    System.out.println("Nome: " + funcionario.get("Column3"));
                    System.out.println("Quantidade Boletos: " + funcionario.get("QuantidadeBoleto"));
                    System.out.println("Valor Boletos: " + funcionario.get("ValorBoleto"));
                    System.out.println("Comissão Boletos: " + funcionario.get("ComissaoBoleto"));
                    System.out.println("Quantidade Recibos: " + funcionario.get("QuantidadeRecibo"));
                    System.out.println("Valor Recibos: " + funcionario.get("ValorRecibo"));
                    System.out.println("Comissão Recibos: " + funcionario.get("ComissaoRecibo"));
                    return;
                }
            }
        }
        System.out.println("Operadora " + codigoOperadora + " não encontrada no polo OPE");
    } // Uso: verificarOperadoraNoCache("987"); // Para a operadora KETRY

    private static String formatarDadosOperadoraComoJson(Map<String, String> operadora) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(operadora);
    }
   

    private void processarAdministrativos(CacheDadosFuncionarios cache) {
        /* if (cache.possuiDadosPolo("ADM")) {
            List<Funcionario> funcionariosAdm = Funcionario.carregarFuncionarios(cache.getTabelaNomes("ADM"));
            List<String> demonstrativo = cache.getDemonstrativo("ADM");
            
            if (funcionariosAdm != null && !funcionariosAdm.isEmpty() && demonstrativo != null && !demonstrativo.isEmpty()) {

                if (demonstrativo == null || demonstrativo.size() < 2) {
                    throw new IllegalArgumentException("O demonstrativo está incompleto ou vazio!");
                }                
                String quantidadeStr = demonstrativo.get(0).split(" -> ")[1].trim();
                String valorStr = demonstrativo.get(1).split(" -> ")[1].trim();

                // Se algum dos valores for vazio, lance um erro ou atribua um padrão
                if (quantidadeStr.isEmpty() || valorStr.isEmpty()) {
                    throw new NumberFormatException("Os valores do demonstrativo não podem estar vazios!");
                }                
                
                for (Funcionario func : funcionariosAdm) {
                    func.setQuantidadePolo(Integer.parseInt(quantidadeStr));
                    func.setValorPolo(Double.parseDouble(valorStr));
                }
                
                IntegradorQuantidadeValor.integrarDadosDoPolo(funcionariosAdm, demonstrativo, ADM);
                //Funcionario.logFuncionarios(funcionariosAdm);
                logger.debug("Objeto de funcionarios do administrativos",funcionariosAdm);
                // Teste manual - criar uma operadora de teste               

                GeradorRelatorio.gerarRelatorio(funcionariosAdm,
                System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx");
            }
        } */
    }

    private String determinarPolo(String valorColuna) {
        logger.info("valorColuna {}",valorColuna);
        if (valorColuna == null || valorColuna.trim().isEmpty()) {
            return "OUTROS";
        }
        
        // Verifica prefixos conhecidos
        if (valorColuna.startsWith("OPE")) return "OPE";
        if (valorColuna.startsWith("ADM")) return "ADM";
        if (valorColuna.startsWith("MSG")) return "MSG";
        
        // Tenta extrair o código do funcionário (primeiros dígitos)
        String[] partes = valorColuna.split("\\s+");
        if (partes.length > 0 && partes[0].matches("\\d+")) {
            String codigo = partes[0];
            // Lógica adicional para mapear código para polo se necessário
        }
        
        return "OUTROS";
    }

    
    private void processarOperadores(CacheDadosFuncionarios cache, 
        Map<String, Map<String, String>> dadosFiltradosOperadoras,
        Map<String, Double> metasPorCodigo) {
            
        if (cache.possuiDadosPolo("OPE")) {
            List<Funcionario> funcionariosOpe = Funcionario.carregarFuncionarios(cache.getTabelaNomes("OPE"));
            
            if (funcionariosOpe != null && !funcionariosOpe.isEmpty()) {
                // Primeiro processa os dados das operadoras
                Map<String, Operadora> operadorasProcessadas = processarDadosOperadoras(dadosFiltradosOperadoras);
                
                // Associa as operadoras aos funcionários
                for (Funcionario funcionario : funcionariosOpe) {
                    if (operadorasProcessadas.containsKey(funcionario.getCodigo())) {
                        funcionario.setOperadora(operadorasProcessadas.get(funcionario.getCodigo()));                        
                    }
                    /* Operadora operadora = operadorasProcessadas.get(funcionario.getCodigo());
                    if (operadora != null) {
                        funcionario.setOperadora(operadora);
                    } */
                }
                
                // Processa as metas
                if (metasPorCodigo != null && !metasPorCodigo.isEmpty()) {
                    Funcionario.atribuirMetaOperadora(funcionariosOpe, metasPorCodigo);
                }
                
                logger.debug("Funcionários operadores processados: {}", funcionariosOpe);
            }
        }
    }

    private Map<String, Operadora> processarDadosOperadoras(Map<String, Map<String, String>> dadosOperadoras) {
        Map<String, Operadora> operadoras = new HashMap<>();
        
        for (Map.Entry<String, Map<String, String>> entry : dadosOperadoras.entrySet()) {
            try {
                Map<String, String> dados = entry.getValue();
                String[] partes = dados.get("Column0").split(" ");
                String codigo = partes[0];
                String nome = String.join(" ", Arrays.copyOfRange(partes, 1, partes.length - 1));
                String tipo = partes[partes.length - 1];
                
                Operadora operadora = operadoras.computeIfAbsent(codigo, k -> new Operadora(codigo, nome));
                
                int quantidade = Integer.parseInt(dados.getOrDefault("Column3", "0"));
                BigDecimal valor = parseMonetaryValue(dados.getOrDefault("Column6", "0"));
                BigDecimal comissao = parseMonetaryValue(dados.getOrDefault("Column12", "0"));
                
                if (tipo.equals("BOL")) {
                    operadora.adicionarBoleto(quantidade, valor, comissao);
                } else if (tipo.equals("REC")) {
                    operadora.adicionarRecibo(quantidade, valor, comissao);
                }
            } catch (Exception e) {
                logger.error("Erro ao processar dados de operadora: {}", entry.getKey(), e);
            }
        }
        
        return operadoras;
    }
      

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private BigDecimal parseMonetaryValue(String value) {
        try {
            String cleaned = value.replace("R$", "").replace(".", "").replace(",", ".").trim();
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    //dadosFiltradosGeralSituacaoMens criei o metodo para mensageiros
     private void processarMsg(CacheDadosFuncionarios cache, 
        Map<String, Map<String, String>> dadosFiltradosGeralSituacaoMens,
        Map<String, Double> metasPorCodigo) {
        if (cache.possuiDadosPolo("MSG")) {
            List<Funcionario> funcionariosMSG = Funcionario.carregarFuncionarios(cache.getTabelaNomes("MSG"));
            
            if (funcionariosMSG != null && !funcionariosMSG.isEmpty()) {
                if (dadosFiltradosGeralSituacaoMens != null && !dadosFiltradosGeralSituacaoMens.isEmpty()) {
                    Funcionario.processarDadosFiltrados(dadosFiltradosGeralSituacaoMens, funcionariosMSG);
                }
                
                if (metasPorCodigo != null && !metasPorCodigo.isEmpty()) {
                    Funcionario.atribuirMetaOperadora(funcionariosMSG, metasPorCodigo);
                }
                
               GeradorRelatorio.gerarRelatorio(funcionariosMSG,
                   System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx",dadosFiltradosGeralSituacaoMens);
            }
        }
        
    } 

    private void processarPolo(CacheDadosFuncionarios cache, String polo, String codigoPolo) {
        if (cache.possuiDadosPolo(polo)) {
            try {
                // 1. Obter dados brutos
                Map<String, String> dadosBrutos = cache.getTabelaNomes(polo);
    
                // 2. Converter para lista de funcionários
                List<Funcionario> funcionarios = converterDadosParaFuncionarios(dadosBrutos, polo);
    
                // 3. Processar demonstrativo
                DemonstrativoDados demonstrativo = cache.getDemonstrativo(polo, "");
    
                if (!funcionarios.isEmpty() && demonstrativo != null && demonstrativo.getQuantidade() >= 2) {
                    // Separar administrativos e funcionários normais
                    List<Funcionario> administrativos = new ArrayList<>();
                    List<Funcionario> outrosFuncionarios = new ArrayList<>();
    
                    for (Funcionario f : funcionarios) {
                        if (f.isAdministrativo()) {  // Supondo que tenha um método que identifica administrativos
                            administrativos.add(f);
                        } else {
                            outrosFuncionarios.add(f);
                        }
                    }
    
                    // Atualizar valores dos funcionários normais
                    atualizarValoresPolo(outrosFuncionarios, demonstrativo);
    
                    // Atualizar valores administrativos (caso precise de lógica específica)
                   /*  if (!administrativos.isEmpty()) {
                        atualizarValoresAdministrativo(administrativos, demonstrativo);
                    } */
    
                    // Gerar relatório
                    String caminhoSaida = System.getProperty("user.dir") + "/output/Planilha-Pagamento.xlsx";
                    GeradorRelatorio.gerarRelatorio(funcionarios, caminhoSaida);
    
                    // Debug: mostrar funcionários processados
                    System.out.println("=== FUNCIONÁRIOS PROCESSADOS ===");
                    funcionarios.forEach(f -> System.out.println(
                        f.getCodigo() + " | " + f.getNome() + " | Polo: " + f.getPolo() +
                        " | ValorPolo: " + f.getValorPolo() + " | QuantidadePolo: " + f.getQuantidadePolo()));
                } else {
                    System.err.println("Demonstrativo inválido ou lista de funcionários vazia para o polo " + polo);
                }
            } catch (Exception e) {
                System.err.println("ERRO ao processar polo " + polo + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
  
   
    private List<Funcionario> converterDadosParaFuncionarios(Map<String, String> dadosBrutos, String polo) {
        List<Funcionario> funcionarios = new ArrayList<>();
        
        // Agrupar dados por linha (funcionário)
        Map<String, Map<String, String>> dadosPorLinha = new HashMap<>();
        
        for (Map.Entry<String, String> entry : dadosBrutos.entrySet()) {
            String[] parts = entry.getKey().split(" - ");
            if (parts.length == 2) {
                String linha = parts[0];
                String coluna = parts[1];
                
                dadosPorLinha.computeIfAbsent(linha, k -> new HashMap<>())
                            .put(coluna, entry.getValue());
            }
        }
        
        // Converter para objetos Funcionario
        for (Map<String, String> linha : dadosPorLinha.values()) {
            try {
                Funcionario func = new Funcionario();
                func.setCodigo(linha.get("Column1"));
                func.setDrt(linha.get("Column2"));
                func.setNome(linha.get("Column3"));
                func.setPolo(linha.get("Column5"));
                func.setSetor(linha.get("Column5")); // Setor = Código do polo                
                
                // Identificar tipo (Mensageiro, Operadora, Administrativo)
                String tipo = linha.get("Column0");
                if (tipo.equalsIgnoreCase("MSG")) {
                    func.setTipoRegistro("MENSAGEIRO");
                } else if (tipo.equalsIgnoreCase("OPE")) {
                    func.setTipoRegistro("OPERADORA");
                } else if (tipo.equalsIgnoreCase("ADM")) {
                    func.setTipoRegistro("ADMINISTRATIVO");
                }

               /*  // Se existir o método setSetor
                if (Arrays.stream(func.getClass().getMethods())
                        .anyMatch(m -> m.getName().equals("setSetor"))) {
                    func.setSetor(linha.getOrDefault("Column5", ""));
                }

                // Identificar tipo
                String tipo = linha.getOrDefault("Column0", "MSG"); // Default como mensageiro
                switch (tipo.toUpperCase()) {
                    case "OPE":
                        func.setTipoRegistro("OPERADORA");
                        break;
                    case "ADM":
                        func.setTipoRegistro("ADMINISTRATIVO");
                        break;
                    default:
                        func.setTipoRegistro("MENSAGEIRO");
                } */
                    
                funcionarios.add(func);
            } catch (Exception e) {
                System.err.println("Erro ao converter linha: " + linha);
                e.printStackTrace();
            }
        }
        
        return funcionarios;
    }

    private void atualizarValoresPolo(List<Funcionario> funcionarios, DemonstrativoDados demonstrativo) {
        try {
            int qtdPolo = demonstrativo.getQuantidade();
            BigDecimal valorPolo = demonstrativo.getValor();
    
            for (Funcionario func : funcionarios) {
                func.setQuantidadePolo(qtdPolo);
                func.setValorPolo(valorPolo);
            }
        } catch (Exception e) {
            System.err.println("ERRO ao processar demonstrativo: " + e.getMessage());
        }
    }
    
       
    // Métodos auxiliares
    private int parseValor(String str) {
        try {
            String[] parts = str.split("->");
            return Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public class JsonFormatter {
        public static String visualizarListaComoJson(List<String> linhasProcessadas) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(linhasProcessadas);
        }
        
        public static String visualizarComoJson(List<?> lista) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            return gson.toJson(lista);
        }
    }
    
     private double parseValorMonetario(String str) {
        try {
            String[] parts = str.split("->");
            return Double.parseDouble(parts[1].replace("R$", "").replace(".", "").replace(",", ".").trim());
        } catch (Exception e) {
            return 0.0;
        }        
    } 
   
    public void processarSetores(String caminhoSetores) throws IOException {
        Map<String, List<Map<String, String>>> dadosFinais = dataProcessor.processarSetores(caminhoSetores);
        ExcelWriter excelWriter = new ExcelWriter();
        excelWriter.escreverDados((List<Map<String, Map<String, String>>>) resultadosParaExcel);
    }

    /**
     * Converte os resultados para um formato adequado para escrita em Excel.
     */
    private List<Map<String, Map<String, String>>> convertToExcelFormat() {
        return resultadosParaExcel.entrySet().stream()
                .map(entry -> Map.of(entry.getKey(), entry.getValue().stream()
                        .map(line -> line.split(" -> "))
                        .filter(parts -> parts.length == 2)
                        .collect(Collectors.toMap(parts -> parts[0].trim(), parts -> parts[1].trim()))))
                .collect(Collectors.toList());
    }

    /**
     * Formata valores numéricos para exibição como moeda.
     */
    private String formatCurrency(double value) {
        return new DecimalFormat("R$ #,##0.00").format(value);
    }

    @Override
    public Map<String, Map<String, String>> getData() {
        return fileData;
    }

    public List<Map<String, String>> getExtractedData() {
        return extractedData;
    }

    public void clearExtractedData() {
        extractedData.clear();
    }

    private Map<String, String> formatarDadosDoMapa(Map<String, String> dados, String linhaReferencia) {
        Map<String, String> dadosFormatados = new HashMap<>();

        dadosFormatados.put("Caixa", dados.get(linhaReferencia + " - Column1"));
        dadosFormatados.put("QTD", dados.get(linhaReferencia + " - Column7"));
        dadosFormatados.put("Valor", dados.get(linhaReferencia + " - Column9"));

        return dadosFormatados;
    }

    /**
     * Metódo responsável para unificar todos os dados em uma lista de mapa
     * @param data
     * @return Retorna uma lista de mapa de string merged
     */
    private Map<String, String> mergeData(List<Map<String, String>> data) {
        Map<String, String> merged = new HashMap<>();
        int rowIndex = 1;
        for (Map<String, String> row : data) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                merged.put("Linha " + rowIndex + " - " + entry.getKey(), entry.getValue());
            }
            rowIndex++;
        }
        // System.out.println(" XlsProcessor L116 "+merged);
        return merged;
    }

    private TreeMap<String, String> ordenarMapaInterno(Map<String, String> mapa) {
        return new TreeMap<>(mapa);
    }

    // Função para localizar e exibir valores com base no nome procurado
    public static void buscarValoresPorNome(Map<String, Map<String, String>> fileData, String arquivo,
            String nomeProcurado) {
        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        boolean encontrou = false;
        nomeProcurado = normalizarTexto(nomeProcurado);

        // Percorrendo todas as chaves do mapa para encontrar correspondências
        for (Map.Entry<String, String> entry : dadosArquivo.entrySet()) {
            String chave = entry.getKey();
            String valor = normalizarTexto(entry.getValue().trim());

            // Verificando se a chave contém o nome procurado
            if (valor.equals(nomeProcurado)) {
                // System.out.println("Encontrado: " + chave + " -> " + valor);
                encontrou = true;

                // Extraindo o número da linha corretamente
                String[] partes = chave.split(" - ");
                String linhaBase = partes.length > 1 ? partes[0] : ""; // Exemplo: "Linha 17"
                // System.out.println("Valores completos da " + linhaBase + ":");

                for (Map.Entry<String, String> linhaEntry : dadosArquivo.entrySet()) {
                    if (linhaEntry.getKey().startsWith(linhaBase)) {
                        // System.out.println(" " + linhaEntry.getKey() + " -> " +
                        // linhaEntry.getValue());
                    }
                }
                break; // Interrompe a busca após encontrar a primeira ocorrência
            }
        }

        if (!encontrou) {
            System.out.println("Nenhum resultado encontrado para: " + nomeProcurado);
        }
    }

    public static List<String> buscarValoresPorNomeRetonarLista(Map<String, Map<String, String>> fileData,
            String arquivo, String nomeProcurado) {
        List<String> resultados = new ArrayList<>();

        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado.");
            return resultados; // Retorna lista vazia caso o arquivo não seja encontrado
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        nomeProcurado = normalizarTexto(nomeProcurado);

        // Usando TreeMap para garantir ordenação sem sobrescrever valores repetidos
        TreeMap<String, List<String>> resultadosOrdenados = new TreeMap<>();

        for (Map.Entry<String, String> entry : dadosArquivo.entrySet()) {
            String chave = entry.getKey();
            String valor = normalizarTexto(entry.getValue().trim());

            if (valor.equals(nomeProcurado)) {
                // System.out.println("Encontrado: " + chave + " -> " + valor);

                // Extraindo a linha base (exemplo: "Linha 17")
                String[] partes = chave.split(" - ");
                String linhaBase = partes.length > 1 ? partes[0] : "";

                // Adiciona chave e valor a uma lista para evitar sobrescrita
                resultadosOrdenados.putIfAbsent(linhaBase, new ArrayList<>());
                resultadosOrdenados.get(linhaBase).add(chave + " -> " + valor);

                for (Map.Entry<String, String> linhaEntry : dadosArquivo.entrySet()) {
                    if (linhaEntry.getKey().startsWith(linhaBase)) {
                        resultadosOrdenados.get(linhaBase).add(linhaEntry.getKey() + " -> " + linhaEntry.getValue());
                    }
                }
            }
        }

        if (resultadosOrdenados.isEmpty()) {
            System.out.println("Nenhum resultado encontrado para: " + nomeProcurado);
        } else {
            for (List<String> lista : resultadosOrdenados.values()) {
                resultados.addAll(lista);
            }
        }

        return resultados; // Retorna os resultados sem sobrescrever
    }

    public static DemonstrativoDados buscarQuantidadeEValor(Map<String, Map<String, String>> fileData, String arquivo, String nomeProcurado) {
        System.out.println("DEBUG - Chave encontrada: " + arquivo);
        // Verifica se fileData contém a chave
        if (!fileData.containsKey(arquivo)) {
            logger.error("Erro: arquivo '{}' não encontrado no mapa de dados!", arquivo);
            return null;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);

        // Verifica se dadosArquivo não é null
        if (dadosArquivo == null) {
            logger.error("Erro: os dados para o arquivo '{}' estão nulos!", arquivo);
            return new DemonstrativoDados(0, BigDecimal.ZERO); // Retorna um objeto vazio em caso de erro
        }

        logger.debug("L1857  FILE DATA {}  ARQUIVO {}  NOME PROCU ADO {} ", fileData, arquivo, nomeProcurado );

        Object valorObj = fileData.get(arquivo);
        System.out.println("DEBUG - Valor obtido para a chave " + arquivo + ": " + valorObj);

        //Map<String, String> dadosArquivo = fileData.get(arquivo);
        nomeProcurado = normalizarTexto(nomeProcurado);
        int quantidade = 0;
        BigDecimal valor = BigDecimal.ZERO;

        for (Map.Entry<String, String> entry : dadosArquivo.entrySet()) {
            String chave = entry.getKey();
            String valorStr = normalizarTexto(entry.getValue().trim());

            if (valorStr.contains(nomeProcurado)) {
                String[] partes = chave.split(" - ");
                String linhaBase = partes.length > 1 ? partes[0] : "";

                for (Map.Entry<String, String> linhaEntry : dadosArquivo.entrySet()) {
                    if (linhaEntry.getKey().startsWith(linhaBase)) {

                        if (linhaEntry.getKey().contains("Column10")) {
                            String valorString = linhaEntry.getValue().trim();
                            if (!valorString.isEmpty()) {
                                try {
                                    quantidade = Integer.parseInt(valorString);
                                } catch (NumberFormatException e) {
                                    logger.error("Erro ao converter '{}' para inteiro (Column10)", valorString);
                                }
                            } else {
                                logger.warn("Column10 está vazia, valor padrão 0 será usado.");
                            }
                        } else if (linhaEntry.getKey().contains("Column12")) {
                            String valorString = linhaEntry.getValue().trim();
                            if (!valorString.isEmpty()) {
                                try {
                                    valor = new BigDecimal(valorString);
                                } catch (NumberFormatException e) {
                                    logger.error("Erro ao converter '{}' para BigDecimal (Column12)", valorString);
                                }
                            } else {
                                logger.warn("Column12 está vazia, valor padrão ZERO será usado.");
                            }
                        }                             
                    }
                }
                break;
            }
        }

        DemonstrativoDados resultado = new DemonstrativoDados(quantidade, valor);
        System.out.println("Resultados encontrados: " + resultado);
        logger.info("Resultados encontrados: {}", resultado);
        return resultado;
    }

    public static List<String> encontraQuantidadeEValor(Map<String, Map<String, String>> fileData, String arquivo,
            String nomeProcurado) {
        List<String> resultados = new ArrayList<>();

        if (!fileData.containsKey(arquivo)) {
            System.out.println("Arquivo não encontrado: " + arquivo);
            return resultados;
        }

        Map<String, String> dadosArquivo = fileData.get(arquivo);
        nomeProcurado = normalizarTexto(nomeProcurado);

        for (Map.Entry<String, String> entry : dadosArquivo.entrySet()) {
            String chave = entry.getKey(); // Ex: "19 - Column2"
            String valor = entry.getValue().trim();

            // Verifica se é a linha que contém "Total (Recebido..."
            if (valor.contains("Total (Recebido") || valor.contains(nomeProcurado)) {
                String[] partesChave = chave.split(" - ");
                String linha = partesChave[0]; // Ex: "19"

                // 1. Busca a QUANTIDADE (10ª coluna - Column9)
                String chaveQuantidade = linha + " - Column10"; // Ajuste o índice conforme sua planilha
                String quantidade = dadosArquivo.get(chaveQuantidade);
                if (quantidade != null) {
                    resultados.add("quantidadePolo -> " + quantidade.trim());
                }

                // 2. Busca o VALOR (12ª coluna - Column11)
                String chaveValor = linha + " - Column12"; // Ajuste o índice conforme sua planilha
                String valorMonetario = dadosArquivo.get(chaveValor);
                if (valorMonetario != null) {
                    // Formatação do valor monetário
                    String valorNumerico = valorMonetario.trim()
                            .replace("R$", "")
                            .replace(".", "")
                            .replace(",", ".");
                    resultados.add("valorPolo -> " + valorNumerico);
                }
                break; // Sai do loop após encontrar
            }
        }

        return resultados;
    }

    // Normaliza o texto removendo espaços extras e caracteres invisíveis
    public static String normalizarTexto(String texto) {
        if (texto == null)
            return "";
        return texto.replaceAll("[\\s\u00A0\u200B]+", " ").trim();// .toLowerCase()
    }

    // Método para extrair o número da coluna

    private static long extrairNumeroDaColuna(String coluna) {
        String numeros = coluna.replaceAll("[^0-9]", "");
        return numeros.isEmpty() ? 0 : Long.parseLong(numeros);
    }

    public void processar() {
        int totalFiles = 100;
        int processedFiles = 0;

        for (int i = 0; i < totalFiles; i++) {
            // Lógica de processamento...

            // Atualiza a barra de progresso
            barraProgresso.atualizarProgresso(processedFiles++, totalFiles);

            // Simulação de processamento (substitua por seu código real)
            try {
                Thread.sleep(500); // Simula um pequeno delay
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    } 
    // Método para exibir o mapa formatado
    private void exibirMapaFormatado(String polo, Map<String, Map<String, String>> mapa) {
        System.out.println("Polo: " + polo);
        for (Map.Entry<String, Map<String, String>> entry : mapa.entrySet()) {
            System.out.println("Linha " + entry.getKey() + ":");
            Map<String, String> valores = entry.getValue();
            for (Map.Entry<String, String> valor : valores.entrySet()) {
                System.out.println("  " + valor.getKey() + " -> " + valor.getValue());
            }
            System.out.println();
        }
    }

    /**
     * Metódo para converter estrutura TreeMap<String, Map<String, String>> fileData em uma estrutura formatada para Map<String, Map<String, String>>
     * @param adaptarEstrutura
     * @return Retorna a estrutura aninhada formatada para Map<String, Map<String, String>>
     * 
    */
    private Map<String, Map<String, String>> adaptarEstrutura(Map<String, String> mapaPlano) {
        Map<String, Map<String, String>> estruturaAninhada = new TreeMap<>();
        
        for (Map.Entry<String, String> entry : mapaPlano.entrySet()) {
            String[] parts = entry.getKey().split(" - ");
            String linha = parts[0];
            String coluna = parts.length > 1 ? parts[1] : "DEFAULT";
            
            estruturaAninhada.computeIfAbsent(linha, k -> new TreeMap<>())
                            .put(coluna, entry.getValue());
        }
        
        return estruturaAninhada;
    }

    private Map<String, String> processarColumn0(Map<String, String> dadosBrutos) {
        if (!dadosBrutos.containsKey("Column0")) {
            return dadosBrutos;
        }

        String valorOriginal = dadosBrutos.get("Column0").trim();
        Map<String, String> dadosProcessados = new HashMap<>(dadosBrutos);
        
        // Expressão regular atualizada:
        // ^(\d+) - código (1 ou mais dígitos)
        // \s+ - espaços
        // ([A-Za-zÀ-ú\s]+?) - nome (letras e espaços, mínimo possível)
        // \s+ - espaços
        // (REC|BOL|DEP|OUT|CHE|PIX)$ - tipo (3 letras, pode adicionar mais)
        Pattern pattern = Pattern.compile("^(\\d+)\\s+([A-Za-zÀ-ú\\s]+?)\\s+(REC|BOL|DEP|OUT|CHE|PIX)$");
        Matcher matcher = pattern.matcher(valorOriginal);
        
        if (matcher.find()) {
            dadosProcessados.put("Column0", matcher.group(1).trim());  // Código (pode ter mais de 3 dígitos)
            dadosProcessados.put("NomeOPE", matcher.group(2).trim());  // Nome (sem espaços extras)
            dadosProcessados.put("TipoOpe", matcher.group(3).trim());  // Tipo
        } else {
            logger.warn("Formato inválido para Column0: '{}'", valorOriginal);
            // Tenta fallback mais simples para códigos longos
            String[] partes = valorOriginal.split("\\s+", 3);
            if (partes.length >= 3) {
                dadosProcessados.put("Column0", partes[0]);
                dadosProcessados.put("NomeOPE", partes[1]);
                dadosProcessados.put("TipoOpe", partes[2]);
            } else {
                // Mantém o original se não conseguir parsear
                dadosProcessados.put("Column0", valorOriginal);
            }
        }
        
        return dadosProcessados;
    }

    public Map<String, String> processarAColumn0(Map<String, String> dadosBrutos) {
        Map<String, String> dadosProcessados = new HashMap<>(dadosBrutos);
        
        if (dadosBrutos.containsKey("Column0")) {
            String valorOriginal = dadosBrutos.get("Column0").trim();
            
            // Padrão: "014 JANICE BOL" ou "076 CRISTINA LUIZA BOL"
            Pattern pattern = Pattern.compile("^(\\d+)\\s+([A-Z].+?)\\s+(BOL|REC)$");
            Matcher matcher = pattern.matcher(valorOriginal);
            
            if (matcher.find()) {
                dadosProcessados.put("Column0", matcher.group(1)); // Código
                dadosProcessados.put("NomeOPE", matcher.group(2).trim()); // Nome
                dadosProcessados.put("TipoOpe", matcher.group(3)); // Tipo
            }
        }
        
        return dadosProcessados;
    }
    
    /**
    * Retorna um mapa imutável contendo os dados processados.
    * O mapa associa nomes de arquivos a listas de objetos {@code XlsData}.
    *
    * @return Um {@code Map<String, List<XlsData>>} imutável contendo os dados processados.
    */
    public Map<String, List<XlsData>> getProcessedData() {
        return Collections.unmodifiableMap(processedData);
    }

    /**
    * Registra no log os dados processados, incluindo o nome do arquivo e a quantidade de registros.
    * Para tipos específicos de {@code XlsData}, imprime detalhes adicionais no log.
    */
    public void logProcessedData() {
        logger.info("=== DADOS PROCESSADOS ===");
        for (Map.Entry<String, List<XlsData>> entry : processedData.entrySet()) {
            logger.info("Arquivo: {}", entry.getKey());
            logger.info("Registros: {}", entry.getValue().size());
            
            for (XlsData data : entry.getValue()) {
                logger.debug("Detalhes: {}", data);
                // Se precisar de mais detalhes específicos:
                if (data instanceof RecebidoOperador) {
                    RecebidoOperador operador = (RecebidoOperador) data;
                    logger.debug("Operador: {} - {}", operador.getCodigo(), operador.getNome());
                }

                else if (data instanceof MetaParaReciboPorOperador) {
                MetaParaReciboPorOperador meta = (MetaParaReciboPorOperador) data;
                logger.debug("Meta Operador: {} - DRT {} - Valor: {}", 
                    meta.getCodigo(), meta.getDrt(), meta.getValorLiquido());
                }
            }
        }
    } 
    
    public void visualizarComoJson(List<XlsData> objectsList) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        
        for (XlsData data : objectsList) {
            String json = gson.toJson(data);
            System.out.println(json);
            
            // Ou para o log:
            logger.debug("Objeto JSON:\n{}", json);
        }
    }

}