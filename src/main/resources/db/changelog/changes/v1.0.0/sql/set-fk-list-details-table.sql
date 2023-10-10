ALTER TABLE list_details DROP CONSTRAINT IF EXISTS fk_in_progress_refresh;
ALTER TABLE list_details ADD CONSTRAINT fk_in_progress_refresh FOREIGN KEY (in_progress_refresh_id)
                                    REFERENCES list_refresh_details (id) MATCH SIMPLE ;
ALTER TABLE list_details DROP CONSTRAINT IF EXISTS fk_success_refresh;
ALTER TABLE list_details ADD CONSTRAINT fk_success_refresh FOREIGN KEY (success_refresh_id)
                                    REFERENCES list_refresh_details (id) MATCH SIMPLE ;
ALTER TABLE list_details DROP CONSTRAINT IF EXISTS fk_failed_refresh;
ALTER TABLE list_details ADD CONSTRAINT fk_failed_refresh FOREIGN KEY (failed_refresh_id)
                                    REFERENCES list_refresh_details (id) MATCH SIMPLE ;
