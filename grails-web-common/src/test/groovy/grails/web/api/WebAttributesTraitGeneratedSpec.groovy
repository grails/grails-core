package grails.web.api

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class WebAttributesTraitGeneratedSpec extends Specification {

    void "test that all WebAttributes trait methods are marked as Generated"() {
        expect: "all WebAttributes methods are marked as Generated on implementation class"
        WebAttributes.getMethods().each { Method traitMethod ->
            assert TestWebAttributes.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestWebAttributes implements WebAttributes {

}
