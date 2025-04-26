import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; // Ensure this is included
import { routes } from './app.routes';
import { provideClientHydration, withEventReplay } from '@angular/platform-browser';
import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { HttpClientModule, withFetch } from '@angular/common/http';  // Required for HttpClient
import { NetworkTrafficService } from './services/NMT_API_Service/network-traffic.service';  // Your service
import { provideHttpClient } from '@angular/common/http';
import { NO_ERRORS_SCHEMA } from '@angular/core';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(withFetch()) 
  ]
};

@NgModule({
  declarations: [ // Declare the component
  ],
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    HttpClientModule  // Ensure HttpClientModule is imported
  ],
  schemas: [NO_ERRORS_SCHEMA],
  providers: [NetworkTrafficService],  // Providing the service
})
export class AppModule { }