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

