CREATE UNIQUE INDEX IF NOT EXISTS idx_sort_seq
  ON list_contents USING btree
  (list_id ASC NULLS LAST, refresh_id ASC NULLS LAST, sort_seq ASC NULLS LAST);
