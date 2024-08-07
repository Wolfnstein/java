import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ThreadDump {

    private Date threadDate;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_TIME_FORMAT_HH = new SimpleDateFormat("yyyy-MM-dd HH:00:00");
    private static final SimpleDateFormat DATE_TIME_FORMAT_MI = new SimpleDateFormat("yyyy-MM-dd HH:mm:00");
    private static final SimpleDateFormat DATE_TIME_FORMAT_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    private String threadName;
    private String threadType;
    private String state;
    private List<String> stackTrace = new ArrayList<>();

    private String quotedPart;

    public void parseDateLine(String line){

        try {
            this.threadDate = DATE_TIME_FORMAT_SS.parse(line);
        } 
        catch (ParseException e) {
            e.printStackTrace();
        }        
    }

    // "http-nio-7012-exec-177" #108 daemon prio=5 os_prio=0 tid=0x00007f44d00a3800 nid=0x790 runnable [0x00007f44f4d6e000]
    public void parseThreadLine(String line) {

        Pattern pattern = Pattern.compile("\"([^\"]*)\"(.*)");
        Matcher matcher = pattern.matcher(line);
        
        if (matcher.find()) {
            quotedPart = matcher.group(1); // The part inside quotes
        }

        this.threadName = quotedPart.replaceAll("\"", "");

        // Extract thread type by removing the last segment after the last hyphen
        int lastDashIndex = this.threadName.lastIndexOf('-');
        if (lastDashIndex != -1) {
            this.threadType = this.threadName.substring(0, lastDashIndex);
        } else {
            this.threadType = this.threadName;  // If no hyphen, use the entire thread name
        }
    }

    //java.lang.Thread.State: RUNNABLE
    public void parseStateLine(String line) {
        this.state = line.split("java.lang.Thread.State: ")[1];
    }

    public void addStackTrace(String line) {
        this.stackTrace.add(line.trim());
    }

    public List<String> toCSVRecord() {
        List<String> record = new ArrayList<>();

        record.add(DATE_FORMAT.format(this.threadDate));
        record.add(DATE_TIME_FORMAT_HH.format(this.threadDate));
        record.add(DATE_TIME_FORMAT_MI.format(this.threadDate));
        record.add(DATE_TIME_FORMAT_SS.format(this.threadDate));
        record.add(this.threadType);
        record.add(this.threadName);
        record.add(this.state);
        record.add(stackTrace.isEmpty() ? "" : stackTrace.get(0).replaceAll("at ", ""));
        record.add(getLastCustomCall());

        return record;
    }

    private String getLastCustomCall() {
        // Custom logic to identify the last custom call from the stack trace
        for (String line : stackTrace) {
            if (line.contains("com.rabbitmq.client")) {  // Adjust based on your application's package
                return line;
            }
        }
        return stackTrace.isEmpty() ? "" : stackTrace.get(stackTrace.size() - 1).replaceAll("at ", "");
    }
}
