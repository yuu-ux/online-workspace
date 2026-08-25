UPDATE rooms
SET visibility_id = (SELECT id FROM visibilities WHERE code = 'PUBLIC')
WHERE visibility_id = (SELECT id FROM visibilities WHERE code = 'FRIENDS_ONLY');

DELETE FROM visibilities WHERE code = 'FRIENDS_ONLY';
