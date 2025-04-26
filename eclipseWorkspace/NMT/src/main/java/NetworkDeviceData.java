import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import org.json.JSONObject;
import java.sql.ResultSet;
import java.math.BigDecimal;

@WebServlet("/networkDeviceData")
public class NetworkDeviceData extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DBHandler dbHandler;

    public NetworkDeviceData() {
        dbHandler = new DBHandler();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder requestBody = new StringBuilder();
        String line;
        PrintWriter out = response.getWriter();
        JSONObject responseJson = new JSONObject(); // JSON object for the response message

        try {
            // Read the request body
            while ((line = request.getReader().readLine()) != null) {
                requestBody.append(line);
            }

            // Parse request body into JSON
            JSONObject jsonData = new JSONObject(requestBody.toString());
            String ipAddress = jsonData.optString("ipAddress");
            String sysName = jsonData.optString("sysName");
            String sysLocation = jsonData.optString("sysLocation");
            String sysObjectId = jsonData.optString("sysObjectId");
            String sysDescr = jsonData.optString("sysDescr");
            String recordTime = jsonData.optString("recordTime");
            
            // System.out.println(sysName+","+sysLocation+","+sysObjectId+","+sysDescr+","+recordTime);
            // Check if the device already exists
            int deviceId = dbHandler.isDeviceRecorded(ipAddress);
            if (deviceId == -1) {
                // Insert the device if not recorded
                deviceId = dbHandler.insertNetworkDevice(ipAddress,sysName, sysLocation, sysObjectId, sysDescr);
                if (deviceId > 0) {
                    responseJson.put("deviceMessage", "New device recorded with ID: " + deviceId);
                } else {
                    responseJson.put("deviceMessage", "Failed to record new device.");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.println(responseJson.toString());
                    return;
                }
            } else {
                responseJson.put("deviceMessage", "Device already recorded with ID: " + deviceId);
            }

            // Process network device interfaces
            JSONObject interfaces = jsonData.getJSONObject("interfaces");
            for (Object key : interfaces.keySet()) {
                JSONObject interfaceData = interfaces.getJSONObject(key.toString());
                int interfaceIndex = Integer.parseInt(key.toString());
                String interfaceName = interfaceData.optString("interfaceName");
                long inTraffic = interfaceData.optLong("inTraffic(bps)");
                long outTraffic = interfaceData.optLong("outTraffic(bps)");
                BigDecimal discards = new BigDecimal(interfaceData.optDouble("discards(%)"));
                BigDecimal errors = new BigDecimal(interfaceData.optDouble("errors(%)"));
                String operationalStatus = interfaceData.optString("operationalStatus");

                dbHandler.insertNetworkDeviceInterface(deviceId, interfaceIndex, interfaceName, inTraffic, outTraffic, discards, errors, operationalStatus, recordTime);
            }

            // Set successful response
            responseJson.put("status", "success");
            responseJson.put("message", "Data pushed successfully.");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
            // Set error response
            responseJson.put("status", "error");
            responseJson.put("message", "Data push failure: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // Write response as JSON
        out.println(responseJson.toString());
    }
}
