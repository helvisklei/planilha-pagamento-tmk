package com.tmkfolha.app.controllers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import com.tmkfolha.processor.XlsProcessor;

import org.apache.poi.ss.util.CellRangeAddress;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class GeradorRelatorio {
    private static final Logger logger = LogManager.getLogger(GeradorRelatorio.class);

    // Cores em RGB
    private static final XSSFColor COR_AZUL = new XSSFColor(new java.awt.Color(197, 217, 241), null);
    private static final XSSFColor COR_VERMELHA = new XSSFColor(new java.awt.Color(255, 0, 0), null);
    private static final XSSFColor COR_ROSA = new XSSFColor(new java.awt.Color(255, 192, 203), null);
    private static final XSSFColor COR_CINZA = new XSSFColor(new java.awt.Color(166, 166, 166), null);
    private final Map<String, Funcionario> funcionariosPorCodigo = new HashMap<>();
    

    /* public static void gerarRelatorio(List<Funcionario> funcionarios, String caminhoSaida) {
        gerarRelatorio(funcionarios, caminhoSaida, null);
    } */
    public static void gerarRelatorio(List<Funcionario> funcionarios, String caminhoSaida) {

        
        // Dividir por polo
        Map<String, List<Funcionario>> polosMap = dividirPorPolo(funcionarios);
        
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Criar abas
            XSSFSheet sheetDPFolha = workbook.createSheet("DP FOLHA");
            XSSFSheet sheetDPFolhaOrigem = workbook.createSheet("DP FOLHA ORIGEM");
            XSSFSheet sheetOperadora = workbook.createSheet("OPERADORA");
            
            // Preencher abas
           /*  preencherAbaDPFolha(sheetDPFolha, polosMap);
            preencherAbaDPFolhaOrigem(sheetDPFolhaOrigem, polosMap);
            preencherAbaOperadora(sheetOperadora, polosMap); */
            
            // Salvar arquivo
            try (FileOutputStream fos = new FileOutputStream(caminhoSaida)) {
                workbook.write(fos);
            }
        } catch (IOException e) {
            logger.error("Erro ao gerar relatório", e);
        }
    }
    public static void gerarRelatorio(List<Funcionario> funcionarios, String caminhoSaida, 
                                Map<String, Map<String, String>> dadosOperadoras) {
            // 1. Processar dados das operadoras se existirem
        //if (dadosOperadoras != null && !dadosOperadoras.isEmpty()) {
            processarDadosOperadoras(funcionarios, dadosOperadoras);
       // }            
            // Restante do fluxo permanece igual
        logFuncionariosProcessados(funcionarios);
        calcularCamposDerivados(funcionarios);
        Map<String, List<Funcionario>> polosMap = dividirPorPolo(funcionarios);
        //gerarExcelRelatorio(funcionarios, polosMap, caminhoSaida);
        // Modificação principal - não limpar automaticamente
         atualizarExcelRelatorio(funcionarios, polosMap, caminhoSaida);
    }
    
    

    private static void atualizarExcelRelatorio(List<Funcionario> funcionarios, 
                                          Map<String, List<Funcionario>> polosMap,
                                          String caminhoSaida) {
            System.out.println("\nAtualizando relatório em: " + caminhoSaida);
            File arquivo = new File(caminhoSaida);
            
            try (XSSFWorkbook workbook = arquivo.exists() ? 
                new XSSFWorkbook(new FileInputStream(arquivo)) : new XSSFWorkbook()) {
                
                // Removemos a chamada para limparAbasExistente
                
                atualizarAbaDPFolha(workbook, polosMap);
                atualizarAbaDPFolhaOrigem(workbook, polosMap);
                atualizarAbaOperadora(workbook, polosMap);
                
                try (FileOutputStream fos = new FileOutputStream(arquivo)) {
                    workbook.write(fos);
                    System.out.println("\nRelatório atualizado com sucesso!");
                }
            } catch (IOException e) {
                System.err.println("ERRO ao atualizar relatório: " + e.getMessage());
            }
    }
    private static void atualizarAbaDPFolha(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        XSSFSheet sheet = workbook.getSheet("DP FOLHA");
        if (sheet == null) {
            sheet = workbook.createSheet("DP FOLHA");
            sheet.setTabColor(COR_AZUL);
           // preencherAbaDPFolha(sheet, polosMap); // Preenche do zero se nova aba
            return;
        }
        
        // Lógica para atualizar dados existentes
        Map<String, Integer> linhaPorCodigo = mapearLinhasPorCodigo(sheet);
        
        for (List<Funcionario> funcionariosPolo : polosMap.values()) {
            for (Funcionario func : funcionariosPolo) {
                Integer linha = linhaPorCodigo.get(func.getCodigo());
                if (linha != null) {
                    // Atualiza linha existente
                    atualizarLinhaDPFolha(sheet.getRow(linha), func);
                } else {
                    // Adiciona nova linha no final
                    Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                    preencherLinhaDPFolha(row, func);
                }
            }
        }
    }
    private static void preencherLinhaDPFolha(Row row, Funcionario func) {
        if (row == null || func == null) {
            throw new IllegalArgumentException("Row e Funcionário não podem ser nulos");
        }
        
        XSSFWorkbook workbook = (XSSFWorkbook) row.getSheet().getWorkbook();
        
        try {
            XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
            XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
            
            // Como são tipos primitivos double, não precisamos verificar null
            BigDecimal rendimento = func.getRendimento();
            BigDecimal gratificacaoPercentual = func.getGratificacaoPercentual();
            BigDecimal gratificacaoValor = func.getGratificacaoValor();
            double quantidade = func.getQuantidade();
            double valor = func.getValor();
            BigDecimal recebimento = func.getRecebimento();
            BigDecimal valorPolo = func.getValorPolo();
            
            criarCelula(row, 0, func.getCodigo(), null);
            criarCelula(row, 1, func.getDrt(), null);
            criarCelula(row, 2, func.getNome(), null);
            criarCelula(row, 3, recebimento, estiloMoeda);
            criarCelula(row, 4, rendimento.multiply(BigDecimal.valueOf(100)), estiloPercentual);
            criarCelula(row, 5, valorPolo.multiply(BigDecimal.valueOf(100)), estiloPercentual);
            //criarCelula(row, 5, gratificacaoPercentual * 100, estiloPercentual);
            criarCelula(row, 6, gratificacaoValor, estiloMoeda);
            criarCelula(row, 7, quantidade, null);
            criarCelula(row, 8, valor, estiloMoeda);
        } catch (Exception e) {
            System.err.println("Erro ao preencher linha para funcionário " + func.getCodigo() + ": " + e.getMessage());
            throw e;
        }
    }

    private static void atualizarAbaOperadora(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        XSSFSheet sheet = workbook.getSheet("OPERADORA");
        if (sheet == null) {
            sheet = workbook.createSheet("OPERADORA");
            sheet.setTabColor(COR_ROSA);
        } else {
            limparAbaExistente(sheet);
        }
        
        // Cria cabeçalhos
        Row rowTitulo = sheet.createRow(0);
        criarCelula(rowTitulo, 0, "OPERADORAS - DADOS DETALHADOS", criarEstiloTitulo(workbook));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));
        
        Row rowCabecalho = sheet.createRow(1);
        String[] cabecalhos = {
            "CÓD.", "NOME", 
            "QTD BOL", "VALOR BOL", "COMISSÃO BOL", 
            "QTD REC", "VALOR REC", "COMISSÃO REC",
            "TOTAL"
        };
        for (int i = 0; i < cabecalhos.length; i++) {
            criarCelula(rowCabecalho, i, cabecalhos[i], criarEstiloCabecalho(workbook));
        }
        
        // Estilos
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloNumero = workbook.createCellStyle();
        
        // Filtra funcionários que são operadoras
        List<Funcionario> operadoras = polosMap.values().stream()
            .flatMap(List::stream)
            .filter(Funcionario::isOperadora)
            .collect(Collectors.toList());
        
        // Preenche os dados
        int linhaAtual = 2;
        for (Funcionario func : operadoras) {
            Operadora op = func.getOperadora();
            Row row = sheet.createRow(linhaAtual++);
            
            criarCelula(row, 0, func.getCodigo(), null);
            criarCelula(row, 1, func.getNome(), null);
            
            // Dados de boletos
            criarCelula(row, 2, op.getQuantidadeBoleto(), estiloNumero);
            criarCelula(row, 3, op.getValorBoleto(), estiloMoeda);
            criarCelula(row, 4, op.getComissaoBoleto(), estiloMoeda);
            
            // Dados de recibos
            criarCelula(row, 5, op.getQuantidadeRecibo(), estiloNumero);
            criarCelula(row, 6, op.getValorRecibo(), estiloMoeda);
            criarCelula(row, 7, op.getComissaoRecibo(), estiloMoeda);
            
            // Total
            BigDecimal total = op.getValorBoleto().add(op.getValorRecibo());
            criarCelula(row, 8, total, estiloMoeda);
        }
        
        // Ajusta largura das colunas
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }

   /*  private static void atualizarAbaOperadora(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        XSSFSheet sheet = workbook.getSheet("OPERADORA");
        if (sheet == null) {
            sheet = workbook.createSheet("OPERADORA");
            sheet.setTabColor(COR_ROSA);
            preencherAbaOperadora(sheet, polosMap); // Preenche do zero se nova aba
            return;
        }
        
        // Mapeia linhas existentes pelo código da operadora
        Map<String, Integer> linhaPorCodigo = new HashMap<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                String codigo = row.getCell(0).getStringCellValue();
                if (codigo != null && !codigo.isEmpty()) {
                    linhaPorCodigo.put(codigo, i);
                }
            }
        }
        
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        
        // Filtra apenas operadoras (polo 1)
        List<Funcionario> operadoras = polosMap.values().stream()
            .flatMap(List::stream)
            .filter(f -> f.getPolo() != null && f.getPolo().equals("1") && 
                f.getTipoRegistro().equalsIgnoreCase("OPERADORA"))
            .collect(Collectors.toList());
        
        // Atualiza ou adiciona operadoras
        for (Funcionario op : operadoras) {
            Integer linha = linhaPorCodigo.get(op.getCodigo());
            if (linha != null) {
                // Atualiza linha existente
                Row row = sheet.getRow(linha);
                criarCelula(row, 2, op.getMetaOperadora(), estiloMoeda);
                criarCelula(row, 3, op.getRecebimento(), estiloMoeda);
                
                double percentual = op.getMetaOperadora() > 0 ? 
                    op.getRecebimento() / op.getMetaOperadora() : 0;
                criarCelula(row, 4, percentual * 100, estiloPercentual);
                
                double gratificacao = GratificacaoCalculator.calcularGratificacaoOperadora(
                    op.getRecebimento(), 
                    op.getGratificacaoPercentual()
                );
                criarCelula(row, 5, gratificacao, estiloMoeda);
            } else {
                // Adiciona nova linha no final
                Row row = sheet.createRow(sheet.getLastRowNum() + 1);
                criarCelula(row, 0, op.getCodigo(), null);
                criarCelula(row, 1, op.getNome(), null);
                criarCelula(row, 2, op.getMetaOperadora(), estiloMoeda);
                criarCelula(row, 3, op.getRecebimento(), estiloMoeda);
                
                double percentual = op.getMetaOperadora() > 0 ? 
                    op.getRecebimento() / op.getMetaOperadora() : 0;
                criarCelula(row, 4, percentual * 100, estiloPercentual);
                
                double gratificacao = GratificacaoCalculator.calcularGratificacaoOperadora(
                    op.getRecebimento(), 
                    op.getGratificacaoPercentual()
                );
                criarCelula(row, 5, gratificacao, estiloMoeda);
            }
        }
        
        // Atualiza cabeçalhos e formatação se necessário
        if (sheet.getRow(0) == null || !"OPERADORAS - DADOS CONSOLIDADOS".equals(sheet.getRow(0).getCell(0).getStringCellValue())) {
            Row rowTitulo = sheet.createRow(0);
            criarCelula(rowTitulo, 0, "OPERADORAS - DADOS CONSOLIDADOS", criarEstiloTitulo(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            
            Row rowCabecalho = sheet.createRow(1);
            String[] cabecalhos = {"CÓD.", "NOME", "META", "RECEBIDO", "PERCENTUAL", "GRATIFICAÇÃO"};
            for (int i = 0; i < cabecalhos.length; i++) {
                criarCelula(rowCabecalho, i, cabecalhos[i], criarEstiloCabecalho(workbook));
            }
        }
        
        // Ajusta largura das colunas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    } */
    // Método para atualizar uma linha existente
    private static void atualizarLinhaDPFolha(Row row, Funcionario func) {
        XSSFWorkbook workbook = (XSSFWorkbook) row.getSheet().getWorkbook();
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        
        // Atualiza apenas os campos que podem mudar
        criarCelula(row, 3, func.getRecebimento(), estiloMoeda);
        criarCelula(row, 4, func.getRendimento().multiply(BigDecimal.valueOf(100)), estiloPercentual);
        criarCelula(row, 5, func.getGratificacaoPercentual().multiply(BigDecimal.valueOf(100)), estiloPercentual);
        criarCelula(row, 6, func.getGratificacaoValor(), estiloMoeda);
        criarCelula(row, 7, func.getQuantidade(), null);
        criarCelula(row, 8, func.getValor(), estiloMoeda);
    }

    private static void atualizarAbaDPFolhaOrigem(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        XSSFSheet sheet = workbook.getSheet("DP FOLHA ORIGEM");
        if (sheet == null) {
            sheet = workbook.createSheet("DP FOLHA ORIGEM");
            sheet.setTabColor(COR_VERMELHA);
            //preencherAbaDPFolhaOrigem(sheet, polosMap);
            return;
        }
        
        // Mapeia linhas existentes pelo código do funcionário
        Map<String, Integer> linhaPorCodigo = new HashMap<>();
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                String codigo = row.getCell(0).getStringCellValue();
                if (codigo != null && !codigo.isEmpty()) {
                    linhaPorCodigo.put(codigo, i);
                }
            }
        }
        
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        
        // Verifica se precisa recriar cabeçalhos
        boolean precisaCabeçalhos = true;
        if (sheet.getRow(0) != null && sheet.getRow(0).getCell(0) != null && 
            "DP FOLHA ORIGEM - DADOS CONSOLIDADOS".equals(sheet.getRow(0).getCell(0).getStringCellValue())) {
            precisaCabeçalhos = false;
        }
        
        if (precisaCabeçalhos) {
            // Limpa a aba existente
            for (int i = sheet.getLastRowNum(); i >= 0; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
            
            // Cria título
            Row rowTitulo = sheet.createRow(0);
            criarCelula(rowTitulo, 0, "DP FOLHA ORIGEM - DADOS CONSOLIDADOS", criarEstiloTitulo(workbook));
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            
            // Cria cabeçalhos
            Row rowCabecalho = sheet.createRow(1);
            String[] cabecalhos = {"CÓD.", "NOME", "GRATIFICAÇÃO (R$)", "VALOR TOTAL"};
            for (int i = 0; i < cabecalhos.length; i++) {
                criarCelula(rowCabecalho, i, cabecalhos[i], criarEstiloCabecalho(workbook));
            }
            
            linhaPorCodigo.clear();
        }
        
        // Atualiza ou adiciona funcionários
        for (List<Funcionario> funcionariosPolo : polosMap.values()) {
            for (Funcionario func : funcionariosPolo) {
                Integer linha = linhaPorCodigo.get(func.getCodigo());
                if (linha != null && linha >= 2) { // Pula cabeçalhos
                    // Atualiza linha existente
                    Row row = sheet.getRow(linha);
                    if (row != null) {
                        criarCelula(row, 2, func.getGratificacaoValor(), estiloMoeda);
                        criarCelula(row, 3, func.getValor(), estiloMoeda);
                    }
                } else {
                    // Adiciona nova linha no final
                    int novaRowNum = sheet.getLastRowNum() + 1;
                    Row row = sheet.createRow(novaRowNum);
                    criarCelula(row, 0, func.getCodigo(), null);
                    criarCelula(row, 1, func.getNome(), null);
                    criarCelula(row, 2, func.getGratificacaoValor(), estiloMoeda);
                    criarCelula(row, 3, func.getValor(), estiloMoeda);
                    
                    linhaPorCodigo.put(func.getCodigo(), novaRowNum);
                }
            }
        }
        
        // Ajusta largura das colunas
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // Ordena os registros por código (opcional)
        try {
            ordenarPorCodigo(sheet, linhaPorCodigo);
        } catch (Exception e) {
            System.err.println("Erro ao ordenar por código: " + e.getMessage());
        }
    }

    private static void ordenarPorCodigo(Sheet sheet, Map<String, Integer> linhaPorCodigo) {
        try {
            // Verifica se há linhas para ordenar (pula cabeçalhos)
            if (sheet.getPhysicalNumberOfRows() <= 2) {
                return; // Não há dados para ordenar
            }
            
            List<Row> rows = new ArrayList<>();
            List<String> codigosOrdenados = new ArrayList<>(linhaPorCodigo.keySet());
            Collections.sort(codigosOrdenados);
            
            // Pula cabeçalhos (linhas 0 e 1)
            int firstDataRow = 2;
            for (int i = firstDataRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    rows.add(row);
                }
            }
            
            // Remove as linhas existentes (exceto cabeçalhos)
            for (int i = sheet.getLastRowNum(); i >= firstDataRow; i--) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    sheet.removeRow(row);
                }
            }
            
            // Adiciona as linhas ordenadas
            for (String codigo : codigosOrdenados) {
                Integer originalRowIndex = linhaPorCodigo.get(codigo);
                if (originalRowIndex != null && originalRowIndex >= firstDataRow) {
                    Row originalRow = rows.get(originalRowIndex - firstDataRow);
                    Row newRow = sheet.createRow(sheet.getLastRowNum() + 1);
                    
                    // Copia todas as células da linha original para a nova linha
                    for (int i = 0; i < originalRow.getLastCellNum(); i++) {
                        Cell originalCell = originalRow.getCell(i);
                        if (originalCell != null) {
                            Cell newCell = newRow.createCell(i);
                            newCell.setCellStyle(originalCell.getCellStyle());
                            
                            switch (originalCell.getCellType()) {
                                case STRING:
                                    newCell.setCellValue(originalCell.getStringCellValue());
                                    break;
                                case NUMERIC:
                                    newCell.setCellValue(originalCell.getNumericCellValue());
                                    break;
                                case BOOLEAN:
                                    newCell.setCellValue(originalCell.getBooleanCellValue());
                                    break;
                                case FORMULA:
                                    newCell.setCellFormula(originalCell.getCellFormula());
                                    break;
                                default:
                                    newCell.setCellValue("");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao ordenar por código: " + e.getMessage());
        }
    }

    
/*     // Método auxiliar para ordenar as linhas por código (opcional)
    private static void ordenarPorCodigo(Sheet sheet, Map<String, Integer> linhaPorCodigo) {
        List<Row> rows = new ArrayList<>();
        List<String> codigosOrdenados = new ArrayList<>(linhaPorCodigo.keySet());
        Collections.sort(codigosOrdenados);
        
        // Pula cabeçalhos (linhas 0 e 1)
        for (int i = 2; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                rows.add(row);
            }
        }
        
        // Reordena as linhas conforme os códigos ordenados
        for (int i = 0; i < codigosOrdenados.size(); i++) {
            String codigo = codigosOrdenados.get(i);
            int rowIndex = linhaPorCodigo.get(codigo);
            if (rowIndex > i + 1) { // +1 para pular cabeçalho
                sheet.shiftRows(rowIndex, rowIndex, -(rowIndex - (i + 2)));
            }
        }
    } */

    // Método auxiliar para mapear códigos de funcionário para linhas
    private static Map<String, Integer> mapearLinhasPorCodigo(Sheet sheet) {
        Map<String, Integer> mapeamento = new HashMap<>();
        
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                String codigo = row.getCell(0).getStringCellValue();
                if (codigo != null && !codigo.isEmpty()) {
                    mapeamento.put(codigo, i);
                }
            }
        }
        
        return mapeamento;
    }  

    private static void processarDadosOperadoras(List<Funcionario> funcionarios, 
                                        Map<String, Map<String, String>> dadosOperadoras) {
    logger.info("Iniciando processamento de dados de operadoras...");
    logger.debug("Quantidade de entradas a processar: {}", dadosOperadoras.size());
    
    // Mapa temporário para operadoras processadas
    Map<String, Operadora> operadorasProcessadas = new HashMap<>();
    
    // 1. Processa os dados brutos
    int contador = 0;
    for (Map.Entry<String, Map<String, String>> entry : dadosOperadoras.entrySet()) {
        contador++;
        Map<String, String> dados = entry.getValue();
        try {
            String coluna0 = dados.get("Column0");
            logger.trace("Processando linha {} - Column0: {}", contador, coluna0);
            
            if (coluna0 == null || !coluna0.matches("\\d+\\s+[A-Z].+")) {
                logger.warn("Formato inválido na linha {}: {}", contador, coluna0);
                continue;
            }
            
            String[] partes = coluna0.split("\\s+", 3);
            String codigo = partes[0];
            String nome = partes[1];
            String tipo = partes.length > 2 ? partes[2] : "";
            
            logger.debug("Linha {} - Código: {}, Nome: {}, Tipo: {}", contador, codigo, nome, tipo);
            
            Operadora operadora = operadorasProcessadas.computeIfAbsent(
                codigo, 
                k -> new Operadora(codigo, nome)
            );
            
            int quantidade = Integer.parseInt(dados.getOrDefault("Column3", "0"));
            BigDecimal valor = parseMonetaryValue(dados.getOrDefault("Column6", "0"));
            BigDecimal comissao = parseMonetaryValue(dados.getOrDefault("Column12", "0"));
            
            if (tipo.equals("BOL")) {
                operadora.adicionarBoleto(quantidade, valor, comissao);
                logger.trace("Adicionado boleto - Qtd: {}, Valor: {}, Comissão: {}", 
                    quantidade, valor, comissao);
            } else if (tipo.equals("REC")) {
                operadora.adicionarRecibo(quantidade, valor, comissao);
                logger.trace("Adicionado recibo - Qtd: {}, Valor: {}, Comissão: {}", 
                    quantidade, valor, comissao);
            }
        } catch (Exception e) {
            logger.error("Erro ao processar linha {}: {}", contador, e.getMessage(), e);
        }
    }
    
    logger.info("Operadoras processadas: {}", operadorasProcessadas.size());
    
    // 2. Associa as operadoras aos funcionários
    int associacoes = 0;
    for (Funcionario func : funcionarios) {
        if (operadorasProcessadas.containsKey(func.getCodigo())) {
            Operadora op = operadorasProcessadas.get(func.getCodigo());
            func.setOperadora(op);
            func.setRecebimento(op.getValorBoleto().add(op.getValorRecibo()));
            func.setQuantidade((double)(op.getQuantidadeBoleto() + op.getQuantidadeRecibo()));
            associacoes++;
            logger.debug("Associada operadora {} ao funcionário {}", op.getCodigo(), func.getCodigo());
        }
    }
    
    logger.info("Processamento concluído. Total de associações: {}", associacoes);
}
    
    /* private static void processarDadosOperadoras(List<Funcionario> funcionarios, 
                                            Map<String, Map<String, String>> dadosOperadoras) {
        System.out.println("Processando dados de operadoras...");

        logger.info("ANALISE {}", funcionarios.size() );
        logger.debug("DADOS OPERADORS {}", dadosOperadoras.size());
        
        // Mapa temporário para operadoras processadas
        Map<String, Operadora> operadorasProcessadas = new HashMap<>();
        
        // 1. Processa os dados brutos e cria objetos Operadora
        for (Map<String, String> dados : dadosOperadoras.values()) {
            try {
                String coluna0 = dados.get("Column0");
                if (coluna0 == null || !coluna0.matches("\\d+\\s+[A-Z].+")) continue;
                
                String[] partes = coluna0.split("\\s+", 3);
                String codigo = partes[0];
                String nome = partes[1];
                String tipo = partes.length > 2 ? partes[2] : "";
                
                Operadora operadora = operadorasProcessadas.computeIfAbsent(
                    codigo, 
                    k -> new Operadora(codigo, nome)
                );
                
                int quantidade = Integer.parseInt(dados.getOrDefault("Column3", "0"));
                BigDecimal valor = parseMonetaryValue(dados.getOrDefault("Column6", "0"));
                BigDecimal comissao = parseMonetaryValue(dados.getOrDefault("Column12", "0"));
                
                if (tipo.equals("BOL")) {
                    operadora.adicionarBoleto(quantidade, valor, comissao);
                } else if (tipo.equals("REC")) {
                    operadora.adicionarRecibo(quantidade, valor, comissao);
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar linha de operadora: " + e.getMessage());
            }
        }
        
        // 2. Associa as operadoras aos funcionários
        for (Funcionario func : funcionarios) {
            if (operadorasProcessadas.containsKey(func.getCodigo())) {
                func.setOperadora(operadorasProcessadas.get(func.getCodigo()));
                // Atualiza campos consolidados no Funcionario
                Operadora op = func.getOperadora();
                func.setRecebimento(op.getValorBoleto().add(op.getValorRecibo()).doubleValue());
                func.setQuantidade((double)(op.getQuantidadeBoleto() + op.getQuantidadeRecibo()));
            }
        }
    } */
       
  /*  private static void processarDadosOperadoras(List<Funcionario> funcionarios, 
                                            Map<String, Map<String, String>> dadosOperadoras) {
        System.out.println("Processando dados filtrados de operadoras..." + dadosOperadoras);

        logger.debug("dADOS OIPERADORAS ",dadosOperadoras);
        logger.debug("FUNICONARIIOS ", funcionarios.size());
        
        for (Funcionario func : funcionarios) {

            logger.debug("FUBNCIONARIOS ",func);
       
            if ("OPERADORA".equals(func.getTipoRegistro())) {
                Map<String, String> dadosOp = dadosOperadoras.get(func.getCodigo());
                if (dadosOp != null) {
                    // Atualiza com os dados específicos das operadoras
                    func.setQuantidade(Double.parseDouble(dadosOp.getOrDefault("quantidade", "0")));
                    func.setValor(Double.parseDouble(dadosOp.getOrDefault("valor", "0")));
                    func.setRecebimento(Double.parseDouble(dadosOp.getOrDefault("recebimento", "0")));
                    
                    if (func.getMetaOperadora() <= 0) {
                        func.setMetaOperadora(func.getValor());
                    }
                }
            }
        }
    } */

    private void processarSituacaoMensageiros(Map<String, Map<String, String>> dadosFiltrados) {
        if (dadosFiltrados == null || dadosFiltrados.isEmpty()) {
            System.out.println("Nenhum dado de situação de mensageiros para processar");
            return;
        }
    
        for (Map.Entry<String, Map<String, String>> entry : dadosFiltrados.entrySet()) {
            String linhaKey = entry.getKey();
            Map<String, String> linha = entry.getValue();
            
            // Extrai código do mensageiro (Column0)
            String codigo = linha.get("Column0");
            if (codigo == null || codigo.trim().isEmpty()) {
                continue;
            }
    
            // Cria ou atualiza o funcionário mensageiro
            Funcionario mensageiro = funcionariosPorCodigo.computeIfAbsent(codigo, k -> {
                Funcionario f = new Funcionario();
                f.setCodigo(codigo);
                f.setTipoRegistro("MENSAGEIRO");
                return f;
            });
    
            try {
                // Atualiza dados do mensageiro
                mensageiro.setNome(linha.get("Column1"));
                
                // Quantidade recebida (Column6)
                double qtdRecebida = parseDouble(linha.get("Column6"));
                mensageiro.setQuantidade(qtdRecebida);
                
                // Valor recebido (Column7)
                double valorRecebido = parseDouble(linha.get("Column7"));
                mensageiro.setValor(valorRecebido);
                
                // Percentual de rendimento (Column14)
                BigDecimal valorPercentualRendimento = new BigDecimal(linha.get("Column14"));
                BigDecimal percentualRendimento = valorPercentualRendimento.divide(BigDecimal.valueOf(100),4,RoundingMode.HALF_UP);
                mensageiro.setRendimento(percentualRendimento);
                
                // Quantidade total (Column15 - opcional)
                if (linha.containsKey("Column15")) {
                    double qtdTotal = parseDouble(linha.get("Column15"));
                    mensageiro.setQuantidadePolo(qtdTotal);
                }
                
                System.out.printf("Processado mensageiro %s: qtd=%.2f, valor=%.2f, rend=%.2f%%%n",
                    codigo, qtdRecebida, valorRecebido, percentualRendimento.multiply(BigDecimal.valueOf(100)));
                    
            } catch (NumberFormatException e) {
                System.err.println("Erro ao processar dados numéricos para mensageiro " + codigo);
            }
        }
    }
    
    private double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            // Remove caracteres não numéricos (exceto ponto decimal)
            String cleaned = value.replaceAll("[^\\d.]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    private static void logFuncionariosProcessados(List<Funcionario> funcionarios) {
        System.out.println("\n=== FUNCIONÁRIOS RECEBIDOS PARA RELATÓRIO ===");
        System.out.println("Total: " + funcionarios.size());
        
        funcionarios.forEach(f -> System.out.println(
            f.getCodigo() + " | " + f.getNome() + 
            " | Polo: " + f.getPolo() + 
            " | Tipo: " + f.getTipoRegistro() +
            " | Qtd: " + f.getQuantidade() +
            " | Valor: " + f.getValor() + 
            " | QtdPolo: " + f.getQuantidadePolo() +
            " | ValorPolo: " + f.getValorPolo() + 
            " | Rendimento: " + f.getRendimento() +
            " | Recebimento: " + f.getRecebimento()
            ));
    }
    
    private static void calcularCamposDerivados(List<Funcionario> funcionarios) {
        System.out.println("\nCalculando gratificações...");
        
        funcionarios.forEach(f -> {
            // Garante valores padrão
            if (f.getTipoRegistro() == null) {
                f.setTipoRegistro("MENSAGEIRO");
            }
            
            // Calcula rendimento (recebimento/meta)
            if (f.getMetaOperadora().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal rendimento = f.getRecebimento().divide(f.getMetaOperadora(), 4, RoundingMode.HALF_UP);
                f.setRendimento(rendimento);
            } else {
                f.setRendimento(BigDecimal.ZERO);
            }
            
            // Calcula gratificação específica por tipo
            switch (f.getTipoRegistro().toUpperCase()) {
                case "OPERADORA":
                    f.setGratificacaoValor(GratificacaoCalculator.calcularGratificacaoOperadora(
                        f.getRecebimento(), 
                        f.getGratificacaoPercentual()));
                    break;
                    
                case "MENSAGEIRO":
                    f.setGratificacaoValor(GratificacaoCalculator.calcularGratificacaoMensageiro(
                        f.getRendimento(), 
                        f.getPolo()));
                    break;
                    
                default:
                    f.setGratificacaoValor(BigDecimal.ZERO);
            }
        });
    }

    // 5. Método para obter a lista de funcionários por polo
    public Map<String, List<Funcionario>> agruparPorPolo() {
        Map<String, List<Funcionario>> polosMap = new HashMap<>();
        
        for (Funcionario func : funcionariosPorCodigo.values()) {
            String polo = func.getPolo() != null ? func.getPolo() : "SEM_POLO";
            polosMap.computeIfAbsent(polo, k -> new ArrayList<>()).add(func);
        }
        
        return polosMap;
    }
    
    // 6. Método para acessar funcionários por código (se necessário)
    public Map<String, Funcionario> getFuncionariosPorCodigo() {
        return Collections.unmodifiableMap(funcionariosPorCodigo);
    }
    
    private static void logPolosEncontrados(Map<String, List<Funcionario>> polosMap) {
        System.out.println("\nPolos encontrados: " + polosMap.keySet());
        polosMap.forEach((polo, funcs) -> 
            System.out.println("Polo " + polo + ": " + funcs.size() + " funcionários"));
    }
    
    private static void gerarExcelRelatorio(List<Funcionario> funcionarios, 
                                          Map<String, List<Funcionario>> polosMap,
                                          String caminhoSaida) {
        System.out.println("\nGerando relatório em: " + caminhoSaida);
        File arquivo = new File(caminhoSaida);
        
        try (XSSFWorkbook workbook = arquivo.exists() ? 
              new XSSFWorkbook(new FileInputStream(arquivo)) : new XSSFWorkbook()) {
            
            limparAbasExistente(workbook);
            
           /*  criarPreencherAbaDPFolha(workbook, polosMap);
            criarPreencherAbaDPFolhaOrigem(workbook, polosMap);
            criarPreencherAbaOperadora(workbook, polosMap); */
            
            try (FileOutputStream fos = new FileOutputStream(arquivo)) {
                workbook.write(fos);
                System.out.println("\nRelatório gerado com sucesso!");
                System.out.println("Total de funcionários processados: " + funcionarios.size());
            }
        } catch (IOException e) {
            System.err.println("ERRO ao gerar relatório: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Novo método para limpar todas as abas existentes
    private static void limparAbasExistente(XSSFWorkbook workbook) {
        String[] abas = {"DP FOLHA", "DP FOLHA ORIGEM", "OPERADORA"};
        
        for (String aba : abas) {
            XSSFSheet sheet = workbook.getSheet(aba);
            if (sheet != null) {
                // Remove regiões mescladas
                for (int i = sheet.getNumMergedRegions()-1; i >= 0; i--) {
                    sheet.removeMergedRegion(i);
                }
                // Remove linhas
                for (int i = sheet.getLastRowNum(); i >= 0; i--) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        sheet.removeRow(row);
                    }
                }
            }
        }
    }
/* 
    private static void criarPreencherAbaDPFolha(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        System.out.println("Criando/atualizando aba OPERADORA"); // Debug
        System.out.println("Polos disponíveis: " + polosMap.keySet()); // Debug
        XSSFSheet sheet = workbook.getSheet("DP FOLHA");
        if (sheet == null) {
            sheet = workbook.createSheet("DP FOLHA");
            sheet.setTabColor(COR_AZUL);
        } else {
            // Remover todas as regiões mescladas existentes
            for (int i = sheet.getNumMergedRegions()-1; i >= 0; i--) {
                sheet.removeMergedRegion(i);
            }
            //workbook.setSheetOrder(sheet.getSheetName(), 0);
            limparAbaExistente(sheet);
        }
        
        preencherAbaDPFolha(sheet, polosMap);
    }

    private static void criarPreencherAbaDPFolhaOrigem(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        XSSFSheet sheet = workbook.getSheet("DP FOLHA ORIGEM");
        if (sheet == null) {
            sheet = workbook.createSheet("DP FOLHA ORIGEM");
            sheet.setTabColor(COR_VERMELHA);
        } else {
            workbook.setSheetOrder(sheet.getSheetName(), 1);
            limparAbaExistente(sheet);
        }
        
        preencherAbaDPFolhaOrigem(sheet, polosMap);
    }

    private static void criarPreencherAbaOperadora(XSSFWorkbook workbook, Map<String, List<Funcionario>> polosMap) {
        System.out.println("Criando/atualizando aba OPERADORA"); // Debug
        System.out.println("Polos disponíveis: " + polosMap.keySet()); // Debug
        XSSFSheet sheet = workbook.getSheet("OPERADORA");
        if (sheet == null) {
            System.out.println("Criando nova aba OPERADORA"); // Debug
            sheet = workbook.createSheet("OPERADORA");
            sheet.setTabColor(COR_ROSA);
        } else {
            System.out.println("Aba OPERADORA já existe, atualizando..."); // Debug
            workbook.setSheetOrder(sheet.getSheetName(), 2);
            limparAbaExistente(sheet);
        }
        // Debug: verificar funcionários do polo 1
        List<Funcionario> operadoras = polosMap.getOrDefault("1", Collections.emptyList());
        System.out.println("Número de operadoras encontradas: " + operadoras.size()); // Debug
        for (Funcionario op : operadoras) {
            System.out.println("Operadora: " + op.getNome() + " - Polo: " + op.getPolo()); // Debug
        }
        preencherAbaOperadora(sheet, polosMap);
        System.out.println("Aba OPERADORA processada"); // Debug
    }
 */
    private static void limparAbaExistente(Sheet sheet) {
        // Remover todas as regiões mescladas primeiro
        for (int i = sheet.getNumMergedRegions()-1; i >= 0; i--) {
            sheet.removeMergedRegion(i);
        }
        
        // Limpar linhas existentes
        for (int i = sheet.getLastRowNum(); i >= 0; i--) {
            Row row = sheet.getRow(i);
            if (row != null) {
                sheet.removeRow(row);
            }
        }        
    }

    private static Map<String, List<Funcionario>> dividirPorPolo(List<Funcionario> funcionarios) {  
        System.out.println("Dividindo " + funcionarios.size() + " funcionários por polo"); 
        
        Map<String, List<Funcionario>> polosMap = new LinkedHashMap<>();
        
        for (Funcionario funcionario : funcionarios) {
            String polo = funcionario.getPolo();
            
            // Verifica se o polo é válido
            if (polo == null || polo.trim().isEmpty()) {
                System.err.println("Atenção: Funcionário " + funcionario.getCodigo() + " - " + 
                    funcionario.getNome() + " não tem polo definido. Atribuindo a 'SEM_POLO'");
                polo = "SEM_POLO";
            }

            // Verifica e insere quantidadePolo e valorPolo
            if (funcionario.getQuantidadePolo() == 0 && funcionario.getValorPolo().compareTo(BigDecimal.ZERO) == 0) {
                // Calcula valores padrão caso não estejam definidos
                //funcionario.setQuantidadePolo(funcionario.getQuantidade());
                funcionario.setValorPolo(BigDecimal.valueOf( funcionario.getValor()));
            }
                
            // Adiciona ao mapa de polos
            polosMap.computeIfAbsent(polo, k -> new ArrayList<>()).add(funcionario);
        }        
        // Log dos polos encontrados
        System.out.println("Polos encontrados: " + polosMap.keySet());
        polosMap.forEach((polo, funcs) -> 
        System.out.println("Polo " + polo + ": " + funcs.size() + " funcionários")); 

        return polosMap;
    }

   /*  private static Map<String, List<Funcionario>> dividirPorPolo(List<Funcionario> funcionarios) {  
        System.out.println("Dividindo " + funcionarios.size() + " funcionários por polo"); // Debug 
        System.out.println("Polos encotnrados");
        Map<String, List<Funcionario>> polosMap = new LinkedHashMap<>();
        for (Funcionario funcionario : funcionarios) {
            //System.out.println("L89 GeradorRelatorio Funcionário: " + funcionario.getNome() + " - Polo: " + funcionario.getPolo()); // Debug
            //funcionario.log();
            polosMap.computeIfAbsent(funcionario.getPolo(), k -> new ArrayList<>()).add(funcionario);
        }
        return polosMap;
    }
 */
  /*   private static void preencherAbaDPFolha(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        XSSFWorkbook workbook = (XSSFWorkbook) sheet.getWorkbook();
        int rowIndex = 0;
        //String setor = "";
        
        // Estilo para títulos
        XSSFCellStyle estiloTitulo = criarEstiloTitulo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        
        for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
            String polo = entry.getKey();
            List<Funcionario> funcionarios = entry.getValue();

            
            
            if (funcionarios.isEmpty()) continue;
            
            Funcionario supervisor = funcionarios.get(funcionarios.size() - 1);
            //setor = supervisor.getPolo();
            
            // Linha de Dados Gerais
            Row rowDadosGerais = sheet.createRow(rowIndex++);
            criarCelula(rowDadosGerais, 0, "DADOS GERAIS DO ADMINISTRATIVO " + polo, estiloTitulo);
            sheet.addMergedRegion(new CellRangeAddress(rowDadosGerais.getRowNum(), rowDadosGerais.getRowNum(), 0, 4));
            
            // Linha de Valores do Polo
            Row rowValores = sheet.createRow(rowIndex++);
            criarCelula(rowValores, 0, "Quantidade Total:", null);
            criarCelula(rowValores, 1, supervisor.getQuantidadePolo(), null);
            criarCelula(rowValores, 2, "Valor Total:", null);
            criarCelula(rowValores, 3, supervisor.getValorPolo(), estiloMoeda);
            
            // Cabeçalho da tabela
            Row rowCabecalho = sheet.createRow(rowIndex++);
            String[] cabecalhos = {"CÓD.", "DRT", "NOME", "RECEBIMENTO (R$)", "RENDIMENTO (%)", 
                                  "GRATIFICAÇÃO (%)", "GRATIFICAÇÃO (R$)", "QUANTIDADE", "VALOR"};
            for (int i = 0; i < cabecalhos.length; i++) {
                criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
            }
            
            // Dados dos funcionários
            for (Funcionario func : funcionarios) {                
                Row row = sheet.createRow(rowIndex++);
                criarCelula(row, 0, func.getCodigo(), null);
                criarCelula(row, 1, func.getDrt(), null);
                criarCelula(row, 2, func.getNome(), null);
                criarCelula(row, 3, func.getRecebimento(), estiloMoeda);
                criarCelula(row, 4, func.getRendimento().multiply(BigDecimal.valueOf(100)), estiloPercentual);
                criarCelula(row, 5, func.getGratificacaoPercentual().multiply(BigDecimal.valueOf(100)), estiloPercentual);
                criarCelula(row, 6, func.getGratificacaoValor(), estiloMoeda);
                criarCelula(row, 7, func.getQuantidade(), null);
                criarCelula(row, 8, func.getValor(), estiloMoeda);

                
            }
            
            rowIndex++; // Espaço entre polos
        }
        
        // Ajusta largura das colunas
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }
 *//* 
    private static void preencherAbaDPFolhaOrigem(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        XSSFWorkbook workbook = (XSSFWorkbook) sheet.getWorkbook();
        int rowIndex = 0;
        
        XSSFCellStyle estiloTitulo = criarEstiloTitulo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        
        // Título da aba
        Row rowTitulo = sheet.createRow(rowIndex++);
        criarCelula(rowTitulo, 0, "DP FOLHA ORIGEM - DADOS CONSOLIDADOS", estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(rowTitulo.getRowNum(), rowTitulo.getRowNum(), 0, 4));
        
        for (Map.Entry<String, List<Funcionario>> entry : dadosPorPolo.entrySet()) {
            String polo = entry.getKey();
            List<Funcionario> funcionarios = entry.getValue();
            
            if (funcionarios.isEmpty()) continue;
            
            // Cabeçalho do polo
            Row rowPolo = sheet.createRow(rowIndex++);
            criarCelula(rowPolo, 0, "POLO: " + polo, estiloCabecalho);
            
            // Cabeçalho da tabela
            Row rowCabecalho = sheet.createRow(rowIndex++);
            String[] cabecalhos = {"CÓD.", "NOME", "GRATIFICAÇÃO (R$)", "VALOR TOTAL"};
            for (int i = 0; i < cabecalhos.length; i++) {
                criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
            }
            
            // Dados dos funcionários
            for (Funcionario func : funcionarios) {
                Row row = sheet.createRow(rowIndex++);
                criarCelula(row, 0, func.getCodigo(), null);
                criarCelula(row, 1, func.getNome(), null);
                criarCelula(row, 2, func.getGratificacaoValor(), estiloMoeda);
                criarCelula(row, 3, func.getValor(), estiloMoeda);
            }

            rowIndex++; // Espaço entre polos
        }
        
        // Ajusta largura das colunas
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
 *//* 
    private static void preencherAbaOperadora(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        XSSFWorkbook workbook = (XSSFWorkbook) sheet.getWorkbook();
        int rowIndex = 0;
        
        XSSFCellStyle estiloTitulo = criarEstiloTitulo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        XSSFCellStyle estiloNumero = workbook.createCellStyle();
        
        // Título da aba
        Row rowTitulo = sheet.createRow(rowIndex++);
        criarCelula(rowTitulo, 0, "OPERADORAS - DADOS DETALHADOS", estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(rowTitulo.getRowNum(), rowTitulo.getRowNum(), 0, 11));

        // Cabeçalho da tabela
        Row rowCabecalho = sheet.createRow(rowIndex++);
        String[] cabecalhos = {
            "CÓD.", "NOME", 
            "QTD BOLETOS", "VALOR BOLETOS", "COMISSÃO BOLETOS",
            "QTD RECIBOS", "VALOR RECIBOS", "COMISSÃO RECIBOS",
            "TOTAL RECEBIDO", "META", "PERCENTUAL", "GRATIFICAÇÃO"
        };
        
        for (int i = 0; i < cabecalhos.length; i++) {
            criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
        }

        // Filtrar apenas funcionários que são operadoras
        List<Funcionario> operadoras = dadosPorPolo.values().stream()
            .flatMap(List::stream)
            .filter(Funcionario::isOperadora)
            .collect(Collectors.toList());

        // Preencher dados das operadoras
        for (Funcionario func : operadoras) {
            Operadora op = func.getOperadora();
            Row row = sheet.createRow(rowIndex++);
            
            // Dados básicos
            criarCelula(row, 0, func.getCodigo(), null);
            criarCelula(row, 1, func.getNome(), null);
            
            // Dados de boletos
           // criarCelula(row, 2, op.getQuantidadeBoleto(), estiloNumero);
            //criarCelula(row, 3, op.getValorBoleto(), estiloMoeda);
           // criarCelula(row, 4, op.getComissaoBoleto(), estiloMoeda);
            
            // Dados de recibos
            criarCelula(row, 5, op.getQuantidadeRecibo(), estiloNumero);
            criarCelula(row, 6, op.getValorRecibo(), estiloMoeda);
            criarCelula(row, 7, op.getComissaoRecibo(), estiloMoeda);
            
            // Totais e cálculos
            BigDecimal totalRecebido = op.getValorBoleto().add(op.getValorRecibo());
            criarCelula(row, 8, totalRecebido, estiloMoeda);
            
            criarCelula(row, 9, func.getMetaOperadora(), estiloMoeda);
            
            BigDecimal percentual = func.getMetaOperadora().compareTo(BigDecimal.ZERO) > 0 ? 
                totalRecebido.divide(func.getMetaOperadora()) : BigDecimal.ZERO;
            criarCelula(row, 10, percentual, estiloPercentual);
            
            BigDecimal gratificacao = GratificacaoCalculator.calcularGratificacaoOperadora(
                totalRecebido, 
                func.getGratificacaoPercentual()
            );
            criarCelula(row, 11, gratificacao, estiloMoeda);
        }

        // Ajusta largura das colunas
        for (int i = 0; i < 12; i++) {
            sheet.autoSizeColumn(i);
        }

        // Congela os cabeçalhos
        sheet.createFreezePane(0, 2);
    }

 */   /*  private static void preencherAbaOperadora(Sheet sheet, Map<String, List<Funcionario>> dadosPorPolo) {
        XSSFWorkbook workbook = (XSSFWorkbook) sheet.getWorkbook();
        int rowIndex = 0;
        
        XSSFCellStyle estiloTitulo = criarEstiloTitulo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        
        // Título da aba
        Row rowTitulo = sheet.createRow(rowIndex++);
        criarCelula(rowTitulo, 0, "OPERADORAS - DADOS CONSOLIDADOS", estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(rowTitulo.getRowNum(), rowTitulo.getRowNum(), 0, 5));
        
        // Cabeçalho da tabela
        Row rowCabecalho = sheet.createRow(rowIndex++);
        String[] cabecalhos = {"CÓD.", "NOME", "META", "RECEBIDO", "PERCENTUAL", "GRATIFICAÇÃO"};
        for (int i = 0; i < cabecalhos.length; i++) {
            criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
        } 
        
        // Filtrar operadoras (polo 1 conforme sua planilha)
         List<Funcionario> operadoras = dadosPorPolo.values().stream()
        .flatMap(List::stream)
        .filter(f -> f.getPolo() != null && f.getPolo().equals("1") && 
            f.getTipoRegistro().equalsIgnoreCase("OPERADORA"))
        .collect(Collectors.toList());

        System.out.println("Operadoras encontradas: " + operadoras.size());        

        // Preencher dados das operadoras
         for (Funcionario op : operadoras) {
            Row row = sheet.createRow(rowIndex++);
            criarCelula(row, 0, op.getCodigo(), null);
            criarCelula(row, 1, op.getNome(), null);
            criarCelula(row, 2, op.getMetaOperadora(), estiloMoeda);
            criarCelula(row, 3, op.getRecebimento(), estiloMoeda);
            criarCelula(row, 4, op.getRendimento() * 100, estiloPercentual);
            criarCelula(row, 5, op.getGratificacaoValor(), estiloMoeda);

            // Calcular percentual (recebido/meta)
            double percentual = op.getMetaOperadora() > 0 ? 
            op.getRecebimento() / op.getMetaOperadora() : 0;
            criarCelula(row, 4, percentual, estiloPercentual);           
            double gratificacao = GratificacaoCalculator.calcularGratificacaoOperadora(
                op.getRecebimento(), 
                op.getGratificacaoPercentual()
            );
            criarCelula(row, 5, gratificacao, estiloMoeda);
        } 
        
        // Ajusta largura das colunas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    } */

    // Métodos auxiliares para criação de estilos
    private static XSSFCellStyle criarEstiloTitulo(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(COR_CINZA);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        
        return style;
    }

    private static XSSFCellStyle criarEstiloCabecalho(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        
        return style;
    }

    private static XSSFCellStyle criarEstiloMoeda(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("R$ #,##0.00"));
        return style;
    }

    private static XSSFCellStyle criarEstiloPercentual(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        return style;
    }

    private static void criarCelula(Row row, int coluna, Object valor, CellStyle estilo) {
        Cell cell = row.createCell(coluna);
        
        if (valor instanceof String) {
            cell.setCellValue((String) valor);
        } else if (valor instanceof Number) {
            cell.setCellValue(((Number) valor).doubleValue());
        }
        
        if (estilo != null) {
            cell.setCellStyle(estilo);
        }
    }

    private static BigDecimal parseMonetaryValue(String value) {
        try {
            String cleaned = value.replace("R$", "").replace(".", "").replace(",", ".").trim();
            return new BigDecimal(cleaned);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}