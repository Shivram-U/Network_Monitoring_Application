CREATE KEYSPACE nmtArchive
WITH REPLICATION = { 
    'class' : 'SimpleStrategy', 
    'replication_factor' : 3 
};

CREATE TABLE networkDeviceData (
    archive_Id UUID PRIMARY KEY,       
    archive_Timestamp TIMESTAMP,               
    data BLOB, 
    serialization TEXT,                
    compression_Algorithm TEXT,        
    metadata TEXT                      
);

-- Active: 1733979641310@@127.0.0.1@9042@nmtarchive
CREATE TABLE networkInterfaceMetricsArchive (
    deviceId INT,
    interfaceIndex INT,
    recordTime TIMESTAMP,
    count BIGINT,
    maxInTraffic_bps DOUBLE,
    minInTraffic_bps DOUBLE,
    sumInTraffic_bps DOUBLE,
    avgInTraffic_bps DOUBLE,
    maxOutTraffic_bps DOUBLE,
    minOutTraffic_bps DOUBLE,
    sumOutTraffic_bps DOUBLE,
    avgOutTraffic_bps DOUBLE,
    maxDiscards_percent DOUBLE,
    minDiscards_percent DOUBLE,
    sumDiscards_percent DOUBLE,
    avgDiscards_percent DOUBLE,
    maxErrors_percent DOUBLE,
    minErrors_percent DOUBLE,
    sumErrors_percent DOUBLE,
    avgErrors_percent DOUBLE,
    PRIMARY KEY (deviceId, interfaceIndex, recordTime)
);

