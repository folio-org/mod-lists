CREATE TABLE list_contents
(
    list_id         UUID  NOT NULL,
    refresh_id      UUID NOT NULL,
    content_id      UUID  NOT NULL,
    sort_seq        numeric NOT NULL,
    CONSTRAINT  pk_composite PRIMARY KEY (list_id, refresh_id, content_id),
    CONSTRAINT fk_report_id FOREIGN KEY (list_id)
                REFERENCES list_details (id) MATCH SIMPLE,
    CONSTRAINT fk_refresh_id FOREIGN KEY (refresh_id)
                REFERENCES list_refresh_details (id) MATCH SIMPLE
) PARTITION BY HASH(list_id);

-- Script to create the child tables for each partition
CREATE TABLE list_contents_00 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 0);
CREATE TABLE list_contents_01 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 1);
CREATE TABLE list_contents_02 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 2);
CREATE TABLE list_contents_03 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 3);
CREATE TABLE list_contents_04 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 4);
CREATE TABLE list_contents_05 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 5);
CREATE TABLE list_contents_06 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 6);
CREATE TABLE list_contents_07 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 7);
CREATE TABLE list_contents_08 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 8);
CREATE TABLE list_contents_09 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 9);
CREATE TABLE list_contents_10 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 10);
CREATE TABLE list_contents_11 PARTITION OF list_contents FOR VALUES WITH (MODULUS 12, REMAINDER 11);
