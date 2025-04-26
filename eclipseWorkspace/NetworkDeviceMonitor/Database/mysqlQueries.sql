CREATE TABLE networkDevices(
        deviceId INT AUTO_INCREMENT, 
        sysName VARCHAR(100), 
        sysLocation VARCHAR(100), 
        sysObjectId VARCHAR(100), 
        sysDescr TEXT,
        PRIMARY KEY (deviceId)
);

CREATE TABLE networkDeviceInterfaces (
    deviceId INT,
    interfaceIndex INT,
    interfaceName VARCHAR(100),
    `inTraffic(bps)` BIGINT,
    `outTraffic(bps)` BIGINT,
    `discards(%)` DECIMAL(20,2),
    `errors(%)` DECIMAL(20,2),
    operationalStatus VARCHAR(100),
    recordTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId, interfaceIndex, recordTime),
    FOREIGN KEY (deviceId) REFERENCES networkDevices(deviceId)
);


DROP TABLE networkDevices;
DROP TABLE networkDeviceInterfaces;

TRUNCATE networkDevices;
TRUNCATE networkDeviceInterfaces;

Reset Procedure:

DROP TABLE networkDeviceInterfaces;
DROP TABLE networkDevices;

CREATE TABLE networkDevices(
        deviceId INT AUTO_INCREMENT, 
        sysName VARCHAR(100), 
        sysLocation VARCHAR(100), 
        sysObjectId VARCHAR(100), 
        sysDescr TEXT,
        PRIMARY KEY (deviceId)
);

CREATE TABLE networkDeviceInterfaces (
    deviceId INT,
    interfaceIndex INT,
    interfaceName VARCHAR(100),
    `inTraffic(bps)` BIGINT,
    `outTraffic(bps)` BIGINT,
    `discards(%)` DECIMAL(20,2),
    `errors(%)` DECIMAL(20,2),
    operationalStatus VARCHAR(100),
    recordTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (deviceId, interfaceIndex, recordTime),
    FOREIGN KEY (deviceId) REFERENCES networkDevices(deviceId)
);
