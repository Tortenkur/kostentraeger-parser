import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CostUnitData {
    private int ik;
    String name;
    String addressStreet;
    String addressPostalCode;
    String addressCity;
    int idRegion;
    int idState;
    int parentIk;
    int dataCollectionIk;
    
    // constructor
    public CostUnitData(int ikNr) {
        this.ik = ikNr;
    }

    // getter
    public int getIk(){
        return ik;
    }
}
  
public class Parser {
    public static void main(String[] args) {

        // variables
        String seperator = "";

        // read file
        try {
            
            int msgCount = 0;
            int numMessages = 0;
            File costUnit = new File("AO05Q323.ke0");
            Scanner costUnitReader = new Scanner(costUnit, "ISO-8859-1");
            while (costUnitReader.hasNextLine()) {

                // extract messages
                String data = costUnitReader.nextLine();
                Pattern fileHeadPattern = Pattern.compile("^UNA");
                Matcher fileHeadMatcher = fileHeadPattern.matcher(data);
                Pattern fileEndPattern = Pattern.compile("^UNZ");
                Matcher fileEndMatcher = fileEndPattern.matcher(data);
                Pattern messageHeadPattern = Pattern.compile("^UNH");
                Matcher messageHeadMatcher = messageHeadPattern.matcher(data);
                Pattern messageEndPattern = Pattern.compile("^UNT");
                Matcher messageEndMatcher = messageEndPattern.matcher(data);

                if (fileHeadMatcher.find()) {
                    String[] dataArr = data.split("", 0);
                    seperator = "\\" + dataArr[4];
                    continue;
                }

                if (messageHeadMatcher.find()) {
                    msgCount ++;
                }

                if (fileEndMatcher.find()) {
                    String[] dataArr = data.split(seperator, 0);
                    numMessages = Integer.parseInt(dataArr[1]);
                }
            }
            costUnitReader.close();

            // check if all messages have been read
            System.out.println(msgCount == numMessages);
        } catch (FileNotFoundException e) {
            System.out.println("File to parse not found.");
            e.printStackTrace();
        }
    }
}

