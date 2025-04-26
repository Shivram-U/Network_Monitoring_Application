import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NetworkParameterAnalysisComponent } from './network-parameter-analysis.component';

describe('NetworkParameterAnalysisComponent', () => {
  let component: NetworkParameterAnalysisComponent;
  let fixture: ComponentFixture<NetworkParameterAnalysisComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NetworkParameterAnalysisComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NetworkParameterAnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
