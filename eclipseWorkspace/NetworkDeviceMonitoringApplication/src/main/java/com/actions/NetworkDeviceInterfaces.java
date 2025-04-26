package com.actions;

import com.opensymphony.xwork2.ActionSupport;
import java.net.InetSocketAddress;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import org.apache.struts2.json.JSONResult;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

public class NetworkDeviceInterfaces extends ActionSupport {
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/nmt";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "";

    private static final String CASSANDRA_HOST = "127.0.0.1";
    private static final String CASSANDRA_KEYSPACE = "nmtArchive";

    private int deviceId;
    private int interfaceIndex;
    private String fromTime = null,toTime = null,interfaceName,ipAddress;
    private Map<Object, Object> responseJson = new HashMap<>();
    private String responseMessage = "";
    // Getters and Setters
    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public int getInterfaceIndex() {
        return interfaceIndex;
    }

    public void setInterfaceIndex(int interfaceIndex) {
        this.interfaceIndex = interfaceIndex;
    }

    public String getFromTime() {
        return fromTime;
    }

    public void setFromTime(String fromTime) {
        this.fromTime = fromTime;
    }

    public String getToTime() {
        return toTime;
    }

    public void setResponseMessage(String responseMessage) {
        this.responseMessage = responseMessage;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setToTime(String toTime) {
        this.toTime = toTime;
    }

    public Map<Object, Object> getResponseJson() {
        return responseJson;
    }
    
    private Connection getMysqlConnection()
    {
    	try
    	{
    		return DriverManager.getConnection(MYSQL_URL,MYSQL_USER,MYSQL_PASSWORD);
    	}
    	catch(Exception e)
    	{
    		System.out.println(e.getMessage());
    	}
    	return null;
    }
    private CqlSession getCassandraConnection()
    {
    	try
    	{
    		return CqlSession.builder()
                    .addContactPoint(new InetSocketAddress(CASSANDRA_HOST, 9042))
                    .withLocalDatacenter("datacenter1")
                    .withKeyspace(CASSANDRA_KEYSPACE)
                    .build();
    	}
    	catch(Exception e)
    	{
    		System.out.println(e.getMessage());
    	}
    	return null;
    }
    
    public String fetchNetworkInterfacesData() {
        try (Connection mysqlConnection = getMysqlConnection();
             CqlSession cassandraSession = getCassandraConnection(); ) {

        	
            // Fetch data from MySQL
        	String mysqlQuery = "";
        	String mysqlQr1 = "";
        	if(fromTime == null)
        	{
        		if(toTime == null)
        		{
        			mysqlQuery = "SELECT * FROM networkdeviceinterfaces where deviceId = ? ORDER BY deviceId, interfaceIndex, recordTime";
        		}
        		else
        		{
        			mysqlQuery = "SELECT * FROM networkdeviceinterfaces where deviceId = ? && recordTime<=? ORDER BY deviceId, interfaceIndex, recordTime";
        		}        			
        	}
        	else
        	{
        		if(toTime == null)
        		{
        			mysqlQuery = "SELECT * FROM networkdeviceinterfaces where deviceId = ? && recordTime>=? ORDER BY deviceId, interfaceIndex, recordTime";
        		}
        		else
        		{
        			mysqlQuery = "SELECT * FROM networkdeviceinterfaces where deviceId = ? && recordTime>=? && recordTime<=? ORDER BY deviceId, interfaceIndex, recordTime";
        		}   
        	}
            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlQuery);
            ZonedDateTime zonedDateTime;
            LocalDateTime localDateTime;
            Timestamp timestamp;
            mysqlStmt.setInt(1, deviceId);
            if(fromTime!=null)
            {
            	zonedDateTime = ZonedDateTime.parse(fromTime);

            	// Convert to LocalDateTime (removes timezone information)
            	localDateTime = zonedDateTime.toLocalDateTime();

            	// Convert LocalDateTime to java.sql.Timestamp
            	timestamp = Timestamp.valueOf(localDateTime);

            	// Set the timestamp in the prepared statement
            	mysqlStmt.setTimestamp(2, timestamp);
            	if(toTime!=null)
        		{
            		zonedDateTime = ZonedDateTime.parse(toTime);
                	localDateTime = zonedDateTime.toLocalDateTime();
                	timestamp = Timestamp.valueOf(localDateTime);
            		mysqlStmt.setTimestamp(3, timestamp);
        		};
            }
            else if(toTime!=null)
            {
        		zonedDateTime = ZonedDateTime.parse(toTime);
            	localDateTime = zonedDateTime.toLocalDateTime();
            	timestamp = Timestamp.valueOf(localDateTime);
        		mysqlStmt.setTimestamp(2, timestamp);
            }
            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery();

            Map<Integer, Object> networkDeviceData = new HashMap<>();

         // Initialize interface nodes
            String initializeNodesQuery = "SELECT interfaceIndex, interfaceName FROM networkDeviceInterfaceData WHERE deviceId = ?";
            PreparedStatement initializeNodesStmt = mysqlConnection.prepareStatement(initializeNodesQuery);
            initializeNodesStmt.setInt(1, deviceId);
            java.sql.ResultSet initializeNodesResult = initializeNodesStmt.executeQuery();

            while (initializeNodesResult.next()) {
                int interfaceIndex = initializeNodesResult.getInt("interfaceIndex");
                String interfaceName = initializeNodesResult.getString("interfaceName");

                // Ensure the interface node is created with the interfaceName
                ((Map<String, Object>) responseJson.computeIfAbsent(interfaceIndex, k -> new HashMap<>()))
                .put("interfaceName", interfaceName);
            }

            
            while (mysqlResult.next()) {
                int interfaceIndex = mysqlResult.getInt("interfaceIndex");
                String recordTime = mysqlResult.getTimestamp("recordTime").toString();
                Map<String, Object> interfaceData = new HashMap<>();
                
                interfaceData.put("inTraffic(bps)", mysqlResult.getLong("inTraffic(bps)"));
                interfaceData.put("outTraffic(bps)", mysqlResult.getLong("outTraffic(bps)"));
                interfaceData.put("discards(%)", mysqlResult.getBigDecimal("discards(%)"));
                interfaceData.put("errors(%)", mysqlResult.getBigDecimal("errors(%)"));
                interfaceData.put("operationalStatus", mysqlResult.getString("operationalStatus"));
                
                ((Map<String,Object>)((Map<String,Object>)(responseJson.computeIfAbsent(interfaceIndex,k-> new HashMap<>())))
                					.computeIfAbsent("InterfaceData",k -> new HashMap<>()))
                					.put(recordTime,interfaceData);
                
            }

            // Fetch data from Cassandra
            // Step 2: Fetch metrics for each interfaceIndex with time constraints
            StringBuilder metricsQueryBuilder = new StringBuilder("SELECT * FROM networkinterfacemetricsarchive_ByTime WHERE deviceId = ?");

            if (fromTime != null) {
                metricsQueryBuilder.append(" AND recordTime >= ?");
            }
            if (toTime != null) {
                metricsQueryBuilder.append(" AND recordTime <= ?");
            }
            
            String metricsQuery = metricsQueryBuilder.toString();
            com.datastax.oss.driver.api.core.cql.PreparedStatement metricsStmt = cassandraSession.prepare(metricsQuery);

            BoundStatement boundStatement = metricsStmt.bind(deviceId);

            int metricsIndex = 1; // Adjust based on query parameters
            if (fromTime != null) {
                boundStatement = boundStatement.setInstant(metricsIndex++, Instant.parse(fromTime));
            }
            if (toTime != null) {
                boundStatement = boundStatement.setInstant(metricsIndex++, Instant.parse(toTime));
            }

            ResultSet metricsResult = cassandraSession.execute(boundStatement);
            for (Row row : metricsResult) {
            	
            	String recordTime = row.getInstant("recordTime").toString();
                int interfaceIndex = row.getInt("interfaceIndex");
                // System.out.println(interfaceIndex);
                Map<String, Object> interfaceMetrics = new HashMap<>();
                interfaceMetrics.put("count", row.getLong("count"));
                interfaceMetrics.put("maxInTraffic_bps", row.getDouble("maxInTraffic_bps"));
                interfaceMetrics.put("minInTraffic_bps", row.getDouble("minInTraffic_bps"));
                interfaceMetrics.put("sumInTraffic_bps", row.getDouble("sumInTraffic_bps"));
                interfaceMetrics.put("avgInTraffic_bps", row.getDouble("avgInTraffic_bps"));
                interfaceMetrics.put("maxOutTraffic_bps", row.getDouble("maxOutTraffic_bps"));
                interfaceMetrics.put("minOutTraffic_bps", row.getDouble("minOutTraffic_bps"));
                interfaceMetrics.put("sumOutTraffic_bps", row.getDouble("sumOutTraffic_bps"));
                interfaceMetrics.put("avgOutTraffic_bps", row.getDouble("avgOutTraffic_bps"));
                interfaceMetrics.put("maxDiscards_percent", row.getDouble("maxDiscards_percent"));
                interfaceMetrics.put("minDiscards_percent", row.getDouble("minDiscards_percent"));
                interfaceMetrics.put("sumDiscards_percent", row.getDouble("sumDiscards_percent"));
                interfaceMetrics.put("avgDiscards_percent", row.getDouble("avgDiscards_percent"));
                interfaceMetrics.put("maxErrors_percent", row.getDouble("maxErrors_percent"));
                interfaceMetrics.put("minErrors_percent", row.getDouble("minErrors_percent"));
                interfaceMetrics.put("sumErrors_percent", row.getDouble("sumErrors_percent"));
                interfaceMetrics.put("avgErrors_percent", row.getDouble("avgErrors_percent"));

                ((Map<String, Object>) ((Map<String, Object>) (responseJson.computeIfAbsent(interfaceIndex, k -> new HashMap<>())))
                        .computeIfAbsent("InterfaceMetrics", k -> new HashMap<>()))
                        .put(recordTime, interfaceMetrics);
            }

            // System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }

    
    public String fetchNetworkInterfaceData() {
        try (Connection mysqlConnection = getMysqlConnection();
             CqlSession cassandraSession = getCassandraConnection();) {

        	boolean intervalFetch = false;
        	
        	System.out.println(fromTime);
        	System.out.println(toTime);

            // Handle fromTime and toTime
            String currentTime = Instant.now().toString();
            String effectiveFromTime = (fromTime != null) ? fromTime : null;
            String effectiveToTime = (toTime != null) ? toTime : currentTime;

            // Fetch data from MySQL
            StringBuilder mysqlQueryBuilder = new StringBuilder("SELECT * FROM networkdeviceinterfaces WHERE deviceId = ? AND interfaceIndex = ?");
            if (effectiveFromTime != null) {
                mysqlQueryBuilder.append(" AND recordTime >= ?");
                intervalFetch = true;
                if (effectiveToTime != currentTime) {
                    mysqlQueryBuilder.append(" AND recordTime <= ?");
                }
            }
            
            if(intervalFetch)
            {
            	responseJson.clear();	
            }
            else
            {
            	responseJson.clear();
            }
            String mysqlQuery = mysqlQueryBuilder.toString();

            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlQuery);
            int parameterIndex = 1;
            mysqlStmt.setInt(parameterIndex++, deviceId);
            mysqlStmt.setInt(parameterIndex++, interfaceIndex);

            if (effectiveFromTime != null) {
                mysqlStmt.setTimestamp(parameterIndex++, Timestamp.valueOf(LocalDateTime.ofInstant(Instant.parse(effectiveFromTime), ZoneId.of("UTC"))));
            }
            if (effectiveToTime != currentTime) {
                mysqlStmt.setTimestamp(parameterIndex++, Timestamp.valueOf(LocalDateTime.ofInstant(Instant.parse(effectiveToTime), ZoneId.of("UTC"))));
            }

            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery();
            String interfaceName = null;

            Map<String, Map<String, Object>> interfaceData = new HashMap<>();
            while (mysqlResult.next()) {
                String recordTime = mysqlResult.getTimestamp("recordTime").toString();
                Map<String, Object> data = new HashMap<>();
                data.put("inTraffic(bps)", mysqlResult.getLong("inTraffic(bps)"));
                data.put("outTraffic(bps)", mysqlResult.getLong("outTraffic(bps)"));
                data.put("discards(%)", mysqlResult.getBigDecimal("discards(%)"));
                data.put("errors(%)", mysqlResult.getBigDecimal("errors(%)"));
                data.put("operationalStatus", mysqlResult.getString("operationalStatus"));

                interfaceData.put(recordTime, data);
            }
            
            String interfaceNameQuery = "SELECT interfaceName FROM networkdeviceinterfaceData WHERE deviceId = ? AND interfaceIndex = ? LIMIT 1";
            PreparedStatement interfaceNameStmt = mysqlConnection.prepareStatement(interfaceNameQuery);
            interfaceNameStmt.setInt(1, deviceId);
            interfaceNameStmt.setInt(2, interfaceIndex);
            java.sql.ResultSet interfaceNameResult = interfaceNameStmt.executeQuery();

            if (interfaceNameResult.next()) {
                interfaceName = interfaceNameResult.getString("interfaceName");
            }
            
            if(interfaceName == null)
            {
            	return SUCCESS;
            }
            
        	responseJson.put("DeviceId", deviceId);
        	responseJson.put("InterfaceIndex", interfaceIndex);
        	responseJson.put("InterfaceName", interfaceName);
        	responseJson.put("InterfaceData", interfaceData);
            // Fetch data from Cassandra
            StringBuilder cassandraQueryBuilder = new StringBuilder("SELECT * FROM networkinterfacemetricsarchive WHERE deviceId = ? AND interfaceIndex = ?");

            if (effectiveFromTime != null) {
                cassandraQueryBuilder.append(" AND recordTime >= ?");
            }
            if (effectiveToTime != null) {
                cassandraQueryBuilder.append(" AND recordTime <= ?");
            }
            
            String cassandraQuery = cassandraQueryBuilder.toString();

            com.datastax.oss.driver.api.core.cql.PreparedStatement cassandraStmt = cassandraSession.prepare(cassandraQuery);
            BoundStatement boundStatement = cassandraStmt.bind(deviceId,interfaceIndex);

            int cassandraIndex = 2;
            if (effectiveFromTime != null) {
                boundStatement = boundStatement.setInstant(cassandraIndex++, Instant.parse(effectiveFromTime));
            }
            if (effectiveToTime != null) {
                boundStatement = boundStatement.setInstant(cassandraIndex++, Instant.parse(effectiveToTime));
            }

            ResultSet cassandraResult = cassandraSession.execute(boundStatement);

            Map<String, Map<String, Object>> interfaceMetrics = new HashMap<>();
            for (Row row : cassandraResult) {
                String recordTime = row.getInstant("recordTime").toString();
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("count", row.getLong("count"));
                metrics.put("maxInTraffic_bps", row.getDouble("maxInTraffic_bps"));
                metrics.put("minInTraffic_bps", row.getDouble("minInTraffic_bps"));
                metrics.put("sumInTraffic_bps", row.getDouble("sumInTraffic_bps"));
                metrics.put("avgInTraffic_bps", row.getDouble("avgInTraffic_bps"));
                metrics.put("maxOutTraffic_bps", row.getDouble("maxOutTraffic_bps"));
                metrics.put("minOutTraffic_bps", row.getDouble("minOutTraffic_bps"));
                metrics.put("sumOutTraffic_bps", row.getDouble("sumOutTraffic_bps"));
                metrics.put("avgOutTraffic_bps", row.getDouble("avgOutTraffic_bps"));
                metrics.put("maxDiscards_percent", row.getDouble("maxDiscards_percent"));
                metrics.put("minDiscards_percent", row.getDouble("minDiscards_percent"));
                metrics.put("sumDiscards_percent", row.getDouble("sumDiscards_percent"));
                metrics.put("avgDiscards_percent", row.getDouble("avgDiscards_percent"));
                metrics.put("maxErrors_percent", row.getDouble("maxErrors_percent"));
                metrics.put("minErrors_percent", row.getDouble("minErrors_percent"));
                metrics.put("sumErrors_percent", row.getDouble("sumErrors_percent"));
                metrics.put("avgErrors_percent", row.getDouble("avgErrors_percent"));

                interfaceMetrics.put(recordTime, metrics);
            }
            if(intervalFetch)
            {            	
            	responseJson.put("InterfaceMetrics", interfaceMetrics);
            	System.out.println(responseJson);
            }
            else
            {
            	responseJson.put("InterfaceMetrics", interfaceMetrics);
            	System.out.println(responseJson);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }
    public String fetchLatestNetworkInterfaceData() { 
        try (Connection mysqlConnection = getMysqlConnection();
             CqlSession cassandraSession = getCassandraConnection()) {

            // Modified MySQL query to fetch the latest row
            String mysqlQuery = "SELECT * FROM networkdeviceinterfaces WHERE deviceId = ? AND interfaceIndex = ? " +
                                "ORDER BY recordTime DESC LIMIT 1";

            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlQuery);
            mysqlStmt.setInt(1, deviceId);
            mysqlStmt.setInt(2, interfaceIndex);

            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery();
            String interfaceName = null;

            Map<String, Object> latestInterfaceData = new HashMap<>();
            if (mysqlResult.next()) {  // Fetch only the first row (latest)
                String recordTime = mysqlResult.getTimestamp("recordTime").toString();
                latestInterfaceData.put("recordTime", recordTime);
                latestInterfaceData.put("inTraffic(bps)", mysqlResult.getLong("inTraffic(bps)"));
                latestInterfaceData.put("outTraffic(bps)", mysqlResult.getLong("outTraffic(bps)"));
                latestInterfaceData.put("discards(%)", mysqlResult.getBigDecimal("discards(%)"));
                latestInterfaceData.put("errors(%)", mysqlResult.getBigDecimal("errors(%)"));
                latestInterfaceData.put("operationalStatus", mysqlResult.getString("operationalStatus"));
            }


            String interfaceNameQuery = "SELECT interfaceName FROM networkdeviceinterfaceData WHERE deviceId = ? AND interfaceIndex = ? LIMIT 1";
            PreparedStatement interfaceNameStmt = mysqlConnection.prepareStatement(interfaceNameQuery);
            interfaceNameStmt.setInt(1, deviceId);
            interfaceNameStmt.setInt(2, interfaceIndex);
            java.sql.ResultSet interfaceNameResult = interfaceNameStmt.executeQuery();

            if (interfaceNameResult.next()) {
                interfaceName = interfaceNameResult.getString("interfaceName");
            }
            
            if (interfaceName == null) {
                return SUCCESS;  // No data found for the given criteria
            }
            
            responseJson.put("DeviceId", deviceId);
            responseJson.put("InterfaceIndex", interfaceIndex);
            responseJson.put("InterfaceName", interfaceName);
            responseJson.put("InterfaceData", latestInterfaceData);
          
            System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }
    
    public String fetchLatestNetworkDeviceInterfacesData() { 
        try (Connection mysqlConnection = getMysqlConnection();) {

            // Modified MySQL query to fetch the latest data for all interfaces under the given device ID
            String mysqlQuery = "SELECT * FROM networkdeviceinterfaces WHERE deviceId = ? " +
                                "AND recordTime IN (SELECT MAX(recordTime) FROM networkdeviceinterfaces " +
                                "WHERE deviceId = ? GROUP BY interfaceIndex)";

            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlQuery);
            mysqlStmt.setInt(1, deviceId);
            mysqlStmt.setInt(2, deviceId);

            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery();

            Map<Integer, Map<String, Object>> interfacesData = new HashMap<>();
            
            while (mysqlResult.next()) {  // Iterate through all interface rows
                int interfaceIndex = mysqlResult.getInt("interfaceIndex");
                String recordTime = mysqlResult.getTimestamp("recordTime").toString();

                Map<String, Object> latestInterfaceData = new HashMap<>();
                latestInterfaceData.put("inTraffic(bps)", mysqlResult.getLong("inTraffic(bps)"));
                latestInterfaceData.put("outTraffic(bps)", mysqlResult.getLong("outTraffic(bps)"));
                latestInterfaceData.put("discards(%)", mysqlResult.getBigDecimal("discards(%)"));
                latestInterfaceData.put("errors(%)", mysqlResult.getBigDecimal("errors(%)"));
                latestInterfaceData.put("operationalStatus", mysqlResult.getString("operationalStatus"));

                interfacesData.put(interfaceIndex, latestInterfaceData);
            }

            // Query to fetch interface names
            String interfaceNameQuery = "SELECT interfaceIndex, interfaceName FROM networkdeviceinterfaceData WHERE deviceId = ?";
            PreparedStatement interfaceNameStmt = mysqlConnection.prepareStatement(interfaceNameQuery);
            interfaceNameStmt.setInt(1, deviceId);
            java.sql.ResultSet interfaceNameResult = interfaceNameStmt.executeQuery();

            Map<Integer, String> interfaceNames = new HashMap<>();
            while (interfaceNameResult.next()) {
                int interfaceIndex = interfaceNameResult.getInt("interfaceIndex");
                String interfaceName = interfaceNameResult.getString("interfaceName");
                interfaceNames.put(interfaceIndex, interfaceName);
            }

            // Combine data and interface names
            Map<Integer, Object> responseList = new HashMap<>();
            for (Map.Entry<Integer, Map<String, Object>> entry : interfacesData.entrySet()) {
                int interfaceIndex = entry.getKey();
                Map<String, Object> data = entry.getValue();
                String interfaceName = interfaceNames.get(interfaceIndex);

                Map<String, Object> interfaceResponse = new HashMap<>();
                interfaceResponse.put("InterfaceIndex", interfaceIndex);
                interfaceResponse.put("InterfaceName", interfaceName);
                interfaceResponse.put("InterfaceData", data);

                responseList.put(interfaceIndex,interfaceResponse);
            }

            if (responseList.isEmpty()) {
                return SUCCESS;  // No data found for the given device ID
            }

            responseJson.put("Interfaces", responseList);

            System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }
    
    public String clearNetworkInterfaceData() { 
        try (Connection mysqlConnection = getMysqlConnection();
             CqlSession cassandraSession = getCassandraConnection()) {

            // Step 1: Clear data from MySQL
            String mysqlDeleteQuery = "DELETE FROM networkdeviceinterfaces WHERE deviceId = ? AND interfaceIndex = ?";
            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlDeleteQuery);
            mysqlStmt.setInt(1, deviceId);
            mysqlStmt.setInt(2, interfaceIndex);
            
            mysqlDeleteQuery = "DELETE FROM networkdeviceinterfaceData WHERE deviceId = ? AND interfaceIndex = ?";
            PreparedStatement mysqlStmt1 = mysqlConnection.prepareStatement(mysqlDeleteQuery);
            mysqlStmt.setInt(1, deviceId);
            mysqlStmt.setInt(2, interfaceIndex);

            int mysqlRowsDeleted = mysqlStmt.executeUpdate();
            responseMessage = ("Deleted " + mysqlRowsDeleted + " rows from MySQL.");

         // Handle fromTime and toTime dynamically
            if (fromTime == null || toTime == null) {
                String timeQuery = "SELECT MIN(recordTime) as minTime, MAX(recordTime) as maxTime FROM networkinterfacemetricsarchive WHERE deviceId = ? AND interfaceIndex = ?";
                com.datastax.oss.driver.api.core.cql.PreparedStatement timeStmt = cassandraSession.prepare(timeQuery);
                BoundStatement timeBoundStmt = timeStmt.bind(deviceId, interfaceIndex);

                ResultSet timeResult = cassandraSession.execute(timeBoundStmt);
                Row timeRow = timeResult.one();

                if (timeRow != null) {
                    fromTime = (fromTime == null) ? timeRow.getInstant("minTime").toString() : fromTime;
                    toTime = (toTime == null) ? timeRow.getInstant("maxTime").toString() : toTime;
                }
            }
            
            // Step 2: Clear data from Cassandra
            StringBuilder cassandraDeleteQuery = new StringBuilder("DELETE FROM networkinterfacemetricsarchive_ByTime WHERE deviceId = ?");

            if (fromTime != null) {
            	cassandraDeleteQuery.append(" AND recordTime >= ?");
            }
            if (toTime != null) {
            	cassandraDeleteQuery.append(" AND recordTime <= ?");
            }
            cassandraDeleteQuery.append(" AND interfaceIndex = ?");
            
            com.datastax.oss.driver.api.core.cql.PreparedStatement cassandraStmt = cassandraSession.prepare(cassandraDeleteQuery.toString());
            BoundStatement boundStatement = cassandraStmt.bind(deviceId);
            int metricsIndex = 1; // Adjust based on query parameters
            if (fromTime != null) {
                boundStatement = boundStatement.setInstant(metricsIndex++, Instant.parse(fromTime));
            }
            if (toTime != null) {
                boundStatement = boundStatement.setInstant(metricsIndex++, Instant.parse(toTime));
            }
            
            boundStatement = boundStatement.setInt(metricsIndex++, interfaceIndex);
            
            cassandraSession.execute(boundStatement);
            
            String cassandraDeleteQueryIf = "DELETE FROM networkinterfacemetricsarchive WHERE deviceId = ? AND interfaceIndex = ?";
            com.datastax.oss.driver.api.core.cql.PreparedStatement cassandraStmtIf = cassandraSession.prepare(cassandraDeleteQueryIf);
            BoundStatement boundStatementIf = cassandraStmtIf.bind(deviceId, interfaceIndex);
            
            cassandraSession.execute(boundStatementIf);
            
            responseMessage+=("Deleted data from Cassandra for deviceId: " + deviceId + " and interfaceIndex: " + interfaceIndex);

            System.out.println(responseMessage);
            
            responseJson.put("ResponseMessage", responseMessage);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }
    public String updateNetworkInterfaceName() { 
        String responseMessage = "";
        try (Connection mysqlConnection = getMysqlConnection();) {

            // Step 1: Update interface name in MySQL
            String mysqlUpdateQuery = "UPDATE networkdeviceinterfaceData SET interfaceName = ? WHERE deviceId = ? AND interfaceIndex = ?";
            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlUpdateQuery);
            mysqlStmt.setString(1, interfaceName);
            mysqlStmt.setInt(2, deviceId);
            mysqlStmt.setInt(3, interfaceIndex);

            int mysqlRowsUpdated = mysqlStmt.executeUpdate();
            responseMessage = ("Updated " + mysqlRowsUpdated + " rows in MySQL.");

       
            System.out.println(responseMessage);
            
            // Add response message to JSON (if needed)
            responseJson.put("ResponseMessage", responseMessage);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }
    public String fetchNetworkDeviceAndInterfaceIndices() {
        try (Connection mysqlConnection = getMysqlConnection();) {

            // Fetch data from MySQL
            String mysqlQuery = "SELECT DISTINCT(deviceId), interfaceIndex FROM networkdeviceinterfaces ORDER BY deviceId, interfaceIndex";
            Statement mysqlStmt = mysqlConnection.createStatement();
            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery(mysqlQuery);

            while (mysqlResult.next()) {
                int deviceId = mysqlResult.getInt("deviceId");
                int interfaceIndex = mysqlResult.getInt("interfaceIndex");

                ((ArrayList<Integer>)responseJson.computeIfAbsent(deviceId, k -> new ArrayList<>())).add(interfaceIndex);
            }

            System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }
    public String addSuspendedInterface()  {
        String fetchIpQuery = "SELECT ipAddress FROM monitoredIPAddresses WHERE deviceId = ?";
        String insertQuery = "INSERT INTO suspendedInterfaces (ipAddress, deviceId, interfaceIndex) VALUES (?, ?, ?)";
        
        try (Connection conn = getMysqlConnection(); 
             PreparedStatement fetchStmt = conn.prepareStatement(fetchIpQuery);
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            
            // Fetch the IP Address for the given deviceId
            fetchStmt.setInt(1, deviceId);
            java.sql.ResultSet rs = fetchStmt.executeQuery();
            
            if (rs.next()) {
                String ipAddress = rs.getString("ipAddress");

                // Insert into suspendedInterfaces
                insertStmt.setString(1, ipAddress);
                insertStmt.setInt(2, deviceId);
                insertStmt.setInt(3, interfaceIndex);
                insertStmt.executeUpdate();
            } 
        }
        catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        responseJson.put("responseMessage","Interface suspended successfully");
        return SUCCESS;
    }

    public String getSuspendedInterfacesByIp()  {
        String sql = "SELECT interfaceIndex FROM suspendedInterfaces WHERE ipAddress = ?";
        List<Integer> interfaceIndices = new ArrayList<>();
        try (Connection conn = getMysqlConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, ipAddress);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interfaceIndices.add(rs.getInt("interfaceIndex"));
                }
                
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        responseJson.put("InterfaceIndices", interfaceIndices);
        // Convert responseJson to JSON string
        return SUCCESS;
    }

    public String getSuspendedInterfacesByDeviceId()  {
        String sql = "SELECT interfaceIndex FROM suspendedInterfaces WHERE deviceId = ?";
        List<Integer> interfaceIndices = new ArrayList<>();
        try (Connection conn = getMysqlConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deviceId);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    interfaceIndices.add(rs.getInt("interfaceIndex"));
                }
                responseJson.put("InterfaceIndices", interfaceIndices);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        
        // Convert responseJson to JSON string
        responseJson.put("InterfaceIndices", interfaceIndices);
        return SUCCESS;
    }
    
    public String removeSuspendedInterface() {
        String sql = "DELETE FROM suspendedInterfaces WHERE deviceId = ? AND interfaceIndex = ?";
        try (Connection conn = getMysqlConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deviceId);
            stmt.setInt(2, interfaceIndex);
            stmt.executeUpdate();
        }
        catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        responseJson.put("responseMessage","Interface monitoring enabled successfully");
        return SUCCESS;
    }

}
