/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.v31bank.compliance.infra.persistence.jooq;

import java.io.Serial;
import java.time.Instant;
import java.util.UUID;

import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import org.v31bank.jooq.audit.AuditColumns;

/**
 * The {@code compliance_case} table, as jOOQ sees it.
 * <p>
 * This is written by hand rather than generated. Code generation would need a live
 * database at build time, which would make the build fail wherever one is not reachable —
 * a fresh checkout, a build agent — for a service with a single table. What is written
 * here is the same shape the generator emits, and moving to generation later replaces
 * this file without touching anything that uses it.
 * <p>
 * It extends {@link TableImpl} and declares a primary key on purpose: that is what makes
 * {@link ComplianceCaseRecord} an updatable record, and updatable records are what the
 * audit listener registered by the jOOQ starter listens to. A table without a key would
 * still read and write, but nothing would fill in who wrote the row and when.
 * <p>
 * The fields are named in upper case, against the surrounding convention, because that is
 * how a generated table reads and how every jOOQ query is written:
 * {@code COMPLIANCE_CASE.CASE_NUMBER.eq(...)}.
 *
 * @author Xander Wang
 * @since 0.2.0
 */
public final class ComplianceCaseTable extends TableImpl<ComplianceCaseRecord> {

	/**
	 * The single instance, referenced statically by the queries that use it.
	 */
	public static final ComplianceCaseTable COMPLIANCE_CASE = new ComplianceCaseTable();

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * {@code compliance_case.id}, a time-ordered UUIDv7 issued by the audit listener when
	 * the caller does not supply one.
	 */
	public final TableField<ComplianceCaseRecord, UUID> ID = createField(DSL.name(AuditColumns.ID),
			SQLDataType.UUID.nullable(false), this);

	/**
	 * {@code compliance_case.created_by}, filled in by the audit listener.
	 */
	public final TableField<ComplianceCaseRecord, String> CREATED_BY = createField(DSL.name(AuditColumns.CREATED_BY),
			SQLDataType.VARCHAR(64), this);

	/**
	 * {@code compliance_case.created_date}, filled in by the audit listener.
	 */
	public final TableField<ComplianceCaseRecord, Instant> CREATED_DATE = createField(
			DSL.name(AuditColumns.CREATED_DATE), SQLDataType.INSTANT, this);

	/**
	 * {@code compliance_case.last_modified_by}, filled in by the audit listener.
	 */
	public final TableField<ComplianceCaseRecord, String> LAST_MODIFIED_BY = createField(
			DSL.name(AuditColumns.LAST_MODIFIED_BY), SQLDataType.VARCHAR(64), this);

	/**
	 * {@code compliance_case.last_modified_date}, filled in by the audit listener.
	 */
	public final TableField<ComplianceCaseRecord, Instant> LAST_MODIFIED_DATE = createField(
			DSL.name(AuditColumns.LAST_MODIFIED_DATE), SQLDataType.INSTANT, this);

	/**
	 * {@code compliance_case.case_number}, unique.
	 */
	public final TableField<ComplianceCaseRecord, String> CASE_NUMBER = createField(DSL.name("case_number"),
			SQLDataType.VARCHAR(32).nullable(false), this);

	/**
	 * {@code compliance_case.customer_id}, the customer under investigation.
	 */
	public final TableField<ComplianceCaseRecord, UUID> CUSTOMER_ID = createField(DSL.name("customer_id"),
			SQLDataType.UUID.nullable(false), this);

	/**
	 * {@code compliance_case.type}, one of
	 * {@link org.v31bank.compliance.domain.constant.ComplianceCaseType}.
	 */
	public final TableField<ComplianceCaseRecord, String> TYPE = createField(DSL.name("type"),
			SQLDataType.VARCHAR(20).nullable(false), this);

	/**
	 * {@code compliance_case.status}, one of
	 * {@link org.v31bank.compliance.domain.constant.ComplianceCaseStatus}.
	 */
	public final TableField<ComplianceCaseRecord, String> STATUS = createField(DSL.name("status"),
			SQLDataType.VARCHAR(20).nullable(false), this);

	/**
	 * {@code compliance_case.summary}, why the case was opened.
	 */
	public final TableField<ComplianceCaseRecord, String> SUMMARY = createField(DSL.name("summary"),
			SQLDataType.VARCHAR(500), this);

	/**
	 * Declared after the fields it names, since instance initialisers run in the order
	 * they are written.
	 */
	private final transient UniqueKey<ComplianceCaseRecord> primaryKey = Internal.createUniqueKey(this,
			DSL.name("compliance_case_pkey"), this.ID);

	private ComplianceCaseTable() {
		super(DSL.name("compliance_case"));
	}

	@Override
	public Class<ComplianceCaseRecord> getRecordType() {
		return ComplianceCaseRecord.class;
	}

	@Override
	public UniqueKey<ComplianceCaseRecord> getPrimaryKey() {
		return this.primaryKey;
	}

}
