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

package org.grails.orm.hibernate.cfg.domainbinding

import org.grails.orm.hibernate.cfg.ColumnConfig
import org.grails.orm.hibernate.cfg.domainbinding.hibernate.HibernatePersistentProperty
import org.grails.orm.hibernate.cfg.PersistentEntityNamingStrategy
import org.hibernate.usertype.CompositeUserType
import spock.lang.Specification
import spock.lang.Unroll

import org.grails.orm.hibernate.cfg.domainbinding.util.BackticksRemover
import org.grails.orm.hibernate.cfg.domainbinding.util.ColumnNameForPropertyAndPathFetcher
import org.grails.orm.hibernate.cfg.domainbinding.util.CompositeUserTypeColumns
import org.grails.orm.hibernate.cfg.domainbinding.util.DefaultColumnNameFetcher

class ColumnNameForPropertyAndPathFetcherSpec extends Specification {

    def backticksRemover = new BackticksRemover()

    def "when grailsProp returns a column name then it is used"() {
        given:
        def namingStrategy = Mock(PersistentEntityNamingStrategy)
        def defaultColumnFetcher = Mock(DefaultColumnNameFetcher)
        def fetcher = new ColumnNameForPropertyAndPathFetcher(
                namingStrategy,
                defaultColumnFetcher,
                backticksRemover
        )

        def grailsProp = Mock(HibernatePersistentProperty)
        def cc = Mock(ColumnConfig)

        grailsProp.getColumnName(cc) >> "explicit_col"

        when:
        def result = fetcher.getColumnNameForPropertyAndPath(grailsProp, "somePath", cc)

        then:
        result == "explicit_col"
    }

    @Unroll
    def "when grailsProp returns null then builds from path '#path' and default column '#defaultCol' with backticks removed"() {
        given:
        def namingStrategy = Mock(PersistentEntityNamingStrategy)
        def defaultColumnFetcher = Mock(DefaultColumnNameFetcher)
        def fetcher = new ColumnNameForPropertyAndPathFetcher(
                namingStrategy,
                defaultColumnFetcher,
                backticksRemover
        )

        def grailsProp = Mock(HibernatePersistentProperty)

        grailsProp.getColumnName(null) >> null
        namingStrategy.resolveColumnName(path) >> resolvedPath
        defaultColumnFetcher.getDefaultColumnName(grailsProp) >> defaultCol

        when:
        def result = fetcher.getColumnNameForPropertyAndPath(grailsProp, path, null)

        then:
        result == expected

        where:
        path              | resolvedPath       | defaultCol           || expected
        "`order`"        | "`order`"          | "`customer_id`"      || "order_customer_id"
        "invoice"        | "invoice"          | "line_item_id"       || "invoice_line_item_id"
    }

    def "when grailsProp returns null and path is empty falls back to default column name only"() {
        given:
        def namingStrategy = Mock(PersistentEntityNamingStrategy)
        def defaultColumnFetcher = Mock(DefaultColumnNameFetcher)
        def fetcher = new ColumnNameForPropertyAndPathFetcher(
                namingStrategy,
                defaultColumnFetcher,
                backticksRemover
        )

        def grailsProp = Mock(HibernatePersistentProperty)

        grailsProp.getColumnName(null) >> null
        defaultColumnFetcher.getDefaultColumnName(grailsProp) >> "only_default"

        when:
        def result = fetcher.getColumnNameForPropertyAndPath(grailsProp, null, null)

        then:
        result == "only_default"
    }

    def "the column names of a property with one column are that column name, resolved without a column configuration"() {
        given:
        def namingStrategy = Mock(PersistentEntityNamingStrategy)
        def defaultColumnFetcher = Mock(DefaultColumnNameFetcher)
        def compositeUserTypeColumns = Mock(CompositeUserTypeColumns)
        def fetcher = new ColumnNameForPropertyAndPathFetcher(
                namingStrategy,
                defaultColumnFetcher,
                backticksRemover,
                compositeUserTypeColumns
        )
        def grailsProp = Mock(HibernatePersistentProperty) {
            getCompositeUserType() >> null
            getColumnName(null) >> "`explicit_col`"
        }

        when:
        def result = fetcher.getColumnNamesForPropertyAndPath(grailsProp, "somePath")

        then:
        result == ["`explicit_col`"]
        0 * compositeUserTypeColumns._
    }

    def "the column names of a property mapped with a composite user type are the names of its attribute columns"() {
        given:
        def namingStrategy = Mock(PersistentEntityNamingStrategy)
        def defaultColumnFetcher = Mock(DefaultColumnNameFetcher)
        def compositeUserTypeColumns = Mock(CompositeUserTypeColumns)
        def fetcher = new ColumnNameForPropertyAndPathFetcher(
                namingStrategy,
                defaultColumnFetcher,
                backticksRemover,
                compositeUserTypeColumns
        )
        def grailsProp = Mock(HibernatePersistentProperty) {
            getCompositeUserType() >> CompositeUserType
        }
        compositeUserTypeColumns.columnNamesOf(grailsProp, "somePath") >> ["amount_amount", "amount_currency"]

        when:
        def result = fetcher.getColumnNamesForPropertyAndPath(grailsProp, "somePath")

        then: "the property is not treated as a single column"
        result == ["amount_amount", "amount_currency"]
        0 * grailsProp.getColumnName(_)
        0 * defaultColumnFetcher._
    }
}
