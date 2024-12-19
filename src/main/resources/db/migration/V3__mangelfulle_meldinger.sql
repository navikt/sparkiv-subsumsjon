CREATE TABLE mangelfull_melding(
    partisjon INT NOT NULL,
    commit_offset BIGINT NOT NULL,
    json jsonb NOT NULL
);
