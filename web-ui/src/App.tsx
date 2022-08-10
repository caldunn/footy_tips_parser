import type {Component} from 'solid-js';

import CompetitionMain from "./pages/competition-main";
import styles from './App.module.css';
import {createSignal, Show} from "solid-js";
import {buttonStyle} from "./common-styles";
import {readSignal} from "solid-js/types/reactive/signal";

interface InputField {
  label: string
  type: string
}

interface CompFetchRequest {
  user: string
  password: string
  competition: number

}
const App: Component = () => {
  const [comp, setComp] = createSignal(false)
  const InputField: Component<InputField> = (props) => {

    return (
      <div class={"flex flex-col text-base"} >
        <label for="fname">{props.label}</label>
        <input class={"bg-[#282c34] border rounded focus:border-violet-300 focus:outline-none text-sm"}
          type={props.type} id="fname" name="fname" />
      </div>
    )
  }
  const SignIn: Component = () => {
    let user = "caldunn@iinet.net.au"
    let password = "123"
    let competition = 1
    const submit = async () => {
      let kek: CompFetchRequest = {
        competition: competition,
        user: user,
        password: password
      }
      let url = "http://localhost:8080/signin"
      const options = {
        method: "POST",
        body: JSON.stringify(kek)
      }

      let res = await fetch(url, options).then(res => res.body?.getReader())
      if (!res) return
      const readStream = async (reader: ReadableStreamDefaultReader<Uint8Array>) => {
        const {value, done} = await reader.read()
        if (done) return
        const asStr = new TextDecoder().decode(value)
        console.log(asStr)
        await readStream(reader)
      }
      await readStream(res)
    }

    const simpleRequest = async () => {
      const eventSrc = new EventSource("https://localhost/cd")
      eventSrc.onmessage = (event) => console.log(event)
      eventSrc.onerror = () => eventSrc.close()
      let res = await fetch("https://localhost/ping").then((data) => data.text())
      console.log(res)
    }

    return (
      <div class={"border rounded box-content"}>
        <div class={"border rounded text-xl"}>Sign In</div>
        <div class={"flex flex-col p-4 gap-4 border rounded"}>
          <div>
            <InputField ref={user} label={"Email"} type={"email"}/>
          </div>
          <div>
            <InputField ref={password} label={"Password"} type={"password"}/>
          </div>
          <div>
            <InputField ref={competition} label={"Competition"} type={"number"}/>
          </div>
          <button onClick={submit} class={buttonStyle}>Submit</button>
        </div>
      </div>
    )
  }

  return (
    <div class={styles.App}>
      <header class={styles.header}>
        <Show when={comp()} fallback={<SignIn />}>
          <CompetitionMain/>
        </Show>
      </header>
    </div>
  )
}


export default App;
