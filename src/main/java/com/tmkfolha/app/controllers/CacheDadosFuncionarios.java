package com.tmkfolha.app.controllers;

import java.math.BigDecimal;
import java.security.DomainLoadStoreParameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tmkfolha.processor.JavaApp;
import com.tmkfolha.processor.XlsProcessor;

import static com.tmkfolha.app.controllers.Constantes.*;

/**
 * Classe responsável por armazenar em cache os dados de funcionários por polo.
 * Implementa o padrão Singleton para garantir que apenas uma instância seja utilizada.
 */
public class CacheDadosFuncionarios {
    private static final Logger logger = LogManager.getLogger(CacheDadosFuncionarios.class);
    private static CacheDadosFuncionarios instance;
    private Map<String, Map<String, Map<String, String>>> tabelaNomesPorPolo;
    private Map<String, Map<String, DemonstrativoDados>> dadosPorPolo; // Agora cada polo tem uma categoria ADM
    private Map<String, List<String>> dadosDemonstrativosPorPolo;

    // Novo mapa para armazenar valores recebidos por operador
    private Map<String, Operadora> dadosOperadoras;
    
    // Novo mapa para armazenar situação geral dos mensageiros
    private Map<String, BigDecimal> situacaoMensageiro;

    /**
    * Construtor privado para evitar instanciação externa.
    * Inicializa as estruturas de armazenamento de dados.
    */    
    public CacheDadosFuncionarios() {
        dadosPorPolo = new HashMap<>();
        tabelaNomesPorPolo = new HashMap<>();
        dadosDemonstrativosPorPolo = new HashMap<>();

        dadosOperadoras = new HashMap<>();
        situacaoMensageiro = new HashMap<>();
    }

    /**
    * Retorna a instância única da classe (Singleton).
    * O método é sincronizado para garantir segurança em ambientes multithread.
    *
    * @return A instância única de {@code CacheDadosFuncionarios}.
    */    
    public static synchronized CacheDadosFuncionarios getInstance() {
        if (instance == null) {
            instance = new CacheDadosFuncionarios();
        }
        return instance;
    }
    
