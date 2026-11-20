import { describe, expect, it } from 'vitest';
import { verifyResult } from './result-verifier';
import { CountedVotes, Result } from './results.model';

const counted = (resultHash: string): CountedVotes => ({
  processId: 9, finalHash: 'f'.repeat(64), resultHash, eligibleVoters: 4, optionIds: [1, 2],
  votes: [{ voteId: 'b', optionId: 1 }, { voteId: 'a', optionId: null }, { voteId: 'c', optionId: 1 }],
});
const result = (resultHash: string, over: Partial<Result> = {}): Result => ({
  processId: 9, status: 'TALLIED', talliedAt: '2026-11-19T10:00:00Z', voteType: 'SINGLE_CHOICE', question: { es: '¿?' },
  options: [
    { optionId: 1, position: 1, text: { es: 'A' }, votes: 2, percentage: 66.67 },
    { optionId: 2, position: 2, text: { es: 'B' }, votes: 0, percentage: 0 },
  ],
  blankVoteEnabled: true, blankVotes: 1, blankPercentage: 33.33, totalVotes: 3, eligibleVoters: 4, participation: 75,
  finalHash: 'f'.repeat(64), resultHash, ...over,
});

/** D-37: the browser recomputes the tally and its hash from the published list of counted votes. */
describe('verifyResult (recálculo independiente)', () => {
  it('matches when the counts and the hash recomputed from the list are those published', async () => {
    const { recomputedHash } = await verifyResult(counted('x'), result('x'));
    expect(recomputedHash).toMatch(/^[0-9a-f]{64}$/);
    const check = await verifyResult(counted(recomputedHash), result(recomputedHash));
    expect(check.matches).toBe(true);
  });

  it('does not match when a count, the blank votes or the published hash differ', async () => {
    const { recomputedHash: hash } = await verifyResult(counted('x'), result('x'));
    expect((await verifyResult(counted(hash), result(hash, { blankVotes: 0 }))).matches).toBe(false);
    expect((await verifyResult(counted(hash), result(hash, { totalVotes: 4 }))).matches).toBe(false);
    expect((await verifyResult(counted(hash), result('otra'))).matches).toBe(false);
    expect((await verifyResult(counted('otra'), result('otra'))).matches).toBe(false);
  });
});
