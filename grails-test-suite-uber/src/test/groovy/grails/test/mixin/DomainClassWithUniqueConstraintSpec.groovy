/*
 * Copyright 2024 original authors
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
package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

/**
 * Tests the usage of unique contstraint in unit tests
 */
class DomainClassWithUniqueConstraintSpec extends Specification implements DomainUnitTest<Group> {

    void "Test that unique constraint is enforced"() {
        given:"An existing persisted instance"
            new Group(name:"foo").save(flush:true)

        when:"We try to persist another instance"
            def g = new Group(name:"foo")
            g.save()

        then:"a validation error occurs"
            g.hasErrors()
            Group.count() == 1
    }
}

@Entity
class Group {
    String name
    static constraints = {
        name unique:true
    }
}
