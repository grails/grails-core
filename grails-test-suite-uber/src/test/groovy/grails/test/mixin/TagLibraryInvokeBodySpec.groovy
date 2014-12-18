package grails.test.mixin

import grails.artefact.Artefact
import grails.artefact.TagLibrary
import grails.gsp.TagLib
import spock.lang.Specification

@TestFor(SimpleTagLib)
class TagLibraryInvokeBodySpec extends Specification {

    void "Test that a tag can be invoked with a custom body"() {
        given:"A custom body"
            def body = { params ->
                "hello ${params.param}"
            }

        when:"A tag is invoked with the custom body"
            def result = tagLib.output([param: "test"], body)

        then:"The output is rendered correctly"
            result == "hello test"
    }
}

@TagLib
class SimpleTagLib implements TagLibrary {
    def output = { attrs, body ->
        def param = attrs.param
        out << body(param: param)
    }
}
