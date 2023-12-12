CREATE TABLE IF NOT EXISTS list_versions
(
    id                      UUID            NOT NULL PRIMARY KEY,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(1024)           ,
    fql_query               TEXT                    ,
    user_friendly_query     TEXT                    ,
    fields                  VARCHAR[]               ,
    updated_by              UUID            NOT NULL,
    updated_by_username     VARCHAR(1024)   NOT NULL,
    updated_date            TIMESTAMP       NOT NULL,
    is_active               boolean         NOT NULL,
    is_private              boolean         NOT NULL,
    version                 integer         NOT NULL,
    list_id                 UUID            NOT NULL,
    CONSTRAINT fk_report_id FOREIGN KEY (list_id)
                    REFERENCES list_details (id) MATCH SIMPLE
);

 CREATE INDEX IF NOT EXISTS idx_list_id_version ON list_versions (list_id,version);
