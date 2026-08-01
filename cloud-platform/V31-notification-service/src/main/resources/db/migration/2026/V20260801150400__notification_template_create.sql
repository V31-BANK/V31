create table notification_template (
    id                 uuid          not null,
    created_by         varchar(64),
    created_date       timestamptz(6),
    last_modified_by   varchar(64),
    last_modified_date timestamptz(6),
    code               varchar(32)    not null,
    name               varchar(100)   not null,
    channel            varchar(20)    not null,
    status             varchar(20)    not null,
    constraint notification_template_pkey primary key (id),
    constraint uk_notification_template_code unique (code),
    constraint notification_template_status_check check (status in ('DRAFT', 'ACTIVE', 'RETIRED')),
    constraint notification_template_channel_check check (channel in ('EMAIL', 'SMS', 'PUSH', 'WEBHOOK'))
);
