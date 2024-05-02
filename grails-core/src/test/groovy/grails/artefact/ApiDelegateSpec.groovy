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
package grails.artefact

import spock.lang.Specification

/**
 * Tests for {@link grails.artefact.ApiDelegate}
 */
class ApiDelegateSpec extends Specification{
    void "Test that delegate methods are added"() {
        when:
            def manager = getManager().newInstance(name:"Bob")

        then:
            manager.doWork("fix bug") == "done [fix bug] given by Bob"
    }

    void "Test that subclasses can have methods added for classes declared to use the parent class"() {
        when:
            def manager = getSubclassManager().newInstance(name:"Bob")

        then:
            manager.class.simpleName == "GreatManager"
            manager.doWork("fix bug") == "done [fix bug] given by Bob"
    }

    def getManager() {
        def gcl = new GroovyClassLoader()
        gcl.parseClass '''
import grails.artefact.ApiDelegate
class Manager {
    String name
    @ApiDelegate Worker worker = new Worker()
}

class Worker {
    def doWork(Manager manager, String task) {
        "done [${task}] given by ${manager.name}"
    }
}

'''
    }

    def getSubclassManager() {
        def gcl = new GroovyClassLoader()
        gcl.parseClass '''
import grails.artefact.ApiDelegate
class GreatManager extends Manager {
   @ApiDelegate Worker worker = new Worker()
}

class Manager {
    String name

}
class Worker {
    def doWork(Manager manager, String task) {
        "done [${task}] given by ${manager.name}"
    }
}

'''
    }
}

