CREATE TABLE IF NOT EXISTS list_versions
(
    id                      UUID            NOT NULL PRIMARY KEY,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(1024),
    entity_type_id          UUID            NOT NULL,
    fql_query               VARCHAR(1024),
    user_friendly_query     VARCHAR(1024),
    fields                  VARCHAR[],
    updated_by              UUID            NOT NULL,
    updated_by_username     VARCHAR(64)     NOT NULL,
    updated_date            TIMESTAMP       NOT NULL,
    is_active               boolean         NOT NULL,
    is_private              boolean         NOT NULL,
    version                 integer         NOT NULL,
    list_id                 UUID            NOT NULL,
    CONSTRAINT fk_report_id FOREIGN KEY (list_id)
                    REFERENCES list_details (id) MATCH SIMPLE,
);
 CREATE INDEX IF NOT EXISTS idx_versions_list_id ON list_versions (version, list_id);
