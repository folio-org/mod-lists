CREATE TABLE IF NOT EXISTS list_details
(
    id                      UUID            NOT NULL PRIMARY KEY,
    name                    VARCHAR(255)    NOT NULL,
    description             VARCHAR(1024),
    entity_type_id          UUID            NOT NULL,
    fql_query               VARCHAR(1024),
    user_friendly_query     VARCHAR(1024),
    fields                  VARCHAR[],
    created_by              UUID            NOT NULL,
    created_by_username     VARCHAR(64)     NOT NULL,
    created_date            TIMESTAMP       NOT NULL,
    is_active               boolean         NOT NULL,
    is_private              boolean         NOT NULL,
    is_canned               boolean         NOT NULL,
    updated_by              UUID,
    updated_by_username     VARCHAR(64),
    updated_date            TIMESTAMP,
    in_progress_refresh_id  UUID,
    success_refresh_id      UUID,
    failed_refresh_id       UUID,
    version                 integer         NOT NULL
);
