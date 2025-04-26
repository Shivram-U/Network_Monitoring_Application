import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { NetworkTrafficService } from '../services/NMT_API_Service/network-traffic.service';
import * as d3 from 'd3';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Renderer2 } from '@angular/core';

interface NetworkTrafficResponse {
    InterfaceIndex: number;
    InterfaceMetrics: {};
    DeviceId: number;
    InterfaceName: string;
    InterfaceData: {
        [key: string]: {
            'inTraffic(bps)': number;
            'outTraffic(bps)': number;
            'errors(%))': number;
            operationalStatus: string;
            'discards(%)': number;
        };
    };
}

@Component({
    selector: 'app-network-traffic-graph',
    templateUrl: './network-traffic-graph.component.html',
    styleUrls: ['./network-traffic-graph.component.css'],
    imports: [FormsModule, CommonModule],
})
export class NetworkTrafficGraphComponent implements OnInit {
    @ViewChild('chart1') private chart1Container!: ElementRef;
    @ViewChild('chart2') private chart2Container!: ElementRef;
    @ViewChild('chart3') private chart3Container!: ElementRef;
    @ViewChild('chart4') private chart4Container!: ElementRef;

    networkDeviceIds: number[] = [];
    networkInterfaceIndices: number[] = [];
    private data: any[] = [];
    deviceId: number | null = null;
    interfaceIndex: number | null = null;
    private refreshInterval: any = null;

    constructor(private networkTrafficService: NetworkTrafficService, private renderer: Renderer2) {}

    ngOnInit(): void {
        this.fetchNetworkDevicesAndInterfaces();
    }

    fetchNetworkDevicesAndInterfaces(): void {
        this.networkTrafficService.getNetworkDevicesAndInterfaces().subscribe(data => {
            this.networkDeviceIds = Array.from(data.keys());
            this.networkInterfaceIndices = Array.from(data.values()).flat();
        });
    }

    fetchNetworkData(): void {
        if (this.deviceId !== null && this.interfaceIndex !== null) {
            this.networkTrafficService.getNetworkData(this.deviceId, this.interfaceIndex).subscribe((response: NetworkTrafficResponse) => {
                console.log('Fetched Network Data:', response);
                this.prepareChartData(response);
            });
        } else {
            console.error('Device ID and Interface Index are required.');
        }
    }

    onDeviceIdChange(event: any): void {
        console.log('Device ID changed', event);
        if (event && event.target) {
            this.deviceId = event.target.value;
            this.fetchNetworkData();
            this.startRefreshInterval();
        } else {
            console.error('Network Device ID is null or undefined');
        }
    }

    onInterfaceIndexChange(event: any): void {
        console.log('Interface Index changed', event);
        if (event && event.target) {
            this.interfaceIndex = event.target.value;
            this.fetchNetworkData();
            this.startRefreshInterval();
        } else {
            console.error('Interface Index is null or undefined');
        }
    }

