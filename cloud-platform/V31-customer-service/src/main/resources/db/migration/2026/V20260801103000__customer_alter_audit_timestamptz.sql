-- Audit timestamps become timestamptz: a wall-clock reading cannot identify the
-- moment a record was written, which an audit trail spanning regions depends on.
--
-- Existing rows hold wall-clock values written by LocalDateTime.now() on a host
-- running in Asia/Shanghai, so they are interpreted in that zone to preserve the
-- instant each one actually stands for. Without the USING clause PostgreSQL
-- would read them in the session's TimeZone instead, making the result depend on
-- who ran the migration.
alter table customer
    alter column created_date type timestamptz(6)
        using created_date at time zone 'Asia/Shanghai',
    alter column last_modified_date type timestamptz(6)
        using last_modified_date at time zone 'Asia/Shanghai';
