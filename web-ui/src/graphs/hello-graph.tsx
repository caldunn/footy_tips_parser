import {Component, createResource, Show} from "solid-js";
import Chart from 'chart.js/auto';
import Round from "../models/round";

interface Props {
  data: Array<Round>
}
interface DataSet {
  label: string,
  data: number[],
  backgroundColor: string,
  borderColor: string,
  borderWidth: number
}
const rcv = (): number => Math.floor(Math.random() * 255)
export const HelloGraph: Component<Props> = (props) => {
  const labels = props.data.map(r => `${r.round}`)
  let dataSets: DataSet[] = Object.entries(props.data[0].scoreStats).map(v => {
    return {
      label: v[0],
      data: props.data.map(r => r.scoreStats[v[0]].score),
      backgroundColor: `rgba(${rcv()}, ${rcv()}, ${rcv()}, 1)`,
      borderColor: `rgba(${rcv()}, ${rcv()}, ${rcv()}, 1)`,
      borderWidth: 1
    }
  })

  let avg: number[] = []
  for (const i of props.data.keys()) {
    let rAvg = Object.entries(props.data[i].scoreStats)
      .map(v => v[1].score)
      .reduce((a,b) => a + b) / dataSets.length
    avg.push(rAvg)
  }

  let avgDataset: DataSet = {
    label: "avg",
    data: avg,
    backgroundColor: `rgba(255, 255, 255, 1)`,
    borderColor: `rgba(255, 255, 255, 1)`,
    borderWidth: 3
  }
  dataSets.push(avgDataset)

  const chartData = {
    type: 'line',
    data: {
      labels: labels,
      datasets: dataSets
    },
    stepped: 'after',
    drawActiveElementsOnTop: true,
    options: {
      scales: {
        y: {
          beginAtZero: true
        }
      }
    }
  }


  // onMount(() => {
  //   let ctx = canvas.getContext("2d")
  //   new Chart(ctx, kek)
  // })

  return (
    <>

      <canvas ref={canvas => {
        let ctx = canvas.getContext("2d")
        if (ctx == null) return
        // @ts-ignore
        new Chart(ctx, chartData)

      }} id="chart"
              style="border:2px solid #ffffff;"></canvas>

      </>

  )
}