    prepareChartData(data: NetworkTrafficResponse): void {
        if (data && data.InterfaceData) {
            let extractedData = Object.entries(data.InterfaceData)
                .filter(([timestamp]) => timestamp !== undefined)
                .map(([timestamp, values]) => ({
                    timestamp: new Date(new Date(timestamp).setSeconds(0, 0)),
                    inTraffic: values['inTraffic(bps)'] || 0,
                    outTraffic: values['outTraffic(bps)'] || 0,
                    errors: values['errors(%))'] || 0,
                    discards: values['discards(%)'] || 0,
                }));

            extractedData.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());

            const minTimestamp = extractedData[0].timestamp;
            const maxTimestamp = extractedData[extractedData.length - 1].timestamp;

            const fullTimeline = [];
            for (let t = new Date(minTimestamp); t <= maxTimestamp; t.setMinutes(t.getMinutes() + 1)) {
                fullTimeline.push(new Date(t));
            }

            const mergedData = fullTimeline.map(t => {
                const existingData = extractedData.find(d => d.timestamp.getTime() === t.getTime());
                return existingData || { timestamp: t, inTraffic: 0, outTraffic: 0, errors: 0, discards: 0 };
            });

            this.data = mergedData;
            this.createInTrafficChart();
            this.createOutTrafficChart();
            this.createDiscardsChart();
            this.createErrorsChart();
        }
    }

    createInTrafficChart(): void {
        const margin = { top: 100, right: 100, bottom: 50, left: 100 };
        const container = this.chart1Container.nativeElement;
        this.createLineChart(container, this.data, 'inTraffic', 'Incoming Traffic (bps)', 'steelblue', 1000, 600, 2, margin);
    }

    createOutTrafficChart(): void {
        const margin = { top: 100, right: 100, bottom: 50, left: 100 };
        const container = this.chart2Container.nativeElement;
        this.createLineChart(container, this.data, 'outTraffic', 'Outgoing Traffic (bps)', 'green', 1000, 600, 2, margin);
    }

    createDiscardsChart(): void {
        const margin = { top: 100, right: 100, bottom: 50, left: 80 };
        const container = this.chart3Container.nativeElement;
        this.createLineChart(container, this.data, 'discards', 'Discards (%)', 'orange', 1000, 600, 2, margin);
    }

    createErrorsChart(): void {
        const margin = { top: 100, right: 100, bottom: 50, left: 50 };
        const container = this.chart4Container.nativeElement;
        this.createLineChart(container, this.data, 'errors', 'Errors (%)', 'red', 1000, 600, 2, margin);
    }

    createLineChart(
        container: Element,
        data: any[],
        metric: string,
        title: string,
        color: string,
        width: number,
        height: number,
        strokeWidth: number,
        margin: { top: number; right: number; bottom: number; left: number }
    ): void {
        this.renderer.setProperty(container, 'innerHTML', '');
        width = width - margin.left - margin.right;
        height = height - margin.top - margin.bottom;

        const sortedData = data
            .map(d => ({ ...d, timestamp: new Date(d.timestamp) }))
            .sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());

        console.log(sortedData);

        const svg = d3.select(container)
            .append('svg')
            .attr('width', width + margin.left + margin.right)
            .attr('height', height + margin.top + margin.bottom)
            .append('g')
            .attr('transform', `translate(${margin.left},${margin.top})`);

        const x = d3.scaleTime()
            .domain(d3.extent(sortedData, d => d.timestamp) as [Date, Date])
            .range([0, width]);

        const xAxis = d3.axisBottom(x);

        const y = d3.scaleLinear()
            .domain([0, 2 * (d3.max(sortedData, d => d[metric]) || 0)])
            .range([height, 0]);

        const yAxis = d3.axisLeft(y);

        const line = d3.line<any>()
            .x(d => x(d.timestamp))
            .y(d => y(d[metric]));

        svg.append('path')
            .datum(sortedData)
            .attr('fill', 'none')
            .attr('stroke', color)
            .attr('stroke-width', strokeWidth)
            .attr('d', line);

        svg.append('g')
            .attr('transform', `translate(0,${height})`)
            .call(xAxis);

        svg.append('g')
            .call(yAxis);

        svg.append('text')
            .attr('x', width / 2)
            .attr('y', height + margin.bottom - 10)
            .attr('text-anchor', 'middle')
            .style('font-size', '15px')
            .style('font-weight', 'bold')
            .text('Time');

        svg.append('text')
            .attr('transform', 'rotate(-90)')
            .attr('y', -margin.left + 15)
            .attr('x', -height / 2)
            .style('font-size', '15px')
            .style('font-weight', 'bold')
            .attr('text-anchor', 'middle')
            .text(title);

        svg.append('text')
            .attr('x', width / 2)
            .attr('y', -margin.top / 2)
            .attr('text-anchor', 'middle')
            .style('font-size', '16px')
            .style('font-weight', 'bold')
            .text(title);
    }

    startRefreshInterval(): void {
        // Clear any existing interval
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }

        // Set a new interval to refresh at the start of each minute
        this.refreshInterval = setInterval(() => {
            this.fetchNetworkData();
        }, 60000); // 60000 ms = 1 minute
    }

    ngOnDestroy(): void {
        // Clear the interval when the component is destroyed
        if (this.refreshInterval) {
            clearInterval(this.refreshInterval);
        }
    }
}



