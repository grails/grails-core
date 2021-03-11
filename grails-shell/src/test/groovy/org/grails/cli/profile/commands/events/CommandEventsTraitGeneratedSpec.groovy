package org.grails.cli.profile.commands.events

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class CommandEventsTraitGeneratedSpec extends Specification {

    void "test that all CommandEvents trait methods are marked as Generated"() {
        expect: "all CommandEvents methods are marked as Generated on implementation class"
        CommandEvents.getMethods().each { Method traitMethod ->
            assert TestCommandEvents.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestCommandEvents implements CommandEvents {

}
