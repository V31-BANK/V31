create table customer_category (
    id                 uuid         not null,
    created_by         varchar(64),
    created_date       timestamp(6),
    last_modified_by   varchar(64),
    last_modified_date timestamp(6),
    parent_id          uuid,
    sort_order         integer,
    code               varchar(64)  not null,
    name               varchar(100) not null,
    status             varchar(20)  not null,
    constraint customer_category_pkey primary key (id),
    constraint uk_customer_category_code unique (code),
    constraint fk_customer_category_parent foreign key (parent_id) references customer_category (id),
    constraint customer_category_status_check check (status in ('ENABLED', 'DISABLED'))
);

create index idx_customer_category_parent on customer_category (parent_id);
