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

class DomainClassWithDefaultConstraintsUnitTestMixinTests extends Specification implements DomainUnitTest<DomainWithDefaultConstraints> {

    Closure doWithConfig() {{ config ->
        config['grails.gorm.default.constraints'] = {
            '*'(nullable:true)
        }
    }}

    void testCreateDomainSingleLineWithConfigHavingNullableTrueForAllProperties() {
        expect:
        new DomainWithDefaultConstraints(name:"My test").save(flush:true) != null
    }

    void testCreateDomainAllPropertiesWithConfigHavingNullableTrueForAllProperties() {
        when:
        def d = new DomainWithDefaultConstraints(name:"My test",value: "My test value")

        then:
        new DomainWithDefaultConstraints(name:"My test",value: "My test value").save(flush:true) != null
    }
}

@Entity
class DomainWithDefaultConstraints {
    String name
    String value
}