    /**
    * Carrega os nomes dos funcionários no cache a partir de um conjunto de dados extraído de um arquivo.
    * Os dados são organizados por polo de trabalho.
    *
    * @param fileData    Um mapa contendo os dados extraídos do arquivo, onde cada chave representa uma categoria de dados.
    * @param nomeArquivo O nome do arquivo de onde os dados foram extraídos.
    */
    public void carregarNomes(Map<String, Map<String, String>> fileData, String nomeArquivo) {    
        
        if (!tabelaNomesPorPolo.containsKey(nomeArquivo)) {
            tabelaNomesPorPolo.put("SERRA",JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, POLO_SERRA_TALHADA));
            //System.out.println("Estrutura Serra em cach Funcionario " + tabelaNomesPorPolo);
            tabelaNomesPorPolo.put("PETROLINA", 
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, POLO_PETROLINA));
            tabelaNomesPorPolo.put("MATRIZ", 
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, POLO_MATRIZ));
            tabelaNomesPorPolo.put("CARUARU", 
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, POLO_CARUARU));
            tabelaNomesPorPolo.put("GARANHUNS", 
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, POLO_GARANHUNS));
            tabelaNomesPorPolo.put("OPE", 
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, OPE));
            tabelaNomesPorPolo.put("ADM", 
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, ADM)); 
                tabelaNomesPorPolo.put("MSG",
                JavaApp.buscarValoresPorNomeRetornaMapaDeMapaSemColunaEspecifica(fileData, nomeArquivo, MSG));                  
        }
    }

    public void testarTabelaNomesPorPolo() {
        //cache = CacheDadosFuncionarios.getInstance();
        Map<String, Map<String, Map<String, String>>> tabela = getTabelaNomesPorPolo();
        
        if (tabela.isEmpty()) {
            logger.warn("A tabela está vazia. Verifique se carregarNomes() foi chamado.");
            return;
        }
    
        logger.info("=== ESTRUTURA TABELA NOMES POR POLO ===");
        
        tabela.forEach((polo, operadoras) -> {
            logger.info("POLO: {}", polo);
            logger.info("Quantidade de operadoras: {}", operadoras.size());
            
            if (operadoras.isEmpty()) {
                logger.warn("Nenhuma operadora encontrada neste polo!");
            } else {
                operadoras.entrySet().stream()
                    .limit(3) // Mostra apenas 3 operadoras por polo
                    .forEach(op -> {
                        logger.info("Operadora {}: {}", op.getKey(), op.getValue());
                    });
                
                if (operadoras.size() > 3) {
                    logger.info("... (mais {} operadoras)", operadoras.size() - 3);
                }
            }
        });
    }
    
    // TEsta se foram caregado os nomes
    public boolean isNomesCarregados() {
        return !tabelaNomesPorPolo.isEmpty();// Retorna true se já tiver dados carregados
    }

    /**
     * Recupera a tabela de nomes correspondente a um determinado polo.
     *
     * @param polo O nome do polo para o qual a tabela de nomes deve ser recuperada.
     * @return Um mapa contendo os nomes associados ao polo especificado. 
     *         Retorna o primeiro conjunto de valores encontrado na estrutura de dados.
     * @throws NullPointerException Se o polo não existir na estrutura de dados ou não houver valores disponíveis.
    */    
    public Map<String, String> getTabelaNomes(String polo) {
        if (!tabelaNomesPorPolo.containsKey(polo) || tabelaNomesPorPolo.get(polo).isEmpty()) {
            return Collections.emptyMap(); // Retorna um mapa vazio ao invés de lançar exceção
        }    
        return tabelaNomesPorPolo.get(polo).values().iterator().next();
    }
    
    /**
     * Adiciona um demonstrativo de dados para um polo específico.
     *
     * @param polo O nome do polo ao qual os dados pertencem.
     * @param dados A lista de dados do demonstrativo a ser armazenada.
     */
   /*  public void adicionarDemonstrativo(String polo, List<String> dados) {
        dadosDemonstrativosPorPolo.put(polo, dados);
    } */

    public void adicionarDemonstrativo(String polo, String categoria, DemonstrativoDados dados){
        /* dadosPorPolo.putIfAbsent(polo, new HashMap<>());
        dadosPorPolo.get(polo).put(categoria, dados); */

        logger.info("Polo {}",polo);
        dadosPorPolo.computeIfAbsent(polo, k -> new HashMap<>()).put(categoria, dados);
    }
    public DemonstrativoDados getDemonstrativo(String polo, String categoria) {
        return dadosPorPolo.getOrDefault(polo, new HashMap<>()).getOrDefault(categoria, new DemonstrativoDados());
    }
    public void imprimirCache() {
        System.out.println("Dados do Cache:");
        for (var polo : dadosPorPolo.keySet()) {
            System.out.println("Polo: " + polo);
            for (var categoria : dadosPorPolo.get(polo).keySet()) {
                System.out.println("  Categoria: " + categoria + " -> " + dadosPorPolo.get(polo).get(categoria));
            }
        }
    }    

    /**
     * Obtém a lista de demonstrativos associada a um determinado polo.
     *
     * @param polo O nome do polo cujos demonstrativos serão recuperados.
     * @return Uma lista de demonstrativos do polo especificado ou {@code null} se o polo não existir.
     */    
    /* public List<String> getDemonstrativo(String polo) {
        return dadosDemonstrativosPorPolo.getOrDefault(polo, Collections.emptyList());
    } */

    public Map<String, Map<String, Map<String, String>>> getTabelaNomesPorPolo() {
        return this.tabelaNomesPorPolo;
    }

     // Adicionar valor recebido por operador
    /*  public void adicionarRecebidoPorOperador(String operador, BigDecimal valor) {
        recebidoPorOperador.put(operador, valor);
        logger.debug("Valor adicionado para operador '{}': {}", operador, valor);
    }

    // Obter valor recebido por operador
    public BigDecimal getRecebidoPorOperador(String operador) {
        return recebidoPorOperador.getOrDefault(operador, BigDecimal.ZERO);
    }
 */
    // Adicionar situação geral do mensageiro
    public void adicionarSituacaoMensageiro(String mensageiro, BigDecimal valor) {
        situacaoMensageiro.put(mensageiro, valor);
    }

    // Obter situação geral do mensageiro
    public BigDecimal getSituacaoMensageiro(String mensageiro) {
        return situacaoMensageiro.getOrDefault(mensageiro, BigDecimal.ZERO);
    }

    /**
     * Verifica se um determinado polo possui dados armazenados.
     *
     * @param polo O nome do polo a ser verificado.
     * @return {@code true} se o polo possuir dados tanto na tabela de nomes quanto nos demonstrativos, {@code false} caso contrário.
     */    
    public boolean possuiDadosPolo(String polo) {
        return tabelaNomesPorPolo.containsKey(polo) && !tabelaNomesPorPolo.get(polo).isEmpty()
        && dadosDemonstrativosPorPolo.containsKey(polo) && !dadosDemonstrativosPorPolo.get(polo).isEmpty();
    }

    public void adicionarDadosOperadoras(String polo, List<String> dadosOperadoras) {
         // Cria o polo se não existir
         if (!tabelaNomesPorPolo.containsKey(polo)) {
            tabelaNomesPorPolo.put(polo, new HashMap<>());
        }

        Map<String, Map<String, String>> dadosPolo = tabelaNomesPorPolo.get(polo);
        
        for (String dados : dadosOperadoras) {
            // Extrai os valores da string formatada
            Map<String, String> valores = parseDadosOperadora(dados);
            String codigo = valores.get("Codigo");
            
            if (codigo != null) {
                // Atualiza ou adiciona a operadora
                if (dadosPolo.containsKey(codigo)) {
                    dadosPolo.get(codigo).putAll(valores);
                } else {
                    dadosPolo.put(codigo, valores);
                }
            }
        }
        
        logger.info("Dados das operadoras atualizados para o polo {}", polo);
    }

    private Map<String, String> parseDadosOperadora(String dados) {
        Map<String, String> valores = new HashMap<>();
        String[] partes = dados.split("\\|");
        
        for (String parte : partes) {
            String[] keyValue = parte.split(":");
            if (keyValue.length == 2) {
                valores.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        
        return valores;
    }
       

    public void adicionarDadosRecebidoDasOperadoras(String polo, List<String> dadosOperadoras) {
        if (!tabelaNomesPorPolo.containsKey(polo)) {
            tabelaNomesPorPolo.put(polo, new HashMap<>());
        }
    
        Map<String, Map<String, String>> poloData = tabelaNomesPorPolo.get(polo);
        
        for (String linha : dadosOperadoras) {
            // Extrai os valores da linha formatada
            String[] partes = linha.split(", ");
            Map<String, String> valores = new HashMap<>();
            
            for (String parte : partes) {
                String[] keyValue = parte.split(" -> ");
                if (keyValue.length == 2) {
                    valores.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
            
            // Obtém o código da operadora (precisa ser extraído de outra forma)
            // Você pode precisar ajustar isso conforme sua estrutura de dados
            String codigo = valores.getOrDefault("Codigo", "DESCONHECIDO");
            
            // Atualiza ou adiciona os dados no polo
            if (poloData.containsKey(codigo)) {
                poloData.get(codigo).putAll(valores);
            } else {
                poloData.put(codigo, valores);
            }
        }
        
        logger.info("Dados das operadoras atualizados para o polo {}", polo);
    }

    public Operadora getDadosOperadora(String codigo) {
        return dadosOperadoras.get(codigo);
    }

    public Map<String, Operadora> getTodosDadosOperadoras() {
        return new HashMap<>(dadosOperadoras);
    }

    /* public boolean contemOperador(String operador) {
        return recebidoPorOperador.containsKey(operador);
    }
    
    public BigDecimal getValorPorOperador(String operador) {
        return recebidoPorOperador.get(operador);
    } */

    public static Map<String, Map<String, String>> processarDados(Map<String, Map<String, String>> fileData) {
        Map<String, Map<String, String>> novoFileData = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> arquivoEntry : fileData.entrySet()) {
            String nomeArquivo = arquivoEntry.getKey();
            Map<String, String> linhas = arquivoEntry.getValue();

            for (Map.Entry<String, String> linhaEntry : linhas.entrySet()) {
                String linhaKey = linhaEntry.getKey();
                Map<String, String> linhaValores = extrairColunas(linhaEntry.getValue());

                if (linhaValores.containsKey("Linha " + linhaKey.split(" ")[1] + " - Column4")) {
                    String polo = linhaValores.get("Linha " + linhaKey.split(" ")[1] + " - Column4");

                    // Criando um identificador único para evitar conflito
                    String chaveLinha = linhaKey + " (" + polo + ")";
                    novoFileData.put(chaveLinha, linhaValores);
                }
            }
        }
        return novoFileData;
    }

    private static Map<String, String> extrairColunas(String linhaCompleta) {
        Map<String, String> colunas = new HashMap<>();
        String[] valores = linhaCompleta.split(", ");
        for (String valor : valores) {
            String[] partes = valor.split("=");
            if (partes.length == 2) {
                colunas.put(partes[0].trim(), partes[1].trim());
            }
        }
        return colunas;
    }

    public DemonstrativoDados obterDemonstrativo(String setor, String categoria) {
         // Verifica se o polo existe no cache
        if (dadosPorPolo.containsKey(setor)) {
            return dadosPorPolo.get(setor).getOrDefault(categoria, null);
        }
        
        return null; // Retorna null caso não encontre os dados
    }

    public void atualizarDadosOperadora(String polo, Map<String, Map<String, String>> dadosOperadoras) {
        if (!"OPE".equals(polo)) return; // Garante que só atualiza operadoras
        
        Map<String, Map<String, String>> funcionariosPolo = tabelaNomesPorPolo.get(polo);
        
        if (funcionariosPolo != null) {
            for (Map.Entry<String, Map<String, String>> entry : funcionariosPolo.entrySet()) {
                String codigo = entry.getValue().get("Column1"); // Código do funcionário
                
                if (codigo != null && dadosOperadoras.containsKey(codigo)) {
                    Map<String, String> dadosOperadora = dadosOperadoras.get(codigo);
                    Map<String, String> funcData = entry.getValue();
                    
                    // Atualiza os dados do funcionário com os valores da operadora
                    funcData.putAll(dadosOperadora);
                    
                    // Log para debug (opcional)
                    logger.debug("Atualizado funcionário " + codigo + " com dados: " + dadosOperadora);
                }
            }
        }
    }

    /* public void atualizarDadosOperadora(String polo, Map<String, Operadora> operadoras) {
        // Obtém o mapa de funcionários para este polo
        Map<String, Map<String, String>> funcionariosPolo = tabelaNomesPorPolo.get(polo);
        
        if (funcionariosPolo != null) {
            for (Map.Entry<String, Map<String, String>> entry : funcionariosPolo.entrySet()) {
                String codigo = entry.getValue().get("Column1"); // Onde Column1 tem o código
                String nome = entry.getValue().get("Column3");   // Onde Column3 tem o nome
                
                if (operadoras.containsKey(codigo)) {
                    Operadora op = operadoras.get(codigo);
                    
                    // Atualiza os dados do funcionário no cache
                    Map<String, String> funcionarioData = entry.getValue();
                    
                    // Adiciona/atualiza os campos da operadora
                    funcionarioData.put("QuantidadeBoleto", String.valueOf(op.getQuantidadeBoleto()));
                    funcionarioData.put("ValorBoleto", op.getValorBoleto().toString());
                    funcionarioData.put("ComissaoBoleto", op.getComissaoBoleto().toString());
                    funcionarioData.put("QuantidadeRecibo", String.valueOf(op.getQuantidadeRecibo()));
                    funcionarioData.put("ValorRecibo", op.getValorRecibo().toString());
                    funcionarioData.put("ComissaoRecibo", op.getComissaoRecibo().toString());
                }
            }
        }
    } */   
   
}