UPDATE list_details SET name = 'Missing items', description = 'Returns all items with a status of: missing, aged to lost, claimed returned, declared lost, long missing'
   where id = '605a345f-f456-4ab2-8968-22f49cf1fbb6';

UPDATE list_details SET name = 'Inactive patrons with open loans', description = 'Returns all inactive users with a loan status of open'
   where id = '97f5829f-1510-47bc-8454-ae7fa849baef';
