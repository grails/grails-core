package grails.test.mixin

import grails.artefact.Artefact
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class TagLibWithServiceMockTests extends Specification implements TagLibUnitTest<TimeTagLib> {

    Closure doWithSpring() {{ ->
        timeService(TimeService)
    }}

    void canCallServiceMethod() {
        when:
        def content = applyTemplate( "<g:time />" )

        then:
        content == "<time>the time is now</time>"
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
