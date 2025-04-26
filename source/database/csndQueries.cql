CREATE KEYSPACE nmtArchive
WITH REPLICATION = { 
    'class' : 'SimpleStrategy', 
    'replication_factor' : 3 
};

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

CREATE MATERIALIZED VIEW networkInterfaceMetricsArchive_ByTime AS
SELECT * 
FROM networkInterfaceMetricsArchive
WHERE deviceId IS NOT NULL AND recordTime IS NOT NULL AND interfaceIndex IS NOT NULL
PRIMARY KEY (deviceId, recordTime, interfaceIndex);

SELECT * FROM system_schema.views WHERE keyspace_name = 'nmtarchive';


CREATE TABLE networkInterfaceMetricsArchive_RT (
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
    PRIMARY KEY (deviceId, recordTime, interfaceIndex)
);




COPY nmtarchive.networkinterfacemetricsarchive TO 'D:/file_path.csv' WITH HEADER = true;
COPY nmtarchive.networkinterfacemetricsarchive FROM 'D:/file_path.csv' WITH HEADER = true;
COPY nmtarchive.networkinterfacemetricsarchive_IFIN TO 'D:/file_path.csv' WITH HEADER = true;
COPY nmtarchive.networkinterfacemetricsarchive FROM 'D:/file_path.csv' WITH HEADER = true;

COPY nmtarchive.networkinterfacemetricsarchive_RT FROM 'D:/file_path.csv' WITH HEADER = true;
COPY nmtarchive.networkinterfacemetricsarchive_IFIN FROM 'D:/file_path.csv' WITH HEADER = true;



// Rename Queries

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
    PRIMARY KEY (deviceId, recordTime,  interfaceIndex)
);

INSERT INTO temp_networkInterfaceMetricsArchive ( deviceId, interfaceIndex, recordTime, count, maxInTraffic_bps, minInTraffic_bps, sumInTraffic_bps, avgInTraffic_bps, maxOutTraffic_bps, minOutTraffic_bps, sumOutTraffic_bps, avgOutTraffic_bps, maxDiscards_percent, minDiscards_percent, sumDiscards_percent, avgDiscards_percent, maxErrors_percent, minErrors_percent, sumErrors_percent, avgErrors_percent) AS SELECT deviceId, interfaceIndex, recordTime, count,minInTraffic_bps as maxInTraffic_bps, maxInTraffic_bps as minInTraffic_bps,avgInTraffic_bps as sumInTraffic_bps, sumInTraffic_bps as avgInTraffic_bps,minOutTraffic_bps as maxOutTraffic_bps, maxOutTraffic_bps as minOutTraffic_bps,avgOutTraffic_bps as sumOutTraffic_bps, sumOutTraffic_bps as avgOutTraffic_bps,minDiscards_percent as maxDiscards_percent, maxDiscards_percent as minDiscards_percent,avgDiscards_percent as sumDiscards_percent, sumDiscards_percent as avgDiscards_percent,minErrors_percent as maxErrors_percent, maxErrors_percent as minErrors_percent,avgErrors_percent as sumErrors_percent, sumErrors_percent as avgErrors_percent FROM networkInterfaceMetricsArchive;

COPY networkInterfaceMetricsArchive_ FROM 'C:/Software/cassandra_export.csv' WITH HEADER = true;

drop materialized view nmtarchive.networkinterfacemetricsarchive_ByTime; 
DELETE FROM networkInterfaceMetricsArchive_ WHERE recordTime >= now() AND recordTime < dateOf(now());

DELETE FROM networkInterfaceMetricsArchive_ WHERE recordTime >= TO_TIMESTAMP(now()) AND recordTime < dateOf(now());

DELETE FROM networkInterfaceMetricsArchive_ WHERE recordTime >= date() AND recordTime < dateAdd(date(), 1);

DELETE FROM networkInterfaceMetricsArchive_ WHERE recordTime >= timestamp() AND recordTime < timestampAdd(timestamp(), 1, 'days');

DELETE FROM networkInterfaceMetricsArchive_ WHERE dateOf(recordTime) = dateOf(now()); -- Deletes data from today

DELETE FROM networkInterfaceMetricsArchive_ WHERE deviceId=1 AND interfaceIndex=>0 AND interfaceIndex<=2000 AND recordTime >= '2024-10-24 00:00:00+0000';

cqlsh -e "DESCRIBE KEYSPACE nmtarchive" > NMT_Archive_Cassandra_DB_Structure.cql
