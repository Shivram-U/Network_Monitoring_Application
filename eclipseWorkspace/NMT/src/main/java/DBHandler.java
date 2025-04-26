import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.math.BigDecimal;

public class DBHandler {
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/nmt";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    public DBHandler() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public int insertNetworkDevice(String ipAddress,String sysName, String sysLocation, String sysObjectId, String sysDescr) {
        int deviceID = -1;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String insertDeviceSQL = "INSERT INTO networkDevices (sysName, sysLocation, sysObjectId, sysDescr, ipAddress) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertDeviceSQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, sysName);
                preparedStatement.setString(2, sysLocation);
                preparedStatement.setString(3, sysObjectId);
                preparedStatement.setString(4, sysDescr);
                preparedStatement.setString(5, ipAddress);
                preparedStatement.executeUpdate();

                ResultSet rs = preparedStatement.getGeneratedKeys();
                if (rs.next()) {
                    deviceID = rs.getInt(1);
                }
                String updateMonitoredIPDataSQL = "update monitoredIPAddresses set deviceId = ? WHERE ipAddress = ?";
                try (PreparedStatement updateStatement = connection.prepareStatement(updateMonitoredIPDataSQL)) {
                	updateStatement.setInt(1, deviceID);
                	updateStatement.setString(2, ipAddress);

                    updateStatement.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceID;
    }

    public void insertNetworkDeviceInterface(int deviceID, int interfaceIndex, String interfaceName, long inTraffic, long outTraffic, BigDecimal discards, BigDecimal errors, String operationalStatus, String recordTime) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Check if the network interface data already exists
            String checkInterfaceDataSQL = "SELECT * FROM networkdeviceinterfacedata WHERE deviceId = ? AND interfaceIndex = ?";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkInterfaceDataSQL)) {
                checkStatement.setInt(1, deviceID);
                checkStatement.setInt(2, interfaceIndex);

                ResultSet rs = checkStatement.executeQuery();
                if (!rs.next()) {
                    // Insert the new network interface data if not present
                    String insertInterfaceDataSQL = "INSERT INTO networkdeviceinterfacedata (deviceId, interfaceIndex, interfaceName) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertInterfaceDataSQL)) {
                        insertStatement.setInt(1, deviceID);
                        insertStatement.setInt(2, interfaceIndex);
                        insertStatement.setString(3, interfaceName);
                        insertStatement.executeUpdate();
                    }
                }
            }

            // Insert into networkDeviceInterfaces table
            String insertInterfaceSQL = "INSERT INTO networkDeviceInterfaces VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertInterfaceSQL)) {
                preparedStatement.setInt(1, deviceID);
                preparedStatement.setInt(2, interfaceIndex);
                preparedStatement.setLong(3, inTraffic);
                preparedStatement.setLong(4, outTraffic);
                preparedStatement.setBigDecimal(5, discards);
                preparedStatement.setBigDecimal(6, errors);
                preparedStatement.setString(7, operationalStatus);
                preparedStatement.setString(8, recordTime);
                preparedStatement.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int isDeviceRecorded(String ipAddress) {
    	// System.out.println("Device record check:"+sysName+","+sysLocation );
        int deviceID = -1;
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            String checkDeviceSQL = "SELECT deviceID FROM networkdevices WHERE ipAddress = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(checkDeviceSQL)) {
                preparedStatement.setString(1, ipAddress);
                ResultSet rs = preparedStatement.executeQuery();
                if (rs.next()) {
                    String deviceIDStr = rs.getString("deviceID");
                    if(!deviceIDStr.equals("null"))
                    	deviceID = Integer.parseInt(deviceIDStr);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return deviceID;
    }
}
