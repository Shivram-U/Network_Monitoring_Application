import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DeviceInterfacesTableComponent } from './device-interfaces-table.component';

describe('DeviceInterfacesTableComponent', () => {
  let component: DeviceInterfacesTableComponent;
  let fixture: ComponentFixture<DeviceInterfacesTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DeviceInterfacesTableComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DeviceInterfacesTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
