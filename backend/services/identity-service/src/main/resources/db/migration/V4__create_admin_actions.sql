-- RF-19, RI-13 (D-44): administrative actions of every service, appended here and never edited (RNF-13).
CREATE TABLE admin_actions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    organization_id VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_id UUID NOT NULL,
    actor_name VARCHAR(120) NOT NULL,
    action VARCHAR(40) NOT NULL,
    object_type VARCHAR(40) NOT NULL,
    object_id VARCHAR(64),
    object_label VARCHAR(200),
    detail VARCHAR(200),
    process_id BIGINT,
    source VARCHAR(40) NOT NULL
);

CREATE INDEX ix_admin_actions_org_time ON admin_actions (organization_id, occurred_at DESC, id DESC);
CREATE INDEX ix_admin_actions_org_actor ON admin_actions (organization_id, actor_id);
CREATE INDEX ix_admin_actions_org_process ON admin_actions (organization_id, process_id);

-- Append-only: the application role can insert and read, but no row can be changed or removed.
CREATE FUNCTION admin_actions_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'admin_actions is append-only (RNF-13)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tr_admin_actions_no_update BEFORE UPDATE OR DELETE ON admin_actions
    FOR EACH ROW EXECUTE FUNCTION admin_actions_append_only();
CREATE TRIGGER tr_admin_actions_no_truncate BEFORE TRUNCATE ON admin_actions
    FOR EACH STATEMENT EXECUTE FUNCTION admin_actions_append_only();
