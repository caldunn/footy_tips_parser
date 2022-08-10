import {Component, createSignal} from "solid-js";
import {buttonStyle} from "../common-styles";
interface Props {
  progress: number
}

const ProgressBar: Component<Props> = (props) => {

  return (
    <>
      <progress class={buttonStyle} style={"width: 80%"}
        id={"stage-progress"} value={props.progress} max={100} />
    </>
  );
};

export default ProgressBar
