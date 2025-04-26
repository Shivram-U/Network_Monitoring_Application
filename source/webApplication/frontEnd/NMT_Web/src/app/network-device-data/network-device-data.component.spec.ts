import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDeviceDataComponent } from './network-device-data.component';

describe('NetworkDeviceDataComponent', () => {
  let component: NetworkDeviceDataComponent;
  let fixture: ComponentFixture<NetworkDeviceDataComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NetworkDeviceDataComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NetworkDeviceDataComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
