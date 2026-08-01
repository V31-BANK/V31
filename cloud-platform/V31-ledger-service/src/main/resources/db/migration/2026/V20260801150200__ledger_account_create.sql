create table ledger_account (
    id                 uuid          not null,
    created_by         varchar(64),
    created_date       timestamptz(6),
    last_modified_by   varchar(64),
    last_modified_date timestamptz(6),
    code               varchar(32)    not null,
    name               varchar(100)   not null,
    type               varchar(20)    not null,
    status             varchar(20)    not null,
    constraint ledger_account_pkey primary key (id),
    constraint uk_ledger_account_code unique (code),
    constraint ledger_account_status_check check (status in ('ACTIVE', 'CLOSED')),
    constraint ledger_account_type_check check (type in ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'))
);
