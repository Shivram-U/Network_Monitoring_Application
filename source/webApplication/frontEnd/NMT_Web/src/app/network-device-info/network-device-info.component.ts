import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

interface DeviceData {
  deviceId: number;
  sysDescr: string;
  sysName: string;
  sysLocation: string;
  sysObjectId: string;
  ipAddress: string;
}

@Component({
  selector: 'app-network-device-info',
  templateUrl: './network-device-info.component.html',
  // styleUrls: ['./network-device-info.component.css'],
  imports: [FormsModule, CommonModule, RouterModule]
})

export class NetworkDeviceInfoComponent implements OnInit {
  deviceId: number | null = null;
  deviceData: DeviceData | null = null;

  constructor(private route: ActivatedRoute, private http: HttpClient) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.deviceId = Number(params.get('deviceId'));
      this.fetchDeviceData();
    });
  }

  fetchDeviceData(): void {
    console.log("Fetch started");
    if (this.deviceId !== null) {
      this.http.get<DeviceData>(`http://localhost:8091/NetworkDeviceMonitoringApplication/networkDevice?deviceId=${this.deviceId}`)
        .subscribe(data => {
          this.deviceData = data;
        }, error => {
          console.error('Failed to fetch device data', error);
          this.deviceData = null;  // Handle error state
        });
    }
  }
}