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
package org.grails.web.binding

import grails.persistence.Entity
import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class BindToPropertyThatIsNotReadableTests extends Specification implements DomainUnitTest<PropertyNotReadableBook> {

    void testBindToPropertyThatIsNotReadable() {
        when:
        def b = new PropertyNotReadableBook()

        b.properties = [calculatedField:[1,2,3], title:"blah"]

        then:
        6 == b.sum()
    }
}

@Entity
class PropertyNotReadableBook {

    String title

    private List calculateField

    static transients = ['calculatedField']

    void setCalculatedField(List value) {
        this.calculateField = value
    }

    int sum() { calculateField.sum() }
}
