DROP TABLE team_members;

CREATE TABLE participants (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(255)  NOT NULL,
    role             VARCHAR(255)  NOT NULL DEFAULT 'indefinido',
    voice_sample_key VARCHAR(1000),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_participants_name ON participants(name);

CREATE TABLE team_members (
    team_id        UUID        NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    participant_id UUID        NOT NULL REFERENCES participants(id) ON DELETE CASCADE,
    added_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (team_id, participant_id)
);

CREATE INDEX idx_team_members_team_id        ON team_members(team_id);
CREATE INDEX idx_team_members_participant_id ON team_members(participant_id);
