-- RF-05b (D-48): code by email (RF-08) or institutional account for the voters of the process.
ALTER TABLE process_snapshots ADD COLUMN voter_authentication VARCHAR(20) NOT NULL DEFAULT 'EMAIL_CODE';
ALTER TABLE process_snapshots ADD CONSTRAINT ck_process_snapshots_voter_authentication
    CHECK (voter_authentication IN ('EMAIL_CODE', 'INSTITUTIONAL_SSO'));
