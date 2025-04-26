package references;


import com.datastax.oss.driver.api.core.CqlSession;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataArchiveTest_2 {
	
	static long count=0;
    public static void main(String[] args) {
        try {
            analyzeAndArchive();
        } catch (Exception e) {
            System.out.println("Error during analysis and archiving: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void analyzeAndArchive() throws SQLException {
        String mysqlUrl = "jdbc:mysql://localhost:3306/nmt";
        String mysqlUser = "root";
        String mysqlPassword = "";

        try (Connection mysqlConnection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)) {
            try (CqlSession cassandraSession = CqlSession.builder().build()) {

                LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
                LocalDateTime startOfCurrentHour = now.withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endOfCurrentHour = startOfCurrentHour.plusHours(1);

                Timestamp startTimestamp = Timestamp.valueOf(startOfCurrentHour);
                Timestamp endTimestamp = Timestamp.valueOf(endOfCurrentHour);

                String query = "SELECT deviceId, interfaceIndex, COUNT(*) AS count, " +
                        "MIN(`inTraffic(bps)`) AS minInTraffic, MAX(`inTraffic(bps)`) AS maxInTraffic, " +
                        "SUM(`inTraffic(bps)`) AS sumInTraffic," +
                        "MIN(`outTraffic(bps)`) AS minOutTraffic, MAX(`outTraffic(bps)`) AS maxOutTraffic, " +
                        "SUM(`outTraffic(bps)`) AS sumOutTraffic," +
                        "MIN(`discards(%)`) AS minDiscards, MAX(`discards(%)`) AS maxDiscards, " +
                        "SUM(`discards(%)`) AS sumDiscards," +
                        "MIN(`errors(%)`) AS minErrors, MAX(`errors(%)`) AS maxErrors, " +
                        "SUM(`errors(%)`) AS sumErrors " +
                        "FROM networkDeviceInterfaces " +
                        "WHERE recordTime >= ? AND recordTime < ? " +
                        "GROUP BY deviceId, interfaceIndex";

                PreparedStatement statement = mysqlConnection.prepareStatement(query);
                statement.setTimestamp(1, startTimestamp);
                statement.setTimestamp(2, endTimestamp);

                ResultSet resultSet = statement.executeQuery();
                Map<String, DeviceData> aggregatedData = new HashMap<>();

                while (resultSet.next()) {
                    String deviceId = resultSet.getString("deviceId");
                    int interfaceIndex = resultSet.getInt("interfaceIndex");
                    count = resultSet.getLong("count");
                    
                    DeviceData deviceData = aggregatedData.computeIfAbsent(deviceId, k -> new DeviceData());
                    Metrics inTrafficMetrics = new Metrics(
                        resultSet.getDouble("minInTraffic"),
                        resultSet.getDouble("maxInTraffic"),
                        count > 0
                            ? resultSet.getDouble("sumInTraffic") / count
                            : 0,
                        resultSet.getDouble("sumInTraffic"),
                        count
                    );
                    
                    Metrics outTrafficMetrics = new Metrics(
                        resultSet.getDouble("minOutTraffic"),
                        resultSet.getDouble("maxOutTraffic"),
                        count > 0
                            ? resultSet.getDouble("sumOutTraffic") / count
                            : 0,
                        resultSet.getDouble("sumOutTraffic"),
                        count
                    );

                    Metrics discardsMetrics = new Metrics(
                        resultSet.getDouble("minDiscards"),
                        resultSet.getDouble("maxDiscards"),
                        count > 0
                            ? resultSet.getDouble("sumDiscards") / count
                            : 0,
                        resultSet.getDouble("sumDiscards"),
                        count
                    );
                    
                    Metrics errorsMetrics = new Metrics(
                        resultSet.getDouble("minErrors"),
                        resultSet.getDouble("maxErrors"),
                        count > 0
                            ? resultSet.getDouble("sumErrors") / count
                            : 0,
                        resultSet.getDouble("sumErrors"),
                        count
                    );

                    InterfaceData interfaceData = new InterfaceData(
                        inTrafficMetrics,
                        outTrafficMetrics,
                        discardsMetrics,
                        errorsMetrics
                    );

                    deviceData.interfaces.put(interfaceIndex, interfaceData);
                }

                if (!aggregatedData.isEmpty()) {
                    String keyspace = "nmtarchive";
                    String tableName = "networkInterfaceMetricsArchive";
                    String fullyQualifiedTableName = keyspace + "." + tableName;

                    String insertQuery = String.format(
                    	    "INSERT INTO %s (deviceId, interfaceIndex, recordTime, count, maxInTraffic_bps, minInTraffic_bps, sumInTraffic_bps, avgInTraffic_bps, " +
                    	    "maxOutTraffic_bps, minOutTraffic_bps, sumOutTraffic_bps, avgOutTraffic_bps, maxDiscards_percent, minDiscards_percent, " +
                    	    "sumDiscards_percent, avgDiscards_percent, maxErrors_percent, minErrors_percent, sumErrors_percent, avgErrors_percent) " +
                    	    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    	    keyspace+"."+tableName
                    	);


                    for (Map.Entry<String, DeviceData> entry : aggregatedData.entrySet()) {
                        DeviceData deviceData = entry.getValue();
                        for (Map.Entry<Integer, InterfaceData> interfaceEntry : deviceData.interfaces.entrySet()) {
                            InterfaceData interfaceData = interfaceEntry.getValue();

                            System.out.println("INSERT VALUES: " + Arrays.toString(new Object[]{
                            		
                            	    Integer.parseInt(entry.getKey()), // deviceId
                            	    interfaceEntry.getKey(),          // interfaceIndex
                            	    startOfCurrentHour.toInstant(ZoneOffset.UTC),  // recordTime
                            	    count,                            // count
                            	    interfaceData.inTraffic.min,      // minInTraffic_bps
                            	    interfaceData.inTraffic.max,      // maxInTraffic_bps
                            	    interfaceData.inTraffic.avg,      // avgInTraffic_bps
                            	    interfaceData.inTraffic.sum,      // sumInTraffic_bps
                            	    interfaceData.outTraffic.min,     // minOutTraffic_bps
                            	    interfaceData.outTraffic.max,     // maxOutTraffic_bps
                            	    interfaceData.outTraffic.avg,     // avgOutTraffic_bps
                            	    interfaceData.outTraffic.sum,     // sumOutTraffic_bps
                            	    interfaceData.discards.min,       // minDiscards_percent
                            	    interfaceData.discards.max,       // maxDiscards_percent
                            	    interfaceData.discards.avg,       // avgDiscards_percent
                            	    interfaceData.discards.sum,       // sumDiscards_percent
                            	    interfaceData.errors.min,         // minErrors_percent
                            	    interfaceData.errors.max,         // maxErrors_percent
                            	    interfaceData.errors.avg,         // avgErrors_percent
                            	    interfaceData.errors.sum          // sumErrors_percent
                            	}));
                            
                            cassandraSession.execute(insertQuery,
                            	    Integer.parseInt(entry.getKey()),               // deviceId
                            	    interfaceEntry.getKey(),       // interfaceIndex
                            	    startOfCurrentHour.toInstant(ZoneOffset.UTC),  // recordTime
                            	    count,                        // count
                            	    interfaceData.inTraffic.min,  // minInTraffic_bps
                            	    interfaceData.inTraffic.max,  // maxInTraffic_bps
                            	    interfaceData.inTraffic.avg,  // avgInTraffic_bps
                            	    interfaceData.inTraffic.sum,  // sumInTraffic_bps
                            	    interfaceData.outTraffic.min, // minOutTraffic_bps
                            	    interfaceData.outTraffic.max, // maxOutTraffic_bps
                            	    interfaceData.outTraffic.avg, // avgOutTraffic_bps
                            	    interfaceData.outTraffic.sum, // sumOutTraffic_bps
                            	    interfaceData.discards.min,   // minDiscards_percent
                            	    interfaceData.discards.max,   // maxDiscards_percent
                            	    interfaceData.discards.avg,   // avgDiscards_percent
                            	    interfaceData.discards.sum,   // sumDiscards_percent
                            	    interfaceData.errors.min,     // minErrors_percent
                            	    interfaceData.errors.max,     // maxErrors_percent
                            	    interfaceData.errors.avg,     // avgErrors_percent
                            	    interfaceData.errors.sum      // sumErrors_percent
                            	);
                            

                        }
                    }

                    System.out.println("Data successfully archived for " + startOfCurrentHour + " to " + endOfCurrentHour);
                } else {
                    System.out.println("No data available for " + startOfCurrentHour + " to " + endOfCurrentHour);
                }
            }
        }
    }
}
