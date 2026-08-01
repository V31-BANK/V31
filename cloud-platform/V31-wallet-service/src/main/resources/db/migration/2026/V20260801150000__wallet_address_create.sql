create table wallet_address (
    id                 uuid          not null,
    created_by         varchar(64),
    created_date       timestamptz(6),
    last_modified_by   varchar(64),
    last_modified_date timestamptz(6),
    address            varchar(128)   not null,
    label              varchar(100)   not null,
    asset              varchar(16)    not null,
    status             varchar(20)    not null,
    constraint wallet_address_pkey primary key (id),
    constraint uk_wallet_address_address unique (address),
    constraint wallet_address_status_check check (status in ('PENDING', 'ACTIVE', 'BLOCKED'))
);
