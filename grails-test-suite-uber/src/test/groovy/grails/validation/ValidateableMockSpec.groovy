package grails.validation

import spock.lang.Issue
import spock.lang.Specification

class ValidateableMockSpec extends Specification {

    @Issue('grails/grails-core#9761')
    void 'ensure command is mocked properly'(){
        given:
        SomeCommand command = GroovyMock()
        1 * command.validate() >> true
        1 * command.validate() >> false
        1 * command.validate() >> true
        1 * command.validate(_) >> true
        1 * command.validate(_) >> false
        1 * command.validate(_, _) >> true

        expect:
        command.validate()
        !command.validate()
        command.validate()
        command.validate([])
        !command.validate([:])
        command.validate([], [:])
    }
}

class SomeCommand implements Validateable {}
