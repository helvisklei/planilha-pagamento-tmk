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
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class RelatorioGeral {
    private static final Logger logger = LogManager.getLogger(RelatorioGeral.class);
    
    // Cores em RGB (mantidas conforme original)
    private static final XSSFColor COR_AZUL = new XSSFColor(new java.awt.Color(197, 217, 241), null);
    private static final XSSFColor COR_VERMELHA = new XSSFColor(new java.awt.Color(255, 0, 0), null);
    private static final XSSFColor COR_ROSA = new XSSFColor(new java.awt.Color(255, 192, 203), null);
    private static final XSSFColor COR_CINZA = new XSSFColor(new java.awt.Color(166, 166, 166), null);
    
    // Cores adicionais para melhor visualização
    private static final XSSFColor COR_VERDE = new XSSFColor(new java.awt.Color(146, 208, 80), null);
    private static final XSSFColor COR_AMARELO = new XSSFColor(new java.awt.Color(255, 255, 0), null);

    public static void gerarRelatorio(List<Funcionario> funcionarios, String caminhoSaida, String mes, int ano) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // Formata o mês/ano para exibição (ex: "ABRIL/2025")
            String mesAnoFormatado = mes.toUpperCase() + "/" + ano;
            // Criar abas com cores originais
            XSSFSheet dpFolha = workbook.createSheet("DP FOLHA");
            dpFolha.setTabColor(COR_AZUL);
            
            XSSFSheet dpFolhaOrigem = workbook.createSheet("DP FOLHA ORIGEM");
            dpFolhaOrigem.setTabColor(COR_VERMELHA);
            
            XSSFSheet operadora = workbook.createSheet("OPERADORA");
            operadora.setTabColor(COR_ROSA);
            
            // Preencher abas conforme estrutura solicitada
            preencherAbaDPFolha(dpFolha, funcionarios, workbook, mesAnoFormatado);
            preencherAbaDPFolhaOrigem(dpFolhaOrigem, funcionarios, workbook, mesAnoFormatado);
            preencherAbaOperadora(operadora, funcionarios, workbook, mesAnoFormatado);
            
            // Salvar arquivo
            try (FileOutputStream fos = new FileOutputStream(caminhoSaida)) {
                workbook.write(fos);
                logger.info("Relatório gerado com sucesso em: {}", caminhoSaida);
            }
        } catch (IOException e) {
            logger.error("Erro ao gerar relatório", e);
        }
    }

    private static void preencherAbaDPFolha(XSSFSheet sheet, List<Funcionario> funcionarios, XSSFWorkbook workbook, String mesAnoFormatado) {
        // Agrupar por polo
        Map<String, List<Funcionario>> funcionariosPorPolo = funcionarios.stream()
            .collect(Collectors.groupingBy(f -> f.getPolo() != null ? f.getPolo() : "SEM POLO"));
        
        // Ordem dos polos
        List<String> ordemPolos = Arrays.asList("SERRA TALHADA", "PETROLINA", "MATRIZ", "CARUARU", "GARANHUNS", "METROPOLITANO");
        
        int linhaAtual = 0;
        
        // Estilos
        XSSFCellStyle estiloTituloPolo = criarEstiloTituloPolo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        XSSFCellStyle estiloDados = criarEstiloDados(workbook);
        
        // Cabeçalho da aba
        Row cabecalhoAba = sheet.createRow(linhaAtual++);
        criarCelula(cabecalhoAba, 0, "DP FOLHA - RELATÓRIO CONSOLIDADO " + mesAnoFormatado, estiloTituloPolo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

        // Para cada polo na ordem definida
        for (String polo : ordemPolos) {
            if (!funcionariosPorPolo.containsKey(polo)) continue;
            
            List<Funcionario> funcionariosPolo = funcionariosPorPolo.get(polo);
           
            // Coleta o valod demonstrativo de caixa do polo
            Double valorDemoPolo = getValorPorPoloStatic(polo, funcionarios);            
            BigDecimal valorDemonstrativoPolo = new BigDecimal(valorDemoPolo);

            // Calcular total do polo
            BigDecimal totalPolo = funcionariosPolo.stream()
                .map(f -> f.getSituacaoMensal() != null ? f.getSituacaoMensal().getComissao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Título do polo
            Row rowPolo = sheet.createRow(linhaAtual++);
            criarCelula(rowPolo, 0, "POLO: " + polo + " Valor: " + formatarMoeda(valorDemonstrativoPolo), estiloTituloPolo);
            sheet.addMergedRegion(new CellRangeAddress(linhaAtual-1, linhaAtual-1, 0, 5));
            
            // Cabeçalhos
            Row rowCabecalho = sheet.createRow(linhaAtual++);
            String[] cabecalhos = {"CÓDIGO", "DRT", "NOME", "GRATIFICAÇÃO (R$)"};
            for (int i = 0; i < cabecalhos.length; i++) {
                criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
            }
            
            // Dados dos funcionários
            for (Funcionario func : funcionariosPolo) {
                String poloDemonstrativo = func.getPolo(); //  Identifico o Polo 
                BigDecimal comissaoMsg = func.getSituacaoMensal() != null ? func.getSituacaoMensal().getComissao() : BigDecimal.ZERO;
                String comissaoFormatadaMsg = formatarParaReal(comissaoMsg); 

               /*  //String comissaoFormatada = formatarParaReal(comissao);
                logger.debug("L105 Plo {} - Comissao {} - MSG {}",polo,comissaoFormatada,func.getCodigo());
                //logger.debug("L106 - FUNCIONÁRIO COMPLETO: {}", func); */
                Row row = sheet.createRow(linhaAtual++);
                criarCelula(row, 0, limparDecimalSeNumeroInteiro(func.getCodigo()), estiloDados);
                criarCelula(row, 1, limparDecimalSeNumeroInteiro(func.getDrt()), estiloDados);
                criarCelula(row, 2, func.getNome(), estiloDados);
                criarCelula(row, 3, comissaoFormatadaMsg, estiloMoeda);//func.getGratificacaoValor()
            }           
           
            // ▶️ Cria linha com o total da comissão do polo
            Row rowTotalComissao = sheet.createRow(linhaAtual++);
            criarCelula(rowTotalComissao, 2, "TOTAL COMISSÃO " + polo + ":", estiloCabecalho);
            criarCelula(rowTotalComissao, 3, formatarMoeda(totalPolo), estiloMoeda);

            // Linha em branco após cada polo
            linhaAtual++;

        }       
        
        // Seção ADMINISTRATIVO
        Row rowAdmin = sheet.createRow(linhaAtual++);
        criarCelula(rowAdmin, 0, "ADMINISTRATIVO", estiloTituloPolo);
        sheet.addMergedRegion(new CellRangeAddress(linhaAtual-1, linhaAtual-1, 0, 5));

        // Filtra somente funcionários do tipo ADM
        List<Funcionario> administrativos = funcionarios.stream()
        .filter(f -> "ADM".equalsIgnoreCase(f.getTipo()))
        .collect(Collectors.toList());

        Row rowCabecalhoADM = sheet.createRow(linhaAtual++);
        String[] cabecalhosADM = {"CÓDIGO", "DRT", "NOME", "COMISSÃO ADM (R$)"};
        for (int i = 0; i < cabecalhosADM.length; i++) {
            criarCelula(rowCabecalhoADM, i, cabecalhosADM[i], estiloCabecalho);            
        }

        BigDecimal totalComissaoADM = BigDecimal.ZERO;

        for (Funcionario func : administrativos) {
            BigDecimal comissaoADM = func.getGratificacao() != null ? func.getGratificacao() : BigDecimal.ZERO;
            totalComissaoADM = totalComissaoADM.add(comissaoADM);

            Row row = sheet.createRow(linhaAtual++);
            criarCelula(row, 0, limparDecimalSeNumeroInteiro(func.getCodigo()), estiloDados);
            criarCelula(row, 1, limparDecimalSeNumeroInteiro(func.getDrt()), estiloDados);
            criarCelula(row, 2, func.getNome(), estiloDados);
            criarCelula(row, 3, formatarMoeda(comissaoADM), estiloMoeda);
        }

        Row rowTotalADM = sheet.createRow(linhaAtual++);
        criarCelula(rowTotalADM, 2, "TOTAL COMISSÃO ADM:", estiloCabecalho);
        criarCelula(rowTotalADM, 3, formatarMoeda(totalComissaoADM), estiloMoeda);

         // Linha em branco após cada polo
         linhaAtual++;

         // Seção OPERAÇÂO
        Row rowOperacao = sheet.createRow(linhaAtual++);
        criarCelula(rowOperacao, 0, "OPERAÇÃO", estiloTituloPolo);
        sheet.addMergedRegion(new CellRangeAddress(linhaAtual - 1, linhaAtual - 1, 0, 5));

        List<Funcionario> operacionais = funcionarios.stream()
        .filter(f -> "OPE".equalsIgnoreCase(f.getTipo()) || "OPERACAO".equalsIgnoreCase(f.getTipo()))
        .collect(Collectors.toList());

        Row rowCabecalhoOperacao = sheet.createRow(linhaAtual++);
        String[] cabecalhosOP = {"CÓDIGO", "DRT", "NOME", "COMISSÃO OP (R$)"};
        for (int i = 0; i < cabecalhosOP.length; i++) {
            criarCelula(rowCabecalhoOperacao, i, cabecalhosOP[i], estiloCabecalho);
        }

        BigDecimal totalComissaoOP = BigDecimal.ZERO;

        for (Funcionario func : operacionais) {
            BigDecimal comissaoOp = func.getGratificacao() != null ? func.getGratificacao() : BigDecimal.ZERO;
            totalComissaoOP = totalComissaoOP.add(comissaoOp);

            Row row = sheet.createRow(linhaAtual++);
            criarCelula(row, 0, limparDecimalSeNumeroInteiro(func.getCodigo()), estiloDados);
            criarCelula(row, 1, limparDecimalSeNumeroInteiro(func.getDrt()), estiloDados);
            criarCelula(row, 2, func.getNome(), estiloDados);
            criarCelula(row, 3, formatarMoeda(comissaoOp), estiloMoeda);
        }

        Row rowTotalOP = sheet.createRow(linhaAtual++);
        criarCelula(rowTotalOP, 2, "TOTAL COMISSÃO OPERAÇÃO:", estiloCabecalho);
        criarCelula(rowTotalOP, 3, formatarMoeda(totalComissaoOP), estiloMoeda);

        
        // Totais por polo
        linhaAtual++;
        Row rowTotais = sheet.createRow(linhaAtual++);
        criarCelula(rowTotais, 0, "TOTAIS POR POLO", estiloCabecalho);
        
        for (String polo : ordemPolos) {
            if (!funcionariosPorPolo.containsKey(polo)) continue;
            
            BigDecimal totalPolo = funcionariosPorPolo.get(polo).stream()
                .map(f -> f.getSituacaoMensal() != null ? f.getSituacaoMensal().getComissao() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            Row row = sheet.createRow(linhaAtual++);
            criarCelula(row, 0, polo, estiloDados);
            criarCelula(row, 1, formatarMoeda(totalPolo), estiloMoeda);
        }
        
        // Total geral
        linhaAtual++;
        Row rowTotalGeral = sheet.createRow(linhaAtual++);
        criarCelula(rowTotalGeral, 0, "TOTAL GERAL", estiloCabecalho);
        
        BigDecimal totalGeral = funcionariosPorPolo.values().stream()
            .flatMap(List::stream)
            .map(f -> f.getSituacaoMensal() != null ? f.getSituacaoMensal().getComissao() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        criarCelula(rowTotalGeral, 1, formatarMoeda(totalGeral), estiloMoeda);
        
        // Ajustar largura das colunas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // ----- SOLUÇÃO 1: Método estático (se não usar xlsProcessor) -----
    public static Double getValorPorPoloStatic(String polo, List<Funcionario> funcionarios) {
        return funcionarios.stream()
            .filter(f -> polo.equals(f.getPolo()))
            .findFirst()
            .map(f -> f.getDemonstrativosCaixa().getOrDefault(polo, 0.0))
            .orElse(0.0);
    } 

    private static String formatarParaReal(BigDecimal comissao) {
        try {
            if (comissao == null) {
                return "R$ 0,00";
            }
            
            // Usa Locale brasileiro
            NumberFormat formato = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            formato.setMinimumFractionDigits(2);
            formato.setMaximumFractionDigits(2);
            
            return formato.format(comissao);
        } catch (Exception e) {
            logger.error("Erro ao formatar valor monetário: {}", e.getMessage());
            return "R$ [ERRO]";
        }
    }

    private static void preencherAbaDPFolhaOrigem(XSSFSheet sheet, List<Funcionario> funcionarios, XSSFWorkbook workbook, String mesAnoFormatado) {
        // Implementação similar à DP FOLHA, mas com colunas diferentes
        Map<String, List<Funcionario>> funcionariosPorPolo = funcionarios.stream()
            .collect(Collectors.groupingBy(f -> f.getPolo() != null ? f.getPolo() : "SEM POLO"));
        
        List<String> ordemPolos = Arrays.asList("SERRA TALHADA", "PETROLINA", "MATRIZ", "CARUARU", "GARANHUNS", "METROPOLITANO");
        
        int linhaAtual = 0;
        
        // Estilos
        XSSFCellStyle estiloTituloPolo = criarEstiloTituloPolo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        XSSFCellStyle estiloDados = criarEstiloDados(workbook);
        
        // Cabeçalho da aba
        Row cabecalhoAba = sheet.createRow(linhaAtual++);
        criarCelula(cabecalhoAba, 0, "DP FOLHA ORIGEM - DADOS DETALHADOS " + mesAnoFormatado, estiloTituloPolo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        
        // Para cada polo na ordem definida
        for (String polo : ordemPolos) {
            if (!funcionariosPorPolo.containsKey(polo)) continue;
            
            List<Funcionario> funcionariosPolo = funcionariosPorPolo.get(polo);

            // Coleta o valod demonstrativo de caixa do polo
            Double valorDemoPolo = getValorPorPoloStatic(polo, funcionarios);            
            BigDecimal valorDemonstrativoPolo = new BigDecimal(valorDemoPolo);
            
            // Calcular total do polo
            BigDecimal totalPolo = funcionariosPolo.stream()
                .map(f -> f.getRecebimento() != null ? f.getRecebimento() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Título do polo
            Row rowPolo = sheet.createRow(linhaAtual++);
            criarCelula(rowPolo, 0, "POLO: " + polo + " Valor: " + formatarMoeda( valorDemonstrativoPolo), estiloTituloPolo);
            sheet.addMergedRegion(new CellRangeAddress(linhaAtual-1, linhaAtual-1, 0, 6));
            
            // Cabeçalhos
            Row rowCabecalho = sheet.createRow(linhaAtual++);
            String[] cabecalhos = {"CÓDIGO", "DRT", "NOME", "RECEBIMENTO (R$)", "RENDIMENTO (%)", "GRATIFICAÇÃO (%)", "GRATIFICAÇÃO (R$)"};
            for (int i = 0; i < cabecalhos.length; i++) {
                criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
            }
            
            // Dados dos funcionários
            for (Funcionario func : funcionariosPolo) {
                String poloDemonstrativo = func.getPolo(); //  Identifico o Polo 
                BigDecimal recebidoValor = func.getSituacaoMensal() != null ? func.getSituacaoMensal().getRecebidoValor() : BigDecimal.ZERO;
                BigDecimal recebidoQtd = func.getSituacaoMensal() != null ? func.getSituacaoMensal().getRendimentoQtd() : BigDecimal.ZERO;
                BigDecimal valorMsg = func.getSituacaoMensal() != null ? func.getSituacaoMensal().getComissao() : BigDecimal.ZERO;
                //String comissaoFormatadaMsg = formatarParaReal(comissaoMsg); 


                Row row = sheet.createRow(linhaAtual++);
                criarCelula(row, 0,limparDecimalSeNumeroInteiro(func.getCodigo()), estiloDados);//Codigo
                criarCelula(row, 1,limparDecimalSeNumeroInteiro(func.getDrt()), estiloDados);// Nome
                criarCelula(row, 2, func.getNome(), estiloDados);//DRT
                criarCelula(row, 3, recebidoValor, estiloMoeda);//REcebido
                criarCelula(row, 4, recebidoQtd, estiloPercentual);// Percentual de quanitdade recebida
                criarCelula(row, 5,obterPercentualGratificacao(recebidoQtd), estiloPercentual);
                criarCelula(row, 6, formatarMoeda(valorMsg), estiloMoeda);
            }
            
            // Linha em branco após cada polo
            linhaAtual++;
        }
        
        // Seção ADMINISTRATIVO
        Row rowAdmin = sheet.createRow(linhaAtual++);
        criarCelula(rowAdmin, 0, "ADMINISTRATIVO", estiloTituloPolo);
        sheet.addMergedRegion(new CellRangeAddress(linhaAtual-1, linhaAtual-1, 0, 6));
        
        // Ajustar largura das colunas
        for (int i = 0; i < 7; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static XSSFCellStyle criarEstiloTituloPolo(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        //CellStyle style = workbook.createCellStyle();
       // Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short)12);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static void preencherAbaOperadora(XSSFSheet sheet, List<Funcionario> funcionarios, XSSFWorkbook workbook, String mesAnoFormatado) {
        // Filtrar apenas operadoras
        List<Funcionario> operadoras = funcionarios.stream()
            .filter(Funcionario::isOperadora)
            .collect(Collectors.toList());

           /*  List<Funcionario> operacionais = funcionarios.stream()
                .filter(f -> "OPE".equalsIgnoreCase(f.getTipo()) || "OPERACAO".equalsIgnoreCase(f.getTipo()))
                .collect(Collectors.toList()); */

            logger.debug("L  390 - FUNCIONÁRIOS OPERADORAS: {}", operadoras.size());
        
        int linhaAtual = 0;
        
        // Estilos
        XSSFCellStyle estiloTitulo = criarEstiloTitulo(workbook);
        XSSFCellStyle estiloCabecalho = criarEstiloCabecalho(workbook);
        XSSFCellStyle estiloMoeda = criarEstiloMoeda(workbook);
        XSSFCellStyle estiloPercentual = criarEstiloPercentual(workbook);
        XSSFCellStyle estiloDados = criarEstiloDados(workbook);
        
        // Título da aba
        Row rowTitulo = sheet.createRow(linhaAtual++);
        criarCelula(rowTitulo, 0, "OPERADORAS - DADOS DETALHADOS " + mesAnoFormatado, estiloTitulo);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));
        
        // Cabeçalhos
        Row rowCabecalho = sheet.createRow(linhaAtual++);
        String[] cabecalhos = {
            "CÓDIGO", "DRT", "NOME", "META REC", "VALOR RECEBIDO", "Meta%", 
            "PERC 7%", "FN +FR (REC)", "VALOR 9%", "VALOR 15%", 
            "VALOR 20%", "VALOR 30%", "TOT(FN REC)", "PROPORC",
            "Receb - FNR", "% Recebido", "%FNR", "vl tot rec"
        };
        
        for (int i = 0; i < cabecalhos.length; i++) {
            criarCelula(rowCabecalho, i, cabecalhos[i], estiloCabecalho);
        }
        
        // Dados das operadoras
        for (Funcionario operadora : operadoras) {
           
            operadoras.forEach(f -> System.out.println("L402 relatorio geral "+f.getNome() + " - isOperadora: " + f.isOperadora()));

            BigDecimal metaOperadora = operadora.getMetaRec() != null ? operadora.getMetaRec() : BigDecimal.ZERO;
            BigDecimal recebidoOperadora = operadora.getRecebidoOperador() != null ? operadora.getRecebidoOperador() : BigDecimal.ZERO;

            BigDecimal recDivideMeta;
            if (metaOperadora.compareTo(BigDecimal.ZERO) == 0) {
                recDivideMeta = BigDecimal.ZERO;
            } else {
                try {
                    recDivideMeta = recebidoOperadora.divide(metaOperadora, 4, RoundingMode.HALF_UP);
                    logger.debug("L413 - META: {} - RECEBIDO: {} - DIVISÃO: {}", metaOperadora, recebidoOperadora, recDivideMeta);
                } catch (Exception e) {
                    recDivideMeta = BigDecimal.ZERO;
                }
            }           

            BigDecimal perc7;
            BigDecimal SETE_PORCENTO = new BigDecimal("0.07"); // Constante para 7%

            if (recDivideMeta.compareTo(BigDecimal.ONE) > 0) { // Se > 100%
                perc7 = SETE_PORCENTO; // Fixa 7%
            } else {
                perc7 = recDivideMeta.multiply(SETE_PORCENTO)
                        .setScale(4, RoundingMode.HALF_UP); // 7% do valor
            }

            // Garante que o valor não seja negativo (proteção adicional)
            if (perc7.compareTo(BigDecimal.ZERO) < 0) {
                perc7 = BigDecimal.ZERO;
            }

            // CAlcula a comissão em PROPORC

            BigDecimal proporc = BigDecimal.ZERO;
            try {
                proporc = recebidoOperadora.multiply(perc7)
                                   .setScale(2, RoundingMode.HALF_UP) // 2 casas para moeda
                                   .max(BigDecimal.ZERO); // garante não negativo
            } catch (Exception e) {
                logger.error("Erro ao calcular valor", e);
                proporc = BigDecimal.ZERO;
            }
          
            Row row = sheet.createRow(linhaAtual++);             
                    
            criarCelula(row, 0,limparDecimalSeNumeroInteiro(operadora.getCodigo()), estiloDados);
            criarCelula(row, 1, limparDecimalSeNumeroInteiro(operadora.getDrt()), estiloDados);
            criarCelula(row, 2, operadora.getNome(), estiloDados);
            criarCelula(row, 3, metaOperadora, estiloMoeda);
            criarCelula(row, 4, recebidoOperadora, estiloMoeda);
            criarCelula(row, 5,recDivideMeta, estiloPercentual);
            criarCelula(row, 6, perc7, estiloPercentual); // se recDivideMeta > 100 então recebe  0.07 ou 7% senão recDivideMeta * 0.07 ou 7%
            criarCelula(row, 13, proporc, estiloMoeda);
            /* criarCelula(row, 4, operadora.getValorRecebido(), estiloMoeda);
            criarCelula(row, 5, operadora.getMetaPercentual(), estiloPercentual);
            criarCelula(row, 6, operadora.getPerc7(), estiloPercentual);
            criarCelula(row, 7, operadora.getFnFrRec(), estiloMoeda);
            criarCelula(row, 8, operadora.getValor9(), estiloMoeda);
            criarCelula(row, 9, operadora.getValor15(), estiloMoeda);
            criarCelula(row, 10, operadora.getValor20(), estiloMoeda);
            criarCelula(row, 11, operadora.getValor30(), estiloMoeda);
            criarCelula(row, 12, operadora.getTotFnRec(), estiloMoeda);            
            criarCelula(row, 14, operadora.getRecebMenosFnr(), estiloMoeda);
            criarCelula(row, 15, operadora.getPercentualRecebido(), estiloPercentual);
            criarCelula(row, 16, operadora.getPercentualFnr(), estiloPercentual);
            criarCelula(row, 17, operadora.getValorTotalRecebido(), estiloMoeda); */
        }
        
        // Total da coluna PROPORC
        /* BigDecimal totalProporc = operadoras.stream()
            .map(f -> f.getProporc() != null ? f.getProporc() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add); */
        
        Row rowTotal = sheet.createRow(linhaAtual++);
       // criarCelula(rowTotal, 12, "TOTAL PROPORC:", estiloCabecalho);
        //criarCelula(rowTotal, 13, totalProporc, estiloMoeda);
        
        // Ajustar largura das colunas
        for (int i = 0; i < cabecalhos.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static XSSFCellStyle criarEstiloDados(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }


    private static XSSFCellStyle criarEstiloTitulo(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short)14);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(47, 84, 150), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static XSSFCellStyle criarEstiloCabecalho(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(new java.awt.Color(79, 129, 189), null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static XSSFCellStyle criarEstiloMoeda(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("R$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private static XSSFCellStyle criarEstiloPercentual(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    // Método auxiliar para criar células
    private static void criarCelula(Row row, int column, Object value, CellStyle style) {
        Cell cell = row.createCell(column);
        
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Double) {
            cell.setCellValue((Double) value);
        } else if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
            CellStyle dateStyle = row.getSheet().getWorkbook().createCellStyle();
            dateStyle.cloneStyleFrom(style);
            dateStyle.setDataFormat(row.getSheet().getWorkbook().createDataFormat().getFormat("dd/MM/yyyy"));
            cell.setCellStyle(dateStyle);
            return;
        }
        
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    // Método auxiliar para formatar moeda
    private static String formatarMoeda(BigDecimal valor) {
        if (valor == null) return "R$ 0,00";
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return format.format(valor);
    }
    // Método auxiliar para parse de valores monetários
    private static BigDecimal parseMonetaryValue(String value) {
        if (value == null || value.isEmpty()) return BigDecimal.ZERO;
        try {
            String cleaned = value.replace("R$", "").replace(".", "").replace(",", ".");
            return new BigDecimal(cleaned.trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static Object limparDecimalSeNumeroInteiro(String codigo) {
        if (codigo == null) return "";
            try {
                BigDecimal bd = new BigDecimal(codigo.trim());
                if (bd.stripTrailingZeros().scale() <= 0) {
                    return bd.toBigInteger().toString();
                }
                return bd.toPlainString(); // mantém decimal se necessário
            } catch (NumberFormatException e) {
                return codigo; // retorna o original se não for número
            }
    }

    public static BigDecimal obterPercentualGratificacao(BigDecimal rendimento) {
        if (rendimento.compareTo(BigDecimal.valueOf(0.91)) >= 0) {
            return BigDecimal.valueOf(0.0600); // 6%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.88)) >= 0) {
            return BigDecimal.valueOf(0.0550); // 5.5%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.85)) >= 0) {
            return BigDecimal.valueOf(0.0500); // 5%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.80)) >= 0) {
            return BigDecimal.valueOf(0.0450); // 4.5%
        } else if (rendimento.compareTo(BigDecimal.valueOf(0.75)) >= 0) {
            return BigDecimal.valueOf(0.0400); // 4%
        } else {
            return BigDecimal.valueOf(0.0350); // 3.5% (padrão)
        }
    }

    public static String formatarPercentualGratificacao(BigDecimal rendimento) {
        BigDecimal percentual = obterPercentualGratificacao(rendimento);
        return percentual.multiply(BigDecimal.valueOf(100)).stripTrailingZeros() + "%"; // Ex: "6%"
    }

}
