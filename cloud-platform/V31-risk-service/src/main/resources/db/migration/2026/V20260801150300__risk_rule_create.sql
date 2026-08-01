create table risk_rule (
    id                 uuid          not null,
    created_by         varchar(64),
    created_date       timestamptz(6),
    last_modified_by   varchar(64),
    last_modified_date timestamptz(6),
    code               varchar(32)    not null,
    name               varchar(100)   not null,
    severity           varchar(20)    not null,
    status             varchar(20)    not null,
    constraint risk_rule_pkey primary key (id),
    constraint uk_risk_rule_code unique (code),
    constraint risk_rule_status_check check (status in ('DRAFT', 'ACTIVE', 'DISABLED')),
    constraint risk_rule_severity_check check (severity in ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);
