package grails.test.mixin

import grails.artefact.Artefact
import org.junit.Test

@TestFor(TimeTagLib)
@Mock(TimeService)
class TagLibWithServiceMockTests {

    @Test void canCallServiceMethod() {
        def content = applyTemplate( "<g:time />" )

        assert content == "<time>the time is now</time>"
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
