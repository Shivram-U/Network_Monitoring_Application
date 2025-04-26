package NMT;

import NMT.DataArchiveService;
import NMT.SnmpQueryService;

public class NMT {
    public static void main(String[] args)
    {
        SnmpQueryService_LAtest snmpService = new SnmpQueryService_LAtest();
        snmpService.startService();
        //DataArchiveService archiveService = new DataArchiveService();
        //archiveService.startService(); 
    }    
}
