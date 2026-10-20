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
 * An owner with several unidirectional hasMany associations to different classes keeps a join table for each of
 * them, and an element added to one of them is written to its own join table.
 */
class MultipleUnidirectionalHasManySpec extends HibernateGormDatastoreSpec {

    void setupSpec() {
        manager.registerDomainClasses(MuhItem, MuhRole, MuhTag)
    }

    void cleanup() {
        MuhItem.withNewTransaction {
            MuhItem.executeUpdate('delete from MuhItem', [:])
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
