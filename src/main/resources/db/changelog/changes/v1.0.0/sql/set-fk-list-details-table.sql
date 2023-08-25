ALTER TABLE list_details ADD CONSTRAINT fk_in_progress_refresh FOREIGN KEY (in_progress_refresh_id)
                                    REFERENCES list_refresh_details (id) MATCH SIMPLE ;
ALTER TABLE list_details ADD CONSTRAINT fk_success_refresh FOREIGN KEY (success_refresh_id)
                                    REFERENCES list_refresh_details (id) MATCH SIMPLE ;
ALTER TABLE list_details ADD CONSTRAINT fk_failed_refresh FOREIGN KEY (failed_refresh_id)
                                    REFERENCES list_refresh_details (id) MATCH SIMPLE ;
