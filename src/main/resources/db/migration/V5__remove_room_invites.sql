UPDATE rooms
SET visibility_id = (SELECT id FROM visibilities WHERE code = 'PUBLIC')
WHERE visibility_id = (SELECT id FROM visibilities WHERE code = 'INVITE_ONLY');

DROP TABLE room_invites;

DELETE FROM visibilities WHERE code = 'INVITE_ONLY';
