import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';

interface NetworkDevice {
  DeviceId: number;
  sysDescr: string;
  sysName: string;
  sysLocation: string;
  sysObjectId: string;
}

@Component({
  selector: 'app-networkdevices',
  templateUrl: './networkdevices.component.html',
  //styleUrls: ['./networkdevices.component.css'],
  imports: [FormsModule, CommonModule, RouterModule]
})
export class NetworkDevices implements OnInit {
  networkDevices: Map<string, NetworkDevice> = new Map();
  newIPAddress: string = '';
  monitoredIPs: { ipAddress: string; deviceId: string | unknown }[] = [];

  get networkDevicesArray(): NetworkDevice[] {
    return Array.from(this.networkDevices.values());
  }  

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.fetchNetworkDevices();
    this.fetchMonitoredIPs();
  }

  fetchNetworkDevices(): void {
    this.http.get<{ [key: string]: NetworkDevice }>('http://127.0.0.1:8091/NetworkDeviceMonitoringApplication/networkDevices')
      .subscribe(data => {
        Object.entries(data).forEach(([key, value]) => {
          value['DeviceId'] = +key; // Assuming DeviceId needs to be treated as a number
          this.networkDevices.set(key, value);
        });
      });
  }

  fetchMonitoredIPs(): void {
    console.log("IPS fetched");
    this.http.get('http://127.0.0.1:8091/NMT/NetworkDevices', { responseType: 'json' })
    .subscribe((response: any) => {
      // Use Map to store IPs and Device IDs
     

      this.monitoredIPs = Object.entries(response).map(([ip, deviceId]) => ({
        ipAddress: ip,
        deviceId: deviceId === "" ? "" : deviceId,
      }));

      console.log("Monitored IPs stored in Map:", this.monitoredIPs);

      // Use monitoredIPs as needed in your application
    }, error => {
      console.error("Error fetching monitored IPs:", error);
    });
  }

  addIPAddress(): void {
    console.log("create"+this.newIPAddress);
    if (this.newIPAddress) {
      console.log("create");
      this.http.post(`http://127.0.0.1:8091/NMT/NetworkDevices?ipAddress=${this.newIPAddress}`, {}, { responseType: 'text' })
        .subscribe(() => {
          this.fetchMonitoredIPs();
        });
    }
  }

  deleteIPAddress(ipAddress: string): void {
    console.log("delete");
    this.http.delete(`http://localhost:8091/NMT/NetworkDevices?ipAddress=${ipAddress}`,{ responseType: 'text' })
      .subscribe(() => {
        this.fetchMonitoredIPs();
      });
  }
}
