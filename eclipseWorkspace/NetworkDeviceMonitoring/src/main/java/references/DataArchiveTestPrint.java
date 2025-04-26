package references;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.gson.Gson;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DataArchiveTestPrint {
    public static void main(String[] args) {
        try (CqlSession cassandraSession = CqlSession.builder().build()) {
            String keyspace = "nmtarchive"; // Replace with your keyspace name
            String tableName = "networkDeviceData"; // Replace with your table name

            String query = String.format("SELECT archive_Id, archive_Timestamp, data FROM %s.%s", keyspace, tableName);

            ResultSet resultSet = cassandraSession.execute(query);
            for (Row row : resultSet) {
                String archiveId = row.getUuid("archive_Id").toString();
                String timestamp = row.getInstant("archive_Timestamp").toString();
                byte[] compressedData = row.getByteBuffer("data").array();

                // Decompress the data
                String jsonData = decompressData(compressedData);

                // Deserialize JSON to a Map structure
                Gson gson = new Gson();
                Object dataObject = gson.fromJson(jsonData, Object.class);

                // Print the archive details
                System.out.println("Archive ID: " + archiveId);
                System.out.println("Timestamp: " + timestamp);
                System.out.println("Data: " + gson.toJson(dataObject));
                System.out.println("-----------------------------------");
            }
        } catch (Exception e) {
            System.out.println("Error during retrieval and decompression: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String decompressData(byte[] compressedData) throws IOException {
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(compressedData);
             ZipInputStream zipStream = new ZipInputStream(byteStream)) {
            ZipEntry entry = zipStream.getNextEntry();
            if (entry != null) {
                StringBuilder decompressedData = new StringBuilder();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zipStream.read(buffer)) != -1) {
                    decompressedData.append(new String(buffer, 0, length));
                }
                zipStream.closeEntry();
                return decompressedData.toString();
            } else {
                throw new IOException("No entry found in the compressed data");
            }
        }
    }
}