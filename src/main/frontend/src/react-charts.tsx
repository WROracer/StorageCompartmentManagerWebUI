import { ReactAdapterElement, RenderHooks } from "Frontend/generated/flow/ReactAdapter";
import React, { useState, useEffect } from "react";
import {
  LineChart,
  BarChart,
  PieChart,
  AreaChart,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  Line,
  Bar,
  Area,
  Pie,
  ResponsiveContainer,
  XAxisProps,
  YAxisProps
} from "recharts";
import moment from 'moment'

// ---------- Typen ----------
export type ChartType = "LINE" | "BAR" | "PIE" | "AREA";

export interface ChartSeries {
  type: ChartType;
  dataKey: string;
  stroke?: string;
  fill?: string;
}

export interface ChartConfig {
  type: ChartType;
  data: Record<string, unknown>[];
  width?: number;
  height?: number;
  legend?: boolean;
  tooltip?: boolean;
  grid?: boolean;
  xAxis?: XAxisProps;
  yAxis?: YAxisProps;
  series: ChartSeries[];
  startDate: string;
  endDate: string;
}

// ---------- React-Komponente ----------
class RechartsElement extends ReactAdapterElement {

  protected render(hooks: RenderHooks) {
    const [config, setConfig] = hooks.useState<ChartConfig | null>('config');

    // useEffect to load configuration when element is initialized
    /*useEffect(() => {
      const configAttr = this.getAttribute("config"); // Holen der Konfiguration von Vaadin
      console.log(configAttr);
      if (configAttr) {
        try {
          const parsedConfig: ChartConfig = JSON.parse(configAttr);
          setConfig(parsedConfig);
        } catch (error) {
          console.error("Error parsing chart config:", error);
        }
      }
    }, []);*/
    console.log(config?.data)
    console.log(moment('2025-11-18_09:34:16','yyyy-MM-dd_HH:mm:ss'))

  



    // Wenn keine Konfiguration gesetzt wurde, zeigen wir eine Fehlermeldung
    if (!config) {
      return <div style={{ color: "red" }}>No config provided.</div>;
    }

    const {
      type,
      data,
      width = 1000,
      height = 500,
      legend,
      tooltip,
      grid,
      xAxis,
      yAxis,
      series,
      startDate,
      endDate
    } = config;

    const ChartComponent =
      {
        LINE: LineChart,
        BAR: BarChart,
        PIE: PieChart,
        AREA: AreaChart
      }[type] ?? LineChart;

    const SeriesComponentMap: { [key in ChartType]: React.ComponentType<any> } = {
      LINE: Line,
      BAR: Bar,
      AREA: Area,
      PIE: Pie
    };

    const formattedDate = data.map((item: any) => ({
      ...item,
      time: moment(item.time,'YYYY-MM-DD_HH:mm:ss').valueOf()
    }))
    console.log(formattedDate)
    const formattedStartDate = moment(startDate,'YYYY-MM-DD_HH:mm:ss').valueOf()
    const formattedEndDate = moment(endDate,'YYYY-MM-DD_HH:mm:ss').valueOf()

    console.log(startDate)
    console.log(endDate)

    return (
      <div>
      <ResponsiveContainer width={width} height={height}>
        <ChartComponent data={formattedDate}>
          {grid && <CartesianGrid strokeDasharray="3 3" />}
          <XAxis   dataKey = 'time'
        domain = {[formattedStartDate, formattedEndDate]}
        name = 'Time'
        tickFormatter = {(unixTime) => moment(unixTime).format('HH:mm DD.MM.YYYY')}
        type = 'number' 
        allowDataOverflow ></XAxis>
          
          {yAxis && <YAxis {...yAxis} />}
          {tooltip && <Tooltip />}
          {legend && <Legend />}
          {series && series.map((s: ChartSeries, index: number) => {
            const SeriesComp = SeriesComponentMap[s.type];
            return <SeriesComp key={index} {...s} />;
          })}
        </ChartComponent>
      </ResponsiveContainer>
      </div>
    );
  }
}

customElements.define("recharts-element", RechartsElement);

export default RechartsElement;

/**
 * 
 * {xAxis && <XAxis {...xAxis} />}
 * 
 *   {series && series.map((s: ChartSeries, index: number) => {
            const SeriesComp = SeriesComponentMap[s.type];
            return <SeriesComp key={index} {...s} />;
          })}
 */
