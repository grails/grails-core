package grails.web.api

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class ServletAttributesTraitGeneratedSpec extends Specification {

    void "test that all ServletAttributes trait methods are marked as Generated"() {
        expect: "all ServletAttributes methods are marked as Generated on implementation class"
        ServletAttributes.getMethods().each { Method traitMethod ->
            assert TestServletAttributes.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestServletAttributes implements ServletAttributes {

}
