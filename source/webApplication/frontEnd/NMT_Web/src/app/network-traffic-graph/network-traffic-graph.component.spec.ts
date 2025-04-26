import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkTrafficGraphComponent } from './network-traffic-graph.component';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

describe('NetworkTrafficGraphComponent', () => {
  let component: NetworkTrafficGraphComponent;
  let fixture: ComponentFixture<NetworkTrafficGraphComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NetworkTrafficGraphComponent,FormsModule,CommonModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NetworkTrafficGraphComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
