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

import org.grails.testing.GrailsUnitTest
import spock.lang.Specification

/**
 * @author Lari Hotari
 */
class StaticCallbacksSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring() {{ ->
        myService(MyService)
    }}
    
    Closure doWithConfig() {{ c ->
        c.myConfigValue = 'Hello'    
    }}
    
    def "grailsApplication is not null"() {
        expect:
        grailsApplication != null
    }
    
    def "doWithSpring callback is executed"() {
        expect:
        grailsApplication.mainContext.getBean('myService') != null
    }

    def "doWithConfig callback is executed"(){
        expect:
        config.myConfigValue == 'Hello'
    }
}
