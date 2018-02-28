package grails.web

import spock.lang.Specification
import spock.lang.Unroll

class CamelCaseUrlConverterSpec extends Specification {

    @Unroll("converting #classOrActionName to url element #expectedUrlElement")
    def 'Test converting class and action names to url elements'() {
        given:
            def converter = new CamelCaseUrlConverter()

        expect:
            converter.toUrlElement(classOrActionName) == expectedUrlElement

        where:
            classOrActionName      | expectedUrlElement
            'Widget'               | 'widget'
            'widget'               | 'widget'
            'MyWidget'             | 'myWidget'
            'myWidget'             | 'myWidget'
            'A'                    | 'a'
            'a'                    | 'a'
            'MyMultiWordClassName' | 'myMultiWordClassName'
            'myMultiWordClassName' | 'myMultiWordClassName'
            'MyUrlHelper'          | 'myUrlHelper'
            'myUrlHelper'          | 'myUrlHelper'
            'MyURLHelper'          | 'myURLHelper'
            'myURLHelper'          | 'myURLHelper'
            'MYUrlHelper'          | 'MYUrlHelper'
            'myNamespace.v1'       | 'myNamespace.v1'
            'MyNamespace.v1'       | 'myNamespace.v1'
            'MyNamespace.V1'       | 'myNamespace.v1'
            ''                     | ''
            null                   | null
    }
}
