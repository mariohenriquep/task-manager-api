CREATE TABLE tasks (
    id          UUID PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20)  NOT NULL,
    due_date    DATE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

CREATE INDEX idx_tasks_status ON tasks (status);
