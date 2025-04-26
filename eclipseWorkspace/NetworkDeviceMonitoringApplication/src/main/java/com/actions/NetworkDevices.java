package com.actions;

import com.opensymphony.xwork2.ActionSupport;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import org.apache.struts2.json.JSONResult;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;

public class NetworkDevices extends ActionSupport {
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/nmt";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "";

    private int deviceId;
    private Map<Object, Object> responseJson = new HashMap<Object, Object>();
    
    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }

    public String fetchNetworkDevicesData() {
        try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);) 
        {
        	
            // Fetch data from MySQL
            String mysqlQuery = "SELECT * FROM networkdevices ORDER BY deviceId";
            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlQuery);
            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery();

            while (mysqlResult.next()) {
                int deviceId = mysqlResult.getInt("deviceId");
              
                Map<Object, Object> networkDeviceData = new HashMap<>();
                
                networkDeviceData.put("sysName", mysqlResult.getString("sysName"));
                networkDeviceData.put("sysLocation", mysqlResult.getString("sysLocation"));
                networkDeviceData.put("sysObjectId", mysqlResult.getString("sysObjectId"));
                networkDeviceData.put("sysDescr", mysqlResult.getString("sysDescr"));
                networkDeviceData.put("ipAddress", mysqlResult.getString("ipAddress"));
                
                responseJson.put(deviceId, networkDeviceData);
            }
            
            System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }

    public String fetchNetworkDeviceData() {
    	try (Connection mysqlConnection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);) 
        {
        	responseJson.clear();
        	
            // Fetch data from MySQL
            String mysqlQuery = "SELECT * FROM networkdevices where deviceId = ?";
            PreparedStatement mysqlStmt = mysqlConnection.prepareStatement(mysqlQuery);
            mysqlStmt.setInt(1,deviceId);
            java.sql.ResultSet mysqlResult = mysqlStmt.executeQuery();

            if(mysqlResult.next()) {
                int deviceId = mysqlResult.getInt("deviceId");
                
                responseJson.put("deviceId", deviceId);
                responseJson.put("sysName", mysqlResult.getString("sysName"));
                responseJson.put("sysLocation", mysqlResult.getString("sysLocation"));
                responseJson.put("sysObjectId", mysqlResult.getString("sysObjectId"));
                responseJson.put("sysDescr", mysqlResult.getString("sysDescr"));
                responseJson.put("ipAddress", mysqlResult.getString("ipAddress"));
            }
            
            System.out.println(responseJson);

        } catch (Exception e) {
            e.printStackTrace();
            return ERROR;
        }
        return SUCCESS;
    }


    public Map<Object, Object> getResponseJson() {
        return responseJson;
    }
}
