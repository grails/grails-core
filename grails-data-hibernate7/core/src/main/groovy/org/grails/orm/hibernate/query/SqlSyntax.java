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

package org.grails.orm.hibernate.query;

import jakarta.persistence.criteria.CriteriaBuilder;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.NodeBuilder;

import org.grails.datastore.gorm.GormRegistry;
import org.grails.datastore.mapping.core.Datastore;
import org.grails.orm.hibernate.HibernateDatastore;
import org.grails.orm.hibernate.dialect.DialectKeyed;

/**
 * How a query text is read: what, besides string literals in single quotes, identifiers in double quotes or
 * backquotes and comments, it may hold. The syntax of native SQL is the database's, resolved from its Hibernate
 * dialect the way {@link RegexDialectPattern} resolves the regular expression operator, so that every reader of
 * native SQL in this package reads it as the database will: on SQL Server and Sybase, square brackets quote an
 * identifier; everywhere else they subscript an array or hold an array literal.
 *
 * @since 8.0.1
 */
public enum SqlSyntax implements DialectKeyed<SqlSyntax> {

    /**
     * Transact-SQL, as SQL Server and Sybase read it: {@code [square brackets]} quote an identifier, in which
     * {@code ]]} stands for a bracket. There are no arrays and no dollar-quoted strings.
     */
    TRANSACT_SQL(true, false, SQLServerDialect.class, SybaseDialect.class),

    /**
     * SQL as every other database reads it: square brackets subscript an array or hold an array literal, so they
     * group what they enclose the way parentheses do, and {@code $tag$...$tag$} is a string.
     */
    SQL(false, true, Dialect.class),

    /** HQL, which has neither bracket identifiers nor dollar-quoted strings; square brackets index. */
    HQL(false, false);

    private final boolean bracketsQuoteIdentifiers;
    private final boolean dollarQuotedStrings;
    private final Class<? extends Dialect>[] dialectClasses;

    @SafeVarargs
    SqlSyntax(boolean bracketsQuoteIdentifiers, boolean dollarQuotedStrings, Class<? extends Dialect>... dialectClasses) {
        this.bracketsQuoteIdentifiers = bracketsQuoteIdentifiers;
        this.dollarQuotedStrings = dollarQuotedStrings;
        this.dialectClasses = dialectClasses;
    }

    /** Returns the syntax of the native SQL of the database the dialect is for: {@link #SQL} unless its dialect is listed. */
    public static SqlSyntax forDialect(Dialect dialect) {
        return DialectKeyed.forDialect(SqlSyntax.class, dialect);
    }

    /** Returns the syntax of the native SQL of the database the session factory connects to. */
    public static SqlSyntax forSessionFactory(SessionFactory sessionFactory) {
        return forDialect(sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect());
    }

    /**
     * Returns the syntax of the native SQL of the database the criteria builder builds queries for, or {@link #SQL}
     * for a criteria builder that is not Hibernate's.
     */
    public static SqlSyntax forCriteriaBuilder(CriteriaBuilder criteriaBuilder) {
        return criteriaBuilder instanceof NodeBuilder nodeBuilder ? forDialect(nodeBuilder.getQueryEngine().getDialect()) : SQL;
    }

    /**
     * Returns the syntax of the native SQL of the database the entity is mapped to, or {@link #SQL} when its
     * datastore is not a Hibernate datastore.
     */
    public static SqlSyntax forEntity(Class<?> entityClass) {
        Datastore datastore = GormRegistry.findDatastore(entityClass);
        return datastore instanceof HibernateDatastore hibernateDatastore ?
                forSessionFactory(hibernateDatastore.getSessionFactory()) : SQL;
    }

    /** Whether {@code [square brackets]} quote an identifier, as in Transact-SQL. */
    public boolean bracketsQuoteIdentifiers() {
        return bracketsQuoteIdentifiers;
    }

    /** Whether {@code $tag$...$tag$} is a string, as in PostgreSQL. */
    public boolean hasDollarQuotedStrings() {
        return dollarQuotedStrings;
    }

    @Override
    public Class<? extends Dialect>[] dialectClasses() {
        return dialectClasses.clone();
    }
}
