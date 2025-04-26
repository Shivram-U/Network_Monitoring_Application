import { Component, Inject, OnInit } from '@angular/core';
import { NetworkTrafficService } from '../services/NMT_API_Service/network-traffic.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { SortPipe } from './sort.pipe';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin, interval, of, Subscription, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

interface InterfaceDataRecord {
  "errors(%)": number;
  "operationalStatus": string;
  "inTraffic(bps)": number;
  "discards(%)": number;
  "outTraffic(bps)": number;
}

interface InterfaceResponse {
  [timestamp: string]: InterfaceDataRecord;
}

interface InterfaceMetricsResponse {
  [key: string]: any; // Adjust as necessary for metrics data
}

interface ApiResponse {
  [interfaceIndex: string]: {
    InterfaceData: InterfaceResponse;
    InterfaceMetrics?: InterfaceMetricsResponse;
  };
}


@Component({
  selector: 'app-device-interfaces-table',
  templateUrl: './device-interfaces-table.component.html',
  //styleUrls: ['./device-interfaces-table.component.css'],
  imports: [FormsModule, CommonModule, SortPipe, RouterModule],
})
export class DeviceInterfacesTableComponent implements OnInit {
  networkDeviceIds: number[] = [];
  interfaceData: any[] = [];
  deviceId: number | null = null;
  deviceMap: Map<number, number[]> = new Map();
  sortField: string = '';
  sortOrder: string = ''; // true for ascending, false for descending
  private refreshIntervalId: any;
  private refreshSubscription: Subscription | null = null;
  private isSchedulerRunning: boolean = false;
  suspendedInterfaceIndices: number[] = [];
  monitoredInterfaces: any[] = [];
  suspendedInterfaces: any[] = [];
  currentView: 'monitored' | 'suspended' = 'monitored';
  timeRange: number = 0;

  // Units
  trafficUnits: string[] = ['bps', 'Bps', 'KBps', 'MBps', 'GBps', 'TBps'];
  inTrafficUnit: string = 'bps';
  outTrafficUnit: string = 'bps';

  constructor(private networkTrafficService: NetworkTrafficService, 
              private route: ActivatedRoute,
              @Inject(PLATFORM_ID) private platformId: Object ) {}

  getInterfaceValues(interfaces: any): any[] {
    return Object.values(interfaces);
  }

  setView(view: 'monitored' | 'suspended') {
    this.currentView = view;
  }
  
  filterInterfaces() {
  
    for (const key in this.interfaceData) {
      const interfaceData = this.interfaceData[key];
      console.log(key,this.suspendedInterfaceIndices.includes(interfaceData.InterfaceIndex));
      if (!this.suspendedInterfaceIndices.includes(interfaceData.InterfaceIndex)) {
        this.monitoredInterfaces[interfaceData.InterfaceIndex] = interfaceData;
      } else {
        this.suspendedInterfaces[interfaceData.InterfaceIndex] = interfaceData;
      }
    }
    console.log(this.suspendedInterfaceIndices);
    console.log(this.suspendedInterfaces);
  }
  
              
  ngOnInit(): void {
    
    // Fetch all device IDs on initialization
    this.networkTrafficService.getNetworkDevicesAndInterfaces().subscribe(deviceMap => {
      this.deviceMap = deviceMap;
      this.networkDeviceIds = Array.from(deviceMap.keys());
    });
    this.route.paramMap.subscribe(params => {
      this.deviceId = Number(params.get('deviceId'));
      if (!isNaN(this.deviceId)) {
        this.fetchAllLatestInterfaceData(this.deviceId);
        // console.log("refresh schedule");
        if (isPlatformBrowser(this.platformId)) { // to avoid Server-side rendering problems
          // console.log("Running in the browser, starting scheduling");
          this.scheduleRefresh();
        }
      }
    });
  }

  updateData() {
    const toTime = new Date();
    const fromTime = new Date(toTime);
  
    if (this.timeRange !== 0) {
      fromTime.setHours(toTime.getHours() - this.timeRange);
    }

    if (this.timeRange === 0) {
      if (!isNaN(Number(this.deviceId))) {
        this.fetchAllLatestInterfaceData(Number(this.deviceId));
      }
    } else {
      // Fetch the data for the specified time range
      this.networkTrafficService.getAllNetworkInterfacesDataWithinInterval(this.deviceId!, fromTime, toTime).subscribe(
        (response) => {
          const monitoredInterfaceIndices: string[] = Object.keys(this.monitoredInterfaces);
          // Iterate over each interface
          const deviceIndices = this.deviceMap.get(Number(this.deviceId));

          if (deviceIndices) {
            for (const interfaceIndex of deviceIndices) {
              // Your iteration logic here
              const paramInterfaceData = response[interfaceIndex]?.InterfaceData;
              if (paramInterfaceData) {
                // Calculate averages and directly update interfaceData
                if(this.monitoredInterfaces[interfaceIndex])
                {
                  this.monitoredInterfaces[interfaceIndex]["InterfaceData"] = this.calculateAverages(paramInterfaceData);
                }
              }
            }
          }
        },
        (error) => console.error(error)
      );
    }
  }
  
  private calculateAverages(interfaceData: any[]) {
    let totalInTraffic = 0;
    let totalOutTraffic = 0;
    let totalErrors = 0;
    let totalDiscards = 0;
    let count = 0;
  
    for (const timestamp in interfaceData) {
      if (interfaceData.hasOwnProperty(timestamp)) {
        const record = interfaceData[timestamp];
        totalInTraffic += record["inTraffic(bps)"] || 0;
        totalOutTraffic += record["outTraffic(bps)"] || 0;
        totalErrors += record["errors(%)"] || 0;
        totalDiscards += record["discards(%)"] || 0;
        count++;
      }
    }
  
    return { 
                "inTraffic(bps)": count ? totalInTraffic / count : 0,
                "outTraffic(bps)": count ? totalOutTraffic / count : 0,
                "errors(%)": count ? totalErrors / count : 0,
                "discards(%)": count ? totalDiscards / count : 0,
   
          };
  }
  

