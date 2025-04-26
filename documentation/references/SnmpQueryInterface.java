import org.snmp4j.Snmp;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.TransportMapping;
import org.snmp4j.CommunityTarget;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.PDU;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.event.ResponseEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SnmpQueryInterface {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        // System.out.print("Enter the IP address of the device: ");
        // String ipAddress = scanner.nextLine();
        String ipAddress = "172.17.48.2";

        String community = "public"; // SNMP community string

        // SNMP Communication Setup
        Address targetAddress = new UdpAddress(ipAddress + "/161");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1000);

        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = null;

        // OIDs for Performance Metrics
        final String IF_NAME_OID = ".1.3.6.1.2.1.2.2.1.2";  // Interface name (ifDescr)
        final String IN_TRAFFIC_OID = ".1.3.6.1.2.1.2.2.1.10"; // ifInOctets
        final String OUT_TRAFFIC_OID = ".1.3.6.1.2.1.2.2.1.16"; // ifOutOctets
        final String IN_ERRORS_OID = ".1.3.6.1.2.1.2.2.1.14";   // ifInErrors
        final String OUT_ERRORS_OID = ".1.3.6.1.2.1.2.2.1.20";  // ifOutErrors
        final String IN_DISCARDS_OID = ".1.3.6.1.2.1.2.2.1.13"; // ifInDiscards
        final String OUT_DISCARDS_OID = ".1.3.6.1.2.1.2.2.1.19"; // ifOutDiscards
        final String STATUS_OID = ".1.3.6.1.2.1.2.2.1.8";       // ifOperStatus

        try {
            snmp = new Snmp(transport);
            transport.listen();

            // Step 1: Fetch Interface Names
            Map<Integer, String> interfaceNames = fetchTable(snmp, target, IF_NAME_OID);

            // Step 2: Fetch Performance Metrics
            Map<Integer, Long> inTraffic = fetchTableAsLong(snmp, target, IN_TRAFFIC_OID);
            Map<Integer, Long> outTraffic = fetchTableAsLong(snmp, target, OUT_TRAFFIC_OID);
            Map<Integer, Long> inErrors = fetchTableAsLong(snmp, target, IN_ERRORS_OID);
            Map<Integer, Long> outErrors = fetchTableAsLong(snmp, target, OUT_ERRORS_OID);
            Map<Integer, Long> inDiscards = fetchTableAsLong(snmp, target, IN_DISCARDS_OID);
            Map<Integer, Long> outDiscards = fetchTableAsLong(snmp, target, OUT_DISCARDS_OID);
            Map<Integer, Integer> statuses = fetchTableAsInt(snmp, target, STATUS_OID);

            // Print Results
            int count = 0;
            //System.out.println("Interface Performance Details:");
            for (Integer index : interfaceNames.keySet()) {
                String name = interfaceNames.get(index);
                long inBps = inTraffic.getOrDefault(index, 0L); // Convert bytes to bits
                long outBps = outTraffic.getOrDefault(index, 0L); // Convert bytes to bits
                long totalTraffic = inBps + outBps;

                double errorPercentage = totalTraffic > 0
                        ? (inErrors.getOrDefault(index, 0L) + outErrors.getOrDefault(index, 0L)) * 100.0 / totalTraffic
                        : 0.0;

                double discardPercentage = totalTraffic > 0
                        ? (inDiscards.getOrDefault(index, 0L) + outDiscards.getOrDefault(index, 0L)) * 100.0 / totalTraffic
                        : 0.0;

                String status = getStatusDescription(statuses.getOrDefault(index, 2));

                System.out.printf("Interface Name: %s\nIn Traffic (bps): %d\nOut Traffic (bps): %d\n" +
                                "Errors (%%): %.2f\nDiscards (%%): %.2f\nOperational Status: %s\n\n",
                        name != null ? name : "Not Available",
                        inBps, outBps, errorPercentage, discardPercentage, status);
            }

        } catch (IOException e) {
            System.out.println("Error initializing SNMP: " + e.getMessage());
        } finally {
            if (snmp != null) {
                snmp.close();
            }
        }
    }

    private static Map<Integer, String> fetchTable(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        Map<Integer, String> table = new HashMap<>();
        OID rootOID = new OID(oid);
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(rootOID));
        pdu.setType(PDU.GETNEXT);

        int count = 0;
        while (true) {
            ResponseEvent responseEvent = snmp.getNext(pdu, target);
            PDU responsePDU = responseEvent.getResponse();
            if (responsePDU == null || responsePDU.getErrorStatus() != PDU.noError) {
                break;
            }

            VariableBinding vb = responsePDU.get(0);
            if (!vb.getOid().startsWith(rootOID)) {
                break;
            }

            int index = vb.getOid().last();
            table.put(index, vb.getVariable().toString());
            pdu.setRequestID(responsePDU.getRequestID());
            pdu.get(0).setOid(vb.getOid());
            if(count==30)
                break;
            count++;
        }

        return table;
    }

    private static Map<Integer, Long> fetchTableAsLong(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        Map<Integer, Long> table = new HashMap<>();
        Map<Integer, String> stringTable = fetchTable(snmp, target, oid);
        for (Map.Entry<Integer, String> entry : stringTable.entrySet()) {
            table.put(entry.getKey(), Long.parseLong(entry.getValue()));
        }
        return table;
    }

    private static Map<Integer, Integer> fetchTableAsInt(Snmp snmp, CommunityTarget target, String oid) throws IOException {
        Map<Integer, Integer> table = new HashMap<>();
        Map<Integer, String> stringTable = fetchTable(snmp, target, oid);
        for (Map.Entry<Integer, String> entry : stringTable.entrySet()) {
            table.put(entry.getKey(), Integer.parseInt(entry.getValue()));
        }
        return table;
    }

    private static String getStatusDescription(int status) {
        switch (status) {
            case 1:
                return "Up";
            case 2:
                return "Down";
            default:
                return "Trouble";
        }
    }
}
