# Database Migrations

Schema for this service is owned by Flyway. Hibernate runs with `ddl-auto: validate`
and never creates or alters tables — it only fails startup when the entities and the
live schema disagree.

## Schema Ownership

Every service in the platform points at the same database, so each one owns a schema
named after it and nothing writes outside its own.

This is not tidiness. Flyway records what it has applied in a `flyway_schema_history`
table inside the schema it manages. Two services sharing `public` share that one table,
and each would find migrations in it that it does not have on its classpath — which
Flyway treats as a corrupted history and refuses to start on. The second service
deployed would simply not come up.

```yaml
spring:
  flyway:
    schemas: customer
    default-schema: customer
    create-schemas: true
  jpa:
    properties:
      hibernate:
        default_schema: customer
```

A service using jOOQ rather than JPA has no `default_schema` to set, because its
generated tables are named without one and resolve against the connection's
`search_path`. There the schema is set on the pool instead, so every connection is
already on it before jOOQ sees it:

```yaml
spring:
  datasource:
    hikari:
      schema: compliance
```

### Moving a service that was already in `public`

Only needed once, for a service whose tables were created before this convention. It is
not a migration and cannot be one: Flyway cannot record a change to where its own
history lives.

```sql
create schema if not exists customer;
alter table public.customer              set schema customer;
alter table public.customer_category     set schema customer;
alter table public.flyway_schema_history set schema customer;
```

Constraints and indexes follow their tables, and the history moves with them, so the
next startup finds the migrations it already applied and adds only the new ones. Run it
before the service starts with the schema configured, not after — otherwise Flyway finds
an empty schema and re-applies everything into it, leaving the real data stranded in
`public`.

Both naming rules below are enforced by the `validateMigrationNames` Gradle task, which
runs before resources are processed — a malformed name or a reused version fails the
build rather than the deployment.

## Versioned Migrations

Applied once, in version order.

```
V<yyyyMMddHHmmss>__<table>_<verb>[_<detail>].sql
```

```
^V\d{14}__[a-z][a-z0-9_]*_(create|drop|add|alter|rename|index|constraint|seed|backfill)(_[a-z0-9_]+)?\.sql$
```

Examples:

```
V20260729010000__customer_create.sql
V20260803142530__customer_add_kyc_status.sql
V20260810093015__customer_alter_email_length.sql
V20260825090000__customer_index_status.sql
```

The version is a zero-padded timestamp rather than a sequence number, so migrations
authored on parallel branches never collide on merge. The table name always occupies
the same position, which keeps `ls **/*__customer_*` a reliable way to read the history
of a single table.

## Repeatable Migrations

Re-applied whenever their checksum changes, always after every pending versioned
migration. Use them for objects that describe a desired end state rather than a
historical change — the file holds the current definition, and its diff is the change
history.

```
R__<object>_<kind>.sql
```

```
^R__[a-z][a-z0-9_]*_(view|function|procedure|trigger)\.sql$
```

Examples:

```
R__customer_summary_view.sql
R__customer_risk_score_function.sql
```

Write them idempotently — `create or replace view` rather than `create view` — since
they run again on every checksum change.

## Verbs

A closed vocabulary — pick one, do not invent new ones.

| Verb         | Use for                                                     |
|--------------|-------------------------------------------------------------|
| `create`     | Creating a table                                             |
| `drop`       | Dropping a table, or dropping a column                       |
| `add`        | Adding a column                                              |
| `alter`      | Changing a column type, length, nullability, or default      |
| `rename`     | Renaming a table or a column                                 |
| `index`      | Creating or dropping an index                                |
| `constraint` | Unique, foreign key, or check constraints                    |
| `seed`       | Reference or lookup data shipped with the schema             |
| `backfill`   | Populating existing rows, typically alongside a new column   |

## Layout

Migrations are grouped into one directory per year. Flyway scans
`classpath:db/migration` recursively, so the grouping is purely for browsing and does
not affect ordering — execution order is global and determined by the version alone.

```
db/migration/
  2026/
    V20260729010000__customer_create.sql
  2027/
```

## Rules

1. **Never modify a migration that has been applied.** Flyway stores a checksum in
   `flyway_schema_history`; editing an applied file fails the next startup with a
   checksum mismatch. Correct a mistake by adding a new migration.
2. **One logical change per file.** Keeps failures easy to locate and review diffs
   readable.
3. **Ship data with the schema change that needs it.** A new `not null` column means
   add the column, backfill existing rows, then apply the constraint — all in the same
   file, so it either fully succeeds or fully rolls back.
4. **Write PostgreSQL.** These scripts are not portable and are not meant to be;
   integration tests run against a real PostgreSQL instance, never an in-memory
   substitute.
5. **Use `R__` for repeatable objects.** Views, functions, and procedures describe a
   desired end state rather than a historical change, and are re-applied whenever their
   checksum changes.
