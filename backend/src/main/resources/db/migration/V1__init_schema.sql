-- ===================
-- Enum types
-- ===================

CREATE TYPE gender_type AS ENUM ('MALE', 'FEMALE', 'OTHER');
CREATE TYPE experiment_status AS ENUM ('DRAFT', 'ACTIVE', 'COMPLETED', 'ARCHIVED');
CREATE TYPE label_type AS ENUM ('BOOLEAN', 'ENUMERATED', 'TEXT_SPAN');

-- ===================
-- Users
-- ===================

CREATE TABLE users (
    id            BIGSERIAL    NOT NULL,
    email         VARCHAR(255) NOT NULL,
    first_name    VARCHAR(100) NOT NULL,
    last_name     VARCHAR(100) NOT NULL,
    birth         DATE,
    gender        gender_type,
    country       VARCHAR(100),
    city          VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_users        PRIMARY KEY (id),
    CONSTRAINT uq_users_email  UNIQUE (email)
);

-- ===================
-- Teams
-- ===================

CREATE TABLE teams (
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id    BIGINT       NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_teams          PRIMARY KEY (id),
    CONSTRAINT fk_teams_owner    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX idx_teams_owner_id ON teams(owner_id);

-- ===================
-- Users - Teams (M:N)
-- ===================

CREATE TABLE user_teams (
    user_id    BIGINT      NOT NULL,
    team_id    BIGINT      NOT NULL,
    joined_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_teams        PRIMARY KEY (user_id, team_id),
    CONSTRAINT fk_user_teams_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_teams_team   FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_teams_team_id ON user_teams(team_id);

-- ===================
-- Experiments
-- ===================

CREATE TABLE experiments (
    id          BIGSERIAL         NOT NULL,
    title       VARCHAR(255)      NOT NULL,
    description TEXT,
    status      experiment_status NOT NULL DEFAULT 'DRAFT',
    owner_id    BIGINT            NOT NULL,
    team_id     BIGINT,
    created_at  TIMESTAMPTZ       NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ       NOT NULL DEFAULT now(),

    CONSTRAINT pk_experiments          PRIMARY KEY (id),
    CONSTRAINT fk_experiments_owner    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_experiments_team     FOREIGN KEY (team_id)  REFERENCES teams(id) ON DELETE SET NULL
);

CREATE INDEX idx_experiments_owner_id ON experiments(owner_id);
CREATE INDEX idx_experiments_team_id  ON experiments(team_id);

-- ===================
-- Guidelines
-- ===================

CREATE TABLE guidelines (
    id            BIGSERIAL   NOT NULL,
    experiment_id BIGINT      NOT NULL,
    content       TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_guidelines               PRIMARY KEY (id),
    CONSTRAINT uq_guidelines_experiment    UNIQUE (experiment_id),
    CONSTRAINT fk_guidelines_experiment    FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

-- ===================
-- Documents
-- ===================

CREATE TABLE documents (
    id            BIGSERIAL   NOT NULL,
    experiment_id BIGINT      NOT NULL,
    content       TEXT        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_documents               PRIMARY KEY (id),
    CONSTRAINT fk_documents_experiment    FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE INDEX idx_documents_experiment_id ON documents(experiment_id);

-- ===================
-- Labels
-- ===================

CREATE TABLE labels (
    id            BIGSERIAL    NOT NULL,
    experiment_id BIGINT       NOT NULL,
    name          VARCHAR(255) NOT NULL,
    type          label_type   NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_labels               PRIMARY KEY (id),
    CONSTRAINT fk_labels_experiment    FOREIGN KEY (experiment_id) REFERENCES experiments(id) ON DELETE CASCADE
);

CREATE INDEX idx_labels_experiment_id ON labels(experiment_id);

-- ===================
-- Annotations
-- ===================

CREATE TABLE annotations (
    id           BIGSERIAL   NOT NULL,
    document_id  BIGINT      NOT NULL,
    label_id     BIGINT      NOT NULL,
    annotator_id BIGINT      NOT NULL,
    value        JSONB,
    start_offset INT,
    end_offset   INT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_annotations              PRIMARY KEY (id),
    CONSTRAINT fk_annotations_document     FOREIGN KEY (document_id)  REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT fk_annotations_label        FOREIGN KEY (label_id)     REFERENCES labels(id)    ON DELETE CASCADE,
    CONSTRAINT fk_annotations_annotator    FOREIGN KEY (annotator_id) REFERENCES users(id)     ON DELETE CASCADE,
    CONSTRAINT chk_annotations_offsets CHECK (
        (start_offset IS NULL AND end_offset IS NULL) OR 
        (start_offset >= 0 AND end_offset > start_offset)
    )
);

CREATE INDEX idx_annotations_document_id  ON annotations(document_id);
CREATE INDEX idx_annotations_label_id     ON annotations(label_id);
CREATE INDEX idx_annotations_annotator_id ON annotations(annotator_id);
