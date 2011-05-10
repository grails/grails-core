package org.codehaus.groovy.grails.compiler.injection

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
}

