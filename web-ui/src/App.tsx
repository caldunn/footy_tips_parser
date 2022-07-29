import type { Component } from 'solid-js';

import logo from './logo.svg';
import styles from './App.module.css';

const App: Component = () => {
  return (
    <div class={styles.App}>
      <header class={styles.header}>
        <img src={logo} class={styles.logo} alt="logo" />
        <p>
          Edit <code>src/App.tsx</code> and save to reload.
        </p>
        <a
          class={styles.link}
          href="https://github.com/solidjs/solid"
          target="_blank"
          rel="noopener noreferrer"
        >
          Learn Solid
        </a>
      <button onClick={simpleRequest}>
        KEKL
      </button>
      </header>
    </div>
  );
};

const simpleRequest = async () => {
  const eventSrc = new EventSource("http://localhost:8080/cd")
  eventSrc.onmessage = (event) => console.log(event)
  eventSrc.onerror = () => eventSrc.close()
  // let res = await fetch("http://localhost:8080/cd").then((data) => data.arrayBuffer())
  // console.log(res)
}
export default App;
