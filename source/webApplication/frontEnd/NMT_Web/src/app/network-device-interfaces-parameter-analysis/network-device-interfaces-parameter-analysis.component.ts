import { Component, OnInit, ViewChild, ElementRef, Inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NetworkTrafficService } from '../services/NMT_API_Service/network-traffic.service';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

import * as d3 from 'd3';

@Component({
  selector: 'app-network-device-interfaces-parameter-analysis',
  templateUrl: './network-device-interfaces-parameter-analysis.component.html',
  styleUrls: ['./network-device-interfaces-parameter-analysis.component.css'],
  imports:[FormsModule,CommonModule],
})
export class NetworkDeviceInterfacesParameterAnalysisComponent implements OnInit {
  @ViewChild('chart') private chartContainer!: ElementRef;

  deviceId: number = 0;
  parameterType: string = '';
  fromTime: string = ''; // Time inputs as strings
  toTime: string = '';
  paramType : string = '';
  title : string = '';
  yaxisLabel : string = '';
  interfaceData: { [key: string]: any } = {};
  interfaceKeys: string[] = [];
  processedData: { [key: string]: { data: { timestamp: Date; value: number }[] } } = {};
  xScale!: d3.ScaleTime<number, number>;
  yScale!: d3.ScaleLinear<number, number>;
  svg!: d3.Selection<SVGGElement, unknown, HTMLElement, any>;
  lineGenerator!: d3.Line<{ timestamp: Date; value: number }>;
  colorMap: { [key: string]: string } = {};
  private enabledKeys = new Set<string>();
  metricKey : string = "";
  maxValue : number = 0;

