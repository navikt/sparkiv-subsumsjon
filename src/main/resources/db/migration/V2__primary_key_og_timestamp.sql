DROP TABLE melding;

CREATE TABLE melding(
    id UUID NOT NULL PRIMARY KEY,
    fødselsnummer VARCHAR NOT NULL,
    tidsstempel timestamp NOT NULL,
    event_name VARCHAR NOT NULL,
    json jsonb NOT NULL
);
