create table compliance_case (
    id                 uuid           not null,
    created_by         varchar(64),
    created_date       timestamptz(6),
    last_modified_by   varchar(64),
    last_modified_date timestamptz(6),
    case_number        varchar(32)    not null,
    customer_id        uuid           not null,
    type               varchar(20)    not null,
    status             varchar(20)    not null,
    summary            varchar(500),
    constraint compliance_case_pkey primary key (id),
    constraint uk_compliance_case_case_number unique (case_number),
    constraint compliance_case_type_check check (type in ('KYC', 'AML', 'SANCTIONS', 'FRAUD')),
    constraint compliance_case_status_check check (status in ('OPEN', 'IN_REVIEW', 'ESCALATED', 'CLOSED'))
);

create index idx_compliance_case_customer on compliance_case (customer_id);