/*
REFERENCE

import { Component, OnInit, ViewChild, ElementRef } from '@angular/core';
import { NetworkTrafficService } from '../services/NMT_API_Service/network-traffic.service';
import * as d3 from 'd3';

interface NetworkTrafficResponse {
  InterfaceIndex: number;
  InterfaceMetrics: {};
  DeviceId: number;
  InterfaceName: string;
  InterfaceData: {
    [key: string]: {
      'inTraffic(bps)': number;
      'outTraffic(bps)': number;
      'errors(%))': number;
      operationalStatus: string;
      'discards(%)': number;
    };
  };
}

@Component({
  selector: 'app-network-traffic-graph',
  templateUrl: './network-traffic-graph.component.html',
  styleUrls: ['./network-traffic-graph.component.css']
})
export class NetworkTrafficGraphComponent implements OnInit {
  @ViewChild('chart') private chartContainer!: ElementRef;

  private data: any[] = [];

  constructor(private networkTrafficService: NetworkTrafficService) { }

  ngOnInit(): void {
    this.fetchNetworkData();
  }

  fetchNetworkData(): void {
    this.networkTrafficService.getNetworkData().subscribe((response: NetworkTrafficResponse) => {
      this.prepareChartData(response);
    });
  }

  prepareChartData(data: NetworkTrafficResponse): void {
    if (data && data.InterfaceData) {
      // Extract and sort the data
      let extractedData = Object.entries(data.InterfaceData)
        .filter(([timestamp]) => timestamp !== undefined)
        .map(([timestamp, values]) => ({
          timestamp: new Date(timestamp),
          inTraffic: values['inTraffic(bps)'] || 0,
          outTraffic: values['outTraffic(bps)'] || 0,
          errors: values['errors(%))'] || 0,
          discards: values['discards(%)'] || 0,
        }));
  
      // Sort the extracted data by timestamp
      extractedData.sort((a, b) => a.timestamp.getTime() - b.timestamp.getTime());
  
      // Determine the earliest and latest timestamps
      const minTimestamp = extractedData[0].timestamp;
      const maxTimestamp = extractedData[extractedData.length - 1].timestamp;
  
      // Generate a timeline with all minute intervals
      const fullTimeline = [];
      for (let t = new Date(minTimestamp); t <= maxTimestamp; t.setMinutes(t.getMinutes() + 1)) {
        fullTimeline.push(new Date(t));
      }
  
      // Merge extracted data with the complete timeline
      const mergedData = fullTimeline.map(t => {
        const existingData = extractedData.find(d => d.timestamp.getTime() === t.getTime());
        return existingData || { timestamp: t, inTraffic: 0, outTraffic: 0, errors: 0, discards: 0 };
      });
  
      // Update the data and create charts
      this.data = mergedData;
      this.createInTrafficChart();
      this.createOutTrafficChart();
      this.createDiscardsChart();
      this.createErrorsChart();
    }
  }
  

  createInTrafficChart(): void {
    const margin = { top: 100, right: 100, bottom: 50, left: 100 };
    const container = this.chartContainer.nativeElement;
    this.createLineChart(container,this.data,'inTraffic', 'Incoming Traffic (bps)', 'steelblue',1000,600,2,margin);
  }
  
  createOutTrafficChart(): void {
    const margin = { top: 100, right: 100, bottom: 50, left: 100 };
    const container = this.chartContainer.nativeElement;
    this.createLineChart(container,this.data,'outTraffic', 'Outgoing Traffic (bps)', 'green',1000,600,2,margin);
  }
  
  createDiscardsChart(): void {
    const margin = { top: 100, right: 100, bottom: 50, left: 50 };
    const container = this.chartContainer.nativeElement;
    this.createLineChart(container,this.data,'discards', 'Discards (%)', 'orange',600,600,2,margin);
  }
  
  createErrorsChart(): void {
    const margin = { top: 100, right: 100, bottom: 50, left: 50 };
    const container = this.chartContainer.nativeElement;
    this.createLineChart(container,this.data,'errors', 'Errors (%)', 'red',600,600,2,margin);
  }

  createLineChart(
    container: Element,
    data: any[],
    metric: string,
    title: string,
    color: string,
    width: number,
    height: number,
    strokeWidth: number,
    margin: {
      top: number;
      right: number;
      bottom: number;
      left: number;
    }
  ): void {
    width = width - margin.left - margin.right;
    height = height - margin.top - margin.bottom;
  
    // Sort the data by timestamp
    const sortedData = data
      .map(d => ({ ...d, timestamp: new Date(d.timestamp) }))
      .sort((a, b) => a.timestamp - b.timestamp);
  
    const svg = d3.select(container)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform',`translate(${margin.left},${margin.top})`);
  
    // X-axis (time)
    const x = d3.scaleTime()
      .domain(d3.extent(sortedData, d => d.timestamp) as [Date, Date])
      .range([0, width]);
  
    const xAxis = d3.axisBottom(x);
  
    // Y-axis (metric value)
    const y = d3.scaleLinear()
      .domain([0, 2 * (d3.max(sortedData, d => d[metric]) || 0)])
      .range([height, 0]);
  
    const yAxis = d3.axisLeft(y);
  
    // Line generator for the metric
    const line = d3.line<any>()
      .x(d => x(d.timestamp))
      .y(d => y(d[metric]));
  
    // Draw the line
    svg.append('path')
      .datum(sortedData)
      .attr('fill', 'none')
      .attr('stroke', color)
      .attr('stroke-width', strokeWidth)
      .attr('d', line);
  
    // Append axes
    svg.append('g')
      .attr('transform', `translate(0,${height})`)
      .call(xAxis);
  
    svg.append('g')
      .call(yAxis);
  
    // Add titles and labels
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', height + margin.bottom - 10)
      .attr('text-anchor', 'middle')
      .style('font-size', '15px')
      .style('font-weight', 'bold')
      .text('Time');
  
    svg.append('text')
      .attr('transform', 'rotate(-90)')
      .attr('y', -margin.left + 15)
      .attr('x', -height / 2)
      .style('font-size', '15px')
      .style('font-weight', 'bold')
      .attr('text-anchor', 'middle')
      .text(title);
  
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', -margin.top / 2)
      .attr('text-anchor', 'middle')
      .style('font-size', '16px')
      .style('font-weight', 'bold')
      .text(title);
  }
}  
*/