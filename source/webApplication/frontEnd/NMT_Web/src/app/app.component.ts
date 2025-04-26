import { Component, NgModule } from '@angular/core';
import { CommonModule } from '@angular/common'; 
import { RouterModule, RouterOutlet } from '@angular/router';
import { NetworkTrafficGraphComponent } from './network-traffic-graph/network-traffic-graph.component';
import { DeviceInterfacesTableComponent } from './device-interfaces-table/device-interfaces-table.component';
import { routes } from './app.routes';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NetworkTrafficGraphComponent,DeviceInterfacesTableComponent, CommonModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  providers: []
})
export class AppComponent {
  title = 'NMT_Web';
}
