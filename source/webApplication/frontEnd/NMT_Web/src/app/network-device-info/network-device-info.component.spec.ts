import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDeviceInfoComponent } from './network-device-info.component';

describe('NetworkDeviceInfoComponent', () => {
  let component: NetworkDeviceInfoComponent;
  let fixture: ComponentFixture<NetworkDeviceInfoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NetworkDeviceInfoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NetworkDeviceInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
