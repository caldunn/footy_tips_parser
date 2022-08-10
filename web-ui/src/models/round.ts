export default interface Round {
  round: number,
  scoreStats: Scores
}
export interface Scores {
  [key: string]: Score
}
export interface Score {
  pos: number
  score: number
  margin: number
}

