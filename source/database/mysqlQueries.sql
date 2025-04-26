create table monitoredIPAddresses(
    ipAddress varchar(15) PRIMARY KEY,
    deviceId INT
);

CREATE TABLE networkDeviceInterfaceData (
    deviceId INT,
    interfaceIndex INT,
    interfaceName VARCHAR(100),
    PRIMARY KEY (deviceId,interfaceIndex),
    FOREIGN KEY (deviceId) REFERENCES networkDevices(deviceId)
);

CREATE TABLE networkDeviceInterfaces (
    deviceId INT,
    interfaceIndex INT,
    `inTraffic(bps)` BIGINT,
    `outTraffic(bps)` BIGINT,
    `discards(%)` DECIMAL(20,2),
    `errors(%)` DECIMAL(20,2),
    operationalStatus VARCHAR(100),
    recordTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId, interfaceIndex, recordTime),
    FOREIGN KEY (deviceId,interfaceIndex) REFERENCES networkDeviceInterfaceData(deviceId,interfaceIndex)
);

CREATE TABLE networkDevices(
        ipAddress varchar(15),
        deviceId INT AUTO_INCREMENT, 
        sysName VARCHAR(100), 
        sysLocation VARCHAR(100), 
        sysObjectId VARCHAR(100), 
        sysDescr TEXT,
        PRIMARY KEY (deviceId)
);

create table suspendedInterfaces(
    ipAddress varchar(15),
    deviceId INT,
    interfaceIndex INT,
    PRIMARY KEY (deviceId, interfaceIndex),
    FOREIGN KEY (ipAddress) REFERENCES monitoredIPAddresses(ipAddress),
    FOREIGN KEY (deviceId) REFERENCES networkDevices(deviceId)
);

DROP TABLE networkDevices;
DROP TABLE networkDeviceInterfaces;

TRUNCATE networkDevices;
TRUNCATE networkDeviceInterfaces;

Reset Procedure:

DROP TABLE networkDeviceInterfaces;
DROP TABLE networkDeviceInterfaceData;
DROP TABLE networkDevices;




mysqldump -u root -p --no-data nmt > NMT_Database_Structure.sql
mysqldump -u root -p nmt > NMT_Database_Structure_With_Data.sql
