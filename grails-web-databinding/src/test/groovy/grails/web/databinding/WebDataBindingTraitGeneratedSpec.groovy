package grails.web.databinding

import groovy.transform.Generated
import spock.lang.Specification

import java.lang.reflect.Method

class WebDataBindingTraitGeneratedSpec extends Specification {

    void "test that all WebDataBinding trait methods are marked as Generated"() {
        expect: "all WebDataBinding methods are marked as Generated on implementation class"
        WebDataBinding.getMethods().each { Method traitMethod ->
            assert TestWebDataBinding.class.getMethod(traitMethod.name, traitMethod.parameterTypes).isAnnotationPresent(Generated)
        }
    }
}

class TestWebDataBinding implements WebDataBinding {

}
