INSERT INTO list_details
  (id, name, description, entity_type_id, fql_query, fields, created_by, created_by_username, created_date, is_active, is_private, is_canned, version, user_friendly_query)
  values
  ('605a345f-f456-4ab2-8968-22f49cf1fbb6',
  'Missing Items',
  'Missing Items List',
  '0cb79a4c-f7eb-4941-a104-745224ae0292',
  '{"item_status": {"$in": ["missing", "aged to lost", "claimed returned", "declared lost", "long missing" ] }}',
  '{id, item_hrid, item_effective_call_number, item_effective_call_number_typeid, item_effective_call_number_type_name, item_holdings_record_id, item_status, item_copy_number, item_barcode, item_created_date, item_updated_date, item_effective_location_id, item_effective_location_name, item_effective_library_id, item_effective_library_name, item_effective_library_code, item_material_type_id, item_material_type, instance_id, instance_title, instance_created_date, instance_updated_date, instance_primary_contributor}',
  '7eb41f7c-ec2f-4637-8ceb-2e9573666ad0',
  'SYSTEM',
  Now(),
  true,
  false,
  true,
  1,
  'item_status in [missing, aged to lost, claimed returned, declared lost, long missing]'),

  ('97f5829f-1510-47bc-8454-ae7fa849baef',
  'Expired Patron Loan',
  'Expired Patron Loan List',
  '4e09d89a-44ed-418e-a9cc-820dfb27bf3a',
  '{"$and": [{"loan_status" : {"$eq": "Open"}}, {"user_active" : {"$eq": "false"}}]}',
  '{user_id, user_first_name, user_last_name, user_full_name, user_active, user_barcode, user_expiration_date, user_patron_group_id, user_patron_group, id, loan_status, loan_checkout_date, loan_due_date, loan_policy_id, loan_policy_name, loan_checkout_servicepoint_id, loan_checkout_servicepoint_name, item_holdingsrecord_id, instance_id, instance_title, instance_primary_contributor, item_id, item_barcode, item_status, item_material_type_id, item_material_type}',
  '7eb41f7c-ec2f-4637-8ceb-2e9573666ad0',
  'SYSTEM',
  Now(),
  true,
  false,
  true,
  1,
  '(loan_status == Open) AND (user_active == false)')
  ON CONFLICT DO NOTHING
  ;
