-- IMP-28: the urn is compared at the tally with the number of people who voted according to voter-access.
ALTER TABLE incidents DROP CONSTRAINT ck_incidents_kind;
ALTER TABLE incidents ADD CONSTRAINT ck_incidents_kind
    CHECK (kind IN ('REVERT_FAILED', 'CHAIN_BROKEN', 'TALLY_FAILED', 'PARTICIPATION_MISMATCH'));
