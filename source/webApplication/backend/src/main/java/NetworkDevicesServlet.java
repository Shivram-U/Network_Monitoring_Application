import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/NetworkDevices")
public class NetworkDevicesServlet extends HttpServlet {
	private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/nmt";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "";

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String sql = "SELECT ipAddress, deviceId FROM monitoredIPAddresses";
        
        try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
             PreparedStatement preparedStatement = mysqlConnection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            response.setContentType("application/json");
            PrintWriter out = response.getWriter();

            JSONObject jsonResponse = new JSONObject();

            while (resultSet.next()) {
                String ipAddress = resultSet.getString("ipAddress");
                String deviceId = String.valueOf(resultSet.getString("deviceId"));
                jsonResponse.put(ipAddress, deviceId);
            }
            
            out.print(jsonResponse.toString());

        } catch (SQLException ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", "*"); // Allows all origins
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); // HTTP methods allowed
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization"); // Custom headers allowed
        response.setHeader("Access-Control-Allow-Credentials", "true"); // Allows sending credentials

        String ipAddress = request.getParameter("ipAddress");
        int deviceID = -1;

        try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)) {
            // Check if the deviceID exists
            String checkDeviceSQL = "SELECT deviceID FROM networkdevices WHERE ipAddress = ?";
            try (PreparedStatement preparedStatement = mysqlConnection.prepareStatement(checkDeviceSQL)) {
                preparedStatement.setString(1, ipAddress);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    String deviceIDStr = rs.getString("deviceID");
                    if (!deviceIDStr.equals("null")) {
                        deviceID = Integer.parseInt(deviceIDStr);
                    }
                }
            }

            // If deviceID is still -1, get the max deviceID and increment it
            if (deviceID == -1) {
            	int devID1 = -1,devID2 = -1;
                String maxDeviceIDSQL = "SELECT COALESCE(MAX(deviceID), 0) AS maxDeviceID FROM networkdevices";
                try (PreparedStatement preparedStatement = mysqlConnection.prepareStatement(maxDeviceIDSQL)) {
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        devID1 = rs.getInt("maxDeviceID") + 1;
                    }
                }
                maxDeviceIDSQL = "SELECT COALESCE(MAX(deviceID), 0) AS maxDeviceID FROM monitoredIPAddresses";
                try (PreparedStatement preparedStatement = mysqlConnection.prepareStatement(maxDeviceIDSQL)) {
                    ResultSet rs = preparedStatement.executeQuery();
                    if (rs.next()) {
                        devID2 = rs.getInt("maxDeviceID") + 1;
                    }
                }
                deviceID = ((devID1>=devID2)?devID1:devID2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Construct the SQL statement
        String sql = "INSERT INTO monitoredIPAddresses VALUES (?,?)";

        try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
             PreparedStatement preparedStatement = mysqlConnection.prepareStatement(sql)) {

            preparedStatement.setString(1, ipAddress);
            preparedStatement.setInt(2, deviceID);

            int rowsInserted = preparedStatement.executeUpdate();

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();

            if (rowsInserted > 0) {
                out.println("<h3>IP Address added successfully!</h3>");
            } else {
                out.println("<h3>Failed to add IP Address.</h3>");
            }
        } catch (SQLIntegrityConstraintViolationException ex) {
            // Handle duplicate primary key violation (duplicate IP Address)
            response.setStatus(HttpServletResponse.SC_CONFLICT);
            response.getWriter().println("<h3>Network Device with the provided IP Address is already being monitored.</h3>");
        } catch (SQLException ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }

    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String ipAddress = request.getParameter("ipAddress");

    	String sql = "DELETE FROM suspendedInterfaces WHERE ipAddress = ?";

    	PreparedStatement preparedStatement;
        try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
                ) {
        	preparedStatement = mysqlConnection.prepareStatement(sql);
            preparedStatement.setString(1, ipAddress);
            int rowsDeleted = preparedStatement.executeUpdate();

            response.setContentType("text/html");
            PrintWriter out = response.getWriter();
        	 preparedStatement.close();
    	 	 sql = "DELETE FROM monitoredIPAddresses WHERE ipAddress = ?";

             preparedStatement = mysqlConnection.prepareStatement(sql);

             preparedStatement.setString(1, ipAddress);
             rowsDeleted = preparedStatement.executeUpdate();

             response.setContentType("text/html");
             if (rowsDeleted > 0) {
            	 preparedStatement.close();
                 out.println("<h3>IP Address deleted successfully!</h3>");
             } else {
                 out.println("<h3>No matching IP Address found for deletion.</h3>");
             }

        } catch (SQLException ex) {
            ex.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database error");
        }
    }
}
