import { ActivatedRoute } from '@angular/router';
import { Component, OnInit, ViewChild, ElementRef, Inject } from '@angular/core';
import { NetworkTrafficService } from '../services/NMT_API_Service/network-traffic.service';
import * as d3 from 'd3';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Renderer2 } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { PLATFORM_ID } from '@angular/core';

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
  selector: 'app-network-parameter-analysis',
  templateUrl: './network-parameter-analysis.component.html',
  //styleUrls: ['./network-parameter-analysis.component.css'],
  imports: [FormsModule, CommonModule],
})
export class NetworkParameterAnalysisComponent implements OnInit {
  @ViewChild('chart') private chartContainer!: ElementRef;
  @ViewChild('metricChart') private metricChartContainer!: ElementRef;

  networkDeviceIds: number[] = [];
  networkInterfaceIndices: number[] = [];
  private data: any[] = [];
  private processedData: any[] = [];
  deviceId: number | null = null;
  interfaceIndex: number | null = null;
  private refreshInterval: any = null;
  parameterType: string = "";
  selectedAggregation: string = 'avg'; // Default to average analysis
  metricsData: any[] = [];
  selectedUnit: string = 'bps';
  dataKey : string = "";
  metricKey : string = "";
  metricskey_ : string = "";
  selectedMetricUnit: string = 'bps';
  top : number = 50;
  processedMetricsData: any[] = [];


