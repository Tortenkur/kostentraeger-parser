import java.io.File;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;

class CostUnitData {
    private String ik;
    String name;
    String addressStreet;
    String addressPostalCode;
    String addressCity;
    String email;
    String parentIk;
    private ArrayList<String[]> dataCollectionVkgs = new ArrayList<String[]>();
    
    // constructor
    public CostUnitData(String ikNr) {
        this.ik = ikNr;
    }

    // getter
    public String getIk() {
        return ik;
    }
    public ArrayList<String[]> getDataCollectionVkgs() {
        return dataCollectionVkgs;
    }

    // setter
    public void addDataCollectionVkg(String idState, String idRegion, String dataCollectionIk) {
        String[] arrayToAdd = {idState, idRegion, dataCollectionIk};
        dataCollectionVkgs.add(arrayToAdd);
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
                    String parentIk = "";
                    Stack<String[]> dataCollectionVkgStack = new Stack<String[]>();
                                    
                    // Read message segments
                    do {
                        data = costUnitReader.nextLine();
                        numSegments ++;
                        data = checkSegment(data, segmentTerminator);
                        dataArr = data.split(seperator, 0);
                        
                        // Unterscheidung Segment-Kennung
                        switch(dataArr[0]) {
                            case "IDK":
                                ik = dataArr[1];
                                break;
                            case "VKG":
                                // Unterscheidung Art der Verknuepfung
                                switch(dataArr[1]) {
                                    case "01":
                                        parentIk = dataArr[2];
                                        break;
                                    case "02":
                                    case "03":
                                        if (dataArr[5].equals("07")) {
                                            dataCollectionVkgStack.push(dataArr);
                                        } else {
                                            System.out.println("Ungueltige Verknuepfung: " + data);
                                        }
                                        break;
                                    case "09":
                                        dataCollectionVkgStack.push(dataArr);
                                        break;
                                }
                                break;
                            case "NAM":
                                String[] arrayNameToAdd = Arrays.copyOfRange(dataArr, 2, dataArr.length);
                                name = String.join(" ", arrayNameToAdd);
                                break;
                            case "ANS":
                                // Nur Beruecksichtigung von Hausanschrift
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
                    messageData.parentIk = parentIk;
                    while (!dataCollectionVkgStack.empty()) {
                        String[] vkgSegment = dataCollectionVkgStack.pop();
                        messageData.addDataCollectionVkg(vkgSegment[7], vkgSegment[8], vkgSegment[2]);
                    }
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
        
    // Build csv file
    File outputFile = new File("output.csv");
    if (!outputFile.exists()) {
        try {
            outputFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    try {
        FileWriter outputWriter = new FileWriter("output.csv");
        for (String i : costUnitHashMap.keySet()) {
            CostUnitData outputObj = new CostUnitData(i);
            outputObj = costUnitHashMap.get(i);
            ArrayList<String[]> outputVkgs = outputObj.getDataCollectionVkgs();
            for (String[] vkg : outputVkgs) {
                String[] outputArr = {outputObj.getIk(),
                                    outputObj.name,
                                    outputObj.addressStreet,
                                    outputObj.addressPostalCode,
                                    outputObj.addressCity,
                                    vkg[1],
                                    vkg[0],
                                    outputObj.email,
                                    outputObj.parentIk,
                                    vkg[2]
                                    };
                String output = String.join(";", outputArr).concat("\n");
                outputWriter.write(output);
            }
        }
        outputWriter.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
    }
}

