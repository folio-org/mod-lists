CREATE TABLE list_refresh_details
(
    id                      UUID NOT NULL PRIMARY KEY,
    list_id                 UUID NOT NULL,
    status                  VARCHAR(64) CHECK (status IN ('IN_PROGRESS','SUCCESS','FAILED','CANCELLED')) NOT NULL,
    refresh_start_date      TIMESTAMP NOT NULL,
    refresh_end_date        TIMESTAMP,
    refreshed_by            UUID NOT NULL,
    refreshed_by_username   VARCHAR(64) NOT NULL,
    cancelled_by            UUID,
    records_count           NUMERIC,
    content_version         NUMERIC,
    error_code              VARCHAR(64),
    error_message           VARCHAR(1024),
    CONSTRAINT fk_report_id FOREIGN KEY (list_id)
       REFERENCES list_details (id) MATCH SIMPLE ON DELETE CASCADE
);
