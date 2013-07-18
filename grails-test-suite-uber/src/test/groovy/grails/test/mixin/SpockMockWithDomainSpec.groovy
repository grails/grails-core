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

package grails.test.mixin

import spock.lang.Specification
import grails.persistence.Entity
import spock.lang.Issue

/**
 */
class SpockMockWithDomainSpec extends Specification{

    @Issue('GRAILS-10217')
    void 'Test that spock mocks can be used with domain classes'() {
        given:"A spock mock"
            User u = Mock()
        when:"A spock mock is used on a domain class"
            u.save(flush:true)

        then:"The mock worked"
            1 * u.save(_)
    }
}

