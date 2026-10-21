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
package grails.gorm.tests

import grails.gorm.annotation.Entity

/**
 * An owner with several unidirectional hasMany associations writes an element added to any of them to the join table
 * of that association: a table of its own when the associations point at different classes or map join tables of their
 * own, or the one table two associations to the same class share, with a key column each. A sibling to-many that is not
 * bound as a collection, such as a serializable one, does not block the session factory.
 */
class MultipleUnidirectionalHasManySpec extends HibernateGormDatastoreSpec {

    void setupSpec() {
        manager.registerDomainClasses(MuhItem, MuhRole, MuhTag, MuhAccount, MuhNote, MuhGroup)
    }

    void cleanup() {
        MuhItem.withNewTransaction {
            MuhItem.executeUpdate('delete from MuhItem', [:])
            MuhAccount.executeUpdate('delete from MuhAccount', [:])
            MuhNote.executeUpdate('delete from MuhNote', [:])
            MuhGroup.executeUpdate('delete from MuhGroup', [:])
            MuhRole.executeUpdate('delete from MuhRole', [:])
            MuhTag.executeUpdate('delete from MuhTag', [:])
        }
    }

    void "an element added to one of two unidirectional hasMany associations is written to its join table"() {
        given: "a persisted item with two unidirectional hasMany associations to different classes"
        def item = new MuhItem(name: 'Example item').save(flush: true)

        when: "a role is added to the managed item and the item is flushed"
        item.addToRoles(new MuhRole(authority: 'ROLE_EXAMPLE'))
        item.save(flush: true)
        MuhItem.withSession { it.clear() }

        then: "the role is found through the reloaded item"
        MuhItem.get(item.id).roles*.authority == ['ROLE_EXAMPLE']
        MuhItem.get(item.id).tags.isEmpty()
    }

    void "elements added to both unidirectional hasMany associations are written to their own join tables"() {
        given:
        def item = new MuhItem(name: 'Example item').save(flush: true)

        when:
        item.addToRoles(new MuhRole(authority: 'ROLE_A'))
        item.addToTags(new MuhTag(name: 'tag-a'))
        item.save(flush: true)
        MuhItem.withSession { it.clear() }

        then:
        MuhItem.get(item.id).roles*.authority == ['ROLE_A']
        MuhItem.get(item.id).tags*.name == ['tag-a']
    }

    void "two unidirectional hasMany associations to the same class with their own join tables are each written"() {
        given: "a persisted account whose allow and deny roles map their own join tables"
        def account = new MuhAccount(name: 'Example account').save(flush: true)

        when: "a role is added to each association and the account is flushed"
        account.addToRoles(new MuhRole(authority: 'ROLE_ALLOW'))
        account.addToDenyRoles(new MuhRole(authority: 'ROLE_DENY'))
        account.save(flush: true)
        MuhAccount.withSession { it.clear() }

        then: "each role is found through its own association of the reloaded account"
        MuhAccount.get(account.id).roles*.authority == ['ROLE_ALLOW']
        MuhAccount.get(account.id).denyRoles*.authority == ['ROLE_DENY']
    }

    void "two unidirectional hasMany associations to the same class sharing a join table are each written"() {
        given: "a persisted group whose allow and deny roles share one join table, with a key column each"
        def group = new MuhGroup(name: 'Example group').save(flush: true)

        when: "a role is added to each association and the group is flushed"
        group.addToRoles(new MuhRole(authority: 'ROLE_ALLOW'))
        group.addToDenyRoles(new MuhRole(authority: 'ROLE_DENY'))
        group.save(flush: true)
        MuhGroup.withSession { it.clear() }

        then: "each role is found through its own association of the reloaded group"
        MuhGroup.get(group.id).roles*.authority == ['ROLE_ALLOW']
        MuhGroup.get(group.id).denyRoles*.authority == ['ROLE_DENY']
    }

    void "one role denied by two groups is written to the shared join table once per group"() {
        given: "a role and two persisted groups"
        def role = new MuhRole(authority: 'ROLE_SHARED').save(flush: true)
        def first = new MuhGroup(name: 'First group').save(flush: true)
        def second = new MuhGroup(name: 'Second group').save(flush: true)

        when: "both groups deny the role, which leaves the first association's key column null in both rows"
        first.addToDenyRoles(role)
        second.addToDenyRoles(role)
        first.save(flush: true)
        second.save(flush: true)
        MuhGroup.withSession { it.clear() }

        then: "each group finds the role, and the rows are unique by the key column of their own association"
        MuhGroup.get(first.id).denyRoles*.authority == ['ROLE_SHARED']
        MuhGroup.get(second.id).denyRoles*.authority == ['ROLE_SHARED']
        MuhGroup.get(first.id).roles.isEmpty()
    }

    void "a unidirectional hasMany next to a serializable to-many is written to its join table"() {
        given: "a persisted note whose tags are a serializable column, not a collection"
        def note = new MuhNote(name: 'Example note', tags: ['tag-a', 'tag-b'] as Set).save(flush: true)

        when: "a role is added to the managed note and the note is flushed"
        note.addToRoles(new MuhRole(authority: 'ROLE_NOTE'))
        note.save(flush: true)
        MuhNote.withSession { it.clear() }

        then: "the role is found through the reloaded note and the serializable tags are intact"
        MuhNote.get(note.id).roles*.authority == ['ROLE_NOTE']
        MuhNote.get(note.id).tags == ['tag-a', 'tag-b'] as Set
    }
}

@Entity
class MuhItem {
    Long id
    String name
    static hasMany = [roles: MuhRole, tags: MuhTag]
}

@Entity
class MuhRole {
    Long id
    String authority
}

@Entity
class MuhTag {
    Long id
    String name
}

@Entity
class MuhAccount {
    Long id
    String name
    static hasMany = [roles: MuhRole, denyRoles: MuhRole]
    static mapping = {
        roles joinTable: 'muh_account_allow_roles'
        denyRoles joinTable: 'muh_account_deny_roles'
    }
}

@Entity
class MuhNote {
    Long id
    String name
    Set<String> tags
    static hasMany = [roles: MuhRole, tags: String]
    static mapping = {
        tags type: 'serializable'
    }
}

@Entity
class MuhGroup {
    Long id
    String name
    static hasMany = [roles: MuhRole, denyRoles: MuhRole]
}
