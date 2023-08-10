import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.Arrays;
import java.util.HashMap;

class CostUnitData {
    private String ik;
    String name;
    String addressStreet;
    String addressPostalCode;
    String addressCity;
    String idRegion;
    String idState;
    String email;
    String parentIk;
    String dataCollectionIk;
    
    // constructor
    public CostUnitData(String ikNr) {
        this.ik = ikNr;
    }

    // getter
    public String getIk(){
        return ik;
    }

    // setter
    public void setIdRegion(String id) {
        if (id != "") {
            this.idRegion = id;
        }
    }
    public void setIdState(String id) {
        if (id != "") {
            this.idState = id;
        }
    }
}
  
public class Parser {
    static String checkSegment(String segment, String segmentTerminator) {
            // check segment and remove segment terminator
            Pattern checkSegmentPattern = Pattern.compile(segmentTerminator + "$");
            Matcher checkSegmentMatcher = checkSegmentPattern.matcher(segment);
            if (!checkSegmentMatcher.find()) {
                System.out.println("Segment failure in segment" + segment);
            } else {
                segment = segment.substring(0, segment.length()-1);
            }
            return segment;
    }

    
    public static void main(String[] args) {

        // variables
        String seperator = "";
        Stack <String> connectionSegmentsVkgStack = new Stack <String>();
        HashMap <String, CostUnitData> costUnitHashMap = new HashMap <String, CostUnitData>(); 

        // read and parse file
        try {
            
            int msgCount = 0;
            int numMessages = 0;
            String segmentTerminator = "";
            File costUnit = new File("AO05Q323.ke0");
            Scanner costUnitReader = new Scanner(costUnit, "ISO-8859-1");
            while (costUnitReader.hasNextLine()) {
                String data = costUnitReader.nextLine();
                Pattern fileHeadPattern = Pattern.compile("^UNA");
                Pattern fileEndPattern = Pattern.compile("^UNZ");
                Pattern messageHeadPattern = Pattern.compile("^UNH");
                Pattern messageEndPattern = Pattern.compile("^UNT");
                Matcher messageEndMatcher;

                Matcher fileHeadMatcher = fileHeadPattern.matcher(data);
                if (fileHeadMatcher.find()) {
                    String[] dataArr = data.split("", 0);
                    seperator = "\\" + dataArr[4];
                    segmentTerminator =  "\\" + dataArr[8];
                    continue;
                }
                
                data = checkSegment(data, segmentTerminator);

                // extract message
                Matcher messageHeadMatcher = messageHeadPattern.matcher(data);
                if (messageHeadMatcher.find()) {
                    msgCount ++;
                    int numSegments = 1;
                    String[] dataArr = data.split(seperator, 0);
                    String messageReference = dataArr[1];

                    // Message variables
                    String ik = "";
                    String name = "";
                    String addressStreet = "";
                    String addressPostalCode = "";
                    String addressCity = "";
                    String email = "";
                                    
                    do {
                        data = costUnitReader.nextLine();
                        numSegments ++;
                        data = checkSegment(data, segmentTerminator);
                        dataArr = data.split(seperator, 0);

                        switch(dataArr[0]) {
                            case "IDK":
                                dataArr = data.split(seperator, 0);
                                ik = dataArr[1];
                                break;
                            case "VKG":
                                String connectionSegmentToAdd = data + seperator + ik;
                                connectionSegmentsVkgStack.push(connectionSegmentToAdd);
                                break;
                            case "NAM":
                                String[] nameToAddArray = Arrays.copyOfRange(dataArr, 2, dataArr.length);
                                name = String.join(" ", nameToAddArray);
                                break;
                            case "ANS":
                                if (dataArr[1].equals("1")) {
                                    addressPostalCode = dataArr[2];
                                    if (dataArr.length > 3) {
                                        addressCity = dataArr[3];
                                    }
                                    if (dataArr.length > 4) {
                                        addressStreet = dataArr[4];
                                    }
                                }
                                break;
                            case "DFU":
                                if (dataArr[2].equals("070")) {
                                    email = dataArr[7];
                                }
                                break;
                        }

                    }
                    while (!dataArr[0].equals("UNT"));

                    // check message integrity
                    dataArr = data.split(seperator, 0);
                    if (!(numSegments == Integer.parseInt(dataArr[1]) && messageReference.equals(dataArr[2]))) {
                        System.out.println("integrity error with message " + messageReference); 
                    }

                    // save message as object in HashMap
                    CostUnitData messageData = new CostUnitData(ik);
                    messageData.name = name;
                    messageData.addressStreet = addressStreet;
                    messageData.addressPostalCode = addressPostalCode;
                    messageData.addressCity = addressCity;
                    messageData.email = email;
                    costUnitHashMap.put(ik, messageData);
                }

                Matcher fileEndMatcher = fileEndPattern.matcher(data);
                if (fileEndMatcher.find()) {
                    String[] dataArr = data.split(seperator, 0);
                    numMessages = Integer.parseInt(dataArr[1]);
                }
            }
            costUnitReader.close();

            // check if all messages have been read
            if (numMessages != costUnitHashMap.size()) {
                System.out.println("Messages have not been properly read.");
            }
        } catch (FileNotFoundException e) {
            System.out.println("File to parse not found.");
            e.printStackTrace();
        }
    // Handle connection segments
    while (!connectionSegmentsVkgStack.empty()) {
        String data = connectionSegmentsVkgStack.pop();
        String[] dataArr = data.split(seperator, 0);
        if (dataArr.length < 11) { // illegal VGK?
            continue;
        }
        String ik = dataArr[2];
        switch (dataArr[1]) { //connection type
            case "01":
                costUnitHashMap.get(ik).parentIk = dataArr[10];
                break;
            case "02":
            case "03":
            case "09":
                costUnitHashMap.get(ik).dataCollectionIk = dataArr[10];
        }
        costUnitHashMap.get(ik).setIdRegion(dataArr[8]);
        costUnitHashMap.get(ik).setIdState(dataArr[7]);
    }
        
    // Build csv file

    }
}

