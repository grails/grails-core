/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import spock.lang.Issue

/**
 * @author Graeme Rocher
 */
class EntityWithEmbeddedInnerClassSpec extends GormSpec {

    @Issue('GRAILS-9627')
    void "Test that an entity with an embedded inner class works"() {
        when:"An entity with an embedded inner class is saved"
            def entity = new EntityWithEmbeddedInnerClassUser(nickname: "Bob")
            final lastVisitDate = new Date().parse('yyyy/MM/dd', '1973/07/09')
            entity.lastVisit.date = lastVisitDate
            entity.save(flush:true)
            session.clear()
            entity = EntityWithEmbeddedInnerClassUser.get(entity.id)
        then:"The entity can be retrieved correctly"
            entity != null
            entity.lastVisit.date == lastVisitDate

    }
    @Issue('GRAILS-9627')
    void "Test that an entity with an enum class works"() {
        when:"An entity with an enum is saved"
            def entity = new EntityWithEnumBook(title: "Grails in Action")
            entity.status = EntityWithEnumBook.Status.EAP
            entity.save(flush:true)
            session.clear()
            entity = EntityWithEnumBook.get(entity.id)
        then:"The entity can be retrieved correctly"
            entity != null
            entity.status ==EntityWithEnumBook.Status.EAP

    }
    @Override
    List getDomainClasses() {
        [EntityWithEmbeddedInnerClassUser, EntityWithEnumBook]
    }
}

@Entity
class EntityWithEmbeddedInnerClassUser {
    String nickname
    Date dateCreated
    Date lastUpdated

    LastVisit lastVisit = new LastVisit()

    static embedded = ['lastVisit']

    static class LastVisit {
        Date date
    }
}
@Entity
class EntityWithEnumBook {
    String title

    Status status = Status.PLANNED

    enum Status {
        PLANNED,
        EAP,
        PUBLISHED
    }
}
