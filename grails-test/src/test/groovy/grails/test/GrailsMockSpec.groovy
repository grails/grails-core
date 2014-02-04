package grails.test

import spock.lang.Issue
import spock.lang.Specification

class GrailsMockSpec extends Specification {

    @Issue('GRAILS-11075')
    void 'Test that verify() can be called in the then block of a Spock spec'() {
        given:
        def mock = new GrailsMock(String)
        mock.demand.trim(1..1) { ->
            'trimmed string'
        }
        def str = mock.createMock()
        
        when:
        def result = str.trim()
        
        then: 
        result == 'trimmed string'
        mock.verify()
    }
}
