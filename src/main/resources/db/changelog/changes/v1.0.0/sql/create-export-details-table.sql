CREATE TABLE export_details
(
    export_id       UUID  NOT NULL PRIMARY KEY,
    list_id         UUID  NOT NULL,
    status          VARCHAR(64) CHECK (status IN ('IN_PROGRESS','SUCCESS','FAILED', 'CANCELLED')) NOT NULL,
    created_by      UUID  NOT NULL,
    start_date      TIMESTAMP NOT NULL,
    end_date        TIMESTAMP,
    CONSTRAINT fk_list_id FOREIGN KEY (list_id)
                REFERENCES list_details (id) MATCH SIMPLE ON DELETE CASCADE
);
