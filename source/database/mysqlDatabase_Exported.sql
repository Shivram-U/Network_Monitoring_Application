-- MySQL dump 10.13  Distrib 8.0.35, for Win64 (x86_64)
--
-- Host: localhost    Database: nmt
-- ------------------------------------------------------
-- Server version	8.0.35

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `monitoredipaddresses`
--

DROP TABLE IF EXISTS `monitoredipaddresses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `monitoredipaddresses` (
  `ipAddress` varchar(15) NOT NULL,
  `deviceId` int DEFAULT NULL,
  PRIMARY KEY (`ipAddress`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `networkdeviceinterfacedata`
--

DROP TABLE IF EXISTS `networkdeviceinterfacedata`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `networkdeviceinterfacedata` (
  `deviceId` int NOT NULL,
  `interfaceIndex` int NOT NULL,
  `interfaceName` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`deviceId`,`interfaceIndex`),
  CONSTRAINT `networkdeviceinterfacedata_ibfk_1` FOREIGN KEY (`deviceId`) REFERENCES `networkdevices` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `networkdeviceinterfaces`
--

DROP TABLE IF EXISTS `networkdeviceinterfaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `networkdeviceinterfaces` (
  `deviceId` int NOT NULL,
  `interfaceIndex` int NOT NULL,
  `inTraffic(bps)` bigint DEFAULT NULL,
  `outTraffic(bps)` bigint DEFAULT NULL,
  `discards(%)` decimal(20,2) DEFAULT NULL,
  `errors(%)` decimal(20,2) DEFAULT NULL,
  `operationalStatus` varchar(100) DEFAULT NULL,
  `recordTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`deviceId`,`interfaceIndex`,`recordTime`),
  CONSTRAINT `networkdeviceinterfaces_ibfk_1` FOREIGN KEY (`deviceId`, `interfaceIndex`) REFERENCES `networkdeviceinterfacedata` (`deviceId`, `interfaceIndex`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `networkdevices`
--

DROP TABLE IF EXISTS `networkdevices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `networkdevices` (
  `deviceId` int NOT NULL AUTO_INCREMENT,
  `sysName` varchar(100) DEFAULT NULL,
  `sysLocation` varchar(100) DEFAULT NULL,
  `sysObjectId` varchar(100) DEFAULT NULL,
  `sysDescr` text,
  `ipAddress` varchar(15) DEFAULT NULL,
  PRIMARY KEY (`deviceId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `suspendedinterfaces`
--

DROP TABLE IF EXISTS `suspendedinterfaces`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `suspendedinterfaces` (
  `ipAddress` varchar(15) DEFAULT NULL,
  `deviceId` int NOT NULL,
  `interfaceIndex` int NOT NULL,
  PRIMARY KEY (`deviceId`,`interfaceIndex`),
  KEY `ipAddress` (`ipAddress`),
  CONSTRAINT `suspendedinterfaces_ibfk_1` FOREIGN KEY (`ipAddress`) REFERENCES `monitoredipaddresses` (`ipAddress`),
  CONSTRAINT `suspendedinterfaces_ibfk_2` FOREIGN KEY (`deviceId`) REFERENCES `networkdevices` (`deviceId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-01-07 11:00:43
