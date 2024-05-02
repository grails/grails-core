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
package grails.test.runtime

import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.mop.ConfineMetaClassChanges

@Stepwise
class DirtiesRuntimeSpec extends Specification {

    @Issue('GRAILS-11671')
    void 'test method 1'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @Issue('GRAILS-11671')
    void 'test method 2'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @ConfineMetaClassChanges([String])
    @Issue('GRAILS-11671')
    void 'test method 3'() {
        when:
        String.metaClass.someNewMethod = {}
        
        then:
        String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @Issue('GRAILS-11671')
    void 'test method 4'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
    
    @Issue('GRAILS-11671')
    void 'test method 5'() {
        expect:
        !String.metaClass.hasMetaMethod('someNewMethod')
    }
}
