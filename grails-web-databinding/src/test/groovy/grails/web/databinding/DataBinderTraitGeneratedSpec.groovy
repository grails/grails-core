package grails.web.databinding

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class DataBinderTraitGeneratedSpec extends Specification {

    void "test that all DataBinder trait methods are marked as Generated"() {
        expect: "all DataBinder methods are marked as Generated on implementation class"
        DataBinder.getMethods().each { Method traitMethod ->
            assert TestDataBinder.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestDataBinder implements DataBinder {

}
