-- RF-05b (D-48): how the voters of the process prove who they are: code by email (RF-08) or institutional account.
ALTER TABLE processes ADD COLUMN voter_authentication VARCHAR(20) NOT NULL DEFAULT 'EMAIL_CODE';
ALTER TABLE processes ADD CONSTRAINT ck_processes_voter_authentication
    CHECK (voter_authentication IN ('EMAIL_CODE', 'INSTITUTIONAL_SSO'));
