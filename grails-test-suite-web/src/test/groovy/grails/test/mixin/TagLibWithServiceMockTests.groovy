package grails.test.mixin

import grails.artefact.Artefact

import org.junit.*

@TestFor(TimeTagLib)
class TagLibWithServiceMockTests {

    @Test void canCallServiceMethod() {
        def control = mockFor(TimeService)
        control.demand.getCurrentTime(1) { -> "2011-12-02T11:19:00" }
        tagLib.timeService = control.createMock()

        def content = applyTemplate( "<g:time />" )

        assert content == "<time>2011-12-02T11:19:00</time>"
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
        new Date().format("yyyy-MM-dd'T'HH:mm:ss")
    }
}
