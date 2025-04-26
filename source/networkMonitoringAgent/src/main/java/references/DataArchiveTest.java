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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DataArchiveTest {

    public static void main(String[] args) {
        try {
            analyzeAndArchive();
        } catch (Exception e) {
            System.out.println("Error during analysis and archiving: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void analyzeAndArchive() throws SQLException, IOException {
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

                String query = "SELECT deviceId, interfaceIndex, " +
                        "MIN(`inTraffic(bps)`) AS minInTraffic, MAX(`inTraffic(bps)`) AS maxInTraffic, " +
                        "SUM(`inTraffic(bps)`) AS sumInTraffic, COUNT(`inTraffic(bps)`) AS countInTraffic, " +
                        "MIN(`outTraffic(bps)`) AS minOutTraffic, MAX(`outTraffic(bps)`) AS maxOutTraffic, " +
                        "SUM(`outTraffic(bps)`) AS sumOutTraffic, COUNT(`outTraffic(bps)`) AS countOutTraffic, " +
                        "MIN(`discards(%)`) AS minDiscards, MAX(`discards(%)`) AS maxDiscards, " +
                        "SUM(`discards(%)`) AS sumDiscards, COUNT(`discards(%)`) AS countDiscards, " +
                        "MIN(`errors(%)`) AS minErrors, MAX(`errors(%)`) AS maxErrors, " +
                        "SUM(`errors(%)`) AS sumErrors, COUNT(`errors(%)`) AS countErrors " +
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

                    DeviceData deviceData = aggregatedData.computeIfAbsent(deviceId, k -> new DeviceData());
                    long count = resultSet.getLong("countInTraffic");
                    Metrics inTrafficMetrics = new Metrics(
                        resultSet.getDouble("minInTraffic"),
                        resultSet.getDouble("maxInTraffic"),
                        resultSet.getLong("countInTraffic") > 0
                            ? resultSet.getDouble("sumInTraffic") / count
                            : 0,
                        resultSet.getDouble("sumInTraffic"),
                        count
                    );
                    count = resultSet.getLong("countOutTraffic");
                    Metrics outTrafficMetrics = new Metrics(
                        resultSet.getDouble("minOutTraffic"),
                        resultSet.getDouble("maxOutTraffic"),
                        resultSet.getLong("countOutTraffic") > 0
                            ? resultSet.getDouble("sumOutTraffic") / count
                            : 0,
                        resultSet.getDouble("sumOutTraffic"),
                        count
                    );
                    count = resultSet.getLong("countDiscards");
                    Metrics discardsMetrics = new Metrics(
                        resultSet.getDouble("minDiscards"),
                        resultSet.getDouble("maxDiscards"),
                        resultSet.getLong("countDiscards") > 0
                            ? resultSet.getDouble("sumDiscards") / count
                            : 0,
                        resultSet.getDouble("sumDiscards"),
                        count
                    );
                    count = resultSet.getLong("countErrors");
                    Metrics errorsMetrics = new Metrics(
                        resultSet.getDouble("minErrors"),
                        resultSet.getDouble("maxErrors"),
                        resultSet.getLong("countErrors") > 0
                            ? resultSet.getDouble("sumErrors") / count
                            : 0,
                        resultSet.getDouble("sumErrors"),
                        count
                    );

                    // Now pass each metrics object to the InterfaceData constructor
                    InterfaceData interfaceData = new InterfaceData(
                        inTrafficMetrics,
                        outTrafficMetrics,
                        discardsMetrics,
                        errorsMetrics
                    );


                    deviceData.interfaces.put(interfaceIndex, interfaceData);
                }

                if (!aggregatedData.isEmpty()) {
                    Gson gson = new Gson();
                    String jsonData = gson.toJson(aggregatedData);
                    byte[] compressedData = compressJson(jsonData);

                    String serializationType = "JSON";
                    String compressionAlgorithm = "ZIP";
                    String metadata = "Aggregated network device data for " + startOfCurrentHour + " to " + endOfCurrentHour;

                    String keyspace = "nmtarchive";
                    String tableName = "networkDeviceData";
                    String fullyQualifiedTableName = keyspace + "." + tableName;

                    String insertQuery = String.format(
                            "INSERT INTO %s (archive_Id, archive_Timestamp, data, serialization, compression_Algorithm, metadata) " +
                                    "VALUES (?, ?, ?, ?, ?, ?)",
                            fullyQualifiedTableName
                    );

                    cassandraSession.execute(insertQuery,
                            UUID.randomUUID(),
                            startOfCurrentHour.toInstant(ZoneOffset.UTC),
                            ByteBuffer.wrap(compressedData),
                            serializationType,
                            compressionAlgorithm,
                            metadata);

                    System.out.println("Data successfully archived for " + startOfCurrentHour + " to " + endOfCurrentHour);
                } else {
                    System.out.println("No data available for " + startOfCurrentHour + " to " + endOfCurrentHour);
                }
            }
        }
    }

    private static byte[] compressJson(String jsonData) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ZipOutputStream zipStream = new ZipOutputStream(byteStream)) {
            zipStream.putNextEntry(new ZipEntry("data.json"));
            zipStream.write(jsonData.getBytes());
            zipStream.closeEntry();
            return byteStream.toByteArray();
        }
    }
}
