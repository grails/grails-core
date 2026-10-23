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
package org.grails.orm.hibernate.cfg.domainbinding.secondpass

import grails.gorm.annotation.Entity
import grails.gorm.tests.HibernateGormDatastoreSpec
import org.hibernate.boot.model.relational.internal.SqlStringGenerationContextImpl
import org.hibernate.boot.spi.MetadataBuildingContext
import org.hibernate.dialect.DB2Dialect
import org.hibernate.dialect.Dialect
import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.MariaDBDialect
import org.hibernate.dialect.MySQLDialect
import org.hibernate.dialect.OracleDialect
import org.hibernate.dialect.PostgreSQLDialect
import org.hibernate.dialect.SQLServerDialect
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl
import org.hibernate.mapping.Collection
import org.hibernate.mapping.Column
import org.hibernate.mapping.Table
import org.hibernate.mapping.UniqueKey
import spock.lang.Unroll

/**
 * The join table two unidirectional hasMany associations to the same class share gets one implicit unique key from
 * Hibernate, over the first association's key column and the element column, and nothing for the second. The binder
 * gives every association a constraint of its own over its key column and the element, in the shape the database
 * needs: a unique key where nulls are distinct or the dialect renders a filtered unique index, and on Oracle, which
 * compares the non-null columns of a row, one unique key over every association's key column and the element. An association with a join table of its own, and one whose key the other side
 * owns, are left as Hibernate binds them.
 */
class SharedJoinTableUniqueKeyBinderSpec extends HibernateGormDatastoreSpec {

    void setupSpec() {
        manager.registerDomainClasses(SjtOwner, SjtSeparateOwner, SjtParent, SjtChild, SjtRole)
    }

    private Collection collectionOf(Class owner, String property) {
        datastore.metadata.getCollectionBinding("${owner.name}.${property}".toString())
    }

    private static List<List<String>> uniqueKeyColumns(Table table) {
        table.uniqueKeys.values().collect { UniqueKey key -> key.columns*.name.sort() }.sort()
    }

    void "the shared join table has a unique key per association, over its own key column and the element"() {
        given:
        Collection roles = collectionOf(SjtOwner, 'roles')
        Collection denyRoles = collectionOf(SjtOwner, 'denyRoles')

        expect: 'both associations share the table'
        roles.collectionTable.is(denyRoles.collectionTable)
        roles.collectionTable.columns*.name.sort() == ['sjt_owner_deny_roles_id', 'sjt_owner_roles_id', 'sjt_role_id']

        and: 'each association is unique by its own key column and the element; H2 treats nulls as distinct'
        uniqueKeyColumns(roles.collectionTable) == [
                ['sjt_owner_deny_roles_id', 'sjt_role_id'],
                ['sjt_owner_roles_id', 'sjt_role_id']
        ]
        roles.collectionTable.indexes.isEmpty()

        and: 'both keys are updatable, so rows of both associations are written'
        roles.key.updateable
        denyRoles.key.updateable
    }

    void "an association with a join table of its own keeps the unique key Hibernate creates for it"() {
        given:
        Collection roles = collectionOf(SjtSeparateOwner, 'roles')
        Collection denyRoles = collectionOf(SjtSeparateOwner, 'denyRoles')

        expect:
        !roles.collectionTable.is(denyRoles.collectionTable)
        uniqueKeyColumns(roles.collectionTable) == [['sjt_role_id', 'sjt_separate_owner_roles_id']]
        uniqueKeyColumns(denyRoles.collectionTable) == [['sjt_role_id', 'sjt_separate_owner_deny_roles_id']]
    }

    void "a bidirectional one-to-many has no join table and is left alone"() {
        given:
        Collection children = collectionOf(SjtParent, 'children')

        expect:
        children.inverse
        children.collectionTable.name == 'sjt_child'
        children.collectionTable.uniqueKeys.isEmpty()
    }

    void "bind leaves a collection whose key the other side owns alone"() {
        given: 'the bidirectional collection, whose key column lives in the child table'
        Collection children = collectionOf(SjtParent, 'children')
        SharedJoinTableUniqueKeyBinder binder = new SharedJoinTableUniqueKeyBinder(Stub(MetadataBuildingContext), new H2Dialect())

        when:
        binder.bind(children)

        then:
        children.collectionTable.uniqueKeys.isEmpty()
        children.collectionTable.indexes.isEmpty()
    }

    @Unroll
    void "#dialect.class.simpleName resolves to #expected"() {
        expect:
        SharedJoinTableUniqueness.forDialect(dialect) == expected

        where:
        dialect                   | expected
        new H2Dialect()           | SharedJoinTableUniqueness.DEFAULT
        new PostgreSQLDialect()   | SharedJoinTableUniqueness.DEFAULT
        new MySQLDialect()        | SharedJoinTableUniqueness.DEFAULT
        new MariaDBDialect()      | SharedJoinTableUniqueness.DEFAULT
        new SQLServerDialect()    | SharedJoinTableUniqueness.DEFAULT
        new DB2Dialect()          | SharedJoinTableUniqueness.DEFAULT
        new OracleDialect()       | SharedJoinTableUniqueness.ORACLE
    }