  constructor(
    private networkTrafficService: NetworkTrafficService,
    private route: ActivatedRoute,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  isChecked(key: string): boolean {
    return this.enabledKeys.has(key);
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.parameterType = params.get('parameterType') || '';
      this.setParameterDetails();
      this.deviceId = Number(params.get('deviceId'));
      if (isPlatformBrowser(this.platformId)) {
        this.fetchNetworkData();
      }
    });
  }

  setParameterDetails(): void {
    if (this.parameterType === "in-Traffic") {
      this.paramType = 'inTraffic(bps)';
      this.title = "Incoming Traffic";
    } else if (this.parameterType === "out-Traffic") {
      this.paramType = 'outTraffic(bps)';
      this.title = "Outgoing Traffic";
    } else if (this.parameterType === "errors") {
      this.paramType = 'errors(%)';
      this.title = "Errors";
    } else if (this.parameterType === "discards") {
      this.paramType = 'discards(%)';
      this.title = "Discards";
    }
  }

  updateChart() {
    // Only update the chart if both fromTime and toTime are provided
    this.fetchNetworkData();
}
  assignColorsToInterfaces(): void {
    // Assign random colors to interfaces
    this.interfaceKeys.forEach(key => {
      const randomColor = `#${Math.floor(Math.random() * 16777215).toString(16)}`;
      this.colorMap[key] = randomColor;
      // console.log(randomColor);
    });
  }

  fetchNetworkData(): void {
    if (this.deviceId !== null) {
      const fromTime = this.fromTime ? new Date(this.fromTime) : null;
      const toTime = this.toTime ? new Date(this.toTime) : null;

      this.networkTrafficService.getAllNetworkInterfacesDataWithinInterval(this.deviceId,fromTime,toTime).subscribe(
        response => {
          this.processData(response);
        },
        error => console.error('Error fetching network data:', error)
      );
    } else {
      console.error('Device ID is required.');
    }
  }

  processData(response: any): void {
    const allTimestamps: Date[] = [];
    const allValues: number[] = [];
    const paramKey = this.getParameterKey();

    this.interfaceKeys = [];
    this.enabledKeys.clear();

    for (const key in response) {
        this.interfaceKeys.push(`${key}-${response[key]?.interfaceName}`);
        if (response[key]?.InterfaceData) {
            let data = Object.entries(response[key].InterfaceData).map(([timestamp, valueObj]: [string, any]) => ({
                timestamp: new Date(new Date(timestamp).setSeconds(0, 0)),
                value: valueObj[paramKey] || 0,
            }));

            // Sort data by timestamp
            data.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());

            // Fill missing minutes
            const filledData: { timestamp: Date; value: number }[] = [];
            if (data.length > 0) {
                const startTime = data[0].timestamp.getTime();
                const endTime = data[data.length - 1].timestamp.getTime();

                for (let time = startTime; time <= endTime; time += 60 * 1000) { // Increment by 1 minute
                    const existingData = data.find(d => d.timestamp.getTime() === time);
                    filledData.push(existingData || { timestamp: new Date(time), value: 0 });
                }
            }

            this.processedData[key] = { data: filledData };
            allTimestamps.push(...filledData.map(d => d.timestamp));
            allValues.push(...filledData.map(d => d.value));
        }
    }

    this.assignColorsToInterfaces();
    // Precompute scales
    const [minTimestamp, maxTimestamp] = d3.extent(allTimestamps) as [Date, Date];
    this.maxValue = d3.max(allValues) || 0;

    this.xScale = d3.scaleTime().domain([minTimestamp, maxTimestamp]).range([0, 1100]);

    if (this.parameterType === "in-Traffic" || this.parameterType === "out-Traffic") {
        this.convertStoredDataUnit();
    } else {
        this.yScale = d3.scaleLinear().domain([0, this.maxValue * 2]).range([500, 0]);
        this.yaxisLabel = `${this.title} (%)`;
        this.lineGenerator = d3.line<{ timestamp: Date; value: number }>()
        .x(d => this.xScale(d.timestamp))
        .y(d => this.yScale(d.value))
        .curve(d3.curveMonotoneX);
    }

    this.initializeChart();
  }

  convertStoredDataUnit(): void {
    // Get the appropriate unit and divisor based on the max value
    const { unit, divisor } = this.getTrafficUnitAndDivisor(this.maxValue);
    // console.log(unit,divisor);
    this.yScale = d3.scaleLinear().domain([0, (this.maxValue / divisor) * 2]).range([500, 0]);
    // Update y-axis label to reflect the unit
    this.yaxisLabel = `${this.title} (${unit})`;

    console.log(this.yaxisLabel);
  
    // Convert all values in processedData to the selected unit
    for (const key in this.processedData) {
      this.processedData[key].data = this.processedData[key].data.map(d => ({
        ...d,
        value: d.value / divisor,
      }));
    }
    this.lineGenerator = d3.line<{ timestamp: Date; value: number }>()
      .x(d => this.xScale(d.timestamp))
      .y(d => this.yScale(d.value))
      .curve(d3.curveMonotoneX);
  }
  

  // Helper function to determine the unit and divisor
  private getTrafficUnitAndDivisor(value: number): { unit: string, divisor: number } {
    if (value >= 8_000_000_000_000) {
      return { unit: 'TBps', divisor: 8_000_000_000_000 };
    } else if (value >= 8_000_000_000) {
      return { unit: 'GBps', divisor: 8_000_000_000 };
    } else if (value >= 8_000_000) {
      return { unit: 'MBps', divisor: 8_000_000 };
    } else if (value >= 8_000) {
      return { unit: 'KBps', divisor: 8_000 };
    } else if (value >= 8) {
      return { unit: 'Bps', divisor: 8 };
    } else {
      return { unit: 'bps', divisor: 1 }; // No conversion needed
    }
  }

  initializeChart(): void {
    const element = this.chartContainer.nativeElement;

    // Clear previous SVG if exists
    d3.select(element).select('svg').remove();


    // Create SVG
    const margin = { top: 100, right: 100, bottom: 100, left: 100 };
    var width = 1300 - margin.left - margin.right;
    var height = 700 - margin.top - margin.bottom;
  
    // Determine parameter type
  
    // Clear any existing SVG
    d3.select(element).select('svg').remove();

      
    this.svg = d3.select<HTMLElement, unknown>(element)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);
    // console.log(this.svg);

    // Add axes
    this.svg.append('g')
      .attr('class', 'x-axis')
      .attr('transform', `translate(0,${height})`)
      .call(d3.axisBottom(this.xScale));

      
    this.svg.append('g')
      .attr('class', 'y-axis')
      .call(d3.axisLeft(this.yScale));

    this.svg.append('text')
          .attr('x', width / 2)
          .attr('y', height + margin.bottom - 40)
          .attr('text-anchor', 'middle')
          .style('font-size', '15px')
          .text('Time');
    this.svg.append('text')
          .attr('transform', 'rotate(-90)')
          .attr('y', -margin.left + 15)
          .attr('x', -height / 2)
          .style('font-size', '15px')
          .attr('text-anchor', 'middle')
          .text(this.yaxisLabel);
        
    this.svg.append('text')
          .attr('x', width / 2)
          .attr('y', -margin.top / 2)
          .attr('text-anchor', 'middle')
          .style('font-size', '16px')
          .text(this.title);
  }

  toggleInterface(interfaceName: string): void {
    var isChecked;
    var interfaceKey = interfaceName.split("-")[0];
    if (this.enabledKeys.has(interfaceName)) {
      isChecked = false;
      this.enabledKeys.delete(interfaceName);
    } else {
      isChecked = true;
      this.enabledKeys.add(interfaceName);
    }
    if (isChecked) {
      
      // Add line
      // console.log("added");
      // console.log(this.processedData[interfaceKey].data);

      this.svg = d3.select('svg');
      this.svg.append("path")
        .datum(this.processedData[interfaceKey].data)
        .attr('class', `line-${interfaceKey}`)
        .attr('fill', 'none')
        .attr('stroke', this.colorMap[interfaceName])
        .attr('stroke-width', 2)
        .attr('transform', 'translate(100,100)')
        .attr('d', this.lineGenerator);
    } else {
      // Remove line
      this.svg.select(`.line-${interfaceKey}`).remove();
    }
  }

  getParameterKey(): string {
    switch (this.parameterType) {
      case 'in-Traffic': return 'inTraffic(bps)';
      case 'out-Traffic': return 'outTraffic(bps)';
      case 'errors': return 'errors(%)';
      case 'discards': return 'discards(%)';
      default: return '';
    }
  }
}

