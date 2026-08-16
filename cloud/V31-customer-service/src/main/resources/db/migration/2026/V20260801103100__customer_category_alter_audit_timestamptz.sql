-- Audit timestamps become timestamptz, matching the change made to customer.
--
-- This table is empty at the time of writing, so the USING clause converts
-- nothing; it is stated anyway so that the migration means the same thing on a
-- database that does hold rows, rather than falling back to the session's
-- TimeZone.
alter table customer_category
    alter column created_date type timestamptz(6)
        using created_date at time zone 'Asia/Shanghai',
    alter column last_modified_date type timestamptz(6)
        using last_modified_date at time zone 'Asia/Shanghai';