    @Unroll
    void "on #dialect.class.simpleName the shared table is given #what"() {
        given: 'a shared table as Hibernate leaves it: the first association has its unique key, the second nothing'
        Table table = new Table('orm', 'shared_roles')
        Column roles = nullable('roles_id')
        Column denyRoles = nullable('deny_roles_id')
        Column role = nullable('role_id')
        [roles, denyRoles, role].each { table.addColumn(it) }
        UniqueKey first = new UniqueKey(table)
        first.name = 'uk_roles_id_role_id'
        first.addColumn(roles)
        first.addColumn(role)
        table.addUniqueKey(first)
        SharedJoinTableUniqueness.Naming naming = [
                uniqueKeyName: { List<Column> columns -> 'uk_' + columns*.name.join('_') },
                indexName    : { List<Column> columns -> 'ix_' + columns*.name.join('_') }
        ] as SharedJoinTableUniqueness.Naming

        when: 'the binder runs for each association, as it does after the keys of each are created'
        SharedJoinTableUniqueness uniqueness = SharedJoinTableUniqueness.forDialect(dialect)
        uniqueness.apply(table, [roles], [role], naming)
        uniqueness.apply(table, [denyRoles], [role], naming)

        then: 'the DDL the dialect renders for what the table now holds, in its create table statement or after it'
        ddl(dialect, table) == statements

        where:
        dialect                 | what                                            | statements
        new H2Dialect()         | 'a unique key per association'                  | [
                ', unique (roles_id, role_id), unique (deny_roles_id, role_id)']
        new PostgreSQLDialect() | 'a unique key per association'                  | [
                ', unique (roles_id, role_id), unique (deny_roles_id, role_id)']
        new MySQLDialect()      | 'a unique key per association'                  | [
                'alter table shared_roles add constraint uk_roles_id_role_id unique (roles_id, role_id)',
                'alter table shared_roles add constraint uk_deny_roles_id_role_id unique (deny_roles_id, role_id)']
        new SQLServerDialect()  | 'a filtered unique index per association'       | [
                'create unique nonclustered index uk_roles_id_role_id on shared_roles (roles_id, role_id) where roles_id is not null and role_id is not null',
                'create unique nonclustered index uk_deny_roles_id_role_id on shared_roles (deny_roles_id, role_id) where deny_roles_id is not null and role_id is not null']
        new DB2Dialect()        | 'a unique index per association excluding nulls' | [
                'create unique index uk_roles_id_role_id on shared_roles (roles_id, role_id) exclude null keys',
                'create unique index uk_deny_roles_id_role_id on shared_roles (deny_roles_id, role_id) exclude null keys']
        new OracleDialect()     | 'one unique key over both key columns and the element' | [
                ', unique (roles_id, role_id, deny_roles_id)']
    }

    void "on Oracle a table one association has to itself keeps the unique key Hibernate creates"() {
        given:
        Table table = new Table('orm', 'own_roles')
        Column roles = nullable('roles_id')
        Column role = nullable('role_id')
        [roles, role].each { table.addColumn(it) }
        UniqueKey first = new UniqueKey(table)
        first.name = 'uk_roles_id_role_id'
        first.addColumn(roles)
        first.addColumn(role)
        table.addUniqueKey(first)

        when:
        SharedJoinTableUniqueness.ORACLE.apply(table, [roles], [role], Stub(SharedJoinTableUniqueness.Naming))

        then:
        table.uniqueKeys.keySet() == ['uk_roles_id_role_id'] as Set
        table.uniqueKeys['uk_roles_id_role_id'].columns*.name == ['roles_id', 'role_id']
    }

    private static Column nullable(String name) {
        Column column = new Column(name)
        column.nullable = true
        column
    }

    /** The DDL the dialect renders for the unique keys and indexes of the table, in that order. */
    private List<String> ddl(Dialect dialect, Table table) {
        def jdbcEnvironment = new JdbcEnvironmentImpl(datastore.sessionFactory.serviceRegistry, dialect)
        def context = SqlStringGenerationContextImpl.forTests(jdbcEnvironment)
        def metadata = datastore.metadata
        List<String> statements = []
        table.uniqueKeys.values().each { UniqueKey key ->
            String alter = dialect.uniqueDelegate.getAlterTableToAddUniqueKeyCommand(key, metadata, context)
            if (alter) {
                statements << alter
            }
        }
        String inCreateTable = dialect.uniqueDelegate.getTableCreationUniqueConstraintsFragment(table, context)
        if (inCreateTable) {
            statements << inCreateTable
        }
        table.indexes.values().each { index ->
            statements.addAll(dialect.indexExporter.getSqlCreateStrings(index, metadata, context))
        }
        statements
    }
}

@Entity
class SjtOwner {
    Long id
    String name
    static hasMany = [roles: SjtRole, denyRoles: SjtRole]
}

@Entity
class SjtSeparateOwner {
    Long id
    String name
    static hasMany = [roles: SjtRole, denyRoles: SjtRole]
    static mapping = {
        roles joinTable: 'sjt_separate_owner_allow'
        denyRoles joinTable: 'sjt_separate_owner_deny'
    }
}

@Entity
class SjtRole {
    Long id
    String authority
}

@Entity
class SjtParent {
    Long id
    String name
    static hasMany = [children: SjtChild]
}

@Entity
class SjtChild {
    Long id
    String name
    static belongsTo = [parent: SjtParent]
}
