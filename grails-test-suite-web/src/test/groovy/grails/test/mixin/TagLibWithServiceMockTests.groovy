package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class TagLibWithServiceMockTests extends Specification implements TagLibUnitTest<TimeTagLib> {

    static doWithSpring = {
        timeService TimeService
    }

    void 'test calling service method'() {
        expect:
        applyTemplate( "<g:time />" ) == "<time>the time is now</time>"
    }
}

@Artefact("TagLib")
class TimeTagLib {

    TimeTagLib() {
    }
    def timeService

    Closure time = {  attrs, body ->
        out << "<time>" << timeService.currentTime << "</time>"
    }
}

@Artefact("Service")
class TimeService {

    String getCurrentTime() {
        'the time is now'
    }
}
