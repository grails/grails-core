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
package grails.test.mixin.support

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

class GrailsUnitTestMixinGrailsApplicationAwareSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring(){{ ->
        someBean SomeBean
    }}
    
    void 'test that the GrailsApplicationAware post processor is effective for beans registered by a unit test'() {
        when: 'when a test registers a bean which implements GrailsApplicationAware'
        def someBean = applicationContext.someBean
        
        then: 'the grailsApplication property is properly initialized'
        someBean.grailsApplication
    }
}

class SomeBean implements GrailsApplicationAware {
    GrailsApplication grailsApplication
}