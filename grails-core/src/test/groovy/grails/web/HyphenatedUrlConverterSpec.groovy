package grails.web

import spock.lang.Specification
import spock.lang.Unroll

class HyphenatedUrlConverterSpec extends Specification {

    @Unroll("converting #classOrActionName to url element #expectedUrlElement")
    def 'Test converting class and action names to url elements'() {
        given:
            def converter = new HyphenatedUrlConverter()

        expect:
            converter.toUrlElement(classOrActionName) == expectedUrlElement

        where:
            classOrActionName      | expectedUrlElement
            'Widget'               | 'widget'
            'widget'               | 'widget'
            'MyWidget'             | 'my-widget'
            'myWidget'             | 'my-widget'
            'A'                    | 'a'
            'a'                    | 'a'
            'MyMultiWordClassName' | 'my-multi-word-class-name'
            'myMultiWordClassName' | 'my-multi-word-class-name'
            'MyUrlHelper'          | 'my-url-helper'
            'myUrlHelper'          | 'my-url-helper'
            'MyURLHelper'          | 'my-u-r-l-helper'
            'myURLHelper'          | 'my-u-r-l-helper'
            'MYUrlHelper'          | 'm-y-url-helper'
            'myNamespace.v1'       | 'my-namespace.v1'
            'MyNamespace.v1'       | 'my-namespace.v1'
            'MyNamespace.V1'       | 'my-namespace.v1'
            ''                     | ''
            null                   | null
    }
}
