import dataArchiveServices.*;
import networkMonitoringServices.*;

public class Main {
	public static void main(String[] args)
	{
		try
		{
			DataArchiveService dataArchvSrvc = new DataArchiveService();
			dataArchvSrvc.startService();
			SnmpQueryService snmpQrySrvc = new SnmpQueryService("172.17.48.2");
			snmpQrySrvc.startService();
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
		}
	}
}
