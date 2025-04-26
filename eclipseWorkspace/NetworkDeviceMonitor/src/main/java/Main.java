import dataArchiveServices.*;
import networkMonitoringServices.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

    // Map to track SnmpQueryServices by IP address
    private static Map<String, SnmpQueryService> services = new HashMap<>();

    private static String apiUrl = "http://127.0.0.1:8091/NMT/NetworkDevices";

    // Method to fetch IP addresses from the API
    private static List<String> fetchIPAddressesFromAPI() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Check the response code
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) { // HTTP OK
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(jsonContent.toString());
				// Parse the JSON response
                Iterator<String> keys = jsonObject .keys(); // Use keys() method

                while (keys.hasNext()) {
                    String ip = keys.next();
                    String deviceId = jsonObject.optString(ip).trim();
                    if (!deviceId.isEmpty()) { // Add only if IP is valid
                        ipAddresses.add(ip);
                    }
                }
            } else {
                System.out.println("Failed to fetch IP addresses. HTTP Response Code: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error fetching IP addresses from API: " + e.getMessage());
        }
        return ipAddresses;
    }

    // Service that runs continuously
    public static void startService() {
        Set<String> runningIPs = services.keySet();

        while (true) {
            try {
                List<String> ipAddresses = fetchIPAddressesFromAPI();

                // New IP addresses
                List<String> newIPs = new ArrayList<>(ipAddresses);
                newIPs.removeAll(runningIPs);

                // Stale IP addresses
                List<String> staleIPs = new ArrayList<>(runningIPs);
                staleIPs.removeAll(ipAddresses);

                // Stop services for stale IPs
                for (String ip : staleIPs) {
                    System.out.println("Stopping service for IP: " + ip);
                    SnmpQueryService service = services.get(ip);
                    if (service != null) {
                        service.stopService(); // Assuming stopService exists
                        services.remove(ip);
                    }
                }

                // Start services for new IPs
                for (String ip : newIPs) {
                    System.out.println("Starting service for new IP: " + ip);
                    SnmpQueryService service = new SnmpQueryService(ip);
                    service.startService();
                    services.put(ip, service);
                }

                // Sleep for 1 second before next check
                Thread.sleep(1000);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
    }

    public static void main(String[] args) {
        DatabaseCleanupService dbCln = new DatabaseCleanupService();
        dbCln.startService();
        DataArchiveService dtArchvSrvc = new DataArchiveService();
        dtArchvSrvc.startService();
        startService();
    }
}

/*

import dataArchiveServices.*;
import networkMonitoringServices.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Main {

    // Map to track SnmpQueryServices by IP address
    private static Map<String, SnmpQueryService> services = new HashMap<>();

    private static String apiUrl = "http://127.0.0.1:8091/NMT/NetworkDevices";

    // Method to fetch IP addresses from the API
    private static List<String> fetchIPAddressesFromAPI() {
        List<String> ipAddresses = new ArrayList<>();
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Check the response code
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) { // HTTP OK
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder jsonContent = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonContent.append(line);
                }
                reader.close();

                // Parse the JSON response
                JSONObject jsonObject = new JSONObject(jsonContent.toString());
                for (String ip : jsonObject.keySet()) {
                    String deviceId = jsonObject.getString(ip).trim();
                    ipAddresses.add(ip);
                }
            } else {
                System.out.println("Failed to fetch IP addresses. HTTP Response Code: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Error fetching IP addresses from API: " + e.getMessage());
        }
        return ipAddresses;
    }

    // Service that runs continuously
    public static void startService(String filePath) {
        Set<String> runningIPs = services.keySet();

        while (true) {
            try {
                List<String> ipAddresses = fetchIPAddressesFromAPI();

                // New IP addresses
                List<String> newIPs = new ArrayList<>(ipAddresses);
                newIPs.removeAll(runningIPs);

                // Stale IP addresses
                List<String> staleIPs = new ArrayList<>(runningIPs);
                staleIPs.removeAll(ipAddresses);

                // Stop services for stale IPs
                for (String ip : staleIPs) {
                    System.out.println("Stopping service for IP: " + ip);
                    SnmpQueryService service = services.get(ip);
                    if (service != null) {
                        service.stopService(); // Assuming stopService exists
                        services.remove(ip);
                    }
                }

                // Start services for new IPs
                for (String ip : newIPs) {
                    System.out.println("Starting service for new IP: " + ip);
                    SnmpQueryService service = new SnmpQueryService(ip);
                    service.startService();
                    services.put(ip, service);
                }

                // Sleep for 1 second before next check
                Thread.sleep(1000);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                break;
            }
        }
    }

    public static void main(String[] args) {
        String filePath = "C:\\Software\\Eclipse_Workspace\\NetworkDeviceMonitor\\NetworkDevices.json";
        File configFile = new File(filePath);

        if (!configFile.exists()) {
            try {
                // Create the file
                configFile.createNewFile();
                System.out.println("NetworkDevices.json file created successfully.");

                // Write an initial empty JSON object to the file
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write("{}");
                    System.out.println("Initial empty JSON content written to the file.");
                } catch (IOException e) {
                    System.err.println("Error writing initial JSON content: " + e.getMessage());
                }

            } catch (IOException e) {
                System.err.println("Error creating NetworkDevices.json file: " + e.getMessage());
            }
        } else {
            System.out.println("NetworkDevices.json file already exists.");
        }
        DatabaseCleanupService dbCln = new DatabaseCleanupService();
        dbCln.startService();
        DataArchiveService dtArchvSrvc = new DataArchiveService();
        dtArchvSrvc.startService();
        startService(filePath);
    }
}

*/
