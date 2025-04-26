import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkDevices } from './networkdevices.component';

describe('NetworkDevicesComponent', () => {
  let component: NetworkDevices;
  let fixture: ComponentFixture<NetworkDevices>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NetworkDevices]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NetworkDevices);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
