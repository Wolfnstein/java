import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
public class ThreadDumpParser {
    
    // nota: devia por estas duas varaiaveis como parametros de entrada para serem definidos por ai
    // e não estarem hard coded mas 

    // diretoria onde se encontram os dumps a serem analisados
    private static String inputDirPath   = "C:\\crossjoin_td_test"; 
    // diretoria de output do ficheiro csv
    private static String outputFilePath = "C:\\crossjoin_td_test\\output.csv";

    // array de strings com o cabeçalho do csv
    private static final String[] headers = {
            "yyyy-mm-dd", "yyyy-mm-dd hh24:00:00", "yyyy-mm-dd hh24:mi:00", "yyyy-mm-dd hh24:mis:ss", "thread type", "thread name", "thread state", "last call", "last custom call"
            //",prio value", "os_prio value", "cpu value", "elapsed value", "tid value", "nid value"
    };

    // array de strings com exclusões de inicis de linha que não quero guardar
    // nota: são Threads comuns a todos os ficheiros não contam aparentemente para o numero de Threads na _java_thread_list 
    //       escolhi não contar com elas.
    private static String[] exclusions =  {
        "VM Thread", "GC Thread#0", "G1 Main Marker", "G1 Conc#0", "G1 Refine#0", "G1 Young RemSet Sampling", "VM Periodic Task Thread"
    };

    // metodo main, basta correr este java para se conseguir 
    public static void main(String[] args) {
        try {

            // criar uma lista vazia de objectos do tipo ThreadDump para se adicionar todas os threads dumps analisados
            // de todos os ficheiros
            List<ThreadDump> allThreadsDumps = new ArrayList<>();

            // criar uma lista de ficheiros encontrados numa directoria para serem analisados
            List<File> files = listFiles(inputDirPath);

            // percorrer a lista de ficheiros e por cada ficheiro que se encontra na lista ...
            for (File file : files) {
                // adicionar a lista com a analise de todos os thread dump encontrados num ficheiro 
                // à lista de thread dumps a serem enviadas para o csv
                allThreadsDumps.addAll(parseThreadDump(file.getAbsolutePath()));
            }
            // chamar o metodo writeCSV para escrever no ficheiro indicado todos os registos 
            // analisados e que se encontram listados em allThreadsDumps.
            writeCSV(outputFilePath, allThreadsDumps);
       
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // metodo para devolver uma lista com a analise de todos os ThreadDumps de um ficheiro.
    private static List<ThreadDump> parseThreadDump(String filePath) throws IOException {
        
        // lista vazia de objectos do tipo ThreadDump para se adicionar todos os thread dumps analisados do ficheiro
        List<ThreadDump> allThreads = new ArrayList<>();

        // buffer standard de leitura de um ficheiro
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        
        // string onde vamos guardar a data do ficheiro que estamos a ler
        String fileDateTime = "";

        // linha a ser analisada
        String line;    
        
        // Objeto do tipo ThreadDump
        ThreadDump currentThreadInfo = null;

        // enquanto o buffer de leitura tiver linhas para serem lidas ... 
        while ((line = reader.readLine()) != null) {

            // ver se a linha encontrada é uma data 
            if (isDate(line)){
                // se for preencher a string fileDateTime com a data 
                fileDateTime = line;
            }
            
            // cada ThreadDump começa com " no inicio da linha, excepto os Threads que estiverem na
            if (line.startsWith("\"") && !IsExcluded(line,exclusions)  ) {
                
                // garantir que o ultimo objeto do tipo ThreadDump é adicionado
                if (currentThreadInfo != null) {
                    allThreads.add(currentThreadInfo);
                }                

                // criamos um novo objeto do tipo ThreadDump
                currentThreadInfo = new ThreadDump();

                // fazer o parse da data
                currentThreadInfo.parseDateLine(fileDateTime);
                
                // fazer o parse da linha              
                currentThreadInfo.parseThreadLine(line);                                
            
            // para cada Thread podemos ter o seu estado se encontrar-mos um .. 
            } else if (line.contains("java.lang.Thread.State:")) {

                // garantir que currentThreadInfo não é null antes de iniciar o parse da linha
                if (currentThreadInfo != null) {
                    // fazer parse da linha com o estado do Thread
                    currentThreadInfo.parseStateLine(line);
                }
                
            // para cada Thread podemos ver os stacktrace nas linhas que começam por "at "
            } else if (line.contains("at ")) {
                
                // garantir que currentThreadInfo não é null antes de iniciar o parse da linha
                if (currentThreadInfo != null) {
                    // fazer o parse das linhas de stacktrace 
                    currentThreadInfo.addStackTrace(line);
                }
            }
        }
        // garantir que adicionamos a thread na lista com todas as Threads encontradas.
        if (currentThreadInfo != null) {
            allThreads.add(currentThreadInfo);
        }

        // fechar o stream reader.
        reader.close();
        // devolver a lista com todas as Threads
        return allThreads;
    }    

    // metodo para verificar se a linha é uma data
    private static boolean isDate(String line) {
        // definir o formato da data que se encontra nos ficheiros
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // tentar fazer parse da data, se for possivel é porque é uma data se for 
        // para excepção do parse é porque a linha não é uma data 
        // nota: codigo um pouco feito gostava de fazer melhor aqui !! mas funciona !
        try { @SuppressWarnings("unused")
            Date date = dateFormat.parse(line);  
            return true; 
        } 
        catch (ParseException e) { 
            return false; 
        }
    }

    // metodo para vermos se a linha contem uma Thread a ser excluida
    private static boolean IsExcluded(String line, String[] exclusions) {
        // por cada exclusão encontrada no array de strings de exclusies colocar essa string em str
        for (String str : exclusions) {
            // verificar se a linha contem essa string str 
            if (line.contains(str)) {
                // contem a exclusão
                return true;
            }
        }
        // não contem a exclusão
        return false;
    }

    // metodo para listar todos os ficheiros de uma directoria 
    // partindo do principio que nesta directoria apenas estão os ficheiros que queremos analisar
    private static List<File> listFiles(String dirPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            return paths.filter(Files::isRegularFile).map(Path::toFile).collect(Collectors.toList());
        }
    }

    // metodo para escrever gerar o ficheiro csv onde os campos estãi delimitados por ;
    private static void writeCSV(String filePath, List<ThreadDump> allThreadsDumps) throws IOException {
        FileWriter writer = new FileWriter(filePath);

        // escrever o cabeçalho
        writer.append(String.join(",", headers));
        // proxima linha
        writer.append("\n");
        
        // para cada registo na lista de allThreadsDumps colocar o valor num objeto do tipo ThreadDump
        for (ThreadDump info : allThreadsDumps) {
            // adicionar no ficheiro os campos que se encontram no objeto do tipo ThreadDump delimitados por ','
            // ',' para facilitar o import do csv no excel.
            writer.append(String.join(",", info.toCSVRecord()));          
            // proxima linha
            writer.append("\n");
        }
        
        // fechar writer
        writer.flush();
        writer.close();
    } 
}
