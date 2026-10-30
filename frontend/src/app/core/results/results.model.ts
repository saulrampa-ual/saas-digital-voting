import { LocalizedText } from '../admin/process-api.service';

/** `Result` of the ballot-box contract (RF-13, RF-15, D-37). */
export interface OptionResult {
  optionId: number;
  position: number;
  text: LocalizedText;
  votes: number;
  percentage: number;
}

export interface Result {
  processId: number;
  status: string;
  talliedAt: string;
  voteType: string;
  question: LocalizedText;
  options: OptionResult[];
  blankVoteEnabled: boolean;
  blankVotes: number;
  blankPercentage: number;
  totalVotes: number;
  eligibleVoters: number;
  participation: number;
  finalHash: string;
  resultHash: string;
}

/** Published list to recompute the tally (D-37): random id and option only, sorted by id. */
export interface CountedVotes {
  processId: number;
  finalHash: string;
  resultHash: string;
  eligibleVoters: number;
  optionIds: number[];
  votes: { voteId: string; optionId: number | null }[];
}
