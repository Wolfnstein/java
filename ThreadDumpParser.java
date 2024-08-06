import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ThreadDumpParser {

    private static final String[] HEADERS = {
            "yyyy-mm-dd", "yyyy-mm-dd hh24:00:00", "yyyy-mm-dd hh24:mi:00", "yyyy-mm-dd hh24:mis:ss", "thread type", "thread name", "thread state", "last call", "last custom call"
            //",prio value", "os_prio value", "cpu value", "elapsed value", "tid value", "nid value"
    };

    public static void main(String[] args) {
        String inputDirPath = "C:\\Users\\vrica\\Downloads\\crossjoin\\crossjoin_td_test"; 
        String outputFilePath = "output.csv";

        try {
            List<ThreadInfo> allThreadInfos = new ArrayList<>();
            List<File> files = listFiles(inputDirPath);

            for (File file : files) {
                List<ThreadInfo> threadInfos = parseThreadDump(file.getAbsolutePath());
                allThreadInfos.addAll(threadInfos);
            }

            writeCSV(outputFilePath, allThreadInfos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<File> listFiles(String dirPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(dirPath))) {
            return paths.filter(Files::isRegularFile)
                        .map(Path::toFile)
                        .collect(Collectors.toList());
        }
    }

    private static List<ThreadInfo> parseThreadDump(String filePath) throws IOException {



        List<ThreadInfo> threadInfos = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        ThreadInfo currentThreadInfo = null;

        while ((line = reader.readLine()) != null) {


            if (line.startsWith("\"")) {
                if (currentThreadInfo != null) {
                    threadInfos.add(currentThreadInfo);
                }
            
                currentThreadInfo = new ThreadInfo();
                currentThreadInfo.parseThreadLine(line);
            
            } else if (line.contains("java.lang.Thread.State:")) {
                currentThreadInfo.parseStateLine(line);
            } else if (line.contains("at ")) {
                currentThreadInfo.addStackTrace(line);
            } else if (line.contains("prio=")) {
                currentThreadInfo.parseDetailsLine(line);
            }



        }
        if (currentThreadInfo != null) {
            threadInfos.add(currentThreadInfo);
        }

        reader.close();
        return threadInfos;
    }

    private static void writeCSV(String filePath, List<ThreadInfo> threadInfos) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        writer.append(String.join(";", HEADERS));
        writer.append("\n");
        
        for (ThreadInfo info : threadInfos) {
            writer.append(String.join(";", info.toCSVRecord()));
            writer.append("\n");
        }
        
        writer.flush();
        writer.close();
    }
}
