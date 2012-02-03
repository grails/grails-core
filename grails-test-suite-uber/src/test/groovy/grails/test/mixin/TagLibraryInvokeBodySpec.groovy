package grails.test.mixin

import spock.lang.Specification
import grails.artefact.Artefact

/**
 */
@TestFor(SimpleTagLib)
class TagLibraryInvokeBodySpec extends Specification{


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

@Artefact("TagLibrary")
class SimpleTagLib {
    def output = { attrs, body ->
        def param = attrs.param
        out << body(param: param)
    }
}