  private scheduleRefresh(): void {
    // Calculate the time until the next minute starts
    const now = new Date();
    const msToNextMinute = (60 - now.getSeconds()) * 1000 - now.getMilliseconds();
  
    timer(msToNextMinute, 60000) // Start after the delay, then every 60 seconds
      .subscribe(() => {
        this.updateData(); // Call updateData() at the start of each minute
      });
  }
  
  

  fetchsuspendedInterfaceIndices(): void {
      this.networkTrafficService.getSuspendedInterfaces(this.deviceId).subscribe(response => {
          this.suspendedInterfaceIndices = response.InterfaceIndices || [];
          this.filterInterfaces();
      });
  }
  fetchAllLatestInterfaceData(deviceId: number): void {
    this.networkTrafficService.getAllNetworkInterfacesLatestData(deviceId).subscribe(response => {
      const interfaces = response?.Interfaces || {};
      const formattedInterfaceData: any[] = [];
  
      for (const key in interfaces) {
        const interfaceIndex = interfaces[key]?.InterfaceIndex;
        if (interfaceIndex !== undefined) {
          formattedInterfaceData[interfaceIndex] = interfaces[key];
        } else {
          console.warn(`Interface data missing InterfaceIndex for key: ${key}`);
        }
      }
  
      console.log(this.interfaceData);
      this.interfaceData = formattedInterfaceData;
      console.log(this.interfaceData);
      this.fetchsuspendedInterfaceIndices();
    });
  }
  

  onSort(field: string, sortOrd : string): void {
    this.sortField = field;
    this.sortOrder = sortOrd;
  }

  autoConvertTraffic(value: number): string {
    var returnValue = 0.0;
    var returnUnit = '';
    if (value >= 8_000_000_000_000) {
      returnValue = value / 8_000_000_000_000;
      returnUnit = 'TBps';
    } 
    if (value >= 8_000_000_000) {
      returnValue = value / 8_000_000_000;
      returnUnit = 'GBps';
    } else if (value >= 8_000_000) {
      returnValue = value / 8_000_000;
      returnUnit = 'MBps';
    } else if (value >= 8_000) {
      returnValue = value / 8_000;
      returnUnit = 'KBps';
    } else if (value >= 8) {
      returnValue = value / 8;
      returnUnit = 'Bps';
    } else {
      returnValue = value;
      returnUnit = 'bps';
    }
    return `${returnValue.toFixed(2)} ${returnUnit}`; // Default
}


  updateTrafficUnits(type: string): void {
    // console.log("Units update");
    if (type === 'inTraffic') {
      this.interfaceData = [...this.interfaceData]; // Trigger change detection
    } else if (type === 'outTraffic') {
      this.interfaceData = [...this.interfaceData]; // Trigger change detection
    }
  }

  ngOnDestroy(): void {
    // Unsubscribe from the refresh subscription to avoid memory leaks
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  toggleMenu(paramInterface: any, event: Event): void {
    // Close other open menus
    this.interfaceData.forEach(item => {
      if (item !== paramInterface) {
        item.showMenu = false;
      }
    });
  
    // Toggle menu for the clicked button
    paramInterface.showMenu = !paramInterface.showMenu;
  
    // Prevent click from propagating and closing menu immediately
    event.stopPropagation();
  }


  changeInterfaceName(paramInterface: any): void {

    // Show a dialog box to enter the new interface name
    const newName = prompt('Enter new interface name:', paramInterface.InterfaceName);

    if (newName && newName.trim() !== '') {
      // Use the new NetworkTrafficService method to update the interface name
      this.networkTrafficService.updateNetworkInterfaceName(this.deviceId, paramInterface.InterfaceIndex, newName.trim()).subscribe(
        response => {
          console.log('Interface name updated successfully', response);

          // On success, update the interface name in the local data structure
          paramInterface.InterfaceName = newName.trim(); // Assuming InterfaceIndex should be updated, update as necessary
          paramInterface.showMenu = false; // Close the menu
        },
        error => {
          console.error('Failed to update interface name', error);
        }
      );
    } else {
      console.log('No valid name entered.');
      paramInterface.showMenu = false;
    }
  }

  

  suspendMonitoring(paramInterface: any) {
    console.log(paramInterface);
    paramInterface.showMenu = false;

    // Call the service method
    this.networkTrafficService.suspendInterfaceService(this.deviceId, paramInterface.InterfaceIndex)
        .subscribe(
            response => {
                console.log(response);
                // Move the interface from monitored to suspended
                this.monitoredInterfaces = this.monitoredInterfaces.filter(
                    (iface) => iface.InterfaceIndex !== paramInterface.InterfaceIndex
                );
                this.suspendedInterfaces.push(paramInterface);
            },
            error => {
                console.error(error);
                // Handle error
            }
        );
  }

  enableMonitoring(paramInterface: any) {
    console.log(paramInterface);
    paramInterface.showMenu = false;

    this.networkTrafficService.removeSuspendedInterface(this.deviceId, paramInterface.InterfaceIndex)
        .subscribe(
            response => {
                console.log(response.responseMessage);
                // Move the interface from suspended to monitored
                this.suspendedInterfaces = this.suspendedInterfaces.filter(
                    (iface) => iface.InterfaceIndex !== paramInterface.InterfaceIndex
                );
                this.monitoredInterfaces.push(paramInterface);
            },
            error => {
                console.error(error);
                // Handle error
            }
        );
  }


}