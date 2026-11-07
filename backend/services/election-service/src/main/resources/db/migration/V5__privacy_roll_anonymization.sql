-- RF-18 (D-46): the outbox also tells voter-access to anonymize people of a roll (only their references travel).
ALTER TABLE process_sync DROP CONSTRAINT ck_process_sync_type;
ALTER TABLE process_sync ADD CONSTRAINT ck_process_sync_type CHECK (message_type IN ('SNAPSHOT', 'STATUS', 'ANONYMIZE'));
