package networkMonitoringServices;

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
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class SnmpQueryService {
    private static String cloudURL = "http://127.0.0.1:8091/NMT/networkDeviceData"; 
    private String ipAddress, community;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService suspendedIndicesScheduler;
    private Set<Integer> suspendedIndices;

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
    private Boolean firstPoll = true;
    private Map<Integer,Long> prevInTraffic, prevOutTraffic, prevTotalErrors = new HashMap<Integer,Long>(), prevTotalDiscards = new HashMap<Integer,Long>();

    
    public SnmpQueryService(String ipAddress) {
        this.ipAddress = ipAddress; 
        this.community = "public"; 
        this.suspendedIndices = new HashSet<>();
        initializeSuspendedIndicesScheduler();
    }
    
    public SnmpQueryService()
    {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter the IP address of the device to be monitored :\t");
        this.ipAddress = input.next();
        this.community = "public"; 
        this.suspendedIndices = new HashSet<>();
        initializeSuspendedIndicesScheduler();
    }
    
    private void initializeSuspendedIndicesScheduler() {
        this.suspendedIndicesScheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = this::fetchSuspendedIndicesFromDatabase;
        suspendedIndicesScheduler.scheduleAtFixedRate(task, 0, 50, TimeUnit.MILLISECONDS); // Adjust as necessary
    }
    
    private void fetchSuspendedIndicesFromDatabase() {
        try {
            // Assuming a method to fetch suspended indices from database
            Set<Integer> newSuspendedIndices = fetchSuspendedIndices();
            synchronized (this.suspendedIndices) {
                this.suspendedIndices = newSuspendedIndices;
            }
            // System.out.println("Suspended indices updated: " + newSuspendedIndices);
        } catch (Exception e) {
            System.out.println("Error fetching suspended indices: " + e.getMessage());
        }
    }
    
    private Set<Integer> fetchSuspendedIndices() {
        // Assuming an HTTP GET method to fetch suspended indices
        try {
            URL url = new URL("http://localhost:8091/NetworkDeviceMonitoringApplication/getSuspendedInterfacesDataByIp?ipAddress=" + this.ipAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }


            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray interfaceIndices = jsonResponse.getJSONArray("InterfaceIndices");
            Set<Integer> indicesSet = new HashSet<>();
            for (int i = 0; i < interfaceIndices.length(); i++) {
                indicesSet.add(interfaceIndices.getInt(i));
            }
            return indicesSet;
        } catch (Exception e) {
            System.out.println("Error fetching suspended indices from API: " + e.getMessage());
            return new HashSet<>();
        }
    }
    
    private static boolean checkDeviceReachability(CommunityTarget target) {
        TransportMapping transport;
        Snmp snmp;

        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID("1.3.6.1.2.1.1.1.0"))); // SNMP System OID

            ResponseEvent response = snmp.get(pdu, target);
            PDU responsePDU = response.getResponse();

            if (responsePDU != null && responsePDU.getErrorStatus() == PDU.noError) {
                return true; // Device is reachable
            } else {
                return false; // Device is not reachable
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false; // Device is not reachable
        }
    }

    public void startService() throws JSONException {
    	System.out.println("Monitoring of the device with IP "+ ipAddress+ " is started");
        // Setup SNMP communication
        Address targetAddress = new UdpAddress(ipAddress + "/161");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1000);
        
        boolean isReachable = checkDeviceReachability(target);

        if (isReachable) {
            System.out.println("Device is reachable via SNMP.");
        } else {
            System.out.println("Device is NOT reachable via SNMP.");
            return;
        }

        TransportMapping transport;
        Snmp snmp;

        try {
            transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();

            Runnable task = () -> {
                try {
                	System.out.println("Collecting network device data...");
                	/*
                	if(firstPoll)
                    {
                    	prevInTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.IN_TRAFFIC_OID);
                    	prevOutTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_TRAFFIC_OID);
                    	Map<Integer, Long> inErrors = fetchTableAsLong(snmp, target, SnmpQueryService.IN_ERRORS_OID);
                        Map<Integer, Long> outErrors = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_ERRORS_OID);
                        Map<Integer, Long> inDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.IN_DISCARDS_OID);
                        Map<Integer, Long> outDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_DISCARDS_OID);
                    	for (Integer index : prevOutTraffic.keySet()) {
                    		prevTotalErrors.put(index,(inErrors.getOrDefault(index, 0L) + outErrors.getOrDefault(index, 0L)));
                    		prevTotalDiscards.put(index,(inDiscards.getOrDefault(index, 0L) + outDiscards.getOrDefault(index, 0L)));
                    	}
                    	System.out.println("Reference Network device data collected");
                    	firstPoll = false;
                    }
                	else
                	{*/
	                    // Fetch additional system metadata
	                    String sysName = fetchSingleValue(snmp, target, SnmpQueryService.SYS_NAME_OID);
	                    String sysLocation = fetchSingleValue(snmp, target, SnmpQueryService.SYS_LOCATION_OID);
	                    String sysObjectID = fetchSingleValue(snmp, target, SnmpQueryService.SYS_OID_OID);
	                    String sysDescr = fetchSingleValue(snmp, target, SnmpQueryService.SYS_DESCR_OID);
	
	                    JSONObject jsonData = new JSONObject();
	                    jsonData.put("recordTime", java.time.LocalDateTime.now().toString());
	                    jsonData.put("ipAddress", this.ipAddress);
	                    jsonData.put("sysName", sysName);
	                    jsonData.put("sysLocation", sysLocation);
	                    jsonData.put("sysObjectId", sysObjectID);
	                    jsonData.put("sysDescr", sysDescr);
	                  
	                    Map<Integer, String> interfaceNames = fetchTable(snmp, target, SnmpQueryService.IF_NAME_OID);
	                    Map<Integer, Long> inTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.IN_TRAFFIC_OID);
	                    Map<Integer, Long> outTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_TRAFFIC_OID);
	                    Map<Integer, Long> inErrors = fetchTableAsLong(snmp, target, SnmpQueryService.IN_ERRORS_OID);
	                    Map<Integer, Long> outErrors = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_ERRORS_OID);
	                    Map<Integer, Long> inDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.IN_DISCARDS_OID);
	                    Map<Integer, Long> outDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_DISCARDS_OID);
	                    Map<Integer, Integer> statuses = fetchTableAsInt(snmp, target, SnmpQueryService.STATUS_OID);
	
	                    // Create interfaces object
	                    JSONObject interfaces = new JSONObject();
	
	                    synchronized (this.suspendedIndices) {
		                    for (Integer index : interfaceNames.keySet()) {
		                    	// System.out.println(this.suspendedIndices);
		                    	if (!suspendedIndices.contains(index)) {
			                        JSONObject interfaceData = new JSONObject();
			                        String name = interfaceNames.get(index);
			                        // System.out.println(inTraffic.getOrDefault(index, 0L)+","+prevInTraffic.getOrDefault(index,0L));
			                        // System.out.println(inTraffic.getOrDefault(index, 0L)-prevInTraffic.getOrDefault(index,0L));
			                        // long inBps = ((inTraffic.getOrDefault(index, 0L)-prevInTraffic.getOrDefault(index,0L))*8)/60L;; // Convert bytes to bits
			                        long inBps = inTraffic.getOrDefault(index, 0L); // Convert bytes to bits
			                        // System.out.println(outTraffic.getOrDefault(index, 0L)+","+prevOutTraffic.getOrDefault(index,0L));
			                        // System.out.println(outTraffic.getOrDefault(index, 0L)-prevOutTraffic.getOrDefault(index,0L));
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
		                    }
	                    /*}*/
	
	                    jsonData.put("interfaces", interfaces);
	
	                    // Send JSON data via POST
	                    System.out.println("Network device data collected");
	                    sendPostRequest(jsonData);
                	}
                } catch (Exception e) {
                    System.out.println("Error fetching SNMP data: " + e.getMessage());
                }
            };

            // Schedule task every minute
            this.scheduler = Executors.newScheduledThreadPool(1);
            long currentMillis = System.currentTimeMillis();
            long nextMinuteMillis = (currentMillis / 60000 + 1) * 60000;
            long delay = nextMinuteMillis - currentMillis;

            scheduler.scheduleAtFixedRate(task, delay, 60000, TimeUnit.MILLISECONDS);

            // Add a hook to stop the scheduler on application exit
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                scheduler.shutdown();
            }));

        } catch (Exception e) {
            System.out.println("Error initializing SNMP: " + e.getMessage());
        }
    }

    private static void saveJsonToFile(JSONObject jsonData) throws JSONException {
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


    private static void sendPostRequest(JSONObject jsonData) throws JSONException {
        try {
            // Specify the URL to send the POST request to
            // saveJsonToFile(jsonData);
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

            // Read the response body
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Print the response code and message
            System.out.println("HTTP Response Code: " + responseCode);
            System.out.println("Response Body: " + response.toString());
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

    public void stopService() {
    	//System.out.println(scheduler);
    	//System.out.println(scheduler.isShutdown());
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("Monitoring service of the device with IP "+ipAddress+" has been stopped.");
        }
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
/*
package networkMonitoringServices;

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
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class SnmpQueryService {
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
    
    private Boolean firstPoll = true;
    private Map<Integer,Long> prevInTraffic, prevOutTraffic, prevTotalErrors = new HashMap<Integer,Long>(), prevTotalDiscards = new HashMap<Integer,Long>();

    public SnmpQueryService(String ipAddress) {
        this.ipAddress = ipAddress; 
        this.community = "public"; 
    }
    
    public SnmpQueryService()
    {
        Scanner input = new Scanner(System.in);
        System.out.print("Enter the IP address of the device to be monitored :\t");
        this.ipAddress = input.next();
        this.community = "public"; 
    }

    public void startService() throws JSONException {
    	System.out.println("Network Device Monitoring Service Started");
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
                	System.out.println("Collecting network device data...");     
                    if(firstPoll)
                    {
                    	prevInTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.IN_TRAFFIC_OID);
                    	prevOutTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_TRAFFIC_OID);
                    	Map<Integer, Long> inErrors = fetchTableAsLong(snmp, target, SnmpQueryService.IN_ERRORS_OID);
                        Map<Integer, Long> outErrors = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_ERRORS_OID);
                        Map<Integer, Long> inDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.IN_DISCARDS_OID);
                        Map<Integer, Long> outDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_DISCARDS_OID);
                    	for (Integer index : prevOutTraffic.keySet()) {
                    		prevTotalErrors.put(index,(inErrors.getOrDefault(index, 0L) + outErrors.getOrDefault(index, 0L)));
                    		prevTotalDiscards.put(index,(inDiscards.getOrDefault(index, 0L) + outDiscards.getOrDefault(index, 0L)));
                    	}
                    	System.out.println("Reference Network device data collected");
                    	firstPoll = false;
                    }
                    else {
                	// Fetch additional system metadata
                    String sysName = fetchSingleValue(snmp, target, SnmpQueryService.SYS_NAME_OID);
                    String sysLocation = fetchSingleValue(snmp, target, SnmpQueryService.SYS_LOCATION_OID);
                    String sysObjectID = fetchSingleValue(snmp, target, SnmpQueryService.SYS_OID_OID);
                    String sysDescr = fetchSingleValue(snmp, target, SnmpQueryService.SYS_DESCR_OID);
                	JSONObject jsonData = new JSONObject();
                    jsonData.put("recordTime", java.time.LocalDateTime.now().toString());
                    jsonData.put("sysName", sysName);
                    jsonData.put("sysLocation", sysLocation);
                    jsonData.put("sysObjectId", sysObjectID);
                    jsonData.put("sysDescr", sysDescr);
                    Map<Integer, String> interfaceNames = fetchTable(snmp, target, SnmpQueryService.IF_NAME_OID);
                    Map<Integer, Long> inTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.IN_TRAFFIC_OID);
                    Map<Integer, Long> outTraffic = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_TRAFFIC_OID);
                    Map<Integer, Long> inErrors = fetchTableAsLong(snmp, target, SnmpQueryService.IN_ERRORS_OID);
                    Map<Integer, Long> outErrors = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_ERRORS_OID);
                    Map<Integer, Long> inDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.IN_DISCARDS_OID);
                    Map<Integer, Long> outDiscards = fetchTableAsLong(snmp, target, SnmpQueryService.OUT_DISCARDS_OID);
                    Map<Integer, Integer> statuses = fetchTableAsInt(snmp, target, SnmpQueryService.STATUS_OID);

                    // Create interfaces object
                    JSONObject interfaces = new JSONObject();

                    for (Integer index : interfaceNames.keySet()) {
                        JSONObject interfaceData = new JSONObject();
                        String name = interfaceNames.get(index);
                        long inBps = (inTraffic.getOrDefault(index, 0L)-prevInTraffic.getOrDefault(index,0L))/60L; // Convert bytes to bits
                        long outBps = (outTraffic.getOrDefault(index, 0L)-prevOutTraffic.getOrDefault(index,0L))/60L; // Convert bytes to bits
                        long totalTraffic = inBps + outBps;

                        double errorPercentage = totalTraffic > 0
                                ? ((inErrors.getOrDefault(index, 0L) + outErrors.getOrDefault(index, 0L))-prevTotalErrors.getOrDefault(index,0L)) * 100.0 / totalTraffic
                                : 0.0;

                        double discardPercentage = totalTraffic > 0
                                ? ((inDiscards.getOrDefault(index, 0L) + outDiscards.getOrDefault(index, 0L))-prevTotalDiscards.getOrDefault(index,0L)) * 100.0 / totalTraffic
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
                    System.out.println("Network device data collected");
                    sendPostRequest(jsonData);
                  }
                } catch (Exception e) {
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

        } catch (Exception e) {
            System.out.println("Error initializing SNMP: " + e.getMessage());
        }
    }

    private static void saveJsonToFile(JSONObject jsonData) throws JSONException {
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


    private static void sendPostRequest(JSONObject jsonData) throws JSONException {
        try {
            // Specify the URL to send the POST request to
            // saveJsonToFile(jsonData);
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

            // Read the response body
            int responseCode = connection.getResponseCode();
            StringBuilder response = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }

            // Print the response code and message
            System.out.println("HTTP Response Code: " + responseCode);
            System.out.println("Response Body: " + response.toString());
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
*/