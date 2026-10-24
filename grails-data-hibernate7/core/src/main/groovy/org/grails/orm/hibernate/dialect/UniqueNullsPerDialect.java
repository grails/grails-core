/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.grails.orm.hibernate.dialect;

import java.util.Arrays;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;

import org.grails.datastore.mapping.config.UniqueNulls;

/**
 * How the unique index Hibernate creates for a {@code unique} constraint with a group treats a {@code null} in the key,
 * on each database, so that the validation can read {@code null}s the way the index will.
 * <ul>
 * <li>{@link #ORACLE} and {@link #DB2}: Oracle indexes a row that has a {@code null} in some of the key columns and
 * compares its other columns, so {@code ('en', null)} twice is a violation, and only a key that is {@code null} in
 * every column is left out. Db2 for Linux, UNIX and Windows comes to the same: Hibernate creates the index with
 * {@code exclude null keys}. A {@code null} is a value: {@link UniqueNulls#VALUE}.</li>
 * <li>{@link #DB2_IBM_I}, {@link #DB2_ZOS} and {@link #DEFAULT}: H2, PostgreSQL, MySQL and MariaDB treat {@code null}s
 * as distinct, so such a row never conflicts. SQL Server compares them, but Hibernate renders the index for a nullable
 * key as a filtered unique index that leaves the rows with a {@code null} out, which comes to the same, and on Db2 for
 * IBM i and z/OS as {@code create unique where not null index}, under which {@code null}s are not equal.
 * {@link UniqueNulls#SKIP}.</li>
 * </ul>
 *
 * @since 9.0
 */
public enum UniqueNullsPerDialect {

    ORACLE(UniqueNulls.VALUE, OracleDialect.class),

    // Db2 for IBM i and z/OS: `create unique where not null index`, under which nulls are not equal; their dialects
    // extend the one for Linux, UNIX and Windows, so they come first
    DB2_IBM_I(UniqueNulls.SKIP, DB2iDialect.class),

    DB2_ZOS(UniqueNulls.SKIP, DB2zDialect.class),

    // Db2 for Linux, UNIX and Windows: `exclude null keys` leaves out only a key that is null in every column
    DB2(UniqueNulls.VALUE, DB2Dialect.class),

    DEFAULT(UniqueNulls.SKIP, Dialect.class);

    private final UniqueNulls uniqueNulls;
    private final Class<? extends Dialect> dialectClass;

    UniqueNullsPerDialect(UniqueNulls uniqueNulls, Class<? extends Dialect> dialectClass) {
        this.uniqueNulls = uniqueNulls;
        this.dialectClass = dialectClass;
    }

    /** The reading that fits the dialect, {@link #DEFAULT} unless the dialect is listed. */
    public static UniqueNullsPerDialect forDialect(Dialect dialect) {
        return Arrays.stream(values())
                .filter(entry -> entry != DEFAULT && entry.dialectClass.isInstance(dialect))
                .findFirst()
                .orElse(DEFAULT);
    }

    public UniqueNulls uniqueNulls() {
        return uniqueNulls;
    }
}
