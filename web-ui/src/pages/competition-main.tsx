import type {Component} from 'solid-js';
import {createResource, createSignal, For, Match, Show, Switch} from "solid-js";


import ProgressBar from '../components/progress-bar'
import Round, {Score} from "../models/round";
import {tableNames} from "../common-styles";
import {HelloGraph} from "../graphs/hello-graph";

enum RoundChange {
  INCREMENT,
  DECREMENT
}

interface ControlButton {
  text: string
  roundChange: RoundChange
}


const CompetitionMain: Component = () => {
  const [count, setCount] = createSignal(0)
  const [data, {mutate, refetch}] = createResource(fetchResults)
  const [round, setRound] = createSignal(1)
  const changeRound = (rc: RoundChange): void => {
    let newRound: number = round()
    switch (rc) {
      case RoundChange.INCREMENT:
        newRound++;
        break;
      case RoundChange.DECREMENT:
        newRound--;
        break;
    }
    if (newRound <= 0) newRound = 1
    // @ts-ignore
    else if (newRound >= data()?.length) newRound = data()?.length

    setRound(newRound)
  }
  setInterval(() => onCountClick(), 1000)
  const onCountClick = async (key?: MouseEvent) => {
    for (const x of Array(10).keys()) {
      await new Promise(r => setTimeout(r, 100))
      if (count() === 100) setCount(0)
      setCount(c => c + 1)
    }
  }
  const buttonStyle = "px-4 py-2 font-semibold text-sm bg-violet-500 text-white"
  const Control: Component<ControlButton> = (props) => {
    return (
      <>
        <button class={buttonStyle} onClick={() => changeRound(props.roundChange)}>
          {props.text}
        </button>
      </>
    )
  }

  return (
    <>
      <ProgressBar progress={count()}/>

      <div class={"container my-10 border object-fill"}>
        <div class="flex justify-between rounded border">
          <Control text={"<"} roundChange={RoundChange.DECREMENT}/>
          <span class={"grow"}>Round {round()} / {data()?.length}</span>
          <Control text={">"} roundChange={RoundChange.INCREMENT}/>
        </div>
        <div class={"flex"}>
          <Show when={data()} fallback={<div class={"animate-pulse duration-100 grow"}>Loading...</div>}>
            {(e) =>
              <Switch fallback={<>NO ROUND EXISTS</>}>
                <For each={e} fallback={<div>Loading...</div>}>
                  {(item, index) => (
                    <Match when={round() - 1 === index()}>
                      <Table round={item}/>
                    </Match>
                  )}
                </For>
              </Switch>
            }
          </Show>
        </div>
      </div>
      <div class={"container my-10 border object-fill"}>
        <div>Competition Timeline</div>
        <Show when={data()} fallback={<div class={"animate-pulse duration-100 grow"}>Loading...</div>}>
          {data => <HelloGraph data={data}/>}
        </Show>
      </div>
    </>
  );
};

interface RoundProps {
  round: Round
}
const Table: Component<RoundProps> = (props) => {

  // @ts-ignore
  const values: Array<[string, Score]> = Object.entries(props.round.scoreStats)
    .sort((a, b) => a[1].pos - b[1].pos)


  return (
    <>
      <table class="border-collapse border-slate-500 grow text-base">
        <thead>
        <tr>
          <th class="border border-slate-600">Position</th>
          <th class="border border-slate-600">Name</th>
          <th class="border border-slate-600">Score</th>
          <th class="border border-slate-600">Margin</th>
        </tr>
        </thead>
        <tbody>
        {  /*@ts-ignore*/ }
        <For each={values} fallback={<div>LOADING</div>}>
          {(item , i) => <tr>
            <td class="border border-slate-700">{item[1].pos}</td>
            <td class={"border border-slate-700" + tableNames}>
              <button onClick={async () => console.log(await fetchResults())}>
                {item[0]}
              </button>
            </td>
            <td class="border border-slate-700">{item[1].score}</td>
            <td class="border border-slate-700">{item[1].margin}</td>
          </tr>}
        </For>
        </tbody>
      </table>
    </>
  )
}

export const fetchResults = async () => {
  let res: Array<Round> = await fetch("http://localhost:8080/results").then((data) => data.json())
  return res
}
export default CompetitionMain;
