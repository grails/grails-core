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
package org.grails.orm.hibernate.dialect

import org.hibernate.dialect.CockroachDialect
import org.hibernate.dialect.DB2Dialect
import org.hibernate.dialect.DB2iDialect
import org.hibernate.dialect.DB2zDialect
import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.MariaDBDialect
import org.hibernate.dialect.MySQLDialect
import org.hibernate.dialect.OracleDialect
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.dialect.SQLServerDialect
import spock.lang.Specification
import spock.lang.Unroll

import org.grails.datastore.mapping.config.UniqueNulls

class UniqueNullsPerDialectSpec extends Specification {

    @Unroll
    void '#dialect.class.simpleName reads a null in a unique key as #expected'() {
        expect:
        UniqueNullsPerDialect.forDialect(dialect).uniqueNulls() == expected

        where:
        dialect                 || expected
        new H2Dialect()         || UniqueNulls.SKIP
        new PostgreSQLDialect() || UniqueNulls.SKIP
        new CockroachDialect()  || UniqueNulls.SKIP
        new MySQLDialect()      || UniqueNulls.SKIP
        new MariaDBDialect()    || UniqueNulls.SKIP
        new SQLServerDialect()  || UniqueNulls.SKIP
        new OracleDialect()     || UniqueNulls.VALUE
        new DB2Dialect()        || UniqueNulls.VALUE
        new DB2iDialect()       || UniqueNulls.SKIP
        new DB2zDialect()       || UniqueNulls.SKIP
    }
}
