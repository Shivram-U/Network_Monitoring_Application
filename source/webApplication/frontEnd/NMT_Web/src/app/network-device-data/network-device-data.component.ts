import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule,Router } from '@angular/router';

@Component({
  selector: 'app-network-device-data',
  templateUrl: './network-device-data.component.html',
  // styleUrl: './network-device-data.component.css',
  imports: [FormsModule, CommonModule, RouterModule],
})
export class NetworkDeviceDataComponent {
  deviceId: number | null = null;

  constructor( private route: ActivatedRoute,  private router: Router) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.deviceId = Number(params.get('deviceId'));
      console.log(this.deviceId);
    });
    // Navigate to the Network Device Info tab by default
    if (this.deviceId !== null) {
      this.router.navigate(['network-device-data',this.deviceId,'network-device-info',this.deviceId]);
    }
  }
}