/*

import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NetworkTrafficService } from '../services/NMT_API_Service/network-traffic.service';
import * as d3 from 'd3';

@Component({
  selector: 'app-network-device-interfaces-parameter-analysis',
  templateUrl: './network-device-interfaces-parameter-analysis.component.html',
  styleUrls: ['./network-device-interfaces-parameter-analysis.component.css'],
})
export class NetworkDeviceInterfacesParameterAnalysisComponent implements OnInit {
  @ViewChild('chart') private chartContainer!: ElementRef;

  deviceId: number = 0;
  parameterType: string = '';
  interfaceData: { [key: string]: any } = {};
  interfaceKeys: string[] = [];
  timestamps: string[] = [];
  maxValue: number = 0;
  minValue: number = Infinity;
  minTime: Date | null = null;
  maxTime: Date | null = null;

  constructor(
    private networkTrafficService: NetworkTrafficService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.parameterType = params.get('parameterType') || '';
      this.deviceId = Number(params.get('deviceId'));
      this.fetchNetworkData();
    });
  }

  fetchNetworkData(): void {
    if (this.deviceId !== null) {
      this.networkTrafficService.getAllNetworkInterfacesData(this.deviceId).subscribe(
        response => {
          for (const key in response) {
            if (!this.interfaceData[key]) {
              this.interfaceData[key] = {};
            }
            if (response[key]?.InterfaceData) {
              const transformedData: Map<Date, any> = new Map();
  
              for (const timestamp in response[key].InterfaceData) {
                // Convert the timestamp key to a Date object and map it to the same data
                const dateKey = new Date(new Date(timestamp).setSeconds(0,0));
                transformedData.set(dateKey, response[key].InterfaceData[timestamp]);
              }
              // Replace the original InterfaceData with the transformed one
              this.interfaceData[key]["InterfaceData"] = transformedData;
            }
          }

          if(this.parameterType === "in-Traffic") {
            this.createInTrafficChart();
        } else if(this.parameterType === 'out-Traffic') {
            this.createOutTrafficChart();
        } else if(this.parameterType === 'discards') {
            this.createDiscardsChart();
        } else if(this.parameterType === 'errors') {
            this.createErrorsChart();
        }
        },
        error => {
          console.error('Error fetching network data:', error);
        }
      );
    } else {
      console.error('Device ID is required.');
    }
  }

  createInTrafficChart(): void {
    const margin = { top: 100, right: 100, bottom: 100, left: 100 };
    const container = this.chartContainer.nativeElement;
    this.createLineChart(container, 'inTraffic', 'Incoming Traffic (bps)', '#388e3c', 1350, 700, 2, margin);
  }
  createOutTrafficChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, 'outTraffic', 'Outgoing Traffic (bps)', '#2e7d32', 1350, 700, 2, margin);
  }

  createDiscardsChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, 'discards', 'Discards (%)', '#FFA500', 1350, 700, 2, margin);
  }

  createErrorsChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, 'errors', 'Errors (%)', '#ff0000', 1350, 700, 2, margin);
  }
  createLineChart(
    container: Element,
    metric: string,
    title: string,
    color: string,
    width: number,
    height: number,
    strokeWidth: number,
    margin: { top: number; right: number; bottom: number; left: number }
  ): void {
    const element = this.chartContainer.nativeElement;
    width = 1350 - margin.left - margin.right;
    height = 700 - margin.top - margin.bottom;
  
    // Determine parameter type
    let paramType: string = "";
    if (this.parameterType === "in-Traffic")
      paramType = 'inTraffic(bps)';
    else if (this.parameterType === "out-Traffic")
      paramType = 'outTraffic(bps)';
    else if (this.parameterType === "errors")
      paramType = 'errors(%)';
    else if (this.parameterType === "discards")
      paramType = 'discards(%)';
  
    // Clear any existing SVG
    d3.select(element).select('svg').remove();
  
    const svg = d3
      .select(element)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);
  
    // Gather all timestamps and values
    let allTimestamps: Date[] = [];
    const allValues: { timestamp: Date; value: number }[] = [];
  
      // Safely get min and max timestamps
    
    for (const key in this.interfaceData) {
      const interfaceData = this.interfaceData[key]?.InterfaceData as Map<Date, any>;
      interfaceData.forEach((valueObj, timestamp) => {
      allTimestamps.push(timestamp);
      allValues.push({ timestamp, value: valueObj[paramType] || 0 });
    });
    }
    const minTimestamp = d3.min(allTimestamps);
    const maxTimestamp = d3.max(allTimestamps);
    //console.log("Values:"+allValues);
  
    // console.log("meow",allValues);
    console.log(minTimestamp,maxTimestamp);
    // Calculate the max value
    this.maxValue = d3.max(allValues, d => d.value) || 0;
  
    // Scale X-axis including both date and minutes
    const xScale = d3.scaleTime().domain(d3.extent(allTimestamps) as [Date, Date]).range([0, width]);
    const yScale = d3.scaleLinear().domain([0, 2 * this.maxValue]).range([height, 0]);
  
    // Draw X and Y axes
    const xAxis = d3.axisBottom(xScale);
    const yAxis = d3.axisLeft(yScale);
  
    svg.append('g').attr('transform', `translate(0,${height})`).call(xAxis);
    svg.append('g').call(yAxis);
  
    // Line generator
    const line = d3.line<{ timestamp: Date; value: number }>()
      .x(d => xScale(d.timestamp))
      .y(d => yScale(d.value))
      .curve(d3.curveMonotoneX);

      if (allTimestamps.length === 0) {
        console.error('No timestamps found.');
        return; // Early exit if no timestamps are available
      }
  
    // Plot lines for each interface
    for (const key in this.interfaceData) {
    const interfaceData = this.interfaceData[key]?.InterfaceData as Map<Date, any>;
    const data = Array.from(interfaceData.entries())
      .filter(([timestamp]) => timestamp >= minTimestamp! && timestamp <= maxTimestamp!)
      .map(([timestamp, valueObj]) => ({
        timestamp,
        value: valueObj[paramType] || 0,
      }));
  
      //console.log(data);
      svg
        .append('path')
        .datum(data)
        .attr('fill', 'none')
        .attr('stroke', d3.schemeCategory10[+key % 10])
        .attr('stroke-width', strokeWidth)
        .attr('d', line);
        break;
    }
  }
  
  
}  
*/