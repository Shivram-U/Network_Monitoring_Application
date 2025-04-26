package NMT;

import com.datastax.oss.driver.api.core.CqlSession;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class DeviceData {
    private String deviceId;
    private int interfaceIndex;
    private double inTrafficBps;
    private double outTrafficBps;
    private double discardsPercentage;
    private double errorsPercentage;

    public DeviceData(String deviceId, int interfaceIndex, double inTrafficBps, double outTrafficBps, 
                      double discardsPercentage, double errorsPercentage) {
        this.deviceId = deviceId;
        this.interfaceIndex = interfaceIndex;
        this.inTrafficBps = inTrafficBps;
        this.outTrafficBps = outTrafficBps;
        this.discardsPercentage = discardsPercentage;
        this.errorsPercentage = errorsPercentage;
    }

    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }

    public int getInterfaceIndex() {
        return interfaceIndex;
    }

    public double getInTrafficBps() {
        return inTrafficBps;
    }

    public double getOutTrafficBps() {
        return outTrafficBps;
    }

    public double getDiscardsPercentage() {
        return discardsPercentage;
    }

    public double getErrorsPercentage() {
        return errorsPercentage;
    }
}


class AggregatedData {
    private List<DeviceData> deviceDataList;

    public AggregatedData() {
        this.deviceDataList = new ArrayList<>();
    }

    public static AggregatedData fromResultSet(ResultSet resultSet) throws SQLException {
        AggregatedData aggregatedData = new AggregatedData();
        
        while (resultSet.next()) {
            DeviceData deviceData = new DeviceData(
                resultSet.getString("deviceId"),
                resultSet.getInt("interfaceIndex"),
                resultSet.getDouble("inTraffic(bps)"),
                resultSet.getDouble("outTraffic(bps)"),
                resultSet.getDouble("discards(%)"),
                resultSet.getDouble("errors(%)")
            );
            aggregatedData.deviceDataList.add(deviceData);
        }
        return aggregatedData;
    }

    public List<DeviceData> getDeviceDataList() {
        return deviceDataList;
    }
}

public class DataArchiveService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public void startService() {
        try {
            Runnable task = () -> {
                try {
                    analyzeAndArchive();
                } catch (Exception e) {
                    System.out.println("Error during analysis and archiving: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            long currentMillis = System.currentTimeMillis();
            long nextHourMillis = (currentMillis / 3600000 + 1) * 3600000;
            long delay = nextHourMillis - currentMillis;

            scheduler.scheduleAtFixedRate(task, delay, 3600000, TimeUnit.MILLISECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(scheduler::shutdown));

        } catch (Exception e) {
            System.out.println("Error initializing service: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void analyzeAndArchive() throws SQLException, IOException {
        // MySQL connection
        String mysqlUrl = "jdbc:mysql://localhost:3306/yourDatabase";
        String mysqlUser = "root";
        String mysqlPassword = ""; // Use appropriate credentials

        try (Connection mysqlConnection = DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword)) {
            // Cassandra connection
            try (CqlSession cassandraSession = CqlSession.builder().build()) {

                // Calculate the hour range
                LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                LocalDateTime startOfLastHour = now.minusHours(1).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime endOfLastHour = startOfLastHour.plusHours(1);

                Timestamp startTimestamp = Timestamp.valueOf(startOfLastHour);
                Timestamp endTimestamp = Timestamp.valueOf(endOfLastHour);

                // Query data for the last hour
                String query = "SELECT deviceId, interfaceIndex, inTraffic(bps), outTraffic(bps), discards(%), errors(%) " +
                        "FROM networkDeviceInterfaces WHERE recordTime >= ? AND recordTime < ?";

                PreparedStatement statement = mysqlConnection.prepareStatement(query);
                statement.setTimestamp(1, startTimestamp);
                statement.setTimestamp(2, endTimestamp);

                ResultSet resultSet = statement.executeQuery();

                // Perform aggregation
                AggregatedData aggregatedData = AggregatedData.fromResultSet(resultSet);

                // Serialize to JSON
                Gson gson = new Gson();
                String jsonData = gson.toJson(aggregatedData);

                // Compress JSON
                byte[] compressedData = compressJson(jsonData);

                // Prepare metadata
                String serializationType = "JSON";
                String compressionAlgorithm = "ZIP";
                String metadata = "Aggregated network device data for " + startOfLastHour + " to " + endOfLastHour;

                // Insert into Cassandra
                String insertQuery = "INSERT INTO networkDeviceData (archive_Id, archive_Timestamp, data, serialization, compression_Algorithm, metadata) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                cassandraSession.execute(insertQuery,
                        UUID.randomUUID(),
                        startOfLastHour.toInstant(ZoneOffset.UTC),
                        ByteBuffer.wrap(compressedData),
                        serializationType,
                        compressionAlgorithm,
                        metadata);
            }
        }
    }

    private byte[] compressJson(String jsonData) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ZipOutputStream zipStream = new ZipOutputStream(byteStream)) {
            zipStream.putNextEntry(new ZipEntry("data.json"));
            zipStream.write(jsonData.getBytes());
            zipStream.closeEntry();
            return byteStream.toByteArray();
        }
    }
}
