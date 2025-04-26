package NMT;

import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.TransportMapping;
import org.snmp4j.CommunityTarget;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.event.ResponseEvent;

import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.uSnmpQueryService_LAtest {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter the IP address of the device to be monitored :\t");
        String ipAddress = input.next();

        SnmpQueryService_LAtest qrSrvc = new SnmpQueryService_LAtest(ipAddress);
        qrSrvc.startService();
    }

    private static String cloudURL = "http://127.0.0.1:8091/NMT/networkDeviceData"; 
    private String ipAddress, community;

    // SNMP OIDs of Metrics
    private static final String SYS_NAME_OID = "1.3.6.1.2.1.1.5.0";
    private static final String SYS_LOCATION_OID = "1.3.6.1.2.1.1.6.0";
    private static final String SYS_OID_OID = "1.3.6.1.2.1.1.2.0";
    private static final String SYS_DESCR_OID = "1.3.6.1.2.1.1.1.0";

    // SNMP OIDs of interface metrics
    private final static String IF_NAME_OID = ".1.3.6.1.2.1.2.2.1.2";
    private final static String IN_TRAFFIC_OID = ".1.3.6.1.2.1.2.2.1.10"; 
    private final static String OUT_TRAFFIC_OID = ".1.3.6.1.2.1.2.2.1.16"; 
    private final static String IN_ERRORS_OID = ".1.3.6.1.2.1.2.2.1.14";   
    private final static String OUT_ERRORS_OID = ".1.3.6.1.2.1.2.2.1.20";  
    private final static String IN_DISCARDS_OID = ".1.3.6.1.2.1.2.2.1.13"; 
    private final static String OUT_DISCARDS_OID = ".1.3.6.1.2.1.2.2.1.19"; 
    private final static String STATUS_OID = ".1.3.6.1.2.1.2.2.1.8";  
    

    public SnmpQueryService_LAtest(String ipAddress) {
        this.ipAddress = ipAddress; 
        this.community = "public"; 
    }

    public void startService() {
        // Setup SNMP communication
        Address targetAddress = new UdpAddress(ipAddress + "/161");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1000);

        TransportMapping transport;
        Snmp snmp;

        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            Runnable task = () -> {
                try {
                    System.out.println("Collecting SNMP data...");
                    // Fetch additional system metadata
                    String sysName = fetchSingleValue(snmp, target, SnmpQueryService_LAtest.SYS_NAME_OID);
                    String sysLocation = fetchSingleValue(snmp, target, SnmpQueryService_LAtest.SYS_LOCATION_OID);
                    String sysObjectID = fetchSingleValue(snmp, target, SnmpQueryService_LAtest.SYS_OID_OID);
                    String sysDescr = fetchSingleValue(snmp, target, SnmpQueryService_LAtest.SYS_DESCR_OID);

                    JSONObject jsonData = new JSONObject();
                    jsonData.put("recordTime", java.time.LocalDateTime.now().toString());
                    jsonData.put("sysName", sysName);
                    jsonData.put("sysLocation", sysLocation);
                    jsonData.put("sysObjectId", sysObjectID);
                    jsonData.put("sysDescr", sysDescr);

                    Map<Integer, String> interfaceNames = fetchTable(snmp, target, SnmpQueryService_LAtest.IF_NAME_OID);
                    Map<Integer, Long> inTraffic = fetchTableAsLong(snmp, target, SnmpQueryService_LAtest.IN_TRAFFIC_OID);
                    Map<Integer, Long> outTraffic = fetchTableAsLong(snmp, target, SnmpQueryService_LAtest.OUT_TRAFFIC_OID);
                    Map<Integer, Long> inErrors = fetchTableAsLong(snmp, target, SnmpQueryService_LAtest.IN_ERRORS_OID);
                    Map<Integer, Long> outErrors = fetchTableAsLong(snmp, target, SnmpQueryService_LAtest.OUT_ERRORS_OID);
                    Map<Integer, Long> inDiscards = fetchTableAsLong(snmp, target, SnmpQueryService_LAtest.IN_DISCARDS_OID);
                    Map<Integer, Long> outDiscards = fetchTableAsLong(snmp, target, SnmpQueryService_LAtest.OUT_DISCARDS_OID);
                    Map<Integer, Integer> statuses = fetchTableAsInt(snmp, target, SnmpQueryService_LAteste.OUT_DISCARDS_OID);
                    Map<Integer, Integer> statuses = fetchTableAsInt(snmp, target, SnmpQueryService.STATUS_OID);

                    // Create interfaces object
                    JSONObject interfaces = new JSONObject();

                    for (Integer index : interfaceNames.keySet()) {
                        JSONObject interfaceData = new JSONObject();
                        String name = interfaceNames.get(index);
                        long inBps = inTraffic.getOrDefault(index, 0L); // Convert bytes to bits
                        long outBps = outTraffic.getOrDefault(index, 0L); // Convert bytes to bits
                        long totalTraffic = inBps + outBps;

                        double errorPercentage = totalTraffic > 0
                                ? (inErrors.getOrDefault(index, 0L) + outErrors.getOrDefault(index, 0L)) * 100.0 / totalTraffic
                                : 0.0;

                        double discardPercentage = totalTraffic > 0
                                ? (inDiscards.getOrDefault(index, 0L) + outDiscards.getOrDefault(index, 0L)) * 100.0 / totalTraffic
                                : 0.0;

                        String status = getStatusDescription(statuses.getOrDefault(index, 2));

                        interfaceData.put("interfaceName", name != null ? name : "Not Available");
                        interfaceData.put("inTraffic(bps)", inBps);
                        interfaceData.put("outTraffic(bps)", outBps);
                        interfaceData.put("errors(%)", errorPercentage);
                        interfaceData.put("discards(%)", discardPercentage);
                        interfaceData.put("operationalStatus", status);

                        interfaces.put(index.toString(), interfaceData);
                    }

                    jsonData.put("interfaces", interfaces);

                    // Send JSON data via POST
                    sendPostRequest(jsonData);

                } catch (IOException e) {
                    System.out.println("Error fetching SNMP data: " + e.getMessage());
                }
            };

            // Schedule task every minute
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            long currentMillis = System.currentTimeMillis();
            long nextMinuteMillis = (currentMillis / 60000 + 1) * 60000;
            long delay = nextMinuteMillis - currentMillis;

            scheduler.scheduleAtFixedRate(task, delay, 60000, TimeUnit.MILLISECONDS);

            // Add a hook to stop the scheduler on application exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                scheduler.shutdown();
            }));

        } catch (IOException e) {
            System.out.println("Error initializing SNMP: " + e.getMessage());
        }
    }

    private static void saveJsonToFile(JSONObject jsonData) {
        try {
            // Specify the file name and path where the data should be saved
            File file = new File("snmp_data.json");
    
            // If file does not exist, create a new one
            if (!file.exists()) {
                file.createNewFile();
            }
    
            // Set up FileWriter and BufferedWriter to write data to the file
            try (FileWriter fileWriter = new FileWriter(file, false);  // 'false' to overwrite the file
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
    
                // Convert JSON data to string and write to the file
                bufferedWriter.write(jsonData.toString(4)); // Indented format with 4 spaces
                System.out.println("Data saved to snmp_data.json successfully.");
            }
        } catch (IOException e) {
            System.out.println("Error saving JSON data to file: " + e.getMessage());
        }
    }


    private static void sendPostRequest(JSONObject jsonData) {
        try {
            // Specify the URL to send the POST request to
            saveJsonToFile(jsonData);
            URL url = new URL(cloudURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send JSON data
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Get the response code
            String responseStatus = connection.getResponseMessage();
            //System.out.println("Data pushed to cloud, status : " + responseStatus);
        } catch (IOException e) {
            System.out.println("Error sending POST request: " + e.getMessage());
        }
    }

    private static String fetchSingleValue(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        OID oidInstance = new OID(oid);
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(oidInstance));
        pdu.setType(PDU.GET);
    
        ResponseEvent responseEvent = snmp.get(pdu, target);
        PDU responsePDU = responseEvent.getResponse();
    
        if (responsePDU != null && responsePDU.getErrorStatus() == PDU.noError) {
            VariableBinding vb = responsePDU.get(0);
            return vb.getVariable().toString();
        }
        return "Error fetching value";
    }

    private static Map<Integer, String> fetchTable(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        Map<Integer, String> table = new HashMap<>();
        OID rootOID = new OID(oid);
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(rootOID));
        pdu.setType(PDU.GETNEXT);

        while (true) {
            ResponseEvent responseEvent = snmp.getNext(pdu, target);
            PDU responsePDU = responseEvent.getResponse();
            if (responsePDU == null || responsePDU.getErrorStatus() != PDU.noError) {
                break;
            }

            VariableBinding vb = responsePDU.get(0);
            if (!vb.getOid().startsWith(rootOID)) {
                break;
            }

            int index = vb.getOid().last();
            table.put(index, vb.getVariable().toString());
            pdu.setRequestID(responsePDU.getRequestID());
            pdu.get(0).setOid(vb.getOid());
        }

        return table;
    }

    private static Map<Integer, Long> fetchTableAsLong(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        Map<Integer, Long> table = new HashMap<>();
        Map<Integer, String> stringTable = fetchTable(snmp, target, oid);
        for (Map.Entry<Integer, String> entry : stringTable.entrySet()) {
            table.put(entry.getKey(), Long.parseLong(entry.getValue()));
        }
        return table;
    }

    private static Map<Integer, Integer> fetchTableAsInt(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        Map<Integer, Integer> table = new HashMap<>();
        Map<Integer, String> stringTable = fetchTable(snmp, target, oid);
        for (Map.Entry<Integer, String> entry : stringTable.entrySet()) {
            table.put(entry.getKey(), Integer.parseInt(entry.getValue()));
        }
        return table;
    }

    private static String getStatusDescription(int status) {
        switch (status) {
            case 1:
                return "Up";
            case 2:
                return "Down";
            default:
                return "Trouble";
        }
    }
}