  constructor(private networkTrafficService: NetworkTrafficService, private renderer: Renderer2, private route: ActivatedRoute, @Inject(PLATFORM_ID) private platformId: Object) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.parameterType = params.get('parameterType') || '';
      this.deviceId = Number(params.get('deviceId'));
      this.interfaceIndex = Number(params.get('interfaceIndex'));
      if (isPlatformBrowser(this.platformId)) {
        console.log("fetching data");
        this.fetchNetworkData();
      }
      if (this.parameterType === "in-Traffic") {
        this.metricKey = "InTraffic_bps";
      } else if (this.parameterType === "out-Traffic") {
        this.metricKey = "OutTraffic_bps";
      } else if (this.parameterType === "errors") {
        this.metricKey = "Errors_percent";
      } else if (this.parameterType === "discards") {
        this.metricKey = "Discards_percent";
      }
    });
  }
  
  fetchNetworkData(): void {
    if (this.deviceId !== null && this.interfaceIndex !== null) {
        this.networkTrafficService.getNetworkData(this.deviceId, this.interfaceIndex).subscribe((response: NetworkTrafficResponse) => {
            this.prepareChartData(response);
        });
    } else {
        console.error('Device ID and Interface Index are required.');
    }
  }

  startRefreshInterval(): void {
    if (this.refreshInterval) {
        clearInterval(this.refreshInterval);
    }
    this.refreshInterval = setInterval(() => {
        this.fetchNetworkData();
    }, 60000); // 60000 ms = 1 minute
  }


  onDeviceIdChange(event: any): void {
      if (event && event.target) {
          this.deviceId = event.target.value;
          this.fetchNetworkData();
          this.startRefreshInterval();
      }
  }

  onInterfaceIndexChange(event: any): void {
      if (event && event.target) {
          this.interfaceIndex = event.target.value;
          this.fetchNetworkData();
          this.startRefreshInterval();
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
        if(this.parameterType === "in-Traffic" || this.parameterType === "out-Traffic")
        {
          this.convertStoredDataUnit();
        }
        else
        {
          this.processedData = this.data;
          if(this.parameterType === 'discards') {
              this.createDiscardsChart();
          } else if(this.parameterType === 'errors') {
              this.createErrorsChart();
          }
        }
        this.processInterfaceMetrics(data.InterfaceMetrics);
      }
  }

  convertStoredDataUnit(): void {
      let maxTrafficValue: number;
      let divisor: number;
      let unit : string;
      if (this.parameterType === 'in-Traffic') {
          // Determine the maximum value of inTraffic
          maxTrafficValue = Math.max(...this.data.map(d => d.inTraffic));
          const { unit, divisor } = this.getTrafficUnitAndDivisor(maxTrafficValue);
          this.selectedUnit = unit;
          // Automatically convert inTraffic
          this.processedData = this.data.map(d => ({
              ...d,
              inTraffic: (d.inTraffic / divisor),
          }));
          this.createInTrafficChart();
      } else if (this.parameterType === 'out-Traffic') {
          // Determine the maximum value of outTraffic
          maxTrafficValue = Math.max(...this.data.map(d => d.outTraffic));
          const { unit, divisor } = this.getTrafficUnitAndDivisor(maxTrafficValue);
          this.selectedUnit = unit;
          // Automatically convert outTraffic
          this.processedData = this.data.map(d => ({
              ...d,
              outTraffic: (d.outTraffic / divisor),
          }));
          this.createOutTrafficChart();
      }
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

  createInTrafficChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, this.data, 'inTraffic', `Incoming Traffic (${this.selectedUnit})`, '#388e3c', 1350, 700, 2, margin);
  }
  createOutTrafficChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, this.data, 'outTraffic', `Outgoing Traffic (${this.selectedUnit})`, '#2e7d32', 1350, 700, 2, margin);
  }

  createDiscardsChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, this.data, 'discards', 'Discards (%)', '#FFA500', 1350, 700, 2, margin);
  }

  createErrorsChart(): void {
      const margin = { top: 100, right: 100, bottom: 100, left: 100 };
      const container = this.chartContainer.nativeElement;
      this.createLineChart(container, this.data, 'errors', 'Errors (%)', '#ff0000', 1350, 700, 2, margin);
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

      const sortedData = this.processedData
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
          .attr('y', height + margin.bottom - 50)
          .attr('text-anchor', 'middle')
          .style('font-size', '15px')
          .text('Time');

      svg.append('text')
          .attr('transform', 'rotate(-90)')
          .attr('y', -margin.left + 15)
          .attr('x', -height / 2)
          .style('font-size', '15px')
          .attr('text-anchor', 'middle')
          .text(title);

      svg.append('text')
          .attr('x', width / 2)
          .attr('y', -margin.top / 2)
          .attr('text-anchor', 'middle')
          .style('font-size', '15px')
          .text(title);
  }

  onAggregationChange(): void {
    this.fetchNetworkData();
    if (this.parameterType && this.metricsData.length > 0) {
      this.createMetricChart();
    }
  }

  convertStoredDataMetrics(): void {
    if (this.metricsData) {
        var maxTrafficValue = Math.max(...this.metricsData.map(d => d.value));
        const { unit, divisor } = this.getTrafficUnitAndDivisor(maxTrafficValue);
        this.selectedMetricUnit = unit;
        this.processedMetricsData = this.metricsData.map(d => ({
            ...d,
            value: (d.value / divisor),
        }));
        this.createMetricChart();
    }
  }

  processInterfaceMetrics(interfaceMetrics: any): void {
    if (interfaceMetrics) {
        const extractedMetrics = Object.entries(interfaceMetrics)
        .map(([timestamp, metrics]: any) => {
          this.metricskey_ = this.selectedAggregation+this.metricKey;
          // console.log(this.selectedAggregation,key);
          // Ensure key exists and fallback to 0 if missing
          // console.log(metrics[key]);
          const value = this.metricskey_ && metrics[this.metricskey_] ? metrics[this.metricskey_] : 0;
    
          return {
            timestamp: new Date(new Date(timestamp).setSeconds(0, 0)), // Round to minute
            value,
          };
        });
        // console.log("Extract",extractedMetrics);
      const minTimestamp = new Date(
        Math.min(...extractedMetrics.map((d) => d.timestamp.getTime()))
      );
      const maxTimestamp = new Date(
        Math.max(...extractedMetrics.map((d) => d.timestamp.getTime()))
      );

      const fullTimeline = [];
      for (
        let t = new Date(minTimestamp);
        t <= maxTimestamp;
        t.setHours(t.getHours() + 1)
      ) {
        fullTimeline.push(new Date(t));
      }

      // Map the extracted metrics to the full timeline, filling gaps with default value (0)
        this.metricsData = fullTimeline.map((t) => {
            const matchingData = extractedMetrics.find(
            (d) => d.timestamp.getTime() === t.getTime()
            );
    
            return {
            timestamp: t,
            value: matchingData ? matchingData.value : 0, // Fill gaps with 0
            };
        });

        // console.log(this.metricsData);

      if(this.parameterType === "in-Traffic" || this.parameterType === "out-Traffic")
      {
        this.convertStoredDataMetrics();
      }
      else
      {
        this.processedMetricsData = this.metricsData;
        this.createMetricChart();
      }
    }
  }

  createMetricChart(): void {
    //console.log(this.metricsData);
    // console.log("Graph reconstruction");
    const container = this.metricChartContainer.nativeElement;
    const margin = { top: 100, right: 100, bottom: 100, left: 100 };
    const width = 1350 - margin.left - margin.right;
    const height = 600 - margin.top - margin.bottom;

    this.renderer.setProperty(container, 'innerHTML', '');

    var selectedAggregation_ : string = "";
    console.log("XYR:",this.processedMetricsData);
    var parameterType_ : string = this.parameterType;
    if (this.parameterType === "in-Traffic" || this.parameterType === "out-Traffic") {
        parameterType_+= ` (${this.selectedMetricUnit})`;
    } else if (this.parameterType === "errors" || this.parameterType === "discards") {
        parameterType_+= " (%)";
    }

    if(this.selectedAggregation == "avg")
    {
        selectedAggregation_ = "Mean";
    }
    else if(this.selectedAggregation == "max")
    {
        selectedAggregation_ = "Maximum range";
    }
    else if(this.selectedAggregation == "min")
    {
        selectedAggregation_ = "Minimum range";
    }

    const svg = d3
      .select(container)
      .append('svg')
      .attr('width', width + margin.left + margin.right)
      .attr('height', height + margin.top + margin.bottom)
      .append('g')
      .attr('transform', `translate(${margin.left},${margin.top})`);

    const x = d3
      .scaleTime()
      .domain(d3.extent(this.processedMetricsData, (d) => d.timestamp) as [Date, Date])
      .range([0, width]);

    const y = d3
      .scaleLinear()
      .domain([0, 2*d3.max(this.processedMetricsData, (d) => d.value) || 0])
      .range([height, 0]);

    const line = d3
      .line<any>()
      .x((d) => x(d.timestamp))
      .y((d) => y(d.value));

    svg
      .append('path')
      .datum(this.processedMetricsData)
      .attr('fill', 'none')
      .attr('stroke', '#4285F4')
      .attr('stroke-width', 2)
      .attr('d', line);

    svg.append('g').attr('transform', `translate(0,${height})`).call(d3.axisBottom(x));
    svg.append('g').call(d3.axisLeft(y));

    svg
      .append('text')
      .attr('x', width / 2)
      .attr('y', -margin.top / 2)
      .attr('text-anchor', 'middle')
      .style('font-size', '16px')
      .text(`${this.parameterType} (${selectedAggregation_})  Analysis`);

    svg
      .append('text')
      .attr('x', width / 2)
      .attr('y', height + margin.bottom-50)
      .attr('text-anchor', 'middle')
      .text('Time');

    svg
      .append('text')
      .attr('transform', 'rotate(-90)')
      .attr('y', -margin.left + 15)
      .attr('x', -height / 2)
      .attr('text-anchor', 'middle')
      .text(parameterType_);
  }
  ngOnDestroy(): void {
      if (this.refreshInterval) {
          clearInterval(this.refreshInterval);
      }
  }
}
