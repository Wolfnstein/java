import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDump {
    
    
    // formatos de data
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_TIME_FORMAT_HH = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
    private static final SimpleDateFormat DATE_TIME_FORMAT_MI = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
    private static final SimpleDateFormat DATE_TIME_FORMAT_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    // variavel do tipo Date pra a data do ficheiro
    private Date threadDate;
    // variavel para o nome do Thread 
    private String threadName;
    // variavel para o typo de Thread
    private String threadType;
    // variavel para o estado do Thread
    private String state;

    // lista de strings para conter os stacktraces do Thread
    private List<String> stackTrace = new ArrayList<>();

    // metodo para fazer parse à data
    public void parseDateLine(String line){
        try {
            // 2021-03-29 06:57:51 este é o formato inicial da data no ficheiro por isso uso o formato DATE_TIME_FORMAT_SS
            // fazer set à variavel threadDate
            this.threadDate = DATE_TIME_FORMAT_SS.parse(line);
        } 
        catch (ParseException e) {
            e.printStackTrace();
        }        
    }

    // "http-nio-7012-exec-177" #108 daemon prio=5 os_prio=0 tid=0x00007f44d00a3800 nid=0x790 runnable [0x00007f44f4d6e000]
    // metodo para fazer parse à linha inicial do Thread
    public void parseThreadLine(String line) {

        // patrão para is buscar tudo o que estiver entre ""
        Pattern pattern = Pattern.compile("\"([^\"]*)\"");
        // definir a string da linha, para fazer match com o padrão definido
        Matcher matcher = pattern.matcher(line);
       
        // se encontrar-mos na lina o padrão 
        if (matcher.find()) {
            // dfazer set à variavel threadName com o que estiver dentro dos "" 
            this.threadName = matcher.group(1);
        }

        // encontrar a posição onde se encontra o ultimo carater - 
        int lastDashIndex = this.threadName.lastIndexOf('-');

        // se existir um carater -
        if (lastDashIndex != -1) {
            // fazemos set à variavel threadType com a substring da variavel threadName até ao ultimo - 
            this.threadType = this.threadName.substring(0, lastDashIndex);
        } else {
            // se não existir nenhum - então fazemos set ao threadType = threadName
            this.threadType = this.threadName;  
        }
    }

    // java.lang.Thread.State: RUNNABLE
    // metodo para fazer parse ao estado do Thread
    public void parseStateLine(String line) {
        // fazemos set à variavel state com a string que se encontra na frente de 'java.lang.Thread.State: '
        this.state = line.split("java.lang.Thread.State: ")[1];
    }

    // metodo para agrupar todos os stacktraces do Thread
    public void addStackTrace(String line) {
        // fazemos set à variavel stackTrace adicionando, limpando no caso de haver espaços no inicio e fim
        this.stackTrace.add(line.trim());
    }

    // metodo que devolve uma lista de strings que conterá o Thread que está a ser analisado
    public List<String> toCSVRecord() {
        // criação da lista de strings
        List<String> record = new ArrayList<>();

        // adicionamos à lista as datas nos diferentes formatos
        record.add(DATE_FORMAT.format(this.threadDate));
        record.add(DATE_TIME_FORMAT_HH.format(this.threadDate));
        record.add(DATE_TIME_FORMAT_MI.format(this.threadDate));
        record.add(DATE_TIME_FORMAT_SS.format(this.threadDate));
        // adicionar o threadType
        record.add(this.threadType);
        // adicionar o threadName
        record.add(this.threadName);
        // adicionar o state
        record.add(this.state);
        // adicionar o stackTrace
        // se o stackTrace vier vazio então preenche "" caso contrario 
        // colocar o primeiro stackTrace guardado na lista de stackTrace deste Thread
        record.add(stackTrace.isEmpty() ? "" : stackTrace.get(0));
        // colocar o resultado do parse do customCall
        record.add(getLastCustomCall());

        // devolver o resultado
        return record;
    }

    // metodo para fazer o parse do CustomCall 
    // nota: tive que tentar perceber o que queriam dizer com customcall num stacktrace 
    //       considerei customcall no caso de ser stacktraces de class's do código do cliente e não das api de java 
    //       mas não consegui identificar esse código nem se existe outra maneira da o identificar pelo dump
    //       neste caso usei com.rabbitmq.client como sendo um CustomCall do código de cliente.
    private String getLastCustomCall() {
        // para cada linha da lista de stackTrace valorizar a string line
        for (String line : stackTrace) {
            // se essa string conter com.rabbitmq.client é um CustomCall
            if (line.contains("com.rabbitmq.client")) {  
                // devolve essa linha do stackTrace
                return line;
            }
        }
        // no caso de nehum regito da lista de stackTrace conter um CustomCall
        // no caso do stackTrace estiver vazia devolve "" caso contrario vai buscar o ultimo da lista do stackTrace
        return stackTrace.isEmpty() ? "" : stackTrace.get(stackTrace.size() - 1);
    }
}
