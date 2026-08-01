create table transfer_limit (
    id                 uuid          not null,
    created_by         varchar(64),
    created_date       timestamptz(6),
    last_modified_by   varchar(64),
    last_modified_date timestamptz(6),
    code               varchar(32)    not null,
    name               varchar(100)   not null,
    daily_max          numeric(38,18),
    status             varchar(20)    not null,
    constraint transfer_limit_pkey primary key (id),
    constraint uk_transfer_limit_code unique (code),
    constraint transfer_limit_status_check check (status in ('ACTIVE', 'SUSPENDED'))
);
