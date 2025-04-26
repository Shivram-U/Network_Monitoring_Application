import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDeviceInterfacesParameterAnalysisComponent } from './network-device-interfaces-parameter-analysis.component';

describe('NetworkDeviceInterfacesParameterAnalysisComponent', () => {
  let component: NetworkDeviceInterfacesParameterAnalysisComponent;
  let fixture: ComponentFixture<NetworkDeviceInterfacesParameterAnalysisComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NetworkDeviceInterfacesParameterAnalysisComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NetworkDeviceInterfacesParameterAnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
