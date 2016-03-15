package grails.web

import grails.converters.JSON
import grails.core.GrailsApplication
import grails.test.mixin.TestMixin
import grails.test.mixin.web.ControllerUnitTestMixin
import spock.lang.Ignore
import spock.lang.Specification

@TestMixin(ControllerUnitTestMixin)
class JSONRenderSpec extends Specification {

    @Ignore
    void 'test groovy class with groovy parent'() {
        given:
        def json = new JSON(new SomeGroovyClass(grailsApplication: grailsApplication)).toString(true)

        expect:
        json.contains '"title": null'
    }
}

class SomeGroovyClass {
    String title
    GrailsApplication grailsApplication
}
