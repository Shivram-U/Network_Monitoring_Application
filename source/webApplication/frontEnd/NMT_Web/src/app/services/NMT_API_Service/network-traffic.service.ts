import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class NetworkTrafficService {
  private nameChangeApiUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/updateNetworkInterfaceName'; 
  private interfacesDataUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/networkInterfaces';
  private interfacesDataIntervalUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/networkInterfacesWithinInterval';
  private latestInterfacesUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/networkDevicesLatestInterfacesData';
  private latestInterfaceUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/networkLatestInterface'; // Replace with your API URL
  private apiUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/networkInterface'; // Replace with your API URL
  private devicesAndInterfacesUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/getNetworkDevicesAndInterfaces'; // Endpoint for device IDs and interface indices

  constructor(private http: HttpClient) {}

  // Fetch network data based on deviceId and interfaceIndex
  getNetworkData(deviceId: number, interfaceIndex: number): Observable<any> {
    return this.http.get(`${this.apiUrl}?deviceId=${deviceId}&interfaceIndex=${interfaceIndex}`);
  }


  getNetworkInterfaceLatestData(deviceId: number, interfaceIndex: number): Observable<any> {
    return this.http.get(`${this.latestInterfaceUrl}?deviceId=${deviceId}&interfaceIndex=${interfaceIndex}`);
  }

   // Fetch all latest interface data for a given device
  getAllNetworkInterfacesLatestData(deviceId: number): Observable<any> {
    return this.http.get(`${this.latestInterfacesUrl}?deviceId=${deviceId}`);
  }
  
  
  getAllNetworkInterfacesData(deviceId: number): Observable<any> {
    return this.http.get(`${this.interfacesDataUrl}?deviceId=${deviceId}`);
  }

  updateNetworkInterfaceName(deviceId: number | null, interfaceIndex: number, newName: string): Observable<any> {
    return this.http.get(`${this.nameChangeApiUrl}?deviceId=${deviceId}&interfaceIndex=${interfaceIndex}&interfaceName=${newName}`);
  }

    // Example Service Method
  suspendInterfaceService(deviceId: number | null, interfaceIndex: number): Observable<any> {
    return this.http.get(`http://localhost:8091/NetworkDeviceMonitoringApplication/addSuspendedInterface?deviceId=${deviceId}&interfaceIndex=${interfaceIndex}`);
  }

  removeSuspendedInterface(deviceId: number | null, interfaceIndex: number): Observable<any> {
    return this.http.get(`http://localhost:8091/NetworkDeviceMonitoringApplication/removeSuspendedInterface?deviceId=${deviceId}&interfaceIndex=${interfaceIndex}`);
  }

  getSuspendedInterfaces(deviceId: number | null): Observable<any> {
    return this.http.get<any>(`http://localhost:8091/NetworkDeviceMonitoringApplication/getSuspendedInterfacesDataByDeviceId?deviceId=${deviceId}`);
  }

  toCustomISOString(date : Date) : string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // Months are 0-indexed
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
  
    // Return formatted string in ISO format without changing time
    return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}Z`;
  }

  getAllNetworkInterfacesDataWithinInterval(deviceId: number, fromTime : Date | null, toTime : Date | null): Observable<any> {
    if(fromTime!=null)
    {
      if(toTime!=null)
      {
        var uri = `${this.interfacesDataIntervalUrl}?deviceId=${deviceId}&fromTime=${this.toCustomISOString(fromTime)}&toTime=${this.toCustomISOString(toTime)}`;
        console.log(uri);
        return this.http.get(uri);
      }
      else
      {
        return this.http.get(`${this.interfacesDataIntervalUrl}?deviceId=${deviceId}&fromTime=${this.toCustomISOString(fromTime)}`);
      }
    }
    else 
    {
      if(toTime!=null)
      {
        return this.http.get(`${this.interfacesDataIntervalUrl}?deviceId=${deviceId}&toTime=${this.toCustomISOString(toTime)}`);
      }
      else
      {
        return this.http.get(`${this.interfacesDataIntervalUrl}?deviceId=${deviceId}`);
      }
    }
  }




  // Fetch available network device IDs and interface indices
  getNetworkDevicesAndInterfaces(): Observable<Map<number , number[]>> {
    return this.http.get<Record<string, number[]>>(this.devicesAndInterfacesUrl) // Record<string, number[]> implies a mapping from string to number[]
      .pipe(
        map(response => {
          const deviceMap = new Map<number, number[]>();
          Object.keys(response).forEach(key => {
            const numberKey = Number(key);  // Explicit conversion to number
            deviceMap.set(numberKey, response[key]);
          });
          return deviceMap;
        })
      );
  }
}
