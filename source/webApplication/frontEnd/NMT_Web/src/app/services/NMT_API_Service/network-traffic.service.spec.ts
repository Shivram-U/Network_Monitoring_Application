// src/app/network-traffic.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class NetworkTrafficService {
  private apiUrl = 'http://localhost:8091/NetworkDeviceMonitoringApplication/networkInterface?deviceId=1&interfaceIndex=1'; // Replace with your API URL

  constructor(private http: HttpClient) {}

  getNetworkData(): Observable<any> {
    return this.http.get(this.apiUrl);
  }
}
