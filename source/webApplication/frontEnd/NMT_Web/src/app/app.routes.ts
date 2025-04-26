import { RouterModule, Routes } from '@angular/router';
import { NetworkParameterAnalysisComponent } from './network-parameter-analysis/network-parameter-analysis.component';
import { NgModule } from '@angular/core';
import { DeviceInterfacesTableComponent } from './device-interfaces-table/device-interfaces-table.component';
import { AppComponent } from './app.component';
import { NetworkDevices } from './networkdevices/networkdevices.component';
import { NetworkDeviceDataComponent } from './network-device-data/network-device-data.component';
import { NetworkDeviceInfoComponent } from './network-device-info/network-device-info.component';
import { NetworkDeviceInterfacesParameterAnalysisComponent } from './network-device-interfaces-parameter-analysis/network-device-interfaces-parameter-analysis.component';

export const routes: Routes = [
  { path: '', component: NetworkDevices }, // Default route
  {
    path: 'network-device-data/:deviceId',
    component: NetworkDeviceDataComponent, // Parent component with navbar
    children: [
      {
        path: 'network-device-interfaces/:deviceId',
        component: DeviceInterfacesTableComponent, // Child route
      },
      {
        path: 'network-device-info/:deviceId',
        component: NetworkDeviceInfoComponent,
      }
    ],
  },
  {
    path: 'network-parameter-analysis/:deviceId/:interfaceIndex/:parameterType',
    component: NetworkParameterAnalysisComponent,
  },
  {
    path: 'network-parameter-analysis/:deviceId/:parameterType',
    component: NetworkDeviceInterfacesParameterAnalysisComponent,
  },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
