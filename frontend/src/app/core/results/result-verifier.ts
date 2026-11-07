import { CountedVotes, Result } from './results.model';

export interface Verification {
  /** The recomputed hash equals the published one and the counts equal the shown result. */
  matches: boolean;
  recomputedHash: string;
}

/**
 * RF-13 steps 6–8 in the browser, with the same rules as the ballot box (D-37), so the person does not have to trust
 * the server's word: counts and result hash are recomputed from the published counted votes.
 */
export async function verifyResult(counted: CountedVotes, result: Result): Promise<Verification> {
  const sorted = [...counted.votes].sort((a, b) => (a.voteId < b.voteId ? -1 : a.voteId > b.voteId ? 1 : 0));
  const votesHash = await sha256(sorted.map((v) => `${v.voteId}:${v.optionId ?? 'blank'}`).join('\n'));
  const counts = new Map<number, number>(counted.optionIds.map((id) => [id, 0]));
  let blank = 0;
  for (const vote of counted.votes) {
    if (vote.optionId === null) blank++;
    else counts.set(vote.optionId, (counts.get(vote.optionId) ?? 0) + 1);
  }
  const canonical = [
    'result', counted.processId, counted.finalHash, counted.eligibleVoters, counted.votes.length, blank,
    ...counted.optionIds.map((id) => `${id}:${counts.get(id) ?? 0}`), votesHash,
  ].join('|');
  const recomputedHash = await sha256(canonical);
  const sameCounts = result.blankVotes === blank && result.totalVotes === counted.votes.length
    && result.options.every((option) => counts.get(option.optionId) === option.votes);
  return {
    matches: recomputedHash === counted.resultHash && counted.resultHash === result.resultHash && sameCounts,
    recomputedHash,
  };
}

async function sha256(text: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, '0')).join('');
}
