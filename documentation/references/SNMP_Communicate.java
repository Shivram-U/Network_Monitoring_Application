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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class SNMP_Communicate {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the IP address of the device: ");
        String ipAddress = scanner.nextLine();

        String community = "public";  // Default community string

        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("1.3.6.1.2.1.1.7.0", "sysServices");
        metadata.put("1.3.6.1.2.1.1.6.0", "sysLocation");
        metadata.put("1.3.6.1.2.1.1.5.0", "sysName");
        metadata.put("1.3.6.1.2.1.1.4.0", "sysContact");
        metadata.put("1.3.6.1.2.1.1.3.0", "sysUpTime");
        metadata.put("1.3.6.1.2.1.1.2.0", "sysObjectID");
        metadata.put("1.3.6.1.2.1.1.1.0", "sysDescr");

        Address targetAddress = new UdpAddress(ipAddress + "/161");
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(community));
        target.setVersion(SnmpConstants.version2c);
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(1000);

        // Create SNMP session
        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = null;

        try {
            snmp = new Snmp(transport);
            transport.listen();

            // Create the PDU (Protocol Data Unit) for SNMP GET
            PDU pdu = new PDU();
            for (String oid : metadata.keySet()) {
                pdu.add(new VariableBinding(new OID(oid)));
            }
            pdu.setType(PDU.GET);

            // Send SNMP request
            ResponseEvent response = snmp.get(pdu, target);

            // Check response and print the result
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                for (int i = 0; i < responsePDU.size(); i++) {
                    VariableBinding vb = responsePDU.get(i);
                    String field = metadata.get(vb.getOid().toString());
                    String value = vb.getVariable().toString();
                    System.out.println(field + ":--> " + value);
                }
            } else {
                System.out.println("Request timed out or error occurred.");
            }
        } catch (IOException e) {
            System.out.println("Error initializing SNMP: " + e.getMessage());
        } finally {
            try {
                if (snmp != null) {
                    snmp.close();
                }
            } catch (IOException e) {
                System.out.println("Error closing SNMP session: " + e.getMessage());
            }
        }
    }
}
