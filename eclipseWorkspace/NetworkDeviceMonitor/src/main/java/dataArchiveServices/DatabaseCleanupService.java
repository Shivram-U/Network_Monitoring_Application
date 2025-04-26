package dataArchiveServices;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DatabaseCleanupService {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/nmt";
    private static final String DB_USER = "root"; // replace with your username
    private static final String DB_PASSWORD = ""; // replace with your password
    
    public void startService() {
    	System.out.println("Database Cleanup Service Started");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        // Calculate initial delay until the 59th minute
        long initialDelay = calculateInitialDelay();
        
        //System.out.println(initialDelay/60000.0);
        
        // Schedule task to run at the 59th minute of every hour
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performCleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, initialDelay, 60000, TimeUnit.MILLISECONDS);
    }
    
    private static long calculateInitialDelay() {
    	long currentMillis = System.currentTimeMillis();
        long nextMinuteMillis = (currentMillis / 60000 + 1) * 60000;
        long delay = nextMinuteMillis - currentMillis;

        return delay;
    }
    
    private static void performCleanup() throws SQLException {
    	System.out.println("MYSQL Database cleanup in started");
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            connection.setAutoCommit(false); // Use transactions
            
            // Step 1: Delete data older than 6 hours in networkdeviceinterfaces
            String deleteOldDataQuery = "DELETE FROM networkdeviceinterfaces WHERE recordTime < NOW() - INTERVAL 6 HOUR;";
            try (PreparedStatement deleteOldDataStmt = connection.prepareStatement(deleteOldDataQuery)) {
                int deletedRows = deleteOldDataStmt.executeUpdate();
                System.out.println("Deleted " + deletedRows + " old rows from networkdeviceinterfaces.");
            }
        
            connection.commit(); // Commit transaction
            System.out.println("MYSQL Database cleanup completed");
        } catch (SQLException e) {
            e.printStackTrace();
            throw e; // Rethrow exception after logging
        }
    }
}
