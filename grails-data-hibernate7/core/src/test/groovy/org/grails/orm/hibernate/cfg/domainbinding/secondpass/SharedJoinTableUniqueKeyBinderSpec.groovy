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
import org.hibernate.mapping.Collection
import org.hibernate.mapping.Table
import org.hibernate.mapping.UniqueKey
import spock.lang.Subject

/**
 * The join table two unidirectional hasMany associations to the same class share gets one implicit unique key from
 * Hibernate, over the first association's key column and the element column. The binder adds the key column of every
 * association that shares the table, so a row is unique by the key column of its own association and the element.
 * An association with a join table of its own, and one whose key the other side owns, are left as Hibernate binds them.
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

    void "the shared join table has one unique key over the key column of each association and the element"() {
        given:
        Collection roles = collectionOf(SjtOwner, 'roles')
        Collection denyRoles = collectionOf(SjtOwner, 'denyRoles')

        expect: 'both associations share the table'
        roles.collectionTable.is(denyRoles.collectionTable)
        roles.collectionTable.columns*.name.sort() == ['sjt_owner_deny_roles_id', 'sjt_owner_roles_id', 'sjt_role_id']

        and: 'its unique key covers both key columns and the element column'
        uniqueKeyColumns(roles.collectionTable) == [['sjt_owner_deny_roles_id', 'sjt_owner_roles_id', 'sjt_role_id']]

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

    void "bind leaves a collection without a join table or without a key alone"() {
        given:
        Collection children = collectionOf(SjtParent, 'children')
        int before = children.collectionTable.uniqueKeys.size()

        when:
        new Object()

        then:
        children.collectionTable.uniqueKeys.size() == before
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
