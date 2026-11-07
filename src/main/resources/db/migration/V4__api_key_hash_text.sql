-- V1 declared api_keys.key_hash as char(64) (fixed-width, space-padded). Now that
-- ApiKey is a mapped JPA entity, Hibernate ddl-auto: validate expects a varchar/text
-- for the String field. Switch to text - the correct type for a hex hash and
-- consistent with every other string column. The UNIQUE + NOT NULL constraints are
-- preserved by the type change.
alter table api_keys alter column key_hash type text;
