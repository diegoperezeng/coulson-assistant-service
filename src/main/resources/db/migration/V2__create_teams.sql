CREATE TABLE teams (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255)  NOT NULL UNIQUE,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE TABLE team_members (
    id         UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id    UUID          NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name       VARCHAR(255)  NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_team_member_name UNIQUE (team_id, name)
);

CREATE INDEX idx_team_members_team_id ON team_members(team_id);
