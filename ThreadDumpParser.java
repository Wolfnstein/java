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
      
    private static String inputDirPath   = "D:\\Work\\GitRepo\\Workspace\\CodeTest\\crossjoin\\crossjoin_td_test"; 
    private static String outputFilePath = "D:\\Work\\GitRepo\\Workspace\\CodeTest\\crossjoin\\java\\output.csv";

    private static final String[] headers = {
            "yyyy-mm-dd", "yyyy-mm-dd hh24:00:00", "yyyy-mm-dd hh24:mi:00", "yyyy-mm-dd hh24:mis:ss", "thread type", "thread name", "thread state", "last call", "last custom call"
            //",prio value", "os_prio value", "cpu value", "elapsed value", "tid value", "nid value"
    };

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
              
        String fileDateTime = "";

        // linha a ser analisada
        String line;    
        
        ThreadDump currentThreadInfo = null;

        // enquanto o buffer de leitura tiver linhas para serem lidas ... 
        while ((line = reader.readLine()) != null) {

            if (isDate(line)){
                fileDateTime = line;
            }
            
            // cada ThreadDump começa com " no inicio da linha
            if (line.startsWith("\"")) {
                if (currentThreadInfo != null) {
                    allThreads.add(currentThreadInfo);
                }                
                currentThreadInfo = new ThreadDump();

                currentThreadInfo.parseDateLine(fileDateTime);
                
                // criamos um ThreadInfo                
                currentThreadInfo.parseThreadLine(line);
                                
            
            // para cada Thread podemos ter o seu estado se encontrar-mos um .. 
            } else if (line.contains("java.lang.Thread.State:")) {
                currentThreadInfo.parseStateLine(line);
            } else if (line.contains("at ")) {
                currentThreadInfo.addStackTrace(line);
            }
        }
        if (currentThreadInfo != null) {
            allThreads.add(currentThreadInfo);
        }

        reader.close();
        return allThreads;
    }    

    private static boolean isDate(String line) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try { Date date = dateFormat.parse(line);  return true; } catch (ParseException e) { return false; }
    }

    // metodo para listar todos os ficheiros de uma directoria 
    // partindo do principio que nesta directoria apenas estão os ficheiros que queremos analisar
    private static List<File> listFiles(String dirPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            return paths.filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .collect(Collectors.toList());
        }
    }

    // metodo para escrever gerar o ficheiro csv onde os campos estãi delimitados por ;
    private static void writeCSV(String filePath, List<ThreadDump> allThreadsDumps) throws IOException {
        FileWriter writer = new FileWriter(filePath);

        // escrever o cabeçalho
        writer.append(String.join(";", headers));
        writer.append("\n");
        
        // para cada registo em allThreadsDumps ...
        for (ThreadDump info : allThreadsDumps) {
            writer.append(String.join(";", info.toCSVRecord()));
            writer.append("\n");
        }
        
        writer.flush();
        writer.close();
    } 
}
